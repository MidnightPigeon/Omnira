package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.pulse.*;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** The base's stress account is separate from the fixed-speed output network. */
public final class PulseOrbKinetics {
    private static final Map<KineticNetwork,InputGroup> INPUTS=new WeakHashMap<>();
    public static PulseOrbBlock block(boolean resonant){return new OrbBlock(resonant);}
    public static BlockEntity entity(BlockPos pos,BlockState state){return new Output(pos,state);}
    public static void tick(BlockEntity entity){((Output)entity).tick();}
    private static boolean shaft(LevelReader level,BlockPos pos,Direction face){
        if(!level.hasChunkAt(pos))return false;
        var state=level.getBlockState(pos);
        if(state.getBlock() instanceof OrbBlock)return false;
        if(state.getBlock() instanceof com.mcmagic.omnira.space.SpacetimePortBlock)
            return state.getValue(com.mcmagic.omnira.space.SpacetimePortBlock.FACING).getOpposite()==face;
        // Gearboxes have several perpendicular shafts; their nominal axis is not an outlet axis.
        return state.getBlock() instanceof IRotate rotate&&rotate.hasShaftTowards(level,pos,state,face);
    }
    private static final class OrbBlock extends PulseOrbBlock implements IRotate {
        OrbBlock(boolean resonant){super(resonant);}
        public Direction.Axis getRotationAxis(BlockState state){return state.getValue(MOUNT).getAxis();}
        public boolean hasShaftTowards(LevelReader level,BlockPos pos,BlockState state,Direction face){
            return face!=state.getValue(MOUNT).getOpposite()&&shaft(level,pos.relative(face),face.getOpposite())
                    &&(!(level instanceof Level world)||!PulseOutput.nativeConnection(world,pos,face));
        }
    }
    public static final class Output extends GeneratingKineticBlockEntity implements PulseOrbHost {
        private final PulseOrbEntity orb=new PulseOrbEntity(getBlockPos(),getBlockState(),this);
        private int provided;private float previousCapacity=Float.NaN,previousSpeed=Float.NaN;
        private KineticNetwork lastInput;
        Output(BlockPos pos,BlockState state){super(PulseContent.ENTITY.get(),pos,state);}
        public PulseOrbEntity orb(){return orb;}
        @Override public boolean isSpeedRequirementFulfilled(){var source=inputShaft(orb);return source==null||CrystalShaftInput.sufficient(source.getSpeed());}
        @Override public boolean addToTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip,boolean sneaking){
            boolean shown=super.addToTooltip(tooltip,sneaking);
            // The base is isolated: our output can be stopped while the actual input turns too slowly.
            var source=inputShaft(orb);
            if(!shown&&getSpeed()==0&&source!=null&&source.getSpeed()!=0&&!isSpeedRequirementFulfilled()){
                com.simibubi.create.foundation.utility.CreateLang.translate("tooltip.speedRequirement").style(net.minecraft.ChatFormatting.GOLD).forGoggles(tooltip);
                com.simibubi.create.foundation.utility.CreateLang.translate("gui.contraptions.not_fast_enough",getBlockState().getBlock().getName()).forGoggles(tooltip);
                return true;
            }return shown;
        }
        @Override public void setLevel(Level level){super.setLevel(level);orb.setLevel(level);}
        @Override public void onLoad(){super.onLoad();orb.onLoad();}
        @Override public void invalidate(){if(orb!=null)orb.setRemoved();super.invalidate();}
        @Override public void clearRemoved(){super.clearRemoved();orb.clearRemoved();}
        @Override public float getGeneratedSpeed(){return provided>0?16:0;}
        @Override public float calculateAddedStressCapacity(){return lastCapacityProvided=provided/16F;}
        @Override public float calculateStressApplied(){return lastStressApplied=0;}
        private boolean connected(KineticBlockEntity other){
            var delta=other.getBlockPos().subtract(worldPosition);
            if(delta.distManhattan(BlockPos.ZERO)!=1)return false;
            Direction face=Direction.getNearest(delta.getX(),delta.getY(),delta.getZ());
            return face!=orb.inlet()&&shaft(level,other.getBlockPos(),face.getOpposite())
                    &&!PulseOutput.nativeConnection(level,worldPosition,face);
        }
        @Override public boolean isCustomConnection(KineticBlockEntity other,BlockState here,BlockState there){return connected(other);}
        @Override public float propagateRotationTo(KineticBlockEntity other,BlockState here,BlockState there,BlockPos diff,boolean axis,boolean gears){return connected(other)?1:0;}
        @Override public List<BlockPos> addPropagationLocations(IRotate block,BlockState state,List<BlockPos> list){
            var result=super.addPropagationLocations(block,state,list);
            for(var face:Direction.values())if(face!=orb.inlet()&&shaft(level,worldPosition.relative(face),face.getOpposite())&&!result.contains(worldPosition.relative(face)))result.add(worldPosition.relative(face));
            return result;
        }
        @Override public void tick(){
            super.tick();if(level==null||level.isClientSide)return;
            var incoming=input(orb);var inputNetwork=incoming==null?null:incoming.getOrCreateNetwork();
            if(inputNetwork!=lastInput){lastInput=inputNetwork;PulseNetworks.changed(level,worldPosition);}
            int requested=0,transit=0;
            if(hasNetwork()){
                var net=getOrCreateNetwork();double demand=0,foreign=0;int relays=0;
                demand=PulseKinetics.independentStress(net);
                for(var source:List.copyOf(net.sources.keySet())){
                    if(source instanceof Output)relays++;else foreign+=net.getActualCapacityOf(source);
                }
                requested=(int)Math.ceil(Math.max(0,demand-foreign)/Math.max(1,relays));
                transit=(int)Math.ceil(Math.max(0,PulseOrbKinetics.importedStress(net)-Math.max(0,foreign-demand))/Math.max(1,relays));
            }
            int used=orb.conversion.reserve(PulseConversions.KINETIC,requested,false);
            // Idle capacity can start a shaft, but only actual load is reserved as MPU.
            provided=requested==0&&transit==0?orb.conversion.headroom(PulseConversions.KINETIC):Math.min(orb.capacity(),used+transit);
            if(getGeneratedSpeed()!=previousSpeed){previousSpeed=getGeneratedSpeed();updateGeneratedRotation();}
            float capacity=calculateAddedStressCapacity();
            if(capacity!=previousCapacity||hasNetwork()&&!Objects.equals(getOrCreateNetwork().sources.get(this),capacity)){
                previousCapacity=capacity;if(hasNetwork())notifyStressCapacityChange(capacity);sendData();
            }
            orb.tick();
        }
        @Override protected void write(CompoundTag tag,HolderLookup.Provider registries,boolean packet){
            super.write(tag,registries,packet);orb.saveState(tag,registries);tag.putInt("RelayCapacity",provided);
            if(packet)tag.put("SharedUpgrade",orb.upgrade.getStackInSlot(0).saveOptional(registries));
        }
        @Override protected void read(CompoundTag tag,HolderLookup.Provider registries,boolean packet){super.read(tag,registries,packet);orb.loadState(tag,registries);provided=tag.getInt("RelayCapacity");}
    }
    public static KineticBlockEntity inputShaft(PulseOrbEntity orb){
        var level=orb.getLevel();if(level==null||!orb.conversion.input(orb.inlet()))return null;
        if(level.isClientSide&&PulseOrbShaftState.mode(orb.shaftState,orb.inlet())!=PulseOutput.ROTATION)return null;
        var pos=orb.getBlockPos().relative(orb.inlet());var face=orb.inlet().getOpposite();
        if(!level.hasChunkAt(pos))return null;
        var port=com.mcmagic.omnira.space.SpacetimePortState.at(level,pos);
        if(port!=null){var other=port.other();if(port.face()!=orb.inlet()||other==null)return null;pos=other.attached();face=other.face();}
        if(!shaft(level,pos,face)||PulseOutput.nativeConnection(level,orb.getBlockPos(),orb.inlet()))return null;
        return level.getBlockEntity(pos) instanceof KineticBlockEntity kinetic?kinetic:null;
    }
    private static KineticBlockEntity input(PulseOrbEntity orb){
        var shaft=inputShaft(orb);
        return shaft!=null&&shaft.hasNetwork()&&CrystalShaftInput.sufficient(shaft.getSpeed())?shaft:null;
    }
    public static PulseImport inputGroup(PulseOrbEntity orb){
        var source=input(orb);if(source==null)return null;var network=source.getOrCreateNetwork();
        var group=INPUTS.computeIfAbsent(network,n->new InputGroup(n,source));
        return new PulseImport(BlockPos.of(network.id),group.receivers(),group.sources(),group::settle);
    }
    public static void maintain(net.minecraft.server.MinecraftServer server){
        for(var iterator=INPUTS.entrySet().iterator();iterator.hasNext();){var entry=iterator.next();var group=entry.getValue();
            if(group.level.getServer()!=server)continue;
            if(group.receivers().isEmpty()){group.close();iterator.remove();}
            else group.refresh();
        }
    }
    public static void clear(net.minecraft.server.MinecraftServer server){
        // ServerStopped runs after chunks close: never query kinetic members here.
        INPUTS.entrySet().removeIf(entry->entry.getValue().level.getServer()==server);
    }
    public static float importedStress(KineticNetwork network){var group=INPUTS.get(network);return group==null?0:group.debit;}
    private static final class InputGroup {
        final KineticNetwork network;final ServerLevel level;float debit;String topology="";List<PulseOrbEntity> previous;
        InputGroup(KineticNetwork network,KineticBlockEntity source){this.network=network;level=(ServerLevel)source.getLevel();previous=receivers();}
        List<PulseOrbEntity> receivers(){return PulseNetworks.relays(level).stream().filter(orb->{var k=input(orb);return k!=null&&k.getOrCreateNetwork()==network;}).toList();}
        double foreign(){
            double amount=0;for(var source:List.copyOf(network.sources.keySet()))if(!source.isRemoved()&&!PulseKinetics.nativeSource(source)&&!(source instanceof Output))amount+=network.getActualCapacityOf(source);
            return Math.max(0,amount-PulseKinetics.independentStress(network));
        }
        List<PulseImport.Source> sources(){
            var result=new ArrayList<PulseImport.Source>();
            for(var source:List.copyOf(network.sources.keySet()))if(!source.isRemoved()&&(PulseKinetics.nativeSource(source)||source instanceof Output))
                result.add(new PulseImport.Source(source.getBlockPos(),PulseKinetics.output(source),()->0,true));
            result.add(new PulseImport.Source(BlockPos.of(network.id),PulseConversions.IMPORT+2,this::foreign,false));
            return result;
        }
        void refresh(){
            var receivers=receivers();String next=receivers.stream().map(o->o.getBlockPos().toString()).sorted().toList()+":"+sources().stream().map(s->s.pos()+"/"+s.output()).sorted().toList();
            if(!next.equals(topology)){topology=next;for(var orb:previous)PulseNetworks.changed(level,orb.getBlockPos());for(var orb:receivers)PulseNetworks.changed(level,orb.getBlockPos());previous=receivers;}
        }
        void settle(double amount){
            float next=(float)Math.max(0,amount);if(Math.abs(next-debit)<.0001F)return;
            debit=next;network.updateStress();
        }
        void close(){
            for(var orb:previous)PulseNetworks.changed(level,orb.getBlockPos());
            if(debit!=0){debit=0;if(network.members.keySet().stream().allMatch(member->member.getLevel().hasChunkAt(member.getBlockPos())))network.updateStress();}
        }
    }
    private PulseOrbKinetics(){}
}
