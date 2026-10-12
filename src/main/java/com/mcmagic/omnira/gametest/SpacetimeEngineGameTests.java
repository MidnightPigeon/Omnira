package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_engine")
@PrefixGameTestTemplate(false)
public final class SpacetimeEngineGameTests {
    private static SpacetimeEngineController engine(GameTestHelper h){
        var root=h.absolutePos(new BlockPos(5,3,5));h.assertTrue(SpacetimeEngineStructure.place(h.getLevel(),root),"Placement failed");
        return SpacetimeEngineBlock.controller(h.getLevel(),root.above());
    }
    @GameTest(template="spell_arena") public static void assemblyIsAtomicAndOnlyTopHasMenu(GameTestHelper h){
        var l=h.getLevel();var root=h.absolutePos(new BlockPos(5,3,5));var player=h.makeMockPlayer(GameType.SURVIVAL);
        var crystal=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
        for(int i=0;i<10;i++)l.setBlockAndUpdate(root.offset(SpacetimeEngineBlock.offset(i)),SpacetimeEngineStructure.required(i).defaultBlockState());
        l.setBlockAndUpdate(root.offset(SpacetimeEngineBlock.offset(0)),Blocks.AIR.defaultBlockState());
        h.assertTrue(!SpacetimeEngineStructure.assemble(l,root.above(),player,crystal)&&crystal.getCount()==2,"Invalid build consumed crystal");
        l.setBlockAndUpdate(root.offset(SpacetimeEngineBlock.offset(0)),SpacetimeEngineStructure.required(0).defaultBlockState());
        var old=(com.mcmagic.omnira.block.entity.ManaEngineAccess)l.getBlockEntity(root.north());old.engineState().inventory.setItem(1,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        h.assertTrue(!SpacetimeEngineStructure.assemble(l,root.above(),player,crystal),"Occupied component overwritten");old.engineState().inventory.clearContent();
        h.assertTrue(SpacetimeEngineStructure.assemble(l,root.above(),player,crystal)&&crystal.getCount()==1,"Valid build failed");
        for(int i=0;i<10;i++){var p=root.offset(SpacetimeEngineBlock.offset(i));var s=l.getBlockState(p);
            h.assertTrue(s.getDestroySpeed(l,p)<0,"Mineable part");h.assertTrue((s.getMenuProvider(l,p)!=null)==(i==9),"Wrong UI part "+i);}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void gridsMultiplyCeilingAndClampOnRemoval(GameTestHelper h){
        var e=engine(h);h.assertTrue(e.limit()==2048,"Base limit");
        h.assertTrue(!e.grids.isItemValid(0,new ItemStack(Items.DIAMOND))&&e.grids.getSlotLimit(0)==1,"Invalid grid slot");
        e.grids.setStackInSlot(0,new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get()));
        e.grids.setStackInSlot(1,new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get()));
        e.grids.setStackInSlot(2,new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get()));
        h.assertTrue(e.limit()==(int)Math.round(2048*1.2*1.5*2),"Different ordinary engine factors");
        for(int i=0;i<3;i++)e.grids.setStackInSlot(i,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        h.assertTrue(e.limit()==131072,"Three grid cap");
        for(int i=0;i<4;i++)h.assertTrue(e.configure(i,131072,128,i%2==0),"Maximum setting rejected");
        var menu=new SpacetimeEngineMenu(1,h.makeMockPlayer(GameType.SURVIVAL).getInventory(),e);
        h.assertTrue(menu.limit()==131072&&menu.capacity(0)==131072,"SU truncated to 16 bits");
        e.grids.setStackInSlot(2,ItemStack.EMPTY);
        for(int i=0;i<4;i++)h.assertTrue(e.capacity(i)==32768&&e.rpm(i)==128,"Removal did not clamp capacity only");
        h.assertTrue(!e.configure(4,1,1,false)&&!e.configure(0,32769,128,false)&&!e.configure(0,1,129,false)&&!e.configure(0,-1,16,false),"Invalid settings accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bottlePreservesAllOutputsAndGrids(GameTestHelper h){
        var e=engine(h);var l=h.getLevel();var root=e.getBlockPos().below();
        e.grids.setStackInSlot(0,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        e.setFront(Direction.WEST);
        for(int i=0;i<4;i++)e.configure(i,3000+i*100,16+i*16,i%2==1);
        var player=h.makeMockPlayer(GameType.SURVIVAL);var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.capture(player,root.north(),bottle),"Capture from output failed");
        for(int i=0;i<10;i++)h.assertTrue(l.getBlockState(root.offset(SpacetimeEngineBlock.offset(i))).isAir(),"Capture left a part");
        var saved=com.mcmagic.omnira.item.bottle.BottleStorage.get(l).get(bottle.get(ModDataComponents.BOTTLE_CAPTURE));
        h.assertTrue(com.mcmagic.omnira.item.bottle.MultiblockBottleCapture.release(l,saved,net.minecraft.world.phys.Vec3.atLowerCornerOf(root),player),"Release failed");
        var restored=SpacetimeEngineBlock.controller(l,root.above());h.assertTrue(restored.complete()&&restored.limit()==8192,"Lost grid");
        h.assertTrue(restored.frontSide()==3,"Bottle lost front orientation");
        for(int i=0;i<4;i++)h.assertTrue(restored.capacity(i)==3000+i*100&&restored.rpm(i)==16+i*16&&restored.reversed(i)==(i%2==1),"Lost output "+i);h.succeed();
    }
    @GameTest(template="spell_arena") public static void configurationRequiresLiveNearbyMenu(GameTestHelper h){
        var e=engine(h);var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(e.getBlockPos()));
        var payload=new EngineConfigPayload(17,0,2048,32,true);
        h.assertTrue(!EngineConfigPayload.apply(player,payload),"Configured without menu");
        player.containerMenu=new SpacetimeEngineMenu(17,player.getInventory(),e);h.assertTrue(EngineConfigPayload.apply(player,payload),"Valid setting failed");
        player.setPos(player.position().add(100,0,0));h.assertTrue(!EngineConfigPayload.apply(player,payload),"Remote configuration accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void incompleteOrCreativeRemovedEngineCannotGenerate(GameTestHelper h){
        var e=engine(h);var root=e.getBlockPos().below();h.assertTrue(e.generatedSpeed(0)==16,"Complete machine inactive");
        h.getLevel().setBlockAndUpdate(root.offset(-1,0,-1),Blocks.AIR.defaultBlockState());h.assertTrue(e.generatedSpeed(0)==0,"Incomplete machine generating");
        var p=root.east();var s=h.getLevel().getBlockState(p);s.getBlock().playerWillDestroy(h.getLevel(),p,s,h.makeMockPlayer(GameType.CREATIVE));
        for(int i=0;i<10;i++)h.assertTrue(h.getLevel().getBlockState(root.offset(SpacetimeEngineBlock.offset(i))).isAir(),"Creative removal left part");h.succeed();
    }
    @GameTest(template="spell_arena") public static void directDriveAndFixedArray(GameTestHelper h){
        var e=engine(h);var root=e.getBlockPos().below();
        for(int i=0;i<4;i++){var side=SpacetimeEngineBlock.OUTPUTS[i];e.configure(i,2048,16,false);
            h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.driveAt(h.getLevel(),root.relative(side),side)==(SpacetimeEngineController.hasKinetics()?4:1),"Direct-drive capacity");
            h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.driveAt(h.getLevel(),root.relative(side),side.getOpposite())==0,"Inward shaft exposed");}
        h.assertTrue(Math.abs(SpacetimeEngineGeometry.arrayAngle(200)-SpacetimeEngineGeometry.arrayAngle(100)-1.2)<1e-9,"Array speed changed");
        for(int slot=0;slot<3;slot++)h.assertTrue(Math.abs(SpacetimeEngineGeometry.cornerAngle(slot)-slot*Math.PI*2/3)<1e-9,"Not alternating corners");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=160) public static void kineticOutputsAreIndependentAndRpmDoesNotMultiplyCapacity(GameTestHelper h){
        var e=engine(h);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))SpacetimeEngineCreateChecks.run(h,e);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void relativeSidesAndMeshAreConsistent(GameTestHelper h){
        var e=engine(h);var menu=new SpacetimeEngineMenu(1,h.makeMockPlayer(GameType.SURVIVAL).getInventory(),e);
        for(int front=0;front<4;front++){
            e.setFront(SpacetimeEngineBlock.OUTPUTS[front]);
            for(int row=0;row<4;row++)h.assertTrue(menu.displaySide(row)==(front+row)%4&&e.relativeSide(menu.displaySide(row))==row,"Relative output mismatch");
        }
        var colors=new java.util.HashSet<Integer>();for(int i=0;i<4;i++)colors.add(SpacetimeEngineGeometry.outputColor(i));
        h.assertTrue(colors.size()==4,"Shaft colors are not distinct");
        for(var mesh:java.util.List.of(SpacetimeEngineGeometry.BASE,SpacetimeEngineGeometry.ROTOR,SpacetimeEngineGeometry.DRIVE,SpacetimeEngineGeometry.FRONT))
            for(var b:mesh)h.assertTrue(b.faces()!=0&&b.x()<b.xx()&&b.y()<b.yy()&&b.z()<b.zz()&&b.xx()-b.x()<=16&&b.yy()-b.y()<=16&&b.zz()-b.z()<=16,"Bad mesh face or atlas spill");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=180) public static void remoteOutputChangesAxisAndDisconnects(GameTestHelper h){
        var e=engine(h);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))SpacetimeEngineCreateChecks.remote(h,e);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=180) public static void remoteIdleAnvilKeepsFixedOccupancy(GameTestHelper h){
        var e=engine(h);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))SpacetimeEngineCreateChecks.idleAnvil(h,e);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void fixedMachinesRotateAndReserveWhileIdle(GameTestHelper h){
        var e=engine(h);
        if(net.neoforged.fml.ModList.get().isLoaded("create"))SpacetimeEngineCreateChecks.fixedMachines(h,e);else h.succeed();
    }
    @GameTest(template="spell_arena") public static void fallbackPortsDrivePoolAndAssembly(GameTestHelper h){
        if(SpacetimeEngineController.hasKinetics()){h.succeed();return;}
        var e=engine(h);var l=h.getLevel();var root=e.getBlockPos().below();
        e.configure(3,0,0,false);
        e.grids.setStackInSlot(0,new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get()));
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setPos(net.minecraft.world.phys.Vec3.atCenterOf(e.getBlockPos()));
        player.containerMenu=new SpacetimeEngineMenu(23,player.getInventory(),e);
        h.assertTrue(EngineConfigPayload.apply(player,new EngineConfigPayload(23,3,999999,128,true)),"Fallback direction rejected");
        h.assertTrue(e.capacity(3)==0&&e.rpm(3)==0&&e.generatedSpeed(3)==-16,"Fallback edited hidden settings or stopped on old zero settings");
        var center=h.absolutePos(new BlockPos(5,3,11));
        h.assertTrue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.place(l,center,Direction.NORTH),"Pool placement failed");
        var source=fallbackPatch(h,root.west(),Direction.WEST);
        var target=fallbackPatch(h,center.east(),Direction.EAST);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(source,target),"Pool link failed");
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(l,center);
        com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity.tick(l,center,pool.getBlockState(),pool);
        h.assertTrue(pool.powered()&&pool.efficiency()==1,"Grid changed fallback power or pool not driven");
        var table=h.absolutePos(new BlockPos(1,3,10));
        l.setBlockAndUpdate(table,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var second=fallbackPatch(h,root.east(),Direction.EAST);
        var input=fallbackPatch(h,table,Direction.NORTH);
        h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(second,input),"Assembly link failed");
        h.assertTrue(com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity.directEngine(l,table,l.getBlockState(table)),"Remote assembly drive missing");
        source.unlink();second.unlink();
        com.mcmagic.omnira.alchemy.ReactionPoolBlockEntity.tick(l,center,pool.getBlockState(),pool);
        h.assertTrue(!pool.powered()&&!com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity.directEngine(l,table,l.getBlockState(table)),"Disconnected fallback retained power");
        h.succeed();
    }
    private static com.mcmagic.omnira.space.SpacetimePortState fallbackPatch(GameTestHelper h,BlockPos attached,Direction face){
        var pos=attached.relative(face);
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.SPACETIME_PORT.get().defaultBlockState().setValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING,face));
        return com.mcmagic.omnira.space.SpacetimePortState.at(h.getLevel(),pos);
    }
}
