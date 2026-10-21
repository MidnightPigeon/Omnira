package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.codex.*;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** Optional Create host; one rear shaft, a continuous 32 SU/RPM load and no source capacity. */
public final class CodexKinetics {
    public static double demand(float rpm){return CrystalShaftInput.sufficient(rpm)?Math.min(16384,32.0*Math.abs(rpm)):0;}
    public static MyriadCodexBlock block(BlockBehaviour.Properties properties){return new KineticBlock(properties);}
    public static BlockEntity entity(BlockPos pos,BlockState state){return new Host(pos,state);}
    public static void tick(BlockEntity entity){if(entity instanceof Host host)host.tick();}
    private static final class KineticBlock extends MyriadCodexBlock implements IRotate {
        KineticBlock(Properties properties){super(properties);}
        public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getAxis();}
        public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face){return face==state.getValue(FACING).getOpposite();}
    }
    public static final class Host extends KineticBlockEntity implements CodexAccess {
        private final MyriadCodexEntity work;
        private float accountedRpm=Float.NaN;
        public Host(BlockPos pos,BlockState state){super(MyriadCodexContent.ENTITY.get(),pos,state);work=new MyriadCodexEntity(pos,state);work.host=this;}
        public MyriadCodexEntity codex(){return work;}
        public float shaftSpeed(){return getSpeed();}
        public boolean isSpeedRequirementFulfilled(){return CrystalShaftInput.sufficient(getSpeed());}
        public double mechanicalPotential(){return hasNetwork()&&!isOverStressed()?demand(getSpeed()):0;}
        @Override public float calculateStressApplied(){
            float rpm=Math.abs(getTheoreticalSpeed());
            return lastStressApplied=rpm>0?(float)(demand(rpm)/rpm):0;
        }
        @Override public void setLevel(Level level){super.setLevel(level);work.setLevel(level);}
        @Override public void tick(){
            super.tick();work.setBlockState(getBlockState());if(level==null||level.isClientSide)return;
            accountedRpm=OmniraKineticAccounting.refreshDynamicImpact(this,calculateStressApplied(),accountedRpm);work.tick();
        }
        @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet){super.write(tag,registries,packet);if(work!=null)work.saveTo(tag,registries);}
        @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);if(work!=null)work.loadWithComponents(tag,registries);}
    }
    private CodexKinetics(){}
}
