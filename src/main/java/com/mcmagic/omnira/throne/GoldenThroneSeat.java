package com.mcmagic.omnira.throne;

import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Invisible, nonpersistent mount; the player's persistent lock recreates it after login. */
public final class GoldenThroneSeat extends Entity {
    private BlockPos anchor=BlockPos.ZERO;
    private boolean toilet;
    private boolean riderPositioned;
    public GoldenThroneSeat(EntityType<? extends GoldenThroneSeat> type,Level level){
        super(type,level);setNoGravity(true);noPhysics=true;
    }
    public void place(BlockPos pos){
        anchor=pos;
        toilet=level().getBlockState(pos).is(ModBlocks.GOLDEN_TOILET.get());
        setPos(pos.getX()+.5,pos.getY()+(toilet?.6:.55),pos.getZ()+.5);
    }
    public BlockPos anchor(){return anchor;}
    public boolean isToilet(){return toilet;}
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
    @Override protected void readAdditionalSaveData(CompoundTag tag){}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide && (!isVehicle() || !level().getBlockState(anchor).is(
                toilet?ModBlocks.GOLDEN_TOILET.get():ModBlocks.GOLDEN_THRONE.get())))discard();
    }
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        if(!hasPassenger(passenger))return;
        if(riderPositioned && !level().isClientSide && passenger instanceof net.minecraft.server.level.ServerPlayer player
                && passenger.position().distanceToSqr(getX(),getY()+.05,getZ())>1){
            if(toilet)GoldenToilet.release(player);
            else GoldenThrone.release(player);
            return;
        }
        move.accept(passenger,getX(),getY()+.05,getZ());
        riderPositioned=true;
    }
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean dismountsUnderwater(){return false;}
}
