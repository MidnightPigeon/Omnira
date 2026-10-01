package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class ShadowLanternBlock extends LanternBlock {
    public static final MapCodec<LanternBlock> CODEC=simpleCodec(ShadowLanternBlock::new);
    public static final BooleanProperty CHAIN=BooleanProperty.create("chain");
    public ShadowLanternBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(CHAIN,false));}
    @Override public MapCodec<LanternBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        super.createBlockStateDefinition(builder);builder.add(CHAIN);
    }
    private BlockState attachment(BlockState state,LevelReader level,BlockPos pos){
        var above=pos.above();var support=level.getBlockState(above);
        return state.setValue(CHAIN,state.getValue(HANGING)
                && !Block.isFaceFull(support.getBlockSupportShape(level,above),Direction.DOWN));
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var state=super.getStateForPlacement(context);
        return state==null?null:attachment(state,context.getLevel(),context.getClickedPos());
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        var updated=super.updateShape(state,direction,neighbor,level,pos,neighborPos);
        return updated.is(this)?attachment(updated,level,pos):updated;
    }
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved){
        super.onPlace(state,level,pos,old,moved);
        if(!level.isClientSide){var updated=attachment(state,level,pos);if(updated!=state)level.setBlock(pos,updated,2);}
    }
}
