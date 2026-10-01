package com.mcmagic.omnira.shop;

import net.minecraft.world.phys.Vec3;

public final class MarisaDisplayMotion {
    private MarisaDisplayMotion(){}
    public static Vec3 position(int slot,double ticks){
        double angle=ticks*.012+slot*Math.PI/3;
        return new Vec3(.5+Math.cos(angle)*.18,.58+Math.sin(ticks*.027+slot*1.7)*.065,.5+Math.sin(angle)*.18);
    }
}
