package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class SpacetimeEngineCreateChecks {
    static void remote(GameTestHelper h,SpacetimeEngineController e){
        var l=h.getLevel();var from=e.getBlockPos().below().east();var target=h.absolutePos(new net.minecraft.core.BlockPos(10,5,10));
        e.configure(1,2048,64,false);
        l.setBlockAndUpdate(target,com.simibubi.create.AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,net.minecraft.core.Direction.Axis.Y));
        var a=port(h,from,net.minecraft.core.Direction.EAST);var b=port(h,target,net.minecraft.core.Direction.DOWN);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(a,b),"Remote pair failed");
        h.runAfterDelay(25,()->{
            var shaft=(KineticBlockEntity)l.getBlockEntity(target);
            h.assertTrue(Math.abs(shaft.getSpeed())==64,"Perpendicular remote output failed");
            h.assertTrue(Math.abs(shaft.getOrCreateNetwork().calculateCapacity()-2048)<.01,"Remote capacity changed");
            b.unlink();h.runAfterDelay(20,()->{h.assertTrue(shaft.getSpeed()==0,"Unlinked shaft still rotating");h.succeed();});
        });
    }
    private static com.mcmagic.omnira.space.SpacetimePortState port(GameTestHelper h,net.minecraft.core.BlockPos source,net.minecraft.core.Direction face){
        var at=source.relative(face);h.getLevel().setBlockAndUpdate(at,com.mcmagic.omnira.registry.ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,face));
        return com.mcmagic.omnira.space.SpacetimePortState.at(h.getLevel(),at);
    }
    static void run(GameTestHelper h,SpacetimeEngineController e){
        var root=e.getBlockPos().below();
        for(int i=0;i<4;i++){
            var d=SpacetimeEngineBlock.OUTPUTS[i];e.configure(i,2048,16<<i,i%2==1);
            h.getLevel().setBlockAndUpdate(root.relative(d,2),com.simibubi.create.AllBlocks.SHAFT.getDefaultState().setValue(BlockStateProperties.AXIS,d.getAxis()));
            var s=h.getLevel().getBlockState(root.relative(d));var block=(IRotate)s.getBlock();
            for(var face:net.minecraft.core.Direction.values())h.assertTrue(block.hasShaftTowards(h.getLevel(),root.relative(d),s,face)==(face==d),"Extra shaft exposed");
        }
        h.runAfterDelay(25,()->{
            check(h,e);
            for(int i=0;i<4;i++)e.configure(i,2048,128>>i,i%2==0);
            h.runAfterDelay(25,()->{check(h,e);e.grids.setStackInSlot(0,new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.registry.ModItems.OMNI_CRYSTAL_GRID.get()));
                e.configure(0,8192,128,false);
                h.runAfterDelay(25,()->{check(h,e);e.grids.setStackInSlot(0,net.minecraft.world.item.ItemStack.EMPTY);
                    h.runAfterDelay(25,()->{check(h,e);h.succeed();});});});
        });
    }
    private static void check(GameTestHelper h,SpacetimeEngineController e){
        var root=e.getBlockPos().below();var networks=new java.util.HashSet<Long>();
        for(int i=0;i<4;i++){
            var d=SpacetimeEngineBlock.OUTPUTS[i];var output=(SpacetimeEngineKinetics.Output)h.getLevel().getBlockEntity(root.relative(d));
            var shaft=(KineticBlockEntity)h.getLevel().getBlockEntity(root.relative(d,2));
            h.assertTrue(shaft.getTheoreticalSpeed()==e.generatedSpeed(i),"Wrong speed for "+d+": "+shaft.getTheoreticalSpeed());
            float total=output.calculateAddedStressCapacity()*Math.abs(output.getTheoreticalSpeed());
            h.assertTrue(total<=e.capacity(i)&&e.capacity(i)-total<.01,"RPM multiplied capacity: "+total);
            h.assertTrue(Math.abs(output.getOrCreateNetwork().calculateCapacity()-e.capacity(i))<.01,"Network capacity stale");
            h.assertTrue(networks.add(output.getOrCreateNetwork().id),"Outputs linked internally");
        }
    }
    private SpacetimeEngineCreateChecks(){}
}
