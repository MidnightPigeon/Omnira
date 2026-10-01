package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;

/** Six neighbour flags drive multipart connected faces without a CTM dependency. */
public final class CrystalCasingBlock extends TransparentBlock {
    public CrystalCasingBlock(Properties properties) {
        super(properties);
        var state=defaultBlockState();
        for(var property:PipeBlock.PROPERTY_BY_DIRECTION.values()) state=state.setValue(property,false);
        registerDefaultState(state);
    }
    private static boolean casing(BlockState state){return state.is(ModBlocks.CRYSTAL_CASING.get())
            ||state.is(ModBlocks.INFUSED_CRYSTAL_CASING.get());}
    @Override protected MapCodec<? extends TransparentBlock> codec() {return simpleCodec(CrystalCasingBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {
        for(var property:PipeBlock.PROPERTY_BY_DIRECTION.values()) builder.add(property);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=defaultBlockState();
        for(var direction:Direction.values()) state=state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                casing(context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        return state;
    }
    @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side),casing(neighbor));
    }
    @Override protected boolean skipRendering(BlockState state,BlockState adjacent,Direction side) {
        return casing(adjacent);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,
            Player player,InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        var wrench=ResourceLocation.fromNamespaceAndPath("create","wrench");
        if(!stack.is(TagKey.create(Registries.ITEM,wrench))
                &&!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(wrench))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!level.isClientSide){
            level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);
            if(!player.isCreative())popResource(level,pos,new ItemStack(state.getBlock()));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation) {
        var result=state;
        for(var side:Direction.values()) result=result.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(rotation.rotate(side)),state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side)));
        return result;
    }
    @Override protected BlockState mirror(BlockState state,Mirror mirror) {
        var result=state;
        for(var side:Direction.values()) result=result.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(mirror.mirror(side)),state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side)));
        return result;
    }
}
