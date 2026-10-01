package com.mcmagic.omnira.block;
import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
public final class VoidCrystalBlock extends BaseEntityBlock {
    public static final IntegerProperty FADE=IntegerProperty.create("fade",0,7);
    public static final MapCodec<VoidCrystalBlock> CODEC=simpleCodec(VoidCrystalBlock::new);
    public VoidCrystalBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FADE,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(FADE);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new VoidCrystalBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,com.mcmagic.omnira.registry.ModBlockEntityTypes.VOID_CRYSTAL.get(),VoidCrystalBlockEntity::tick);
    }
    @Override protected float getDestroyProgress(BlockState state,net.minecraft.world.entity.player.Player player,BlockGetter level,BlockPos pos) {
        float speed=super.getDestroyProgress(state,player,level,pos);
        return level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity be&&be.prisoner()!=null?speed*state.getDestroySpeed(level,pos)/be.hardness():speed;
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) {
        if(context instanceof net.minecraft.world.phys.shapes.EntityCollisionContext c&&c.getEntity()!=null
                &&level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity be&&c.getEntity().getUUID().equals(be.prisoner()))return net.minecraft.world.phys.shapes.Shapes.empty();
        return super.getCollisionShape(state,level,pos,context);
    }
    @Override public void playerDestroy(Level level,net.minecraft.world.entity.player.Player player,BlockPos pos,BlockState state,BlockEntity blockEntity,net.minecraft.world.item.ItemStack tool) {
        if(blockEntity instanceof VoidCrystalBlockEntity be&&be.prisoner()!=null) {
            com.mcmagic.omnira.spell.ConstructionPrison.mine(player,be,tool);return;
        }
        super.playerDestroy(level,player,pos,state,blockEntity,tool);
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        if(params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof VoidCrystalBlockEntity be&&be.prisoner()!=null)return java.util.List.of();
        return super.getDrops(state,params);
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player) {
        if(player.isCreative()&&level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity be&&be.prisoner()!=null)
            com.mcmagic.omnira.spell.ConstructionPrison.mine(player,be,player.getMainHandItem());
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(level.getBlockEntity(pos) instanceof VoidCrystalBlockEntity crystal) crystal.advance();
    }
    @Override protected boolean skipRendering(BlockState state,BlockState neighbor,Direction face) {
        return neighbor.getBlock() instanceof VoidCrystalBlock || neighbor.getBlock() instanceof PermanentVoidCrystalBlock || super.skipRendering(state,neighbor,face);
    }
}
