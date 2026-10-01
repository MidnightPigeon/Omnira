package com.mcmagic.omnira.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.level.Level;

public final class ShadowMist extends Entity {
    public static final int LIFETIME=400, EXPANSION_TICKS=20;
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(ShadowMist.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLLAPSE_START=SynchedEntityData.defineId(ShadowMist.class,EntityDataSerializers.INT);
    public static final int COLLAPSE_TICKS=20;
    private static final EntityDataAccessor<Float> SPELL_SIZE=SynchedEntityData.defineId(ShadowMist.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> SPELL_LIFE=SynchedEntityData.defineId(ShadowMist.class,EntityDataSerializers.INT);
    private java.util.UUID caster;
    private float spellDamage;
    private boolean excludeOwner;
    public void excludeOwner(boolean value){excludeOwner=value;}
    private boolean seeking,holy;
    public boolean spellCloud(){return entityData.get(SPELL_SIZE)>0;}
    public int lifetime(){return spellCloud()?entityData.get(SPELL_LIFE):LIFETIME;}
    public void configureSpell(Entity owner,com.mcmagic.omnira.spell.SpellEffect effect,double power,double range,boolean holy) {
        caster=owner==null?null:owner.getUUID();this.holy=holy;seeking=effect.infusion()>0;
        spellDamage=(float)((1+effect.enhancement())*Math.max(0,power)*(holy?1.5:1));
        entityData.set(SPELL_SIZE,(float)(3*Math.clamp(range,.1,3)));
        entityData.set(SPELL_LIFE,400*(1+effect.delay()));
        refreshDimensions();
    }
    public ShadowMist(EntityType<? extends ShadowMist> type,Level level) {super(type,level);noPhysics=true;}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {builder.define(AGE,0);builder.define(COLLAPSE_START,-1);builder.define(SPELL_SIZE,0F);builder.define(SPELL_LIFE,400);}
    public int age() {return entityData.get(AGE);}
    public boolean collapsing() {return entityData.get(COLLAPSE_START)>=0;}
    public boolean consumeForConversion() {
        if(spellCloud() || level().isClientSide || !isAlive() || collapsing() || age()>=LIFETIME) return false;
        entityData.set(COLLAPSE_START,age());return true;
    }
    public float diameter(float partial) {
        if(spellCloud())return entityData.get(SPELL_SIZE);
        float expansionAge=collapsing()?entityData.get(COLLAPSE_START):age()+partial;
        return 1+2*Math.min(1,expansionAge/EXPANSION_TICKS);
    }
    public float opacity(float partial) {
        if(spellCloud())return Math.clamp((lifetime()-age()-partial)/20F,0,1);
        if(collapsing()) return Math.max(0,(1-entityData.get(COLLAPSE_START)/(float)LIFETIME)
                *(1-(age()+partial-entityData.get(COLLAPSE_START))/COLLAPSE_TICKS));
        return Math.max(0,1-(age()+partial)/LIFETIME);
    }
    @Override public EntityDimensions getDimensions(Pose pose) {float d=diameter(0);return EntityDimensions.scalable(d,d*.65F);}
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide) {
            entityData.set(AGE,age()+1);
            if((spellCloud()?age()>lifetime():age()>=lifetime()) || (collapsing() && age()-entityData.get(COLLAPSE_START)>=COLLAPSE_TICKS)) {discard();return;}
        }
        refreshDimensions();
        if(!level().isClientSide && spellCloud())tickSpell();
        if(!level().isClientSide && !spellCloud() && !collapsing() && age()%10==0) {
            double radius=diameter(0)/2;
            for(var entity:level().getEntitiesOfClass(LivingEntity.class,getBoundingBox(),e->e.isAlive()&&!e.isSpectator())) {
                double dx=entity.getX()-getX(),dz=entity.getZ()-getZ();
                double reach=radius+entity.getBbWidth()/2;
                if(dx*dx+dz*dz<=reach*reach) entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS,200,0));
            }
        }
        if(level().isClientSide && random.nextFloat()<opacity(0)*.5F) {
            double radius=diameter(0)*.5;
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.025F,.015F,.035F),.8F),
                    getX()+(random.nextDouble()-.5)*radius*2,getY()+random.nextDouble()*getBbHeight(),getZ()+(random.nextDouble()-.5)*radius*2,0,.015,0);
        }
    }
    private void tickSpell() {
        var server=(net.minecraft.server.level.ServerLevel)level();
        Entity owner=caster==null?null:server.getEntity(caster);
        if(seeking && age()%5==0) {
            var center=getBoundingBox().getCenter();
            var target=server.getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(5),
                    e->e.isAlive() && !e.isSpectator() && (!excludeOwner||!e.getUUID().equals(caster)) && e.getBoundingBox().getCenter().distanceToSqr(center)<=25)
                    .stream().filter(e->server.clip(new net.minecraft.world.level.ClipContext(center,e.getBoundingBox().getCenter(),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this)).getType()==net.minecraft.world.phys.HitResult.Type.MISS)
                    .min(java.util.Comparator.comparingDouble(e->e.distanceToSqr(this))).orElse(null);
            setDeltaMovement(target==null?net.minecraft.world.phys.Vec3.ZERO:target.getBoundingBox().getCenter().subtract(center).normalize().scale(.04));
        }
        if(seeking) {
            var end=position().add(getDeltaMovement());
            if(server.hasChunkAt(net.minecraft.core.BlockPos.containing(end))) {
                noPhysics=false;move(MoverType.SELF,getDeltaMovement());noPhysics=true;
            }
        }
        if(age()%20!=0)return;
        double radius=diameter(0)/2;
        for(var target:server.getEntitiesOfClass(LivingEntity.class,getBoundingBox(),e->e.isAlive()&&!e.isSpectator()&&(!excludeOwner||!e.getUUID().equals(caster)))) {
            double dx=target.getX()-getX(),dz=target.getZ()-getZ();
            if(dx*dx+dz*dz>radius*radius)continue;
            if(holy)com.mcmagic.omnira.spell.HolyMagic.touch(owner,target);
            float before=target.getHealth();
            boolean hurt=target.hurt(com.mcmagic.omnira.spell.SpellDamageSource.of(target,net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC,this,owner,"spell"),spellDamage);
            float actual=Math.max(0,before-target.getHealth());
            if(hurt && actual>0 && owner instanceof LivingEntity living && living.isAlive() && target!=owner)
                living.heal(holy?(float)Math.ceil(actual*1.5):actual);
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("MistAge",age());tag.putInt("CollapseStart",entityData.get(COLLAPSE_START));
        tag.putFloat("SpellSize",entityData.get(SPELL_SIZE));tag.putInt("SpellLife",lifetime());
        if(caster!=null)tag.putUUID("Caster",caster);
        tag.putFloat("SpellDamage",spellDamage);tag.putBoolean("Seeking",seeking);tag.putBoolean("Holy",holy);
        tag.putBoolean("ExcludeOwner",excludeOwner);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(SPELL_SIZE,Math.clamp(tag.getFloat("SpellSize"),0,9));
        entityData.set(SPELL_LIFE,Math.clamp(tag.getInt("SpellLife"),400,3600));
        caster=tag.hasUUID("Caster")?tag.getUUID("Caster"):null;
        spellDamage=Math.clamp(tag.getFloat("SpellDamage"),0,100000);seeking=tag.getBoolean("Seeking");holy=tag.getBoolean("Holy");
        excludeOwner=tag.getBoolean("ExcludeOwner");
        entityData.set(AGE,Math.clamp(tag.getInt("MistAge"),0,lifetime()));
        entityData.set(COLLAPSE_START,tag.contains("CollapseStart")?Math.clamp(tag.getInt("CollapseStart"),-1,age()):-1);
    }
}
