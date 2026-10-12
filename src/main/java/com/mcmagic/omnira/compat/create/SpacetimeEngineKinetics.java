package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.engine.*;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** Four independent generators, each exposing only its outward shaft. */
public final class SpacetimeEngineKinetics {
    public static SpacetimeEngineBlock block(BlockBehaviour.Properties p){return new EngineBlock(p);}
    public static BlockEntity entity(BlockPos p,BlockState s){return new Output(p,s);}
    public static void tick(BlockEntity be){if(be instanceof Output output)output.tick();}
    private static final class EngineBlock extends SpacetimeEngineBlock implements IRotate {
        EngineBlock(Properties p){super(p);}
        public Direction.Axis getRotationAxis(BlockState s){int side=side(s.getValue(PART));return side<0?Direction.Axis.Y:OUTPUTS[side].getAxis();}
        public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction face){
            int side=side(s.getValue(PART));return side>=0&&face==OUTPUTS[side];
        }
    }
    public static final class Output extends GeneratingKineticBlockEntity {
        private float previousSpeed=Float.NaN,previousCapacity=Float.NaN;
        public Output(BlockPos p,BlockState s){super(SpacetimeEngineContent.OUTPUT.get(),p,s);}
        private SpacetimeEngineController controller(){return SpacetimeEngineBlock.controller(level,worldPosition);}
        private int side(){return SpacetimeEngineBlock.side(getBlockState().getValue(SpacetimeEngineBlock.PART));}
        @Override public float getGeneratedSpeed(){var be=controller();return be==null?0:be.generatedSpeed(side());}
        @Override public float calculateAddedStressCapacity(){
            var be=controller();float speed=getGeneratedSpeed();if(be==null||speed==0)return lastCapacityProvided=0;
            // Create multiplies source capacity by generated RPM, not driven RPM.
            float divisor=Math.abs(speed);
            float coefficient=be.capacity(side())/divisor;
            if((double)coefficient*divisor>be.capacity(side()))coefficient=Math.nextDown(coefficient);
            return lastCapacityProvided=coefficient;
        }
        @Override public float calculateStressApplied(){return lastStressApplied=0;}
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            float speed=getGeneratedSpeed();
            if(speed!=previousSpeed){previousSpeed=speed;updateGeneratedRotation();}
            float capacity=calculateAddedStressCapacity();
            if(capacity!=previousCapacity||hasNetwork()&&!java.util.Objects.equals(getOrCreateNetwork().sources.get(this),capacity)){
                previousCapacity=capacity;if(hasNetwork())notifyStressCapacityChange(capacity);setChanged();sendData();
            }
            if(hasNetwork()&&level.getGameTime()%20==0)OmniraKineticAccounting.repairClosedNetwork(getOrCreateNetwork());
        }
    }
    private SpacetimeEngineKinetics(){}
}
