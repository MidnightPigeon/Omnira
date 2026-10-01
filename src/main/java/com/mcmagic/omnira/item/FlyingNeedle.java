package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** The held item stays with its owner; this is a visual projectile, never a second item. */
public final class FlyingNeedle extends ThrowableItemProjectile {
    private ItemStack weapon=ItemStack.EMPTY;
    private int weaponSlot;
    private Vec3 origin=Vec3.ZERO,pullTarget,impact;
    private boolean returning;
    private int stuck;
    private double flightRange=10;
    public double flightRange() {return flightRange;}
    public static boolean isOut(net.minecraft.world.entity.player.Player player) {
        return !player.level().getEntitiesOfClass(FlyingNeedle.class,player.getBoundingBox().inflate(64),
                needle->!needle.isRemoved() && needle.getOwner()==player).isEmpty();
    }
    public FlyingNeedle(EntityType<? extends FlyingNeedle> type,Level level) {super(type,level);}
    @Override protected Item getDefaultItem() {return ModItems.ARCANE_NEEDLE.get();}
    @Override protected double getDefaultGravity() {return 0;}
    @Override public boolean isPushedByFluid() {return false;}
    public void launch(ServerPlayer owner,ItemStack weapon) {
        setOwner(owner);this.weapon=weapon;weaponSlot=owner.getInventory().selected;
        flightRange=10*SwordActions.reachMultiplier(owner);
        origin=owner.getEyePosition();setPos(origin);setDeltaMovement(owner.getLookAngle());
    }
    @Override protected void onHit(HitResult hit) {
        if(level().isClientSide || returning || pullTarget!=null || !(getOwner() instanceof ServerPlayer p))return;
        if(hit instanceof EntityHitResult entity)SwordActions.strike(p,entity.getEntity(),false);
        Vec3 direction=getDeltaMovement().normalize();
        pullTarget=findLanding(p,hit.getLocation(),direction);
        if(pullTarget==null)returning=true;
        impact=hit.getLocation();setPos(impact);setDeltaMovement(Vec3.ZERO);
    }
    private Vec3 findLanding(ServerPlayer p,Vec3 hit,Vec3 direction) {
        // A hit is not always at eye height. Search upwards too, so a floor or a
        // short mob does not place the destination below the supporting terrain.
        for(double retreat=.65;retreat<=3;retreat+=.25) {
            Vec3 base=hit.subtract(direction.scale(retreat)).add(0,-p.getEyeHeight(),0);
            for(double lift=0;lift<=2;lift+=.125) {
                Vec3 feet=base.add(0,lift,0);
                if(level().noCollision(p,p.getBoundingBox().move(feet.subtract(p.position()))))return feet;
            }
        }
        return null;
    }
    @Override public void tick() {
        if(!level().isClientSide) {
            if(!(getOwner() instanceof ServerPlayer p) || !p.isAlive() || p.level()!=level()
                    || p.getInventory().selected!=weaponSlot || !p.getMainHandItem().is(weapon.getItem())
                    || p.isPassenger() || p.isUsingItem() || tickCount>80) {discard();return;}
            if(pullTarget!=null) {
                tickCount++;
                Vec3 delta=pullTarget.subtract(p.position());
                if(delta.length()<.35){discard();return;}
                Vec3 before=p.position();
                p.move(MoverType.SELF,delta.normalize().scale(Math.min(.9,delta.length())));
                p.fallDistance=0;p.setDeltaMovement(Vec3.ZERO);
                p.connection.teleport(p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());
                p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(p));
                stuck=p.position().distanceToSqr(before)<.001?stuck+1:0;
                if(stuck>2)discard();return;
            }
            if(returning) {
                if(position().distanceToSqr(p.getEyePosition())<.6){discard();return;}
                setDeltaMovement(p.getEyePosition().subtract(position()).normalize().scale(1.2));
            } else {
                var path=getDeltaMovement().normalize();
                for(double d=0;d<=1;d+=.2){
                    var pos=net.minecraft.core.BlockPos.containing(position().add(path.scale(d)));
                    if(com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(p.serverLevel(),pos,p)){
                        returning=true;break;
                    }
                }
                double remaining=flightRange-position().distanceTo(origin);
                if(remaining<=.001){returning=true;setDeltaMovement(p.getEyePosition().subtract(position()).normalize().scale(1.2));}
                else setDeltaMovement(getDeltaMovement().normalize().scale(Math.min(1,remaining)));
            }
        }
        super.tick();
        if(!level().isClientSide && pullTarget!=null && impact!=null)setPos(impact);
    }
}
