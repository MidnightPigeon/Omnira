package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class SpellCoreBlock extends BaseEntityBlock {
    public static final MapCodec<SpellCoreBlock> CODEC=simpleCodec(SpellCoreBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(box(4,3,4,12,13,12),box(3,4,4,13,12,12),box(4,4,3,12,12,13));
    public SpellCoreBlock(Properties p){super(p);}
    @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void allowEmptyMainHandPickup(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        var player=event.getEntity();
        // Sneaking normally bypasses block use when either hand holds an item.
        if(event.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND && player.isShiftKeyDown()
                && player.getMainHandItem().isEmpty() && player.mayBuild()
                && event.getUseBlock()!=net.neoforged.neoforge.common.util.TriState.FALSE
                && event.getLevel().getBlockState(event.getPos()).getBlock() instanceof SpellCoreBlock)
            event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,net.minecraft.world.phys.BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !player.getMainHandItem().isEmpty() || !player.mayBuild())
            return net.minecraft.world.InteractionResult.PASS;
        if (!level.isClientSide && level.removeBlock(pos,false))
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(asItem()));
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new com.mcmagic.omnira.block.entity.SpellCoreBlockEntity(p,s);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return l.getBlockState(p.below()).isFaceSturdy(l,p.below(),Direction.UP);}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState other,LevelAccessor l,BlockPos p,BlockPos q){
        return d==Direction.DOWN && !s.canSurvive(l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,other,l,p,q);
    }
}
