package com.mcmagic.omnira.storage;

import net.minecraft.core.BlockPos;

/** Fixed spatial phases preserve four transfers and one refresh per twenty ticks. */
public final class NodeSchedule {
    public static boolean due(long time,BlockPos pos,int period){return Math.floorMod(time,period)==Math.floorMod(pos.hashCode(),period);}
    private NodeSchedule(){}
}
