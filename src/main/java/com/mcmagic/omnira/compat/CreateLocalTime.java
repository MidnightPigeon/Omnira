package com.mcmagic.omnira.compat;

import net.minecraft.world.entity.Entity;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;

public final class CreateLocalTime {
    public static boolean assembled(Entity entity){return entity instanceof AbstractContraptionEntity;}
    private CreateLocalTime(){}
}
