package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class PureVesselBlock extends CrystalProcessingTableBlock {
    public static final MapCodec<PureVesselBlock> CODEC=simpleCodec(PureVesselBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(box(3,2,3,13,14,13),box(2,4,4,14,12,12),box(4,4,2,12,12,14),box(5,0,5,11,16,11));
    public PureVesselBlock(Properties properties) {super(properties);}
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.ENTITYBLOCK_ANIMATED;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
        return net.minecraft.world.InteractionResult.PASS;
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,
            net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        if(!com.mcmagic.omnira.block.entity.PureVesselBlockEntity.accepts(stack))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server
                && level.getBlockEntity(pos) instanceof com.mcmagic.omnira.block.entity.PureVesselBlockEntity vessel)vessel.activate(server,stack);
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) {return false;}
    @Override protected MapCodec<? extends CrystalProcessingTableBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new com.mcmagic.omnira.block.entity.PureVesselBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide?null:createTickerHelper(type,com.mcmagic.omnira.registry.ModBlockEntityTypes.PURE_VESSEL.get(),(l,p,s,v)->v.tick());
    }
    @Override protected void onRemove(BlockState state,net.minecraft.world.level.Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof com.mcmagic.omnira.block.entity.PureVesselBlockEntity vessel){vessel.ritual.cancel();vessel.refundOffering();}
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPE;}
    @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.Item.TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag) {
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.pure_vessel").withStyle(net.minecraft.ChatFormatting.WHITE,net.minecraft.ChatFormatting.ITALIC));
    }
}
