package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;

public class SpacetimePortBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=BlockStateProperties.FACING;
    public SpacetimePortBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(FACING,Direction.UP));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(SpacetimePortBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace());}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){var support=l.getBlockState(p.relative(s.getValue(FACING).getOpposite()));return !support.isAir()&&!support.is(com.mcmagic.omnira.registry.ModBlocks.SPACETIME_PORT.get());}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos np){return d==s.getValue(FACING).getOpposite()&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():s;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){
        case UP->box(4,0,4,12,1,12);case DOWN->box(4,15,4,12,16,12);
        case NORTH->box(4,4,15,12,12,16);case SOUTH->box(4,4,0,12,12,1);
        case WEST->box(15,4,4,16,12,12);case EAST->box(0,4,4,1,12,12);
    };}
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.CreateSpacetimePort.entity(p,s):new SpacetimePortBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return t==ModBlockEntityTypes.SPACETIME_PORT.get()?(world,pos,state,entity)->{if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.CreateSpacetimePort.tick(entity);else if(!world.isClientSide)((SpacetimePortHost)entity).port().tick();}:null;}
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof SpacetimePortHost host)host.port().unlink();super.onRemove(s,l,p,next,moving);}
    @Override protected BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override protected BlockState mirror(BlockState s,Mirror m){return rotate(s,m.getRotation(s.getValue(FACING)));}
}
