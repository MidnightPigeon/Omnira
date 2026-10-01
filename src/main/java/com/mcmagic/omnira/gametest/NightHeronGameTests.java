package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.NightHeronStatueBlock;
import com.mcmagic.omnira.block.entity.NightHeronStatueBlockEntity;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_night_heron")
@PrefixGameTestTemplate(false)
public final class NightHeronGameTests {
    private static BlockPos place(GameTestHelper h,Direction direction) {
        var p=h.absolutePos(new BlockPos(6,2,6));var block=(NightHeronStatueBlock)ModBlocks.NIGHT_HERON_STATUE.get();
        var state=block.defaultBlockState().setValue(NightHeronStatueBlock.FACING,direction);
        h.getLevel().setBlockAndUpdate(p,state);block.setPlacedBy(h.getLevel(),p,state,null,new ItemStack(block));return p;
    }
    @GameTest(template="spell_arena") public static void redstoneEdgesAndRightSideContainer(GameTestHelper h) {
        for(var facing:Direction.Plane.HORIZONTAL) {
            var p=place(h,facing);var right=facing.getCounterClockWise();BlockPos output=p.relative(right),power=p.relative(right.getOpposite());
            double yaw=Math.toRadians(NightHeronStatueBlock.modelYaw(facing));
            h.assertTrue(Math.abs(Math.cos(yaw)-right.getStepX())<.001 && Math.abs(-Math.sin(yaw)-right.getStepZ())<.001,"Tail does not point toward the viewer's right/output");
            h.getLevel().setBlockAndUpdate(output,Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(output);
            h.getLevel().setBlockAndUpdate(power,Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(!h.getLevel().getBlockState(p).getValue(NightHeronStatueBlock.STANDING),"Rising edge did not change posture");
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==0,"Posture change must not produce milk");
            for(int i=0;i<10;i++)NightHeronStatueBlock.signal(h.getLevel(),p);
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==0,"Continuous power produced milk");
            h.getLevel().removeBlock(power,false);h.getLevel().setBlockAndUpdate(power,Blocks.REDSTONE_BLOCK.defaultBlockState());
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==0 && h.getLevel().getBlockState(p).getValue(NightHeronStatueBlock.STANDING),"Second edge failed");
            var statue=(NightHeronStatueBlockEntity)h.getLevel().getBlockEntity(p);
            var tag=statue.saveWithFullMetadata(h.getLevel().registryAccess());
            tag.putBoolean("MilkPending",true);tag.putLong("StretchStart",h.getLevel().getGameTime()-59);
            tag.putLong("NextStretch",h.getLevel().getGameTime()+500);
            statue.loadWithComponents(tag,h.getLevel().registryAccess());statue.tick();
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==0,"Milk arrived before neck retracted");
            tag.putLong("StretchStart",h.getLevel().getGameTime()-60);
            statue.loadWithComponents(tag,h.getLevel().registryAccess());statue.tick();statue.tick();
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==1,"Retraction must produce exactly one milk");
            var completed=statue.saveWithFullMetadata(h.getLevel().registryAccess());
            statue.loadWithComponents(completed,h.getLevel().registryAccess());statue.tick();
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==1,"Reload duplicated completed output");
            statue.toggleAutoMilk();
            var disabled=statue.saveWithFullMetadata(h.getLevel().registryAccess());
            disabled.putBoolean("MilkPending",true);
            statue.loadWithComponents(disabled,h.getLevel().registryAccess());
            h.assertTrue(!statue.autoMilk(),"Disabled output did not persist");
            statue.tick();statue.toggleAutoMilk();statue.tick();
            h.assertTrue(chest.countItem(ModItems.MILK_SUSPENSION.get())==1,"Disabled stretch output was queued or emitted");
            chest.clearContent();h.getLevel().removeBlock(power,false);h.getLevel().removeBlock(output,false);h.getLevel().removeBlock(p,false);
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void fullContainerDropsInsteadOfDeletingMilk(GameTestHelper h) {
        var p=place(h,Direction.NORTH);h.getLevel().setBlockAndUpdate(p.west(),Blocks.CHEST.defaultBlockState());var chest=(ChestBlockEntity)h.getLevel().getBlockEntity(p.west());
        for(int slot=0;slot<chest.getContainerSize();slot++)chest.setItem(slot,new ItemStack(Items.STONE,64));
        ((NightHeronStatueBlockEntity)h.getLevel().getBlockEntity(p)).ejectMilk();
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.MILK_SUSPENSION.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Full container lost or duplicated milk");h.succeed();
    }
    @GameTest(template="spell_arena") public static void colorPersistenceAndNeckTiming(GameTestHelper h) {
        var p=place(h,Direction.NORTH);var statue=(NightHeronStatueBlockEntity)h.getLevel().getBlockEntity(p);
        h.assertTrue(statue.color()==0,"Adult color must be default");statue.cycleColor();h.assertTrue(statue.color()==1,"Immature color missing");statue.cycleColor();
        var registries=h.getLevel().registryAccess();var tag=statue.saveWithFullMetadata(registries);tag.putLong("StretchStart",h.getLevel().getGameTime()-12);
        var restored=new NightHeronStatueBlockEntity(p,statue.getBlockState());restored.setLevel(h.getLevel());restored.loadWithComponents(tag,registries);
        h.assertTrue(restored.color()==2 && restored.stretch(0)==1,"Crystal color or stretch not saved");
        tag.putLong("StretchStart",h.getLevel().getGameTime()-65);restored.loadWithComponents(tag,registries);
        h.assertTrue(restored.stretch(0)==0,"Neck did not retract");restored.cycleColor();h.assertTrue(restored.color()==0,"Palette does not cycle");h.succeed();
    }
    @GameTest(template="spell_arena") public static void upperHalfBreakDropsOneStatue(GameTestHelper h) {
        var p=place(h,Direction.NORTH);
        h.assertTrue(h.getLevel().getBlockState(p.above()).getValue(NightHeronStatueBlock.HALF)==DoubleBlockHalf.UPPER,"Upper half missing");
        h.getLevel().destroyBlock(p.above(),true);
        h.assertTrue(h.getLevel().getBlockState(p).isAir() && h.getLevel().getBlockState(p.above()).isAir(),"Orphaned statue half");
        var drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2));
        h.assertTrue(drops.stream().filter(e->e.getItem().is(ModItems.NIGHT_HERON_STATUE.get())).mapToInt(e->e.getItem().getCount()).sum()==1,"Upper break did not drop exactly one statue");h.succeed();
    }
    @GameTest(template="spell_arena") public static void workshopLootHasIndependentStatueChance(GameTestHelper h) {
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","chests/crystal_workshop")));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel()).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,h.absoluteVec(Vec3.ZERO)).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int statues=0;
        // Adjacent legacy RNG seeds have correlated first rolls. Sample independent seeds.
        var seeds=new java.util.Random(91L);
        for(int i=0;i<1000;i++)for(var item:table.getRandomItems(params,seeds.nextLong()))if(item.is(ModItems.NIGHT_HERON_STATUE.get()))statues+=item.getCount();
        h.assertTrue(statues>25 && statues<80,"Statue chance is not near 5%: "+statues);h.succeed();
    }
}
