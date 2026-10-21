package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.space.*;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import java.util.*;

/** Native Sable fixtures; no optional types are exposed by the discovery holder. */
final class SableMachineChecks {
    static void run(GameTestHelper h,String kind){
        var level=h.getLevel();var base=h.absolutePos(new BlockPos(5,3,5));
        var states=new LinkedHashMap<BlockPos,BlockState>();
        boolean ports=kind.equals("ports")||kind.endsWith("_ports")||kind.equals("one_port");
        var aPort=base.above();var bPort=base.east(6).above();
        if(ports){
            for(int i=0;i<=6;i++)states.put(base.east(i),Blocks.STONE.defaultBlockState());
            states.put(base,Blocks.CHEST.defaultBlockState());states.put(base.east(6),Blocks.CHEST.defaultBlockState());
            states.put(base.above(),ModBlocks.SPACETIME_PORT.get().defaultBlockState());
            states.put(base.east(6).above(),ModBlocks.SPACETIME_PORT.get().defaultBlockState());
            if(kind.equals("fluid_ports")){
                states.put(base,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
                states.put(base.east(6),ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
            }else if(kind.equals("kinetic_ports")){
                states.remove(aPort);aPort=base.east();
                states.put(base,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
                states.put(aPort,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,Direction.EAST));
                var shaft=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("create:shaft"));
                states.put(base.east(6),shaft.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS,Direction.Axis.Y));
            }
            for(var direction:Direction.values())h.assertTrue(dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper.getMass(
                    level,aPort,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,direction))==0,"Spacetime port has mass on "+direction);
        }else{
            var block=switch(kind){
                case "aggregation"->com.mcmagic.omnira.aggregation.AggregationContent.BLOCK.get();
                case "pool"->ModBlocks.REACTION_POOL.get();
                case "catalysis"->com.mcmagic.omnira.alchemy.catalysis.CatalysisContent.BLOCK.get();
                case "engine"->com.mcmagic.omnira.engine.SpacetimeEngineContent.BLOCK.get();
                case "framework"->com.mcmagic.omnira.myriad.MiniatureWorldContent.BLOCK.get();
                case "drill"->com.mcmagic.omnira.archaeology.ArchaeologyContent.DRILL.get();
                default->throw new IllegalArgumentException(kind);
            };
            int count=switch(kind){case "aggregation"->9;case "catalysis"->3;case "engine"->10;case "framework"->125;default->18;};
            for(int i=0;i<count;i++){
                var state=block.defaultBlockState();BlockPos offset;
                switch(kind){
                    case "aggregation"->{state=state.setValue(com.mcmagic.omnira.aggregation.AggregationRingBlock.PART,i);offset=com.mcmagic.omnira.aggregation.AggregationLayout.part(BlockPos.ZERO,Direction.NORTH,i);}
                    case "pool"->{state=state.setValue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.PART,i);offset=com.mcmagic.omnira.alchemy.ReactionPoolBlock.offset(Direction.NORTH,i);}
                    case "catalysis"->{state=state.setValue(com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.PART,i);offset=new BlockPos(0,i,0);}
                    case "engine"->{state=state.setValue(com.mcmagic.omnira.engine.SpacetimeEngineBlock.PART,i);offset=com.mcmagic.omnira.engine.SpacetimeEngineBlock.offset(i);}
                    case "framework"->{state=state.setValue(com.mcmagic.omnira.myriad.MiniatureWorldBlock.COMPACT,true).setValue(com.mcmagic.omnira.myriad.MiniatureWorldBlock.PART,i);offset=com.mcmagic.omnira.myriad.MiniatureWorldBlock.offset(i);}
                    default->{state=state.setValue(com.mcmagic.omnira.archaeology.HyperDrillBlock.PART,i);offset=com.mcmagic.omnira.archaeology.HyperDrillBlock.offset(i);}
                }
                states.put(base.offset(offset),state);
            }
        }
        states.forEach((pos,state)->level.setBlock(pos,state,18));
        var data=new HashSet<BlockPos>();var inventories=new HashSet<BlockPos>();
        for(var pos:states.keySet()){
            var be=level.getBlockEntity(pos);if(be==null)continue;
            be.getPersistentData().putString("OmniraPhysicsCheck",kind);data.add(pos);
            if(be instanceof Container storage&&!(be instanceof com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)&&storage.getContainerSize()>0){storage.setItem(0,new ItemStack(Items.DIAMOND,1));inventories.add(pos);}
            if(be instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess engine){engine.engineState().inventory.setItem(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.TEST_SPELL_CORE.get()));}
            if(be instanceof com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity frame)
                h.assertTrue(frame.insert(new ItemStack(Items.DIAMOND,4))==4,"Cannot populate world material stack");
            if(be instanceof com.mcmagic.omnira.engine.SpacetimeEngineController engine)
                engine.grids.setStackInSlot(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.ARCANE_CRYSTAL_GRID.get()));
        }
        var positions=new HashSet<>(states.keySet());
        if(ports){
            var a=SpacetimePortState.at(level,aPort);var b=SpacetimePortState.at(level,bPort);
            h.assertTrue(SpacetimePortState.pair(a,b),"Cannot pair physics fixture ports");
            if(kind.equals("fluid_ports"))((com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(base.east(6))).tank.setFluid(
                    new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000));
            if(kind.equals("one_port"))positions=new HashSet<>(Set.of(base,base.above()));
        }
        var selected=positions;
        h.assertTrue(com.mcmagic.omnira.compat.ForgeMovement.completeSelection(level,selected),"Complete machine selection rejected: "+kind);
        if(!ports){var partial=new HashSet<>(selected);partial.remove(selected.iterator().next());
            h.assertTrue(!com.mcmagic.omnira.compat.ForgeMovement.completeSelection(level,partial),"Partial machine selection accepted: "+kind);}
        var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(base,level,512,(a,b,c,d,e)->selected.contains(c));
        h.assertTrue(gathered.assemblyState()==SubLevelAssemblyHelper.GatherResult.State.SUCCESS,"Cannot gather machine: "+kind);
        h.assertTrue(gathered.blocks().containsAll(selected),"Gathering lost machine parts: "+kind);
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,base,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Cannot assemble physics machine: "+kind);
        var delta=ship.getPlot().getCenterBlock().subtract(base);
        var retained=new SableTestBody(ship);
        var firstPort=aPort;var secondPort=bPort;
        h.runAfterDelay(kind.equals("kinetic_ports")||kind.equals("fluid_ports")?40:8,()->{
            try(retained){
                h.assertTrue(!ship.isRemoved(),"Retained machine unloaded: "+kind);
                var mass=ship.getMassTracker();
                h.assertTrue(Double.isFinite(mass.getMass())&&mass.getMass()>0&&mass.getCenterOfMass()!=null&&mass.getCenterOfMass().isFinite(),"Invalid physical mass or center: "+kind);
                if(!ports){
                    h.assertTrue(ship.getSelfMassTracker().getMass()==0,"Crystal machine blocks acquired weight: "+kind);
                    h.assertTrue(mass.getMass()==com.mcmagic.omnira.compat.SableZeroMass.MASS,"Missing numerical mass floor: "+kind);
                }else h.assertTrue(!(mass instanceof com.mcmagic.omnira.compat.SableZeroMass),"Normal mixed ship mass was replaced");
                for(var pos:selected){
                    var moved=pos.offset(delta);h.assertTrue(level.getBlockState(moved).equals(states.get(pos)),"Moved part changed: "+kind+" "+pos);
                    h.assertTrue(level.getBlockState(pos).isAir(),"Source part survived assembly: "+kind);
                    if(data.contains(pos)){var be=level.getBlockEntity(moved);h.assertTrue(be!=null&&be.getPersistentData().getString("OmniraPhysicsCheck").equals(kind),"Block entity data lost: "+kind);}
                    if(inventories.contains(pos))h.assertTrue(((Container)level.getBlockEntity(moved)).getItem(0).is(Items.DIAMOND),"Inventory lost: "+kind);
                    if(level.getBlockEntity(moved) instanceof com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity frame)
                        h.assertTrue(frame.material(0).is(Items.DIAMOND)&&frame.material(0).getCount()==4,"World material stack lost during assembly");
                    if(level.getBlockEntity(moved) instanceof com.mcmagic.omnira.engine.SpacetimeEngineController engine)
                        h.assertTrue(engine.grids.getStackInSlot(0).is(com.mcmagic.omnira.registry.ModItems.ARCANE_CRYSTAL_GRID.get()),"Engine grid lost during assembly");
                }
                if(ports){
                    var a=SpacetimePortState.at(level,firstPort.offset(delta));
                    if(kind.equals("one_port"))h.assertTrue(a.other()==null,"One moved port must not link to stationary structure");
                    else{
                        var b=SpacetimePortState.at(level,secondPort.offset(delta));
                        h.assertTrue(a.other()==b&&b.other()==a,"Port pair lost inside physics structure");
                        if(kind.equals("fluid_ports")){
                            var handler=level.getCapability(Capabilities.FluidHandler.BLOCK,a.host.getBlockPos(),Direction.DOWN);
                            h.assertTrue(handler!=null&&handler.drain(100,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount()==100,"Physical port cannot access remote liquid");
                            h.assertTrue(handler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,100),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==100,"Physical port cannot fill remote liquid");
                        }else if(kind.equals("kinetic_ports")){
                            var shaft=(com.simibubi.create.content.kinetics.base.KineticBlockEntity)level.getBlockEntity(base.east(6).offset(delta));
                            h.assertTrue(Math.abs(shaft.getSpeed())==16,"Physical port did not drive perpendicular shaft");
                        }else{
                            var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,a.host.getBlockPos(),Direction.DOWN);
                            h.assertTrue(handler!=null&&handler.extractItem(0,1,false).is(Items.DIAMOND),"Physical port cannot access remote inventory");
                        }
                    }
                }
            }h.succeed();
        });
    }
}
