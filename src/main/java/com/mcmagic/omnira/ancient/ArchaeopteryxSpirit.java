package com.mcmagic.omnira.ancient;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class ArchaeopteryxSpirit extends AncientCompanion {
    private int attackCooldown;

    public ArchaeopteryxSpirit(EntityType<? extends ArchaeopteryxSpirit> type, Level level) {
        super(type,level);
        setNoGravity(true);
        moveControl = new FlightControl();
    }
    public static AttributeSupplier.Builder attributes() {
        return createMobAttributes().add(Attributes.MAX_HEALTH,16).add(Attributes.ATTACK_DAMAGE,6)
                .add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FLYING_SPEED,.4).add(Attributes.FOLLOW_RANGE,32);
    }
    @Override protected PathNavigation createNavigation(Level level) { return new FlyingPathNavigation(this,level); }
    @Override public void tick() {
        noPhysics=true;
        super.tick();
        noPhysics=false;
        setNoGravity(true);
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackCooldown>0) attackCooldown--;
        if (isOrderedToSit()) {
            ((FlightControl)moveControl).hold();
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        LivingEntity target=getTarget();
        if (target != null && target.isAlive() && canAttack(target)) {
            moveControl.setWantedPosition(target.getX(),target.getY()+target.getBbHeight()*.5,target.getZ(),1.5);
            if (attackCooldown==0 && getBoundingBox().inflate(.3).intersects(target.getBoundingBox())) {
                doHurtTarget(target);
                attackCooldown=20;
                setDeltaMovement(getDeltaMovement().scale(-.5));
            }
        } else if (getOwner() != null) {
            LivingEntity owner=getOwner();
            if (distanceToSqr(owner)>144 && !isLeashed() && !isPassenger()) {
                teleportTo(owner.getX(),owner.getY()+2,owner.getZ());
            } else if (distanceToSqr(owner)>9) {
                moveControl.setWantedPosition(owner.getX(),owner.getY()+1.8,owner.getZ(),1);
            } else setDeltaMovement(getDeltaMovement().scale(.8));
        } else setDeltaMovement(getDeltaMovement().scale(.8));
    }
    private final class FlightControl extends MoveControl {
        FlightControl() { super(ArchaeopteryxSpirit.this); }
        void hold() { operation=Operation.WAIT; }
        @Override public void tick() {
            if (operation != Operation.MOVE_TO) return;
            Vec3 delta=new Vec3(wantedX-getX(),wantedY-getY(),wantedZ-getZ());
            double length=delta.length();
            if(length<.3) { operation=Operation.WAIT; setDeltaMovement(getDeltaMovement().scale(.5)); }
            else {
                setDeltaMovement(getDeltaMovement().add(delta.scale(speedModifier*.05/length)));
                Vec3 velocity=getDeltaMovement();
                setYRot((float)(-Math.atan2(velocity.x,velocity.z)*180/Math.PI));
                yBodyRot=getYRot();
            }
        }
    }
    @Override public boolean isInWall() { return false; }
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source) { return false; }
}
