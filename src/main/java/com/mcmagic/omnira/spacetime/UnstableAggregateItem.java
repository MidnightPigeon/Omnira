package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class UnstableAggregateItem extends Item {
    public UnstableAggregateItem(Properties properties){super(properties.durability(128));}
    public static boolean isAggregate(ItemStack stack){return stack.getItem() instanceof UnstableAggregateItem;}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();
        if(!level.getBlockState(pos).is(com.mcmagic.omnira.time.TemporalSoils.SILTS)||!level.isEmptyBlock(pos.above()))return InteractionResult.PASS;
        if(level instanceof ServerLevel server&&com.mcmagic.omnira.time.TemporalBloom.spread(server,pos)){
            if(context.getPlayer()==null||!context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static boolean wear(ItemStack stack){
        int ticks=stack.getOrDefault(ModDataComponents.AGGREGATE_TICKS,0)+1;
        stack.set(ModDataComponents.AGGREGATE_TICKS,ticks%10);
        if(ticks>=10)stack.setDamageValue(stack.getDamageValue()+1);
        return stack.getDamageValue()>=128;
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity holder,int slot,boolean selected){
        if(holder instanceof Player player && slot>=0 && slot<player.getInventory().getContainerSize()
                && player.getInventory().getItem(slot)==stack)tickCarried(stack,player);
    }
    public static void tickCarried(ItemStack stack,Entity holder){
        if(holder.level() instanceof ServerLevel server && isAggregate(stack) && wear(stack)){
            stack.shrink(1);SpacetimeStorm.trigger(server,holder.position());
        }
    }
    @Override public boolean onEntityItemUpdate(ItemStack stack,ItemEntity entity){
        if(entity.level() instanceof ServerLevel server){entity.discard();stack.setCount(0);SpacetimeStorm.trigger(server,entity.position());}
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(level instanceof ServerLevel){
            var thrown=new ThrownAggregate(ModEntityTypes.UNSTABLE_AGGREGATE.get(),level);
            thrown.setOwner(player);thrown.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
            thrown.setItem(stack.copyWithCount(1));thrown.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.1F,.25F);
            if(level.addFreshEntity(thrown))stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag){
        lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.unstable_aggregate").withStyle(net.minecraft.ChatFormatting.RED));
    }
    @Override public boolean isEnchantable(ItemStack stack){return false;}
    @Override public boolean isValidRepairItem(ItemStack stack,ItemStack ingredient){return false;}
}
