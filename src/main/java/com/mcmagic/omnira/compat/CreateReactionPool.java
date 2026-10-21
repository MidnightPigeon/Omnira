package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** Loaded only when Create exists; each physical port is a real stress consumer. */
public final class CreateReactionPool {
    public static final float STRESS_PER_RPM=32;
    public static float inputStress(float rpm){return com.mcmagic.omnira.compat.create.CrystalShaftInput.sufficient(rpm)?STRESS_PER_RPM*Math.abs(rpm):0;}
    public static net.minecraft.world.level.block.Block block(BlockBehaviour.Properties properties){return new PoolBlock(properties);}
    public static BlockEntity entity(BlockPos pos,BlockState state){return new Port(pos,state);}
    public static void tick(BlockEntity entity){if(entity instanceof Port port)port.tick();}
    public static float shaftSpeed(ReactionPoolBlockEntity pool,int part){
        var at=pool.getBlockPos().offset(ReactionPoolBlock.offset(pool.getBlockState().getValue(ReactionPoolBlock.FACING),part));
        return pool.getLevel().getBlockEntity(at) instanceof Port port?port.getSpeed():0;
    }
    public static double power(ReactionPoolBlockEntity pool){
        double total=0;var facing=pool.getBlockState().getValue(ReactionPoolBlock.FACING);
        for(int part:new int[]{3,5})if(pool.getLevel().getBlockEntity(pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,part))) instanceof Port port
                &&port.hasNetwork()&&!port.isOverStressed()&&port.isSpeedRequirementFulfilled())total+=inputStress(port.getSpeed())/1024.0;
        return total;
    }
    public static final class PoolBlock extends ReactionPoolBlock implements IRotate {
        public PoolBlock(Properties properties){super(properties);}
        public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getClockWise().getAxis();}
        public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face){int part=state.getValue(PART);return (part==3||part==5)&&face==outward(state.getValue(FACING),part);}
    }
    public static final class Port extends KineticBlockEntity {
        private float accountedRpm=Float.NaN;
        public Port(BlockPos pos,BlockState state){super(ModBlockEntityTypes.REACTION_POOL.get(),pos,state);}
        @Override public boolean isSpeedRequirementFulfilled(){return com.mcmagic.omnira.compat.create.CrystalShaftInput.sufficient(getSpeed());}
        @Override public float calculateStressApplied(){return lastStressApplied=eligible(this)?STRESS_PER_RPM:0;}
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            if(hasNetwork()){
                com.mcmagic.omnira.compat.create.OmniraKineticAccounting.repairClosedNetwork(getOrCreateNetwork());
                accountedRpm=com.mcmagic.omnira.compat.create.OmniraKineticAccounting.refreshDynamicImpact(this,calculateStressApplied(),accountedRpm);
            }else lastStressApplied=0;
        }
    }
    private static boolean eligible(Port port){
        var pool=ReactionPoolBlock.controller(port.getLevel(),port.getBlockPos());
        return pool!=null&&pool.complete()&&com.mcmagic.omnira.compat.create.CrystalShaftInput.sufficient(port.getTheoreticalSpeed());
    }
    private CreateReactionPool(){}
}
