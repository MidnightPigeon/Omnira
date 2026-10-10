package com.mcmagic.omnira.alchemy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class EnhancedAlchemicalEnergyCoreBlock extends BaseEntityBlock {
    public static final MapCodec<EnhancedAlchemicalEnergyCoreBlock> CODEC=simpleCodec(EnhancedAlchemicalEnergyCoreBlock::new);
    public EnhancedAlchemicalEnergyCoreBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Core(pos,state);}
    public static final class Core extends BlockEntity {
        public Core(BlockPos pos,BlockState state){super(AlchemyMaterials.CORE_ENTITY.get(),pos,state);}
    }
}
