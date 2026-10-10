package com.mcmagic.omnira.compat.create;

import com.mcmagic.omnira.workstation.*;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

public final class KineticCrystalWorkstationBlock extends CrystalWorkstationBlock implements IRotate {
    public KineticCrystalWorkstationBlock(Properties p,int index){super(p,index);}
    @Override public Direction.Axis getRotationAxis(BlockState s){return s.getValue(FACING).getAxis();}
    @Override public boolean hasShaftTowards(LevelReader l,BlockPos p,BlockState s,Direction face){return shaft(s,face);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){
        return t==CrystalWorkstations.ENTITY.get()?(level,pos,state,be)->((KineticCrystalWorkstationEntity)be).tick():null;
    }
}
