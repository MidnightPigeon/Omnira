package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.*;
import com.mcmagic.omnira.compat.create.CodexKinetics;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;

/** Optional Create linkage is only resolved in the mechanical test branch. */
final class CodexKineticChecks {
    static void check(GameTestHelper h){
        for(float rpm:new float[]{8,-8,16,-16,256,-256,512,1024}){
            double expected=Math.abs(rpm)<16?0:Math.min(16384,32*Math.abs(rpm));
            h.assertTrue(CodexKinetics.demand(rpm)==expected,"Codex potential curve or cap wrong at "+rpm);
        }
        var pos=new BlockPos(5,4,5);h.setBlock(pos,MyriadCodexContent.BLOCK.get());
        var host=(CodexKinetics.Host)h.getBlockEntity(pos);var work=host.codex();var state=host.getBlockState();var rear=state.getValue(MyriadCodexBlock.FACING).getOpposite();
        for(var face:Direction.values())h.assertTrue(((IRotate)state.getBlock()).hasShaftTowards(h.getLevel(),host.getBlockPos(),state,face)==(face==rear),"Codex exposed extra shaft input");
        var motorPos=host.getBlockPos().relative(rear);
        h.getLevel().setBlockAndUpdate(motorPos,com.simibubi.create.AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING,rear.getOpposite()));
        var motor=(CreativeMotorBlockEntity)h.getLevel().getBlockEntity(motorPos);motor.generatedSpeed.setValue(8);
        int[] rpm={8,16,128,256,0};
        for(int i=0;i<rpm.length;i++){
            final int stage=i;h.runAfterDelay((i+1)*30,()->{
                int speed=rpm[stage];h.assertTrue(Math.abs(host.getSpeed())==speed,"Codex shaft failed to follow real input at "+speed);
                h.assertTrue(host.isSpeedRequirementFulfilled()==(speed>=16),"Codex RPM threshold wrong");
                double load=host.hasNetwork()?host.getOrCreateNetwork().getActualStressOf(host):0;
                h.assertTrue(Math.abs(load-(speed>=16?32*speed:0))<.01,"Codex load wrong at "+speed+": "+load);
                h.assertTrue(work.potential==(speed>=256?8192:0)&&work.diameter()==(speed>=256?7:0),"Codex ran without full single-face demand");
                h.assertTrue(!work.electrical&&work.shaftSpeed()==host.getSpeed(),"Codex socket mode/speed lost");
                if(speed==256){
                    h.assertTrue(work.energyInput.receiveEnergy(100,false)==0,"Mechanical codex also accepted FE");
                    var saved=host.saveWithFullMetadata(h.getLevel().registryAccess());var copy=MyriadCodexContent.entity(host.getBlockPos(),state);copy.setLevel(h.getLevel());copy.loadWithComponents(saved,h.getLevel().registryAccess());
                    h.assertTrue(MyriadCodexEntity.from(copy).identity.equals(work.identity),"Kinetic host lost codex identity on reload");
                    h.assertTrue(work.link().find(h.getLevel().getServer())==work,"Heart cannot resolve kinetic codex");
                }
                if(stage+1<rpm.length)motor.generatedSpeed.setValue(rpm[stage+1]);else h.succeed();
            });
        }
    }
    private CodexKineticChecks(){}
}
