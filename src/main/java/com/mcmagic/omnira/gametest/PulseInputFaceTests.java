package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.mcmagic.omnira.energy.EnergyInputFaces;
import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.workstation.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_pulse_faces")
@PrefixGameTestTemplate(false)
public final class PulseInputFaceTests {
    private static void engine(GameTestHelper h,BlockPos pos,Direction face,boolean strong){
        h.setBlock(pos,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face));
        var state=((ManaEngineAccess)h.getBlockEntity(pos)).engineState();
        state.inventory.setItem(0,new ItemStack(strong?ModItems.DREAM_SPELL_CORE.get():ModItems.TEST_SPELL_CORE.get()));
    }
    private static CrystalWorkstationEntity work(GameTestHelper h,BlockPos pos,int index,Direction facing){
        h.setBlock(pos,CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState().setValue(CrystalWorkstationBlock.FACING,facing));
        return CrystalWorkstations.work(h.getBlockEntity(pos));
    }
    @GameTest(template="spell_arena") public static void furnaceBackAndAnvilSingleEndForAllFacings(GameTestHelper h){
        int i=0;
        for(var facing:Direction.Plane.HORIZONTAL){
            var pos=new BlockPos(4+i++*4,3,5);var furnace=work(h,pos,3,facing);var inlet=facing.getOpposite();
            engine(h,pos.relative(inlet),facing,false);furnace.tickDirect();
            h.assertTrue(furnace.pulsePowered&&furnace.inputFaces==1<<inlet.ordinal(),"Furnace ring is not on its back: "+facing);
            var tag=furnace.saveWithFullMetadata(h.getLevel().registryAccess());
            var copy=new CrystalWorkstationEntity(furnace.getBlockPos(),furnace.getBlockState());copy.loadWithComponents(tag,h.getLevel().registryAccess());
            h.assertTrue(copy.inputFaces==furnace.inputFaces,"Input face was not synchronized");
            var anvilPos=pos.south(8);var anvil=work(h,anvilPos,5,facing);
            engine(h,anvilPos.relative(inlet),facing,false);anvil.tickDirect();
            h.assertTrue(anvil.pulsePowered&&anvil.inputFaces==1<<inlet.ordinal(),"Anvil invented a second inlet: "+facing);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=80) public static void bothEndsAndDisconnectedEnd(GameTestHelper h){
        var pos=new BlockPos(5,3,5);var anvil=work(h,pos,5,Direction.NORTH);
        engine(h,pos.north(),Direction.SOUTH,false);engine(h,pos.south(),Direction.NORTH,false);anvil.tickDirect();
        int both=(1<<Direction.NORTH.ordinal())|(1<<Direction.SOUTH.ordinal());
        h.assertTrue(anvil.inputFaces==both,"Two live inputs did not show both ends");
        h.setBlock(pos.north(),Blocks.AIR);
        h.runAfterDelay(22,()->{anvil.tickDirect();h.assertTrue(anvil.inputFaces==1<<Direction.SOUTH.ordinal(),"Disconnected anvil inlet stayed lit");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void wireAndOrbInletsStayOnTheirOwnFaces(GameTestHelper h){
        var pos=new BlockPos(5,3,5);var anvil=work(h,pos,5,Direction.NORTH);
        h.setBlock(pos.north(),PulseContent.WIRE.get());engine(h,pos.north(2),Direction.SOUTH,false);anvil.tickDirect();
        h.assertTrue(anvil.inputFaces==1<<Direction.NORTH.ordinal(),"Wire inlet points to the opposite end");
        var other=new BlockPos(12,3,5);var furnace=work(h,other,3,Direction.SOUTH);
        h.setBlock(other.north(),PulseContent.ORB.get());engine(h,other.north(2),Direction.SOUTH,false);furnace.tickDirect();
        h.assertTrue(furnace.inputFaces==1<<Direction.NORTH.ordinal(),"Orb inlet missing on furnace rear");h.succeed();
    }
    @GameTest(template="spell_arena") public static void electricalReceiptsDoNotInventConnections(GameTestHelper h){
        long[] tick={0};var storage=new net.neoforged.neoforge.energy.EnergyStorage(10000);
        var faces=new EnergyInputFaces(storage,()->tick[0]);var north=faces.port(Direction.NORTH);
        north.receiveEnergy(100,true);h.assertTrue(faces.mask()==0,"Simulated FE query produced a ring");
        north.receiveEnergy(100,false);h.assertTrue(faces.mask()==1<<Direction.NORTH.ordinal(),"FE inlet not recorded");
        h.assertTrue(faces.port(Direction.NORTH)==north,"Capability wrapper changes on every query");
        tick[0]=20;h.assertTrue(faces.mask()==0&&storage.getEnergyStored()==100,"Buffered FE invented an active inlet");
        var pos=new BlockPos(5,3,5);var furnace=work(h,pos,3,Direction.NORTH);
        h.assertTrue(furnace.energyPort(Direction.NORTH)==null,"Furnace accepts power through its front");
        furnace.energyPort(Direction.SOUTH).receiveEnergy(100,false);
        h.runAfterDelay(1,()->{furnace.tickDirect();h.assertTrue(furnace.electrical&&furnace.inputFaces==1<<Direction.SOUTH.ordinal(),"FE ring is not on furnace back");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void poolDoesNotLightUnusedInput(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(8,3,8));
        h.assertTrue(ReactionPoolBlock.place(h.getLevel(),center,Direction.NORTH),"Pool placement failed");
        var pool=ReactionPoolBlock.controller(h.getLevel(),center);int part=3;
        var face=ReactionPoolBlock.outward(Direction.NORTH,part);var source=center.offset(ReactionPoolBlock.offset(Direction.NORTH,part)).relative(face);
        engine(h,new BlockPos(8,3,8).offset(ReactionPoolBlock.offset(Direction.NORTH,part)).relative(face),face.getOpposite(),true);
        h.runAfterDelay(22,()->{
            ReactionPoolBlockEntity.tick(h.getLevel(),pool.getBlockPos(),pool.getBlockState(),pool);
            h.assertTrue(pool.pulsePowered()&&pool.inputFaces==1<<face.ordinal(),"Pool input mismatch: powered="+pool.pulsePowered()+", faces="+pool.inputFaces+", expected="+(1<<face.ordinal()));h.succeed();
        });
    }
    private PulseInputFaceTests(){}
}
