package com.mcmagic.omnira.compat.create;

/** A fixed machine requirement, not Create's configurable visual speed tiers. */
public final class CrystalShaftInput {
    public static final float MIN_RPM=16;
    public static boolean sufficient(float rpm){return Float.isFinite(rpm)&&Math.abs(rpm)>=MIN_RPM;}
    private CrystalShaftInput(){}
}
