package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import com.simibubi.create.content.kinetics.KineticNetwork;
import java.util.ArrayList;

/** Loaded only when Create exists; each physical port is a real stress consumer. */
public final class CreateReactionPool {
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
                &&!port.isOverStressed()&&Math.abs(port.getSpeed())>0)total+=port.demand/512.0;
        return total;
    }
    public static final class PoolBlock extends ReactionPoolBlock implements IRotate {
        public PoolBlock(Properties properties){super(properties);}
        public Direction.Axis getRotationAxis(BlockState state){return state.getValue(FACING).getClockWise().getAxis();}
        public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face){int part=state.getValue(PART);return (part==3||part==5)&&face==outward(state.getValue(FACING),part);}
    }
    public static final class Port extends KineticBlockEntity {
        private float demand;
        public Port(BlockPos pos,BlockState state){super(ModBlockEntityTypes.REACTION_POOL.get(),pos,state);}
        @Override protected void read(net.minecraft.nbt.CompoundTag tag,HolderLookup.Provider registries,boolean packet){
            super.read(tag,registries,packet);
            // Restore before Create rejoins the saved network and queries our stress coefficient.
            demand=Math.max(0,lastStressApplied*Math.abs(getTheoreticalSpeed()));
        }
        @Override public float calculateStressApplied(){float rpm=Math.abs(getTheoreticalSpeed());return lastStressApplied=rpm>0?demand/rpm:0;}
        private void allocate(KineticNetwork net,float next){
            boolean changed=demand!=next;demand=next;
            float coefficient=calculateStressApplied();
            if((double)coefficient*Math.abs(getTheoreticalSpeed())>next)coefficient=Math.nextDown(coefficient);
            lastStressApplied=coefficient;
            changed|=Float.compare(net.members.get(this),coefficient)!=0;
            net.members.put(this,coefficient);
            if(changed)sendData();
        }
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            if(hasNetwork())rebalance(getOrCreateNetwork());
            else if(demand!=0){demand=0;lastStressApplied=0;sendData();}
        }
    }
    private static void rebalance(KineticNetwork net){
        // Old saves may retain a ghost reservation. Only discard it when the topology
        // proves there can be no unloaded consumers, never for an open shaft network.
        if(net.getSize()==net.members.size()||closedRemoteDrive(net)){
            double loadedStress=0,loadedCapacity=0;
            for(var member:net.members.keySet())loadedStress+=net.getActualStressOf(member);
            for(var source:net.sources.keySet())loadedCapacity+=net.getActualCapacityOf(source);
            if(net.getSize()!=net.members.size()||Math.abs(net.calculateStress()-loadedStress)>.001
                    ||Math.abs(net.calculateCapacity()-loadedCapacity)>.001)net.initFromTE(0,0,0);
        }
        // Demand follows shaft RPM, not spare generator capacity. Create owns overload detection.
        var ports=new ArrayList<Port>();
        for(var member:net.members.keySet())if(member instanceof Port port){
            ports.add(port);
        }
        for(var port:ports){
            port.allocate(net,eligible(port)?32*Math.abs(port.getTheoreticalSpeed()):0);
        }
        net.updateNetwork();
    }
    private static boolean eligible(Port port){
        var pool=ReactionPoolBlock.controller(port.getLevel(),port.getBlockPos());
        return pool!=null&&pool.complete()&&Math.abs(port.getTheoreticalSpeed())>0;
    }
    private static boolean closedRemoteDrive(KineticNetwork net){
        if(net.members.size()!=4||net.sources.size()!=1)return false;
        for(var member:net.members.keySet())if(member instanceof Port poolPort){
            var level=poolPort.getLevel();var state=poolPort.getBlockState();
            var side=ReactionPoolBlock.outward(state.getValue(ReactionPoolBlock.FACING),state.getValue(ReactionPoolBlock.PART));
            var local=com.mcmagic.omnira.space.SpacetimePortState.at(level,poolPort.getBlockPos().relative(side));
            var remote=local==null?null:local.other();
            if(remote==null||!local.attached().equals(poolPort.getBlockPos())||!CreateSpacetimePort.shaft(local)||!CreateSpacetimePort.shaft(remote))return false;
            var engine=level.getBlockEntity(remote.attached());
            return engine instanceof com.mcmagic.omnira.block.entity.ManaEngineBlockEntity
                    &&net.sources.containsKey(engine)&&net.members.containsKey(local.host)&&net.members.containsKey(remote.host)
                    &&net.members.containsKey(engine);
        }
        return false;
    }
    private CreateReactionPool(){}
}
