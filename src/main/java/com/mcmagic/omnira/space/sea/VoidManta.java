package com.mcmagic.omnira.space.sea;

import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class VoidManta extends StarSeaCreature implements PlayerRideableJumping {
    private boolean jumpRequested;
    public VoidManta(EntityType<? extends VoidManta> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return createMobAttributes().add(Attributes.MAX_HEALTH,40).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void registerGoals(){super.registerGoals();targetSelector.addGoal(2,new OwnerHurtByTargetGoal(this));targetSelector.addGoal(3,new OwnerHurtTargetGoal(this));}
    @Override public Vec3 attraction(){
        Player nearest=null;double distance=100;
        for(Player player:level().players()){
            if(!player.isAlive()||player.isSpectator()||!player.isHolding(com.mcmagic.omnira.registry.TimeNatureContent.SPATIAL_SHARD.get()))continue;
            double d=distanceToSqr(player);if(d>=distance||!hasLineOfSight(player))continue;
            Vec3 destination=player.position().add(0,.5,0);
            if(!safePath(destination.subtract(position())))continue;
            nearest=player;distance=d;
        }
        return nearest==null?null:distance<4?position():nearest.position().add(0,.5,0);
    }
    @Override public boolean wantsToAttack(LivingEntity target,LivingEntity owner){return target!=owner&&!isAlliedTo(target)&&!(target instanceof TamableAnimal animal&&animal.isTame())&&!(target instanceof Player other&&owner instanceof Player p&&!p.canHarmPlayer(other));}
    @Override protected void combat(LivingEntity target){
        if(attackReady()&&getBoundingBox().inflate(.8).intersects(target.getBoundingBox())&&hasLineOfSight(target)){doHurtTarget(target);attackCooldown(40);}
    }
    @Override public boolean doHurtTarget(Entity target){return target instanceof LivingEntity living&&canAttack(living)&&VoidSeaEffects.trueDamage(living,10,damageSources().mobAttack(this));}
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
        var held=player.getItemInHand(hand);
        if(!isTame()&&held.is(com.mcmagic.omnira.registry.TimeNatureContent.SPATIAL_SHARD.get())){
            if(!level().isClientSide){held.consume(1,player);if(random.nextInt(3)==0){tame(player);setTarget(null);level().broadcastEntityEvent(this,(byte)7);}else level().broadcastEntityEvent(this,(byte)6);}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(!isOwnedBy(player))return InteractionResult.PASS;
        if(!level().isClientSide){
            if(player.isShiftKeyDown()){setOrderedToSit(!isOrderedToSit());setTarget(null);setDeltaMovement(Vec3.ZERO);player.displayClientMessage(net.minecraft.network.chat.Component.translatable(isOrderedToSit()?"message.omnira.companion.stay":"message.omnira.companion.follow"),true);}
            else if(!isVehicle()){setOrderedToSit(false);setTarget(null);player.startRiding(this);}
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public LivingEntity getControllingPassenger(){return getFirstPassenger() instanceof Player p&&isOwnedBy(p)?p:null;}
    @Override protected boolean canAddPassenger(Entity e){return e instanceof Player p&&isOwnedBy(p)&&getPassengers().isEmpty();}
    @Override protected Vec3 getRiddenInput(Player p,Vec3 input){return new Vec3(p.xxa*.5,-Math.sin(p.getXRot()*Math.PI/180)*Math.max(0,p.zza),p.zza*(p.zza<0?.3:1));}
    @Override protected float getRiddenSpeed(Player p){return .32F;}
    @Override protected void tickRidden(Player p,Vec3 input){super.tickRidden(p,input);setRot(p.getYRot(),0);yBodyRot=yHeadRot=getYRot();if(jumpRequested){setDeltaMovement(getDeltaMovement().add(0,.25,0));jumpRequested=false;}}
    @Override public void onPlayerJump(int power){jumpRequested=power>=0;}
    @Override public boolean canJump(){return true;}
    @Override public void handleStartJump(int power){onPlayerJump(power);}
    @Override public void handleStopJump(){}
    @Override public void tick(){super.tick();if(!level().isClientSide&&tickCount%200==0)for(Entity e:getPassengers())if(e instanceof Player p)p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WATER_BREATHING,600,0,false,false));}
}
