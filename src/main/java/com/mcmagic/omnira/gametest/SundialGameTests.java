package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.time.DoomsdayRitePlan;
import com.mcmagic.omnira.time.SundialBlockEntity;
import com.mcmagic.omnira.time.SundialContent;
import com.mcmagic.omnira.time.SundialTimeline;
import com.mcmagic.omnira.item.MemoryCubeBlockItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_sundial")
@PrefixGameTestTemplate(false)
public final class SundialGameTests {
    @GameTest(template="spell_arena") public static void layout(GameTestHelper h) {
        int balls=0,dial=0;
        for(var cell:DoomsdayRitePlan.cells()) {
            h.assertTrue(BuiltInRegistries.BLOCK.containsKey(cell.block()),"Unregistered plan block: "+cell.block());
            if(cell.block().equals(BuiltInRegistries.BLOCK.getKey(SundialContent.BLOCK.get())))dial++;
            if(cell.block().getPath().equals("crystal_ball")||cell.block().getPath().equals("liquid_crystal_ball"))balls++;
        }
        h.assertTrue(balls==2&&dial==1,"The rite needs two crystal balls and one sundial");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void conversion(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,1,4));
        level.setBlockAndUpdate(pos,SundialContent.BLOCK.get().defaultBlockState());
        var machine=(SundialBlockEntity)level.getBlockEntity(pos);
        var input=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:spiritual_crystal"));
        var output=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:spatial_crystal_shard"));
        h.assertTrue(SundialTimeline.choices(new ItemStack(input)).stream().anyMatch(stack->stack.is(output)),"Crystal systems are not in one material group");
        var named=new ItemStack(input);named.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("marked"));
        h.assertTrue(SundialTimeline.choices(named).isEmpty(),"Data-carrying stacks must not be converted");
        h.assertTrue(SundialTimeline.choices(new ItemStack(Items.DIAMOND)).isEmpty(),"Unlisted input was accepted");
        machine.setItem(0,new ItemStack(input));
        int selected=0;
        for(var choice:SundialTimeline.choices(machine.getItem(0))) {
            if(choice.is(output))break;
            selected++;
        }
        for(int i=0;i<selected;i++)machine.cycle(1);
        machine.setItem(2,new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:time_microcore")),4));
        for(int i=0;i<4;i++)machine.tick();
        h.assertTrue(machine.energy()==32,"Fuel did not produce 32 energy");
        h.assertTrue(machine.progress()==1,"Conversion should start automatically");
        for(int i=0;i<78;i++)machine.tick();
        h.assertTrue(machine.energy()==32,"Energy drained before four seconds");
        machine.tick();
        h.assertTrue(machine.energy()==31,"One energy must drain every four seconds");
        for(int i=80;i<SundialBlockEntity.DURATION;i++)machine.tick();
        h.assertTrue(machine.getItem(0).isEmpty()&&machine.getItem(1).is(output)&&machine.energy()==0,
                "Conversion did not consume one input and 32 energy");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void statefulAndCuratedChoices(GameTestHelper h) {
        var peaceful=MemoryCubeBlockItem.withState(new ItemStack(ModItems.MEMORY_CUBE.get()),true);
        var corrupted=MemoryCubeBlockItem.withState(new ItemStack(ModItems.MEMORY_CUBE.get()),false);
        h.assertTrue(SundialTimeline.choices(peaceful).stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,corrupted)),
                "Peaceful memory cube must convert to corrupted state");
        h.assertTrue(SundialTimeline.choices(corrupted).stream().anyMatch(s->ItemStack.isSameItemSameComponents(s,peaceful)),
                "Corrupted memory cube must convert to peaceful state");
        h.assertTrue(SundialTimeline.choices(new ItemStack(Items.SKELETON_SKULL)).stream().anyMatch(s->s.is(Items.ZOMBIE_HEAD)),
                "Vanilla skull group missing");
        h.assertTrue(SundialTimeline.choices(new ItemStack(Items.IRON_INGOT)).stream().anyMatch(s->s.is(Items.GOLD_INGOT)),
                "Common ingots must share a group");
        h.succeed();
    }
}
