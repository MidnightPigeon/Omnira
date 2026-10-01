package com.mcmagic.omnira.archaeology;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Constant-speed excavation, independent of whether a layer contains blocks or air. */
public final class MovingDrill extends Entity {
    private java.util.UUID operator;
    private String operatorName="Drill";
    private int floor,clearedThrough;
    private Vec3 interpolationTarget=Vec3.ZERO;
    private int interpolationTicks;
    public MovingDrill(EntityType<? extends MovingDrill> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    public void start(BlockPos origin,Player owner){setPos(origin.getX()+.5,origin.getY(),origin.getZ()+.5);operator=owner.getUUID();operatorName=owner.getGameProfile().getName();floor=HyperDrillBlockEntity.lowerLimit(level(),origin.getY());clearedThrough=origin.getY();}
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
    @Override protected void addAdditionalSaveData(CompoundTag n){if(operator!=null)n.putUUID("Operator",operator);n.putString("OperatorName",operatorName);n.putInt("Floor",floor);n.putInt("ClearedThrough",clearedThrough);}
    @Override protected void readAdditionalSaveData(CompoundTag n){operator=n.hasUUID("Operator")?n.getUUID("Operator"):null;operatorName=n.getString("OperatorName");floor=n.getInt("Floor");clearedThrough=n.getInt("ClearedThrough");}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){interpolationTarget=new Vec3(x,y,z);interpolationTicks=Math.max(1,steps);}
    @Override public void tick(){
        super.tick();if(com.mcmagic.omnira.time.FleetingTime.bonus(this))return;
        if(level().isClientSide){if(interpolationTicks>0){setPos(position().lerp(interpolationTarget,1.0/interpolationTicks));interpolationTicks--;}return;}
        double next=getY()-.1;
        if(next<floor-1.0e-6){finish();return;}
        int layer=(int)Math.floor(next+1.0e-6);
        if(layer<clearedThrough){if(!clearLayer(layer)){finish();return;}clearedThrough=layer;}
        setPos(getX(),Math.max(floor,next),getZ());
    }
    private boolean clearLayer(int y){
        var level=(ServerLevel)level();if(operator==null)return false;
        Player owner=level.getServer().getPlayerList().getPlayer(operator);
        if(owner==null)owner=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(operator,operatorName));
        var center=BlockPos.containing(getX(),y,getZ());
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var p=center.offset(x,0,z);var state=level.getBlockState(p);
            if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||state.getDestroySpeed(level,p)<0||!level.mayInteract(owner,p)
                ||net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,p,state,owner)).isCanceled())return false;
        }
        if(!DrillFluidSeal.seal(level,center,owner))return false;
        DrillWorkEvents.ERASING.set(true);
        try{for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var p=center.offset(x,0,z);var state=level.getBlockState(p);
            if(!state.isAir())level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,state),p.getX()+.5,p.getY()+.8,p.getZ()+.5,4,.3,.1,.3,.04);
            level.removeBlockEntity(p);level.setBlock(p,Blocks.AIR.defaultBlockState(),18);
        }}finally{DrillWorkEvents.ERASING.remove();}
        return true;
    }
    private void finish(){
        var root=BlockPos.containing(getX(),Math.ceil(getY()-1.0e-6),getZ());
        // Falling sand may refill an already excavated cell before the rig stops.
        for(int dy=0;dy<2;dy++){
            boolean blocked=false;
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!level().getBlockState(root.offset(x,dy,z)).canBeReplaced())blocked=true;
            if(blocked&&!clearLayer(root.getY()+dy))return;
        }
        HyperDrillBlockEntity.install(level(),root,0);
        for(var rider:java.util.List.copyOf(getPassengers())){var seat=ArchaeologyContent.SEAT.get().create(level());if(seat!=null){seat.place(root);level().addFreshEntity(seat);rider.startRiding(seat,true);}}
        discard();
    }
    @Override protected void positionRider(Entity rider,MoveFunction move){if(hasPassenger(rider))move.accept(rider,getX(),getY()+1.72,getZ()+.2);}
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){return position().add(1.1,1.72,0);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean dismountsUnderwater(){return false;}
}
