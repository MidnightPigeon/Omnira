package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.RitualEnergyCoreBlockEntity;
import com.mcmagic.omnira.world.dimension.DreamRitual;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class RitualEnergyCoreBlock extends BaseEntityBlock {
    public static final MapCodec<RitualEnergyCoreBlock> CODEC=simpleCodec(RitualEnergyCoreBlock::new);
    public RitualEnergyCoreBlock(Properties properties) {super(properties);}
    public static boolean supportsBasicRitual(BlockState state) {
        return state.getBlock() instanceof RitualEnergyCoreBlock;
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {return new RitualEnergyCoreBlockEntity(pos,state);}
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return Shapes.or(box(2,0,2,14,3,14),box(4,3,4,12,16,12));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
                                                       Player player,InteractionHand hand,BlockHitResult hit) {
        if(player.isShiftKeyDown())return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(stack.is(com.mcmagic.omnira.registry.ModItems.UNSTABLE_SPACETIME_AGGREGATE.get())
                || stack.is(com.mcmagic.omnira.registry.ModItems.SPACETIME_SPELL_CORE.get())) {
            if(level instanceof net.minecraft.server.level.ServerLevel server)
                com.mcmagic.omnira.spacetime.CorridorRitual.activate(server,pos,player,hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(com.mcmagic.omnira.mana.Affinity.offering(stack)!=com.mcmagic.omnira.mana.Affinity.NONE) {
            if(player instanceof net.minecraft.server.level.ServerPlayer server)
                com.mcmagic.omnira.world.dimension.AffinityRitual.activate(server,pos,hand);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(stack.is(com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get())) {
            if(level instanceof net.minecraft.server.level.ServerLevel server
                    && !com.mcmagic.omnira.world.dimension.CleansingRitual.activate(server,pos,player,hand))
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.cleansing.ritual_invalid"),true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(stack.is(com.mcmagic.omnira.registry.ModItems.SPACETIME_KNOT.get())) {
            if(level instanceof net.minecraft.server.level.ServerLevel server
                    && !com.mcmagic.omnira.world.dimension.ResonanceRitual.activate(server,pos,player,hand))
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.resonance.ritual_invalid"),true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if(!stack.is(ItemTags.BEDS)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        DreamRitual.activate(level,pos,player,hand);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
