package com.mcmagic.omnira.entity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;

/** Replaces the per-tick navigation updates deliberately bypassed by phasing spirits. */
public final class SpiritFlightControl extends FlyingMoveControl {
    private int remaining;
    public SpiritFlightControl(Mob mob) {super(mob,20,true);}
    @Override public void setWantedPosition(double x,double y,double z,double speed) {
        super.setWantedPosition(x,y,z,speed);
        remaining=speed>0?200:0;
    }
    @Override public boolean hasWanted() {return remaining>0;}
    @Override public void tick() {
        if(remaining>0) {
            if(mob.distanceToSqr(wantedX,wantedY,wantedZ)<.16 || --remaining==0) {
                remaining=0;operation=Operation.WAIT;
                mob.setDeltaMovement(mob.getDeltaMovement().scale(.3));
            } else super.setWantedPosition(wantedX,wantedY,wantedZ,speedModifier);
        }
        super.tick();
    }
}
