package com.mcmagic.omnira.entity.spacetime;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.syncher.*;
import com.mcmagic.omnira.weaver.WeaverCombat;

public final class AgecoilSnail extends PathfinderMob {
    private static final EntityDataAccessor<Integer> SHELTER=SynchedEntityData.defineId(AgecoilSnail.class,EntityDataSerializers.INT);
    private LivingEntity aggressor;
    public AgecoilSnail(EntityType<? extends AgecoilSnail> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,16).add(Attributes.MOVEMENT_SPEED,.12).add(Attributes.FOLLOW_RANGE,24);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SHELTER,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,1));goalSelector.addGoal(4,new RandomLookAroundGoal(this));}
    public int shelterTicks(){return entityData.get(SHELTER);}
    @Override public boolean isPickable(){return shelterTicks()==0&&super.isPickable();}
    @Override public boolean isAttackable(){return shelterTicks()==0&&super.isAttackable();}
    @Override public boolean isInvulnerableTo(DamageSource source){return shelterTicks()>0||super.isInvulnerableTo(source);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(shelterTicks()>0)return false;
        boolean hit=super.hurt(source,amount);
        if(hit&&!level().isClientSide&&isAlive()&&source.getEntity() instanceof LivingEntity living&&living!=this){
            // More hits during this shelter do not restart its timer or queue extra shots.
            if(shelterTicks()==0){aggressor=living;entityData.set(SHELTER,20);}
        }
        return hit;
    }
    @Override public void tick(){
        super.tick();
        if(level().isClientSide){
            if(shelterTicks()==0&&tickCount%8==0&&getDeltaMovement().horizontalDistanceSqr()>.00001)
                level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.62F,.85F,.55F),.45F),getX(),getY()+.04,getZ(),0,0,0);
            return;
        }
        if(!isAlive())return;
        int remaining=shelterTicks();
        if(remaining>0){getNavigation().stop();setDeltaMovement(getDeltaMovement().multiply(0,1,0));entityData.set(SHELTER,remaining-1);
            if(remaining==1){
                if(aggressor!=null&&aggressor.isAlive()&&aggressor.level()==level()&&distanceToSqr(aggressor)<=64*64)
                    WeaverCombat.lockedRay(this,aggressor,1,.05F);
                aggressor=null;setTarget(null);setLastHurtByMob(null);
            }
        }else if(tickCount%20==0)getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(tickCount%240<40?.24:.12);
    }
}
