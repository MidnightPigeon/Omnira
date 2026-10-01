package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A slow, unpowered mount. No storage, upgrades or passenger protection. */
public final class CrystalBroomEntity extends Entity {
    private int keys,interpolation;
    private int forwardTicks,liftTicks,lastDirection;
    private long lastInput;
    private Vec3 target=Vec3.ZERO;
    private float targetYaw;
    private float riderTurn;
    private int lastRiderTurnTick=-1;
    public CrystalBroomEntity(EntityType<? extends CrystalBroomEntity> type,Level level){super(type,level);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    public void input(ServerPlayer player,int mask){
        if(getFirstPassenger()!=player || mask<0 || mask>63)return;
        keys=player.containerMenu==player.inventoryMenu?mask:0;lastInput=level().getGameTime();
        if((keys&32)!=0){player.stopRiding();keys=0;}
    }
    public static Vec3 velocity(int keys,float yaw,double previousY,boolean water,int forwardTicks,int liftTicks){
        double forward=((keys&1)!=0?1:0)-((keys&2)!=0?1:0);
        double y=water?-.02:(keys&16)!=0?heldSpeed(liftTicks):Math.max(-.05,previousY-.004);
        var motion=new Vec3(0,y,-forward*heldSpeed(forwardTicks));
        if(!water && (keys&16)!=0 && motion.lengthSqr()>.01)motion=motion.normalize().scale(.1);
        return motion.yRot(-yaw*Mth.DEG_TO_RAD);
    }
    private static double heldSpeed(int ticks){return .05+.05*Mth.clamp(ticks-1,0,20)/20.0;}
    @Override public void tick(){
        super.tick();if(com.mcmagic.omnira.time.FleetingTime.bonus(this))return;riderTurn=0;
        if(level().isClientSide){
            if(interpolation>0){
                setPos(position().lerp(target,1.0/interpolation));float before=getYRot();
                setYRot(Mth.rotLerp(1F/interpolation,before,targetYaw));riderTurn=Mth.wrapDegrees(getYRot()-before);interpolation--;
            }
            if(isVehicle() && !onGround() && tickCount%5==0){
                int[] colors={0xE58265,0x79CFF2,0xDACE85,0x96D8B5};
                var tail=new Vec3(0,.2,.72).yRot(-getYRot()*Mth.DEG_TO_RAD).add(position());
                int color=colors[(tickCount/5)%4];
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(
                        (color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),.55F),tail.x,tail.y,tail.z,0,0,0);
            }
            return;
        }
        if(!isVehicle() || level().getGameTime()-lastInput>30)keys=0;
        float turn=(((keys&8)!=0?1:0)-((keys&4)!=0?1:0))*3;
        setYRot(getYRot()+turn);
        riderTurn=turn;
        int direction=((keys&1)!=0?1:0)-((keys&2)!=0?1:0);
        forwardTicks=direction==0?0:direction==lastDirection?Math.min(21,forwardTicks+1):1;
        liftTicks=(keys&16)!=0 && !isInWater()?Math.min(21,liftTicks+1):0;
        lastDirection=direction;
        var motion=velocity(keys,getYRot(),getDeltaMovement().y,isInWater(),forwardTicks,liftTicks);
        var box=getBoundingBox().move(motion);
        if(box.minY<level().getMinBuildHeight() || box.maxY>level().getMaxBuildHeight()
                || !level().getWorldBorder().isWithinBounds(box)
                || !level().hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))motion=Vec3.ZERO;
        setDeltaMovement(motion);move(MoverType.SELF,motion);
    }
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(player.isShiftKeyDown() || isVehicle())return InteractionResult.PASS;
        if(!level().isClientSide)player.startRiding(this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof Player && !isVehicle();}
    @Override public boolean isControlledByLocalInstance(){return false;}
    @Override public boolean dismountsUnderwater(){return false;}
    @Override public boolean canBeRiddenUnderFluidType(net.neoforged.neoforge.fluids.FluidType type,Entity rider){return true;}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        // The rendered seated pelvis is above the entity's feet; align it with the shaft.
        double seat=.25-(1.501-.75)*.9375+Math.sin(1.4137)*.125*.9375;
        if(hasPassenger(passenger)){
            move.accept(passenger,getX(),getY()+seat,getZ());
            if(lastRiderTurnTick!=tickCount){
                lastRiderTurnTick=tickCount;
                passenger.setYRot(passenger.getYRot()+riderTurn);
                passenger.setYHeadRot(passenger.getYHeadRot()+riderTurn);
            }
        }
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){return position();}
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){target=new Vec3(x,y,z);targetYaw=yaw;interpolation=Math.max(1,steps);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        if(level().isClientSide || isRemoved() || isVehicle() || !(source.getEntity() instanceof Player))return false;
        spawnAtLocation(ModItems.CRYSTAL_BROOM.get());discard();return true;
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){keys=0;}
}
