package com.mcmagic.omnira.space.sea;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.shapes.*;

public final class StarlightBlock extends BaseEntityBlock {
    public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 2);
    public static final IntegerProperty FLUID_LEVEL = IntegerProperty.create("fluid_level", 0, 15);
    private final boolean source;
    public StarlightBlock(boolean source) {
        super(Properties.of().strength(source ? 1.5F : 0F).noOcclusion().noCollission().lightLevel(s -> source ? 15 : 12));
        this.source = source;
        registerDefaultState(stateDefinition.any().setValue(WATER, 0).setValue(FLUID_LEVEL,0));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(WATER,FLUID_LEVEL); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return source ? box(3, 3, 3, 13, 13, 13) : box(1, 0, 1, 15, 1, 15);
    }
    @Override public BlockEntity newBlockEntity(BlockPos p, BlockState s) { return new StarlightEntity(p, s); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return !l.isClientSide && source ? createTickerHelper(t, StarlightContent.ENTITY.get(), StarlightEntity::tick) : null;
    }
    @Override protected FluidState getFluidState(BlockState s) {
        return switch (s.getValue(WATER)) {
            case 1 -> Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,s.getValue(FLUID_LEVEL)).getFluidState();
            case 2 -> VoidSeaContent.LIQUID.get().defaultBlockState().setValue(LiquidBlock.LEVEL,s.getValue(FLUID_LEVEL)).getFluidState();
            default -> Fluids.EMPTY.defaultFluidState();
        };
    }
    @Override protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos q) {
        if (s.getValue(WATER) != 0) l.scheduleTick(p, s.getFluidState().getType(), 5);
        return super.updateShape(s, d, n, l, p, q);
    }
    @Override public void animateTick(BlockState s, Level l, BlockPos p, RandomSource r) {
        if (!source) {
            if (r.nextInt(4)==0) l.addParticle(ParticleTypes.END_ROD,p.getX()+.1+r.nextDouble()*.8,
                    p.getY()+.04,p.getZ()+.1+r.nextDouble()*.8,0,.006,0);
            return;
        }
        double a = r.nextDouble() * Math.PI * 2, y = r.nextDouble() * 2 - 1, h = Math.sqrt(1 - y*y);
        double x = Math.cos(a)*h, z = Math.sin(a)*h;
        l.addParticle(ParticleTypes.END_ROD, p.getX()+.5+x*.2, p.getY()+(source?.5:.25)+y*.2,
                p.getZ()+.5+z*.2, x*.025, y*.025, z*.025);
    }
}
