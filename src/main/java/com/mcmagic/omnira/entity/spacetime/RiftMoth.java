package com.mcmagic.omnira.entity.spacetime;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class RiftMoth extends PathfinderMob {
    private static final EntityDataAccessor<Integer> REGION=SynchedEntityData.defineId(RiftMoth.class,EntityDataSerializers.INT);
    private UUID flock;
    public RiftMoth(EntityType<? extends RiftMoth> type,Level level){super(type,level);moveControl=new FlyingMoveControl(this,12,true);setNoGravity(true);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,6).add(Attributes.FLYING_SPEED,.18).add(Attributes.MOVEMENT_SPEED,.18);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(REGION,0);}
    @Override protected PathNavigation createNavigation(Level level){return new FlyingPathNavigation(this,level);}
    @Override protected void registerGoals(){goalSelector.addGoal(1,new PanicGoal(this,1.5));goalSelector.addGoal(3,new WaterAvoidingRandomFlyingGoal(this,.6));}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        boolean hit=super.hurt(source,amount);
        if(hit&&!level().isClientSide&&flock!=null&&source.getEntity() instanceof LivingEntity attacker)
            for(var member:level().getEntitiesOfClass(RiftMoth.class,getBoundingBox().inflate(12),m->flock.equals(m.flock)))member.setLastHurtByMob(attacker);
        return hit;
    }
    public void setFlock(UUID id){flock=id;} public UUID flock(){return flock;} public int region(){return entityData.get(REGION);}
    @Override public void tick(){
        super.tick();setNoGravity(true);fallDistance=0;
        if(level().isClientSide){
            if(tickCount%12==0)level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(region()==1?.75F:.6F,.8F,region()==1?.5F:1F),.4F),getX(),getY()+.15,getZ(),0,.005,0);
            return;
        }
        if(tickCount%40!=0)return;
        if(getLastHurtByMob()!=null&&tickCount-getLastHurtByMobTimestamp()>100)setLastHurtByMob(null);
        var biome=level().getBiome(blockPosition());entityData.set(REGION,biome.is(com.mcmagic.omnira.time.TimeMosquitoSpawning.BIOMES)?1:biome.is(SpacetimeFaunaSpawning.SPACE)?2:0);
        if(flock==null||getLastHurtByMob()!=null)return;
        var companions=level().getEntitiesOfClass(RiftMoth.class,getBoundingBox().inflate(12),m->m!=this&&flock.equals(m.flock));
        if(!companions.isEmpty()){
            var leader=companions.stream().min(java.util.Comparator.comparingInt(Entity::getId)).orElseThrow();
            if(leader.getId()<getId()&&distanceToSqr(leader)>9)getNavigation().moveTo(leader.getX()+random.nextDouble()*2-1,leader.getY()+random.nextDouble()- .5,leader.getZ()+random.nextDouble()*2-1,.8);
        }
    }
    @Override public void travel(Vec3 input){if(isEffectiveAi()){moveRelative(getSpeed(),input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.91));}else super.travel(input);}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override protected boolean shouldDropLoot(){return false;}
    @Override public void addAdditionalSaveData(CompoundTag t){super.addAdditionalSaveData(t);if(flock!=null)t.putUUID("Flock",flock);}
    @Override public void readAdditionalSaveData(CompoundTag t){super.readAdditionalSaveData(t);flock=t.hasUUID("Flock")?t.getUUID("Flock"):null;}
}
