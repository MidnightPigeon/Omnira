package com.mcmagic.omnira.space.sea;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class StarJellyfish extends StarSeaCreature implements Bucketable {
    private static final EntityDataAccessor<Integer> CHARGE=SynchedEntityData.defineId(StarJellyfish.class,EntityDataSerializers.INT);
    private boolean fromBucket;
    public StarJellyfish(EntityType<? extends StarJellyfish> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.ATTACK_DAMAGE,8).add(Attributes.MOVEMENT_SPEED,.2).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(CHARGE,0);}
    public int charge(){return entityData.get(CHARGE);}
    @Override public boolean canAttack(LivingEntity target){return !(target instanceof StarJellyfish)&&super.canAttack(target);}
    @Override protected void combat(LivingEntity target){
        if(!attackReady()||distanceToSqr(target)>25||!hasLineOfSight(target)){entityData.set(CHARGE,0);return;}
        int charge=charge()+1;entityData.set(CHARGE,charge);
        if(level() instanceof ServerLevel server&&charge%4==0)server.sendParticles(ParticleTypes.END_ROD,getX(),getY()+.7,getZ(),3,.25,.2,.25,.015);
        if(charge>=20){burst();entityData.set(CHARGE,0);attackCooldown(200);}
    }
    @Override protected void customServerAiStep(){super.customServerAiStep();if(getTarget()==null)entityData.set(CHARGE,0);}
    public void burst(){
        if(!(level() instanceof ServerLevel server))return;
        for(LivingEntity target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(5),e->e!=this&&e.isAlive())){
            var b=target.getBoundingBox();double d=position().distanceToSqr(Math.clamp(getX(),b.minX,b.maxX),Math.clamp(getY(),b.minY,b.maxY),Math.clamp(getZ(),b.minZ,b.maxZ));
            if(d<=25)VoidSeaEffects.trueDamage(target,8,damageSources().explosion(this,this));
        }
        for(int ring=0;ring<3;ring++)for(int i=0;i<32;i++){
            double angle=i*Math.PI/16,radius=2+ring*1.5;
            server.sendParticles(ParticleTypes.END_ROD,getX()+Math.cos(angle)*radius,getY()+.45+Math.sin(angle*2)*ring*.15,getZ()+Math.sin(angle)*radius,1,0,0,0,.025);
        }
        playSound(SoundEvents.AMETHYST_BLOCK_RESONATE,1,.7F);
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        if(isAlive()&&held.is(VoidSeaContent.BUCKET.get())){
            if(!level().isClientSide){var filled=getBucketItemStack();saveToBucketTag(filled);player.setItemInHand(hand,net.minecraft.world.item.ItemUtils.createFilledResult(held,player,filled,false));playSound(getPickupSound(),1,1);if(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)net.minecraft.advancements.CriteriaTriggers.FILLED_BUCKET.trigger(serverPlayer,filled);discard();}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return Bucketable.bucketMobPickup(player,hand,this).orElse(InteractionResult.PASS);
    }
    @Override public boolean fromBucket(){return fromBucket;}
    @Override public void setFromBucket(boolean value){fromBucket=value;if(value)setPersistenceRequired();}
    @Override public void saveToBucketTag(ItemStack stack){Bucketable.saveDefaultDataToBucketTag(this,stack);}
    @Override public void loadFromBucketTag(CompoundTag tag){Bucketable.loadDefaultDataFromBucketTag(this,tag);}
    @Override public ItemStack getBucketItemStack(){return new ItemStack(VoidSeaContent.JELLY_BUCKET.get());}
    @Override public SoundEvent getPickupSound(){return SoundEvents.BUCKET_FILL_FISH;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("FromBucket",fromBucket);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);setFromBucket(tag.getBoolean("FromBucket"));}
}
