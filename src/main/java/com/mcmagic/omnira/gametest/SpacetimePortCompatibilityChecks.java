package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Kept outside annotated test holders so optional Create types load only on demand. */
final class SpacetimePortCompatibilityChecks {
    private static SpacetimePortState port(GameTestHelper h,BlockPos support,Direction face){
        var pos=support.relative(face);
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,face));
        return SpacetimePortState.at(h.getLevel(),pos);
    }
    static void verify(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,3,2));var b=h.absolutePos(new BlockPos(8,3,8));
        l.setBlockAndUpdate(a,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        var engine=((ManaEngineAccess)l.getBlockEntity(a)).engineState();engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        var shaft=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:shaft"));
        l.setBlockAndUpdate(b,shaft.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.Y));
        var white=port(h,a,Direction.EAST);var black=port(h,b,Direction.DOWN);
        h.assertTrue(SpacetimePortState.pair(white,black),"Kinetic pair failed");
        h.runAfterDelay(12,()->{
            var source=(KineticBlockEntity)l.getBlockEntity(a);var target=(KineticBlockEntity)l.getBlockEntity(b);
            h.assertTrue(Math.abs(target.getSpeed())==16&&source.network.equals(target.network),"Perpendicular shafts did not share speed/stress network");
            black.unlink();
        });
        h.runAfterDelay(25,()->{
            var target=(KineticBlockEntity)l.getBlockEntity(b);
            h.assertTrue(target.getSpeed()==0,"Unpaired port retained remote rotation");
            h.assertTrue(SpacetimePortState.pair(white,black),"Re-pair failed");
        });
        h.runAfterDelay(37,()->{
            h.assertTrue(Math.abs(((KineticBlockEntity)l.getBlockEntity(b)).getSpeed())==16,"Re-pair did not restore rotation");
            l.setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());
        });
        h.runAfterDelay(50,()->{
            h.assertTrue(((KineticBlockEntity)l.getBlockEntity(b)).getSpeed()==0,"Non-shaft face drove a floating shaft");h.succeed();
        });
    }
}
