package com.mcmagic.omnira.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;

/** Keep Create subclasses out of common registration method signatures and stack frames. */
public final class CrystalWorkstationFactory {
    public static Block block(BlockBehaviour.Properties p,int index){return new KineticCrystalWorkstationBlock(p,index);}
    public static BlockEntity entity(BlockPos pos,BlockState state){return new KineticCrystalWorkstationEntity(pos,state);}
    private CrystalWorkstationFactory(){}
}
