package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_pulse_relay")
@PrefixGameTestTemplate(false)
public final class PulseRelayTests {
    private static PulseOrbEntity orb(GameTestHelper h,BlockPos pos,Direction mount){
        h.setBlock(pos,PulseContent.ORB.get().defaultBlockState().setValue(PulseOrbBlock.MOUNT,mount));
        return PulseOrbEntity.from(h.getBlockEntity(pos));
    }
    static void engine(GameTestHelper h,BlockPos pos,Direction face){
        h.setBlock(pos,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,face));
        var engine=((ManaEngineAccess)h.getBlockEntity(pos)).engineState();
        engine.inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
    }
    @GameTest(template="spell_arena") public static void sixMountsHaveInputOnlyBase(GameTestHelper h){
        int x=2;for(var mount:Direction.values()){
            var orb=orb(h,new BlockPos(x,3,4),mount);x+=2;
            for(var face:Direction.values()){
                var fe=orb.conversion.face(face);
                h.assertTrue(fe.canExtract()==(face!=mount.getOpposite()),"Output face mismatch: "+mount+"/"+face);
                h.assertTrue(fe.canReceive()==(face==mount.getOpposite()),"External input face mismatch");
                if(face==mount.getOpposite())h.assertTrue(fe.extractEnergy(999,false)==0,"Base produced FE");
                else h.assertTrue(fe.receiveEnergy(999,false)==0,"Non-base received FE");
            }
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void directedRelayCannotFeedItsBaseWire(GameTestHelper h){
        var p=new BlockPos(5,3,5);var orb=orb(h,p,Direction.EAST);
        engine(h,p.north(),Direction.SOUTH);h.setBlock(p.west(),PulseContent.WIRE.get());
        close(h,PulseNetworks.available(orb),512);
        close(h,PulseNetworks.input((PulseWireEntity)h.getBlockEntity(p.west())),0);h.succeed();
    }
    @GameTest(template="spell_arena") public static void nativeAndExternalLoadsUseOneBudget(GameTestHelper h){
        var p=new BlockPos(5,3,5);var orb=orb(h,p,Direction.UP);engine(h,p.north(),Direction.SOUTH);
        h.assertTrue(orb.conversion.reserve(PulseConversions.KINETIC,128,false)==128,"SU debit failed");
        var fe=orb.conversion.face(Direction.EAST);int simulated=fe.extractEnergy(Integer.MAX_VALUE,true);
        close(h,PulseNetworks.occupied(orb),128);
        int sent=fe.extractEnergy(Integer.MAX_VALUE,false);
        h.assertTrue(sent==simulated&&sent>0&&fe.extractEnergy(Integer.MAX_VALUE,false)==0,"FE simulation or repeated pull duplicated output");
        close(h,PulseNetworks.occupied(orb),512);
        h.assertTrue(orb.conversion.reserve(PulseConversions.KINETIC,129,true)==128,"Conversions cloned the budget");h.succeed();
    }
    @GameTest(template="spell_arena") public static void conversionsCannotDisplaceMachineMinimum(GameTestHelper h){
        var p=new BlockPos(5,3,5);var orb=orb(h,p,Direction.UP);engine(h,p.north(),Direction.SOUTH);
        var machine=p.south();h.setBlock(machine,com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        h.assertTrue(orb.conversion.reserve(PulseConversions.KINETIC,512,false)==256,"Conversion stole native minimum");
        var power=PulseNetworks.power(h.getLevel(),h.absolutePos(machine));
        h.assertTrue(power.running()&&power.allocated()==256,"Native machine lost its base supply");
        close(h,PulseNetworks.occupied(orb),512);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=70) public static void feInputUsesActualRollingSecondAndExpires(GameTestHelper h){
        var orb=orb(h,new BlockPos(5,3,5),Direction.UP);var input=orb.conversion.face(Direction.DOWN);
        for(int t=1;t<=20;t++)h.runAfterDelay(t,()->{
            h.assertTrue(input.receiveEnergy(100,true)==100,"FE input simulation failed");input.receiveEnergy(100,false);
        });
        h.runAfterDelay(21,()->close(h,orb.conversion.imported(Direction.DOWN),1900));
        h.runAfterDelay(42,()->{close(h,PulseNetworks.available(orb),0);h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=45) public static void feBurstUsesSecondBudgetNotPerTickLimit(GameTestHelper h){
        var orb=orb(h,new BlockPos(5,3,5),Direction.UP);var input=orb.conversion.face(Direction.DOWN);
        h.assertTrue(input.receiveEnergy(8192,true)==8192,"Burst simulation was restricted to one tick");
        close(h,orb.conversion.imported(Direction.DOWN),0);
        h.assertTrue(input.receiveEnergy(8192,false)==8192&&input.receiveEnergy(1,false)==0,"Burst exceeded the second budget");
        close(h,PulseNetworks.available(orb),8192);
        h.runAfterDelay(21,()->{close(h,PulseNetworks.available(orb),0);h.succeed();});
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void actualCreateShaftsAndIdleLoad(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))PulseRelayCreateChecks.outputs(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100) public static void externalCreateInputIsIsolatedAndShared(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))PulseRelayCreateChecks.inputs(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void nativeEngineThroughShaftKeepsItsOriginalBudget(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))PulseRelayCreateChecks.nativeTransit(h);else h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void relayThroughShaftDoesNotMintAnotherSource(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("create"))PulseRelayCreateChecks.relayTransit(h);else h.succeed();
    }
    static void close(GameTestHelper h,double actual,double expected){h.assertTrue(Math.abs(actual-expected)<.01,"Expected "+expected+", got "+actual);}
}
