package com.mcmagic.omnira.codex;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class WorldHeartItem extends Item {
    public WorldHeartItem(Properties p){super(p);}
    @Override public boolean canFitInsideContainerItems(){return false;}
    @Override public boolean onDroppedByPlayer(ItemStack stack,Player player){recall(stack,player.level());return false;}
    @Override public boolean onEntityItemUpdate(ItemStack stack,ItemEntity entity){recall(stack,entity.level());entity.discard();return true;}
    private static void recall(ItemStack stack,Level level){if(!level.isClientSide){var link=HeartLink.of(stack);var codex=link==null?null:link.find(level.getServer());if(codex!=null)codex.recall();stack.setCount(0);}}
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(level.isClientSide)return;
        var link=HeartLink.of(stack);var codex=link==null?null:link.find(level.getServer());
        if(!(entity instanceof ServerPlayer player)||codex==null||!codex.validHolder(player)||player.getMainHandItem()!=stack){recall(stack,level);}
    }
    @Override public InteractionResult useOn(UseOnContext c){
        if(c.getHand()!=InteractionHand.MAIN_HAND||c.getPlayer()==null)return InteractionResult.FAIL;
        var pos=c.getLevel().getBlockState(c.getClickedPos()).canBeReplaced()?c.getClickedPos():c.getClickedPos().relative(c.getClickedFace());
        return place(c.getLevel(),c.getPlayer(),c.getItemInHand(),pos)?InteractionResult.sidedSuccess(c.getLevel().isClientSide):InteractionResult.FAIL;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.fail(stack);
        if(player.pick(5,0,false).getType()!=HitResult.Type.MISS)return InteractionResultHolder.pass(stack);
        var front=BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(2)));
        BlockPos candidate=null;
        for(var p:BlockPos.withinManhattan(front,1,1,1))if(!p.equals(player.blockPosition())&&level.hasChunkAt(p)&&level.getBlockState(p).canBeReplaced()){candidate=p.immutable();break;}
        return candidate!=null&&place(level,player,stack,candidate)?InteractionResultHolder.sidedSuccess(stack,level.isClientSide):InteractionResultHolder.fail(stack);
    }
    private static boolean place(Level level,Player player,ItemStack stack,BlockPos pos){
        if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos)||!level.getBlockState(pos).canBeReplaced()||!player.mayUseItemAt(pos,net.minecraft.core.Direction.UP,stack))return false;
        if(level.isClientSide)return true;
        var link=HeartLink.of(stack);var codex=link==null?null:link.find(level.getServer());
        if(codex==null||!codex.validHolder((ServerPlayer)player)||level!=codex.getLevel())return false;
        return codex.placeHeart((ServerPlayer)player,pos);
    }
}
