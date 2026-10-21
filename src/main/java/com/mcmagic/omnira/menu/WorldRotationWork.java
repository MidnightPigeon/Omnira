package com.mcmagic.omnira.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/** Manual work lives in the open menu, not in the block entity. */
public interface WorldRotationWork {void worldRotation(ServerLevel level,BlockPos pos);}
