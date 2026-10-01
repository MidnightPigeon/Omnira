package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.item.bottle.BottleCaptureRules;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.*;

public final class SpacetimeBrushItem extends BrushItem {
    private record Session(Entity target, int ticks) {}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    public SpacetimeBrushItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context){
        var player=context.getPlayer();
        if(player!=null&&net.neoforged.fml.ModList.get().isLoaded("sable")&&com.mcmagic.omnira.compat.SableBrushStasis.targetsShip(player,context.getClickedPos())){
            player.startUsingItem(context.getHand());return InteractionResult.CONSUME;
        }
        return super.useOn(context);
    }
    @Override public int getUseDuration(ItemStack stack,LivingEntity user){return 72000;}
    public static Entity target(Player player){
        var hit=ProjectileUtil.getHitResultOnViewVector(player,e->!e.isSpectator()&&e.isPickable(),player.entityInteractionRange());
        return hit instanceof EntityHitResult entityHit?entityHit.getEntity():null;
    }
    private static boolean eligible(Entity target){
        return target!=null && !(target instanceof Player)
                && !(target instanceof net.minecraft.world.entity.decoration.ArmorStand)
                && !(target instanceof net.minecraft.world.entity.decoration.HangingEntity)
                && !BottleCaptureRules.forbidden(target)
                && target.getPassengers().stream().noneMatch(e->e instanceof Player);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(!eligible(target(player)))return InteractionResultHolder.pass(player.getItemInHand(hand));
        player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,LivingEntity target,InteractionHand hand){
        if(!eligible(target))return InteractionResult.PASS;
        player.startUsingItem(hand);return InteractionResult.CONSUME;
    }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!(user instanceof Player player))return;
        if(!level.isClientSide&&net.neoforged.fml.ModList.get().isLoaded("sable")){
            boolean active=com.mcmagic.omnira.compat.SableBrushStasis.active(player);
            if(com.mcmagic.omnira.compat.SableBrushStasis.tick(player))return;
            if(active){player.releaseUsingItem();return;}
        }
        Entity target=target(player);
        if(eligible(target)){
            if(level.isClientSide)return;
            var old=SESSIONS.get(player.getUUID());
            if(old!=null&&old.target!=target){player.releaseUsingItem();return;}
            int ticks=old==null?1:old.ticks+1;
            SESSIONS.put(player.getUUID(),new Session(target,ticks));BrushStasis.hold(player,target);
            if(ticks%20==0)stack.hurtAndBreak(1,player,player.getUsedItemHand()==InteractionHand.MAIN_HAND?EquipmentSlot.MAINHAND:EquipmentSlot.OFFHAND);
            if(ticks>=1200){target.ejectPassengers();target.discard();player.releaseUsingItem();}
            else if(stack.isEmpty())player.releaseUsingItem();
            return;
        }
        if(SESSIONS.containsKey(player.getUUID())){player.releaseUsingItem();return;}
        // Preserve vanilla dust/sounds/completion, with one stroke every five ticks.
        int elapsed=getUseDuration(stack,user)-remaining;
        if(elapsed%5==0)super.onUseTick(level,user,stack,getUseDuration(stack,user)-4);
    }
    @Override public void releaseUsing(ItemStack stack,Level level,LivingEntity user,int remaining){
        if(user instanceof Player player){SESSIONS.remove(player.getUUID());BrushStasis.release(player);
            if(net.neoforged.fml.ModList.get().isLoaded("sable"))com.mcmagic.omnira.compat.SableBrushStasis.release(player);}
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(level.isClientSide)return;
        var data=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        long now=level.getGameTime();
        if(!data.contains("BrushRepairAt"))data.putLong("BrushRepairAt",now+6000);
        long next=data.getLong("BrushRepairAt");
        if(now>=next){stack.setDamageValue(Math.max(0,stack.getDamageValue()-(int)Math.min(2,1+(now-next)/6000)*64));data.putLong("BrushRepairAt",now+6000);}
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
    }
}
