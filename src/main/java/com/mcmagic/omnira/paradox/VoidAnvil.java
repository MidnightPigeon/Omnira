package com.mcmagic.omnira.paradox;

import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Native anvil physics without a placed block or recoverable item. */
public final class VoidAnvil extends FallingBlockEntity {
    private static final EntityDataAccessor<Integer> WAIT=SynchedEntityData.defineId(VoidAnvil.class,EntityDataSerializers.INT);
    public VoidAnvil(EntityType<? extends VoidAnvil> type,Level level){
        super(type,level);
        var initial=new CompoundTag();initial.put("BlockState",NbtUtils.writeBlockState(Blocks.ANVIL.defaultBlockState()));
        super.readAdditionalSaveData(initial);dropItem=false;disableDrop();setHurtsEntities(2,40);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(WAIT,60);}
    public int remainingWait(){return entityData.get(WAIT);}
    @Override public void tick(){
        if(remainingWait()>0){
            tickCount++;setDeltaMovement(Vec3.ZERO);
            if(!level().isClientSide)entityData.set(WAIT,remainingWait()-1);
            return;
        }
        super.tick();
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Suspension",remainingWait());}
    @Override protected void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);entityData.set(WAIT,Math.clamp(tag.getInt("Suspension"),0,60));
        dropItem=false;disableDrop();setHurtsEntities(2,40);
    }
    @Override protected net.minecraft.network.chat.Component getTypeName(){return getType().getDescription();}
}
