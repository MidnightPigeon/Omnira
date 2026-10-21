package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.energy.ContinuousEnergyInput;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_continuous_power") @PrefixGameTestTemplate(false)
public final class ContinuousPowerGameTests {
    @GameTest(template="spell_arena",timeoutTicks=100) public static void workstationsAndCodexRequireLiveElectricalRate(GameTestHelper h){
        var work=new java.util.ArrayList<com.mcmagic.omnira.workstation.CrystalWorkstationEntity>();
        int x=2;for(int index:new int[]{3,5}){
            var pos=new net.minecraft.core.BlockPos(x,3,2);x+=3;
            h.setBlock(pos,com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(index).get().defaultBlockState());
            work.add(com.mcmagic.omnira.workstation.CrystalWorkstations.work(h.getBlockEntity(pos)));
        }
        var pos=new net.minecraft.core.BlockPos(10,3,2);h.setBlock(pos,com.mcmagic.omnira.codex.MyriadCodexContent.BLOCK.get());
        var codex=com.mcmagic.omnira.codex.MyriadCodexEntity.from(h.getBlockEntity(pos));
        var rear=codex.getBlockState().getValue(com.mcmagic.omnira.codex.MyriadCodexBlock.FACING).getOpposite();
        for(var face:net.minecraft.core.Direction.values())h.assertTrue((h.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,codex.getBlockPos(),face)!=null)==(face==rear),"Codex exposed FE outside its rear socket");
        int[] total=new int[3];for(int tick=1;tick<=40;tick++)h.runAfterDelay(tick,()->{
            for(int i=0;i<2;i++)total[i]+=work.get(i).energyInput.receiveEnergy(Integer.MAX_VALUE,false);
            total[2]+=codex.energyInput.receiveEnergy(Integer.MAX_VALUE,false);
        });
        h.runAfterDelay(41,()->{
            h.assertTrue(work.get(0).electrical&&work.get(1).electrical&&work.get(0).energyInput.factor()==1&&work.get(1).energyInput.factor()==8,"Fixed furnace or scalable anvil electrical mode wrong");
            h.assertTrue(total[0]==512&&total[1]==4096&&total[2]==32768,"Idle machines did not continuously consume exact FE/s");
            h.assertTrue(codex.electrical&&codex.potential==16384&&codex.diameter()==15,"Codex did not convert FE/s into equivalent potential");
            var copy=new com.mcmagic.omnira.codex.MyriadCodexEntity(codex.getBlockPos(),codex.getBlockState());copy.setLevel(h.getLevel());copy.loadWithComponents(codex.getUpdateTag(h.getLevel().registryAccess()),h.getLevel().registryAccess());copy.tick();
            h.assertTrue(copy.potential==0&&!copy.electrical,"Reloaded codex retained electrical power without fresh input");
        });
        h.runAfterDelay(42,()->{
            h.assertTrue(!work.get(0).electrical&&!work.get(1).electrical&&codex.potential==0&&!codex.electrical,"Stopped FE input did not stop all structures");h.succeed();
        });
    }
    private static int amount(int rate,long tick){int p=Math.floorMod(tick,20);return (int)(((long)(p+1)*rate)/20-((long)p*rate)/20);}
    @GameTest(template="spell_arena") public static void idleReceiptsMeasureRateWithoutBankingOrSimulation(GameTestHelper h){
        for(int[] range:new int[][]{{256,256},{256,2048},{1024,8192},{8192,16384}})for(int rate:new int[]{range[0],range[1]}){
            long[] clock={0};boolean[] enabled={true};
            var input=new ContinuousEnergyInput(range[0],range[1],()->clock[0],()->enabled[0]);
            input.receiveEnergy(Integer.MAX_VALUE,true);clock[0]=1;h.assertTrue(input.factor()==0,"Simulation created power");clock[0]=0;
            int total=0;
            for(int tick=0;tick<40;tick++){
                clock[0]=tick;int offered=amount(rate,tick);
                h.assertTrue(input.receiveEnergy(offered,true)==offered,"Simulation denied valid flow");
                h.assertTrue(input.receiveEnergy(offered,true)==offered,"Repeated simulation spent budget");
                int half=offered/2;int accepted=input.receiveEnergy(half,false)+input.receiveEnergy(offered-half,false);total+=accepted;
                h.assertTrue(accepted==offered&&input.getEnergyStored()==0,"Continuous power retained fuel or lost flow");
                clock[0]=tick+1;if(tick>=20)h.assertTrue(Math.abs(input.factor()-(double)rate/range[0])<.00001,"Wrong sustained power factor");
            }
            h.assertTrue(total==rate*2,"Idle draw did not consume the full two-second supply");
            clock[0]=41;h.assertTrue(input.factor()==0,"Previous receipts powered machine after supply stopped");
            input.reset();h.assertTrue(input.factor()==0,"Reset preserved past power");
            enabled[0]=false;h.assertTrue(input.receiveEnergy(Integer.MAX_VALUE,false)==0&&!input.canReceive(),"Disabled input accepted power");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void inputCallsShareOneBudgetAndPartialPowerCannotRun(GameTestHelper h){
        long[] clock={0};var input=new ContinuousEnergyInput(1024,8192,()->clock[0],()->true);
        int total=0;for(int tick=0;tick<20;tick++){
            clock[0]=tick;total+=input.receiveEnergy(Integer.MAX_VALUE,false);
            h.assertTrue(input.receiveEnergy(Integer.MAX_VALUE,false)==0,"Multiple faces multiplied input capacity");
        }
        h.assertTrue(total==8192,"Maximum FE/s drifted");
        clock[0]=20;h.assertTrue(input.factor()==8,"Maximum supply did not reach eightfold power");
        for(int tick=20;tick<41;tick++){clock[0]=tick;input.receiveEnergy(amount(512,tick),false);}
        clock[0]=41;h.assertTrue(input.factor()==0,"Insufficient sustained power ran machine");
        clock[0]=1000;h.assertTrue(input.receiveEnergy(Integer.MAX_VALUE,false)<=410,"Idle time accumulated a burst budget");h.succeed();
    }
    private ContinuousPowerGameTests(){}
}
