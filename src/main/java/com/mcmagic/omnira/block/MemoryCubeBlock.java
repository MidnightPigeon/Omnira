package com.mcmagic.omnira.block;

import com.mcmagic.omnira.item.MemoryCubeBlockItem;
import com.mcmagic.omnira.mire.MemoryCubeBlockEntity;
import com.mcmagic.omnira.mire.MemoryCubeRewards;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class MemoryCubeBlock extends BaseEntityBlock {
    public static final MapCodec<MemoryCubeBlock> CODEC=simpleCodec(MemoryCubeBlock::new);
    public static final BooleanProperty PEACEFUL=BooleanProperty.create("peaceful");
    private static final VoxelShape SHAPE=Block.box(4,2,4,12,10,12);
    public MemoryCubeBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(PEACEFUL,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(PEACEFUL);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new MemoryCubeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,ModBlockEntityTypes.MEMORY_CUBE.get(),(world,pos,current,cube)->cube.tick());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        if(random.nextInt(2)!=0)return;
        double angle=random.nextDouble()*Math.PI*2;
        double radius=.28+random.nextDouble()*.13;
        double x=Math.cos(angle)*radius,z=Math.sin(angle)*radius;
        level.addParticle(state.getValue(PEACEFUL)?ModParticles.PEACEFUL_MEMORY_GLYPH.get():ModParticles.CORRUPTED_MEMORY_GLYPH.get(),
                pos.getX()+.5+x,pos.getY()+.35+random.nextDouble()*.4,pos.getZ()+.5+z,-z*.04,.006,x*.04);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder params){
        return List.of(MemoryCubeBlockItem.withState(new ItemStack(ModItems.MEMORY_CUBE.get()),state.getValue(PEACEFUL)));
    }
    @Override public ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state){
        return MemoryCubeBlockItem.withState(new ItemStack(ModItems.MEMORY_CUBE.get()),state.getValue(PEACEFUL));
    }
    private static InteractionResult extract(BlockState state,Level level,BlockPos pos,Player player){
        if(player.isSpectator()||player.isShiftKeyDown())return InteractionResult.PASS;
        if(level instanceof ServerLevel server)MemoryCubeRewards.extract(server,player,state.getValue(PEACEFUL));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        return extract(state,level,pos,player);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                        Player player,InteractionHand hand,BlockHitResult hit){
        return extract(state,level,pos,player)==InteractionResult.PASS?ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
                :ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
