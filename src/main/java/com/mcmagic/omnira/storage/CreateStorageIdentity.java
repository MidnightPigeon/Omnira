package com.mcmagic.omnira.storage;

import com.simibubi.create.foundation.blockEntity.IMultiBlockEntityContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

final class CreateStorageIdentity {
    static BlockPos controller(BlockEntity block,BlockPos fallback){
        if(block instanceof IMultiBlockEntityContainer container){
            var controller=container.getController();if(controller!=null)return controller;
        }
        return fallback;
    }
    private CreateStorageIdentity(){}
}
