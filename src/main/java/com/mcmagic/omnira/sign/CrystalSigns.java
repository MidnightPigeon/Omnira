package com.mcmagic.omnira.sign;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class CrystalSigns {
    public static boolean wisdom(Block block){return block instanceof Floor sign&&sign.wisdom||block instanceof Wall wall&&wall.wisdom;}
    public static boolean hanging(Block block){return block instanceof Floor sign&&sign.hanging;}
    private static InteractionResult interact(Level level,BlockPos pos,Player player){
        if(player.isSpectator())return InteractionResult.PASS;
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof CrystalSignEntity sign)sign.draw();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static final class Floor extends StandingSignBlock {
        public final boolean wisdom,hanging;
        public Floor(Properties properties,boolean wisdom,boolean hanging){super(WoodType.OAK,properties);this.wisdom=wisdom;this.hanging=hanging;}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CrystalSignEntity(pos,state);}
        @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
            return level.getBlockState(hanging?pos.above():pos.below()).isSolid();
        }
        @Override protected BlockState updateShape(BlockState state,Direction side,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos adjacent){
            if(side==(hanging?Direction.UP:Direction.DOWN)&&!state.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
            if(hanging){if(state.getValue(WATERLOGGED))level.scheduleTick(pos,net.minecraft.world.level.material.Fluids.WATER,net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));return state;}
            return super.updateShape(state,side,neighbour,level,pos,adjacent);
        }
        @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
            return hanging?Block.box(0,1.6,0,16,16,16):Block.box(0,0,0,16,16.8,16);
        }
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
            super.setPlacedBy(level,pos,state,placer,stack);if(wisdom&&level.getBlockEntity(pos) instanceof CrystalSignEntity sign)sign.draw();
        }
        @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(wisdom){interact(level,pos,player);return player.isSpectator()?ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION:ItemInteractionResult.SUCCESS;}
            if(level.getBlockEntity(pos) instanceof CrystalSignEntity sign&&!sign.isFacingFrontText(player))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            return super.useItemOn(stack,state,level,pos,player,hand,hit);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(wisdom)return interact(level,pos,player);
            if(level.getBlockEntity(pos) instanceof CrystalSignEntity sign&&!sign.isFacingFrontText(player))return InteractionResult.PASS;
            return super.useWithoutItem(state,level,pos,player,hit);
        }
        @Override public void openTextEdit(Player player,SignBlockEntity sign,boolean front){if(!wisdom&&front)super.openTextEdit(player,sign,true);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return createTickerHelper(type,ModBlockEntityTypes.CRYSTAL_SIGN.get(),SignBlockEntity::tick);
        }
    }
    public static final class Wall extends WallSignBlock {
        public final boolean wisdom;
        public Wall(Properties properties,boolean wisdom){super(WoodType.OAK,properties);this.wisdom=wisdom;}
        @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new CrystalSignEntity(pos,state);}
        @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
            super.setPlacedBy(level,pos,state,placer,stack);if(wisdom&&level.getBlockEntity(pos) instanceof CrystalSignEntity sign)sign.draw();
        }
        @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
            if(wisdom){interact(level,pos,player);return player.isSpectator()?ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION:ItemInteractionResult.SUCCESS;}
            if(level.getBlockEntity(pos) instanceof CrystalSignEntity sign&&!sign.isFacingFrontText(player))return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            return super.useItemOn(stack,state,level,pos,player,hand,hit);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(wisdom)return interact(level,pos,player);
            if(level.getBlockEntity(pos) instanceof CrystalSignEntity sign&&!sign.isFacingFrontText(player))return InteractionResult.PASS;
            return super.useWithoutItem(state,level,pos,player,hit);
        }
        @Override public void openTextEdit(Player player,SignBlockEntity sign,boolean front){if(!wisdom&&front)super.openTextEdit(player,sign,true);}
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
            return createTickerHelper(type,ModBlockEntityTypes.CRYSTAL_SIGN.get(),SignBlockEntity::tick);
        }
    }
    public static final class PlacementItem extends SignItem {
        private final Block ceiling;
        public PlacementItem(Properties properties,Block floor,Block wall,Block ceiling){super(properties,floor,wall);this.ceiling=ceiling;}
        @Override protected BlockState getPlacementState(BlockPlaceContext context){
            if(ceiling!=null&&context.getClickedFace()==Direction.DOWN){
                var state=ceiling.getStateForPlacement(context);
                return state!=null&&canPlace(context,state)?state:null;
            }
            return super.getPlacementState(context);
        }
        @Override public void registerBlocks(java.util.Map<Block,Item> map,Item item){super.registerBlocks(map,item);if(ceiling!=null)map.put(ceiling,item);}
        @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
            super.appendHoverText(stack,context,lines,flag);
            if(ceiling!=null)lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.wisdom_sign").withStyle(
                    style->style.withColor(0xA9DFFF).withItalic(true)));
        }
    }
    private CrystalSigns(){}
}
