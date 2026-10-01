package com.mcmagic.omnira.archaeology;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

public final class DrillSeat extends Entity {
    private BlockPos anchor=BlockPos.ZERO;
    public DrillSeat(EntityType<? extends DrillSeat> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    public BlockPos anchor(){return anchor;}
    public void place(BlockPos root){place(root,0);}
    public void place(BlockPos root,float descent){anchor=root.immutable();setPos(root.getX()+.5,root.getY()+1.72-descent,root.getZ()+.7);}
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public void tick(){super.tick();if(!level().isClientSide&&(!isVehicle()||!(level().getBlockEntity(anchor) instanceof HyperDrillBlockEntity)))discard();}
    @Override protected void positionRider(Entity rider,MoveFunction move){if(hasPassenger(rider))move.accept(rider,getX(),getY(),getZ());}
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean dismountsUnderwater(){return false;}
    @Override public net.minecraft.world.phys.Vec3 getDismountLocationForPassenger(LivingEntity passenger){return position().add(1,0,0);}
}
