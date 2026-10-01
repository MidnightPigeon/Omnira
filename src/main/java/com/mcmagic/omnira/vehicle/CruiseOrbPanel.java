package com.mcmagic.omnira.vehicle;

import org.joml.Matrix4f;

/** Shared physical panel transform for rendering and interaction. */
public final class CruiseOrbPanel {
    public static final float BUTTON_WIDTH=.125F, BUTTON_HEIGHT=.095F;
    public static final float BUTTON_X=.29F;
    public static final float SLOT_X=.27F, SLOT_SCALE=.72F;
    // Seat top minus the vanilla player's rendered hip offset, plus seated thigh thickness.
    public static final double RIDER_Y=1.0625-(1.501-.75)*.9375+Math.sin(1.4137)*.125*.9375;
    private CruiseOrbPanel() {}
    public static Matrix4f transform(){
        return new Matrix4f().translation(0,1.43F,-.95F)
                .rotateX((float)Math.toRadians(-18)).scale(.7F,1.12F,.8F)
                .translate(0,-1.65F,.95F);
    }
}
