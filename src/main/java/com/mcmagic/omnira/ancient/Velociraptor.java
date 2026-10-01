package com.mcmagic.omnira.ancient;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Velociraptor extends AncientCompanion implements PlayerRideableJumping {
    private boolean jumpRequested;
    private int biteTicks;

    public Velociraptor(EntityType<? extends Velociraptor> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,6)
                .add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24).add(Attributes.STEP_HEIGHT,1);
    }
    @Override protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.2,true));
        goalSelector.addGoal(3,new FollowOwnerGoal(this,1,6,2));
        goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,8));
        goalSelector.addGoal(5,new RandomLookAroundGoal(this));
    }
    @Override protected InteractionResult ownerInteract(Player player, InteractionHand hand) {
        if (isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) {
            setOrderedToSit(false);
            setTarget(null);
            getNavigation().stop();
            player.startRiding(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player player && isOwnedBy(player) ? player : null;
    }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return passenger instanceof Player player && isOwnedBy(player) && getPassengers().isEmpty();
    }
    @Override protected Vec3 getRiddenInput(Player player, Vec3 input) {
        return new Vec3(player.xxa * .5,0,player.zza * (player.zza < 0 ? .25 : 1));
    }
    @Override protected float getRiddenSpeed(Player player) { return (float)getAttributeValue(Attributes.MOVEMENT_SPEED); }
    @Override protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player,input);
        setRot(player.getYRot(),player.getXRot()*.25F);
        yBodyRot = yHeadRot = getYRot();
        if (jumpRequested && onGround()) { jumpFromGround(); jumpRequested=false; }
    }
    @Override public void onPlayerJump(int power) { if (power >= 0) jumpRequested = true; }
    @Override public boolean canJump() { return true; }
    @Override public void handleStartJump(int power) { onPlayerJump(power); }
    @Override public void handleStopJump() { }
    @Override public boolean doHurtTarget(Entity target) {
        level().broadcastEntityEvent(this,(byte)4);
        return super.doHurtTarget(target);
    }
    @Override public void handleEntityEvent(byte event) {
        if (event == 4) biteTicks=8; else super.handleEntityEvent(event);
    }
    @Override public void tick() { super.tick(); if (biteTicks>0) biteTicks--; }
    public float bite(float partialTick) { return Math.max(0,biteTicks-partialTick)/8; }
}
