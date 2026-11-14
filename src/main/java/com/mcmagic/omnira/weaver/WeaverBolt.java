package com.mcmagic.omnira.weaver;

import com.mcmagic.omnira.entity.WeaverSpirit;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.phys.*;
import java.util.UUID;

public final class WeaverBolt extends Projectile {
    private static final EntityDataAccessor<Integer> GLYPH=SynchedEntityData.defineId(WeaverBolt.class,EntityDataSerializers.INT);
    private UUID targetId,immuneBoss;
    private static final double STRAIGHT_RANGE=128;
    private boolean straightShot;
    private Vec3 origin=Vec3.ZERO;
    public WeaverBolt(EntityType<? extends WeaverBolt> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(GLYPH,0);}
    public int glyph(){return entityData.get(GLYPH);}
    public void launch(LivingEntity owner,LivingEntity target,Vec3 at,Vec3 direction){
        straightShot=target==null && owner instanceof net.minecraft.world.entity.player.Player;
        setOwner(owner);setPos(at);origin=at;targetId=target==null?null:target.getUUID();immuneBoss=owner instanceof WeaverSpirit?owner.getUUID():null;
        entityData.set(GLYPH,random.nextInt(26));setDeltaMovement(direction.normalize().scale(.8));
    }
    static LivingEntity contactTarget(Entity contact) {
        if (contact instanceof net.minecraft.world.entity.boss.EnderDragonPart part) return part.parentMob;
        if (contact instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) return null;
        return contact instanceof LivingEntity living ? living : null;
    }
    @Override protected boolean canHitEntity(Entity contact) {
        var target = contactTarget(contact);
        return target != null && target.isAlive() && !target.getUUID().equals(immuneBoss) && super.canHitEntity(contact);
    }
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide){
            if(straightShot ? position().distanceTo(origin)>=STRAIGHT_RANGE-1e-7 || tickCount>160 : tickCount>100 || position().distanceToSqr(origin)>64*64){discard();return;}
            if(targetId!=null&&level() instanceof ServerLevel level&&level.getEntity(targetId) instanceof LivingEntity target&&target.isAlive()){
                Entity aim = target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon ? dragon.head : target;
                var desired=aim.getBoundingBox().getCenter().subtract(position()).normalize().scale(.8);
                setDeltaMovement(getDeltaMovement().scale(.7).add(desired.scale(.3)).normalize().scale(.8));
            }
            Vec3 end=position().add(getDeltaMovement());
            var hit=ProjectileUtil.getEntityHitResult(level(),this,position(),end,getBoundingBox().expandTowards(getDeltaMovement()).inflate(.3),this::canHitEntity);
            if(hit!=null){hitTargetOrDeflectSelf(hit);if(isRemoved())return;}
        }
        move(MoverType.SELF,getDeltaMovement());updateRotation();
    }
    @Override protected void onHitEntity(EntityHitResult hit){
        var contact=hit.getEntity();
        var target=contactTarget(contact);
        if(target!=null)impact(target,contact);
    }
    public boolean impact(LivingEntity target){return impact(target,target);}
    private boolean impact(LivingEntity target,Entity contact){
        if(target.getUUID().equals(immuneBoss))return false;
        if(!(getOwner() instanceof LivingEntity owner)){discard();return false;}
        var physical=WeaverCombat.source(target,DamageTypes.MOB_PROJECTILE,this,owner,true,false);
        float amount=WeaverCombat.damage(owner,3);
        // The shield receives both channels together; the shared hurt entry adds the other flat bonus.
        if(target.isDamageSourceBlocked(physical))target.hurt(physical,amount*2+com.mcmagic.omnira.combat.HeldCombatAttributes.flatBonus(owner));
        else{
            contact.hurt(physical,amount);
            if(target.isAlive())contact.hurt(WeaverCombat.source(target,DamageTypes.INDIRECT_MAGIC,this,owner,true,false),amount);
        }
        discard();return true;
    }
    @Override protected void onDeflection(Entity entity,boolean byPlayer){super.onDeflection(entity,byPlayer);targetId=null;}
    @Override protected void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("StraightShot",straightShot);if(targetId!=null)tag.putUUID("Target",targetId);if(immuneBoss!=null)tag.putUUID("ImmuneBoss",immuneBoss);tag.putInt("Glyph",glyph());tag.putDouble("OriginX",origin.x);tag.putDouble("OriginY",origin.y);tag.putDouble("OriginZ",origin.z);tag.putInt("Age",tickCount);}
    @Override protected void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);straightShot=tag.getBoolean("StraightShot");targetId=tag.hasUUID("Target")?tag.getUUID("Target"):null;immuneBoss=tag.hasUUID("ImmuneBoss")?tag.getUUID("ImmuneBoss"):null;entityData.set(GLYPH,Math.clamp(tag.getInt("Glyph"),0,25));origin=new Vec3(tag.getDouble("OriginX"),tag.getDouble("OriginY"),tag.getDouble("OriginZ"));tickCount=Math.max(0,tag.getInt("Age"));}
}
