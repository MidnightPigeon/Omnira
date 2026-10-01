package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.level.BlockGetter;

public final class ArcaneAssemblyTableBlock extends CrystalProcessingTableBlock {
    public static final MapCodec<ArcaneAssemblyTableBlock> CODEC=simpleCodec(ArcaneAssemblyTableBlock::new);
    public ArcaneAssemblyTableBlock(Properties properties) {super(properties);}
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new ArcaneAssemblyTableBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,com.mcmagic.omnira.registry.ModBlockEntityTypes.ARCANE_ASSEMBLY_TABLE.get(),
                (world,pos,block,entity)->{if(entity instanceof ArcaneAssemblyTableBlockEntity table) table.tickDirectDrive();});
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return AssemblyGeometry.shape(state.getValue(FACING));
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(level.getBlockEntity(pos) instanceof ArcaneAssemblyTableBlockEntity table && table.finished() && !player.isSpectator()) {
            if(!level.isClientSide) {
                table.collect(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useWithoutItem(state,level,pos,player,hit);
    }
}
