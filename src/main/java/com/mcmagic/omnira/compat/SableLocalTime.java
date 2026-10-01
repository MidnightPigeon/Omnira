package com.mcmagic.omnira.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;

public final class SableLocalTime {
    public static boolean inPlot(Level level,BlockPos pos){
        var container=SubLevelContainer.getContainer(level);
        return container!=null&&container.inBounds(pos)&&container.getPlot(new ChunkPos(pos))!=null;
    }
    private SableLocalTime(){}
}
