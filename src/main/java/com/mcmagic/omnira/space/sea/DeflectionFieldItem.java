package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.item.HeldManaRepair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class DeflectionFieldItem extends ShieldItem {
    public DeflectionFieldItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        player.startUsingItem(hand);return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public void onUseTick(Level level,LivingEntity user,ItemStack stack,int remaining){
        if(!level.isClientSide&&(getUseDuration(stack,user)-remaining+1)%20==0)wear(user,stack);
    }
    public static void wear(LivingEntity user,ItemStack stack){
        var slot=user.getMainHandItem()==stack?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1,user,slot);
    }
    @Override public void inventoryTick(ItemStack stack,Level level,Entity entity,int slot,boolean selected){
        if(entity instanceof ServerPlayer player&&level.getGameTime()%10==0)HeldManaRepair.repair(stack,player);
    }
}
