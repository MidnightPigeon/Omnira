package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_ports") @PrefixGameTestTemplate(false)
public final class SpacetimePortGameTests {
    private static SpacetimePortState port(GameTestHelper h,BlockPos support,Direction face){
        var pos=support.relative(face);h.getLevel().setBlockAndUpdate(pos,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(SpacetimePortBlock.FACING,face));
        var result=SpacetimePortState.at(h.getLevel(),pos);h.assertTrue(result!=null,"Missing port host");return result;
    }
    @GameTest(template="spell_arena") public static void itemOrbsShareRemoteFaceWithoutAutomaticTransfer(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,3,2));var b=h.absolutePos(new BlockPos(8,3,8));
        l.setBlockAndUpdate(a,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());l.setBlockAndUpdate(b,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var one=(CrystalBallBlockEntity)l.getBlockEntity(a);var two=(CrystalBallBlockEntity)l.getBlockEntity(b);
        one.setItem(0,new ItemStack(Items.DIAMOND,4));two.setItem(0,new ItemStack(Items.EMERALD,3));
        var white=port(h,a,Direction.EAST);var black=port(h,b,Direction.UP);
        h.assertTrue(SpacetimePortState.pair(white,black),"Cross-face pair failed");white.tick();black.tick();
        h.assertTrue(one.getItem(0).getCount()==4&&two.getItem(0).getCount()==3,"Port moved items without automation");
        var handler=l.getCapability(Capabilities.ItemHandler.BLOCK,white.host.getBlockPos(),Direction.WEST);
        h.assertTrue(handler!=null&&handler.extractItem(0,1,true).is(Items.EMERALD)&&two.getItem(0).getCount()==3,"Remote face or simulation incorrect");
        h.assertTrue(handler.extractItem(0,1,false).is(Items.EMERALD)&&two.getItem(0).getCount()==2,"Remote extraction failed");
        var output=new ItemStack(ModItems.OUTPUT_UPGRADE.get());var settings=new net.minecraft.nbt.CompoundTag();settings.putInt("Directions",1<<1);output.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(settings));
        two.upgrades.setStackInSlot(0,output);OrbAutomation.tick(two);
        h.assertTrue(one.getItem(1).is(Items.EMERALD)&&one.getItem(1).getCount()==1&&two.getItem(0).getCount()==1,"Output upgrade did not use remote face");
        h.assertTrue(l.getCapability(Capabilities.ItemHandler.BLOCK,white.host.getBlockPos(),Direction.UP)==null,"Unattached face exposed remote storage");
        black.unlink();h.assertTrue(white.peer==null&&black.peer==null&&handler.extractItem(0,1,false).isEmpty(),"Cached handler survived unlink");h.succeed();
    }
    @GameTest(template="spell_arena") public static void liquidOrbsUseDifferentFacesAndIntake(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,3,2));var b=h.absolutePos(new BlockPos(8,3,8));
        l.setBlockAndUpdate(a,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());l.setBlockAndUpdate(b,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var one=(LiquidCrystalBallBlockEntity)l.getBlockEntity(a);var two=(LiquidCrystalBallBlockEntity)l.getBlockEntity(b);
        two.tank.fill(new FluidStack(Fluids.LAVA,1000),FluidAction.EXECUTE);
        var white=port(h,a,Direction.UP);var black=port(h,b,Direction.WEST);h.assertTrue(SpacetimePortState.pair(white,black),"Liquid pair failed");
        white.tick();h.assertTrue(one.tank.isEmpty()&&two.tank.getFluidAmount()==1000,"Port moved liquid automatically");
        h.runAfterDelay(20,()->{
        var handler=l.getCapability(Capabilities.FluidHandler.BLOCK,white.host.getBlockPos(),Direction.DOWN);
        h.assertTrue(handler!=null&&handler.drain(250,FluidAction.SIMULATE).getAmount()==250&&two.tank.getFluidAmount()==1000,"Fluid simulation changed inventory");
        var input=new ItemStack(ModItems.INTAKE_UPGRADE.get());var settings=new net.minecraft.nbt.CompoundTag();settings.putInt("Directions",1<<5);input.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(settings));one.upgrades.setStackInSlot(0,input);
        OrbAutomation.tick(one);h.assertTrue(one.tank.getFluidAmount()>0&&one.tank.getFluidAmount()<=500&&one.tank.getFluidAmount()+two.tank.getFluidAmount()==1000,"Intake upgrade did not access remote side: "+one.tank.getFluidAmount()+"/"+two.tank.getFluidAmount());
        l.destroyBlock(black.host.getBlockPos(),false);h.assertTrue(white.peer==null&&handler.drain(250,FluidAction.EXECUTE).isEmpty(),"Broken port retained fluid access");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void rangeIdentityAndRecipePair(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,3,2));var b=h.absolutePos(new BlockPos(8,3,8));
        l.setBlockAndUpdate(a,Blocks.STONE.defaultBlockState());l.setBlockAndUpdate(b,Blocks.STONE.defaultBlockState());var one=port(h,a,Direction.UP);var two=port(h,b,Direction.UP);
        var far=new SpacetimePortBlockEntity(one.host.getBlockPos().offset(33,0,0),ModBlocks.SPACETIME_PORT.get().defaultBlockState());far.setLevel(l);
        h.assertTrue(!SpacetimePortState.pair(one,far.port())&&one.peer==null,"Overrange pair accepted");
        h.assertTrue(SpacetimePortState.pair(one,two),"Pair failed");var tag=new net.minecraft.nbt.CompoundTag();one.write(tag);var identity=one.identity;one.read(tag);
        h.assertTrue(one.other()==two&&one.identity.equals(identity),"Saved peer identity lost");two.identity=java.util.UUID.randomUUID();one.tick();h.assertTrue(one.peer==null,"Replaced peer retained link");
        var slots=new SimpleContainer(7);slots.setItem(0,new ItemStack(ModItems.SPACE_WARP_POINT.get()));slots.setItem(2,new ItemStack(ModItems.TIME_WARP_POINT.get()));
        slots.setItem(3,new ItemStack(ModItems.SPACE_WARP_POINT.get()));slots.setItem(5,new ItemStack(ModItems.TIME_WARP_POINT.get()));slots.setItem(6,new ItemStack(ModItems.RESONANCE_CORE.get()));
        var input=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(slots);var recipe=l.getRecipeManager().getRecipeFor(ModRecipes.ASSEMBLY_TYPE.get(),input,l).orElseThrow();
        var result=recipe.value().assemble(input,l.registryAccess());h.assertTrue(result.is(ModItems.SPACETIME_PORT.get())&&result.getCount()==2,"Shapeless port recipe did not produce a pair");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=240) public static void assemblyKeepsBothPorts(GameTestHelper h){
        var table=new ArcaneAssemblyTableBlockEntity(h.absolutePos(new BlockPos(2,3,2)),ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState());table.setLevel(h.getLevel());
        table.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get()));table.setItem(1,new ItemStack(ModItems.SPACE_WARP_POINT.get()));
        table.setItem(2,new ItemStack(ModItems.TIME_WARP_POINT.get()));table.setItem(3,new ItemStack(ModItems.SPACE_WARP_POINT.get()));table.setItem(6,new ItemStack(ModItems.RESONANCE_CORE.get()));
        table.setMechanicalPowered(true);
        for(int i=0;i<20;i++)h.runAfterDelay(1+i*10,()->{if(!table.finished())h.assertTrue(table.automaticStrike(),"Assembly strike failed");});
        h.runAfterDelay(202,()->{h.assertTrue(table.finished()&&table.getItem(6).is(ModItems.SPACETIME_PORT.get())&&table.getItem(6).getCount()==2,"Assembly lost second port");h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void shaftsChangeAxisAndRejectNonShaftFaces(GameTestHelper h){
        if(!net.neoforged.fml.ModList.get().isLoaded("create")){h.succeed();return;}
        SpacetimePortCompatibilityChecks.verify(h);
    }
    @GameTest(template="spell_arena") public static void remoteEngineRequiresItsOutputFace(GameTestHelper h){
        var l=h.getLevel();var a=h.absolutePos(new BlockPos(2,3,2));var b=h.absolutePos(new BlockPos(8,3,8));
        l.setBlockAndUpdate(a,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        var engine=((ManaEngineAccess)l.getBlockEntity(a)).engineState();engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        l.setBlockAndUpdate(b,Blocks.STONE.defaultBlockState());
        var white=port(h,a,Direction.EAST);var black=port(h,b,Direction.UP);h.assertTrue(SpacetimePortState.pair(white,black),"Engine pair failed");
        h.assertTrue(SpacetimePortState.engineAt(l,black.host.getBlockPos(),Direction.DOWN)!=null,"Remote engine output not found");
        h.assertTrue(SpacetimePortState.engineAt(l,black.host.getBlockPos(),Direction.NORTH)==null,"Unattached side supplied engine power");
        l.setBlockAndUpdate(a,l.getBlockState(a).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
        h.assertTrue(SpacetimePortState.engineAt(l,black.host.getBlockPos(),Direction.DOWN)==null,"Engine back face supplied power");h.succeed();
    }
    @GameTest(template="spell_arena") public static void energyFacesArePassiveAndColorIndependent(GameTestHelper h){
        var l=h.getLevel();var centers=new BlockPos[]{h.absolutePos(new BlockPos(3,3,3)),h.absolutePos(new BlockPos(10,3,10))};
        var ports=new SpacetimePortState[2];
        var pools=new com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity[2];
        for(int i=0;i<2;i++){
            h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(l,centers[i],Direction.NORTH),"Pool fixture failed");
            pools[i]=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(l,centers[i]);
            var socket=centers[i].offset(com.mcmagic.omnira.alchemy.ReactionPoolBlock.offset(Direction.NORTH,3));
            ports[i]=port(h,socket,com.mcmagic.omnira.alchemy.ReactionPoolBlock.outward(Direction.NORTH,3));
        }
        h.assertTrue(SpacetimePortState.pair(ports[0],ports[1]),"Energy pair failed");
        pools[0].energyInput.receiveEnergy(100,false);
        for(var p:ports)p.tick();
        h.assertTrue(pools[0].energyInput.getEnergyStored()==100&&pools[1].energyInput.getEnergyStored()==0,"Ports moved energy automatically");
        for(int i=0;i<2;i++){
            var p=ports[i];var target=pools[1-i].energyInput;
            var proxy=l.getCapability(Capabilities.EnergyStorage.BLOCK,p.host.getBlockPos(),p.face().getOpposite());
            h.assertTrue(proxy!=null&&proxy.canReceive()&&!proxy.canExtract(),"Remote permissions depend on pair color");
            int before=target.getEnergyStored();
            h.assertTrue(proxy.receiveEnergy(120,true)==120&&target.getEnergyStored()==before,"Simulation changed energy");
            h.assertTrue(proxy.receiveEnergy(120,false)==120&&target.getEnergyStored()==before+120,"Caller-driven transfer failed");
            int remaining=target.receiveEnergy(Integer.MAX_VALUE,true);
            h.assertTrue(proxy.receiveEnergy(Integer.MAX_VALUE,false)==remaining&&target.receiveEnergy(1,false)==0,"Remote and direct input did not share a rate budget");
            h.assertTrue(proxy.extractEnergy(120,false)==0,"Receiver became an energy source");
            h.assertTrue(l.getCapability(Capabilities.EnergyStorage.BLOCK,p.host.getBlockPos(),Direction.UP)==null,"Wrong face exposed energy");
        }
        var cached=ports[0].energyPort(ports[0].face().getOpposite());ports[1].unlink();
        h.assertTrue(!cached.canReceive()&&cached.receiveEnergy(120,false)==0,"Cached energy connection survived unlink");h.succeed();
    }
    @GameTest(template="spell_arena") public static void engineDrivesEnergyTransferRatherThanPort(GameTestHelper h){
        if(!com.mcmagic.omnira.energy.EnergyIntegration.available()){h.succeed();return;}
        var l=h.getLevel();var center=h.absolutePos(new BlockPos(7,3,7));
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(l,center,Direction.NORTH),"Pool fixture failed");
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(l,center);
        var socket=center.offset(com.mcmagic.omnira.alchemy.ReactionPoolBlock.offset(Direction.NORTH,3));
        var direction=com.mcmagic.omnira.alchemy.ReactionPoolBlock.outward(Direction.NORTH,3);
        var source=h.absolutePos(new BlockPos(2,3,2));l.setBlockAndUpdate(source,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
        var engine=((ManaEngineAccess)l.getBlockEntity(source)).engineState();engine.redstoneControl=false;engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        var white=port(h,source,Direction.EAST);var black=port(h,socket,direction);h.assertTrue(SpacetimePortState.pair(white,black),"Energy pair failed");
        white.tick();black.tick();h.assertTrue(pool.energyInput.getEnergyStored()==0,"Port autonomously extracted energy");
        engine.tickEnergy();h.assertTrue(pool.energyInput.getEnergyStored()>0,"Engine could not drive remote receiver");
        white.unlink();h.assertTrue(SpacetimePortState.pair(black,white),"Reverse pairing failed");
        var output=black.energyPort(direction.getOpposite());h.assertTrue(output.canExtract()&&!output.canReceive(),"Remote source permissions depend on color");
        int before=pool.energyInput.getEnergyStored();white.tick();black.tick();
        h.assertTrue(pool.energyInput.getEnergyStored()==before,"Reversed ports autonomously moved energy");h.succeed();
    }
}
