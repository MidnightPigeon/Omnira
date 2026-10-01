package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_aggregation_process")
@PrefixGameTestTemplate(false)
public final class AggregationProcessGameTests {
    @GameTest(template="spell_arena") public static void completionAndPersistence(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,pos);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);h.getLevel().addFreshEntity(player);
        try{
            AggregationStructure.assemble(h.getLevel(),pos,Direction.NORTH);
            var machine=(AggregationRingBlockEntity)h.getLevel().getBlockEntity(pos);
            machine.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get(),2));
            machine.setItem(8,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2));
            machine.setItem(9,new ItemStack(ModItems.SPELL_INK.get()));machine.setItem(10,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
            h.assertTrue(machine.start(player),"Cannot start synthesis");
            for(int i=0;i<80;i++)machine.tick();
            var saved=machine.saveWithFullMetadata(h.getLevel().registryAccess());machine.loadWithComponents(saved,h.getLevel().registryAccess());
            h.assertTrue(machine.progress()==81,"Progress lost across save");
            for(int i=80;i<159;i++)machine.tick();
            h.assertTrue(machine.getItem(11).isEmpty(),"Synthesis completed before eight seconds");
            machine.tick();
            h.assertTrue(machine.getItem(11).is(AggregationContent.CRYSTAL.get()),"Output missing");
            h.assertTrue(machine.getItem(0).getCount()==1&&machine.getItem(8).getCount()==1,"Ingredient count incorrect");
            h.assertTrue(machine.powered()&&machine.getItem(9).getDamageValue()==1,"Core consumed or ink durability incorrect");
            h.assertTrue(machine.getItem(11).get(ModDataComponents.SPELL_PAYLOAD).baseCost()==10,"Crafting surcharge leaked into spell cost");
            h.assertTrue(machine.collect(player)&&!machine.collect(player),"Output duplicated");
        }finally{player.discard();}
        h.succeed();
    }
}
