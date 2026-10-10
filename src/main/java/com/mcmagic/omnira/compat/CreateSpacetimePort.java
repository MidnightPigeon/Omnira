package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

/** Optional Create bridge: both endpoints must touch actual exposed shafts. */
public final class CreateSpacetimePort {
    public static net.minecraft.world.level.block.Block block(net.minecraft.world.level.block.state.BlockBehaviour.Properties p){return new PortBlock(p);}
    public static BlockEntity entity(BlockPos p,BlockState s){return new PortEntity(p,s);}
    public static void tick(BlockEntity e){((PortEntity)e).tick();}
    public static boolean shaft(SpacetimePortState p){
        var l=p.host.getLevel();if(l==null||!l.hasChunkAt(p.attached()))return false;
        var state=l.getBlockState(p.attached());
        return state.getBlock() instanceof IRotate rotate&&l.getBlockEntity(p.attached()) instanceof KineticBlockEntity
                &&rotate.hasShaftTowards(l,p.attached(),state,p.face());
    }
    public static final class PortBlock extends SpacetimePortBlock implements IRotate {
        public PortBlock(Properties p){super(p);}
        public Direction.Axis getRotationAxis(BlockState s){return s.getValue(FACING).getAxis();}
        public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction d){
            if(d!=s.getValue(FACING).getOpposite())return false;
            var state=l.getBlockState(p.relative(d));
            return state.getBlock() instanceof IRotate rotate&&rotate.hasShaftTowards(l,p.relative(d),state,d.getOpposite());
        }
    }
    public static final class PortEntity extends KineticBlockEntity implements SpacetimePortHost {
        private final SpacetimePortState port=new SpacetimePortState(this);
        private boolean connected;
        private boolean disconnecting;
        public PortEntity(BlockPos p,BlockState s){super(ModBlockEntityTypes.SPACETIME_PORT.get(),p,s);}
        public SpacetimePortState port(){return port;}
        private boolean linkedShaft(){var other=port.other();return other!=null&&shaft(port)&&shaft(other);}
        @Override public List<BlockPos> addPropagationLocations(IRotate block,BlockState state,List<BlockPos> list){
            var result=super.addPropagationLocations(block,state,list);
            if((linkedShaft()||disconnecting&&port.peer!=null)&&!result.contains(port.peer))result.add(port.peer);
            return result;
        }
        @Override public boolean isCustomConnection(KineticBlockEntity other,BlockState here,BlockState there){
            return other instanceof PortEntity&&other.getBlockPos().equals(port.peer)&&(linkedShaft()||disconnecting);
        }
        @Override public float propagateRotationTo(KineticBlockEntity other,BlockState here,BlockState there,BlockPos diff,boolean axis,boolean gears){
            return other instanceof PortEntity?isCustomConnection(other,here,there)?1:0:super.propagateRotationTo(other,here,there,diff,axis,gears);
        }
        public void detachPortKinetics(){
            if(level==null||level.isClientSide)return;
            // Removal must still traverse the old link after its support loses the shaft.
            disconnecting=connected;
            try{detachKinetics();}finally{disconnecting=false;}
        }
        public void attachPortKinetics(){if(level!=null&&!level.isClientSide){connected=linkedShaft();attachKinetics();}}
        @Override public void removeSource(){
            if(connected&&!disconnecting&&!linkedShaft()){
                detachPortKinetics();connected=false;
            }
            super.removeSource();
        }
        @Override public float calculateStressApplied(){return lastStressApplied=0;}
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            boolean next=linkedShaft();if(next!=connected){detachPortKinetics();connected=next;attachKinetics();}
            port.tick();
        }
        @Override protected void write(CompoundTag t,HolderLookup.Provider r,boolean packet){super.write(t,r,packet);port.write(t);}
        @Override protected void read(CompoundTag t,HolderLookup.Provider r,boolean packet){super.read(t,r,packet);port.read(t);}
    }
}
