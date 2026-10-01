package com.mcmagic.omnira.item.staff;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.spell.CastAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class StaffEvents {
    public static final int SHOT_INTERVAL=6;
    private static final Map<UUID,Burst> BURSTS=new HashMap<>();
    private record Burst(ItemStack staff,ItemStack snapshot,ItemStack grid,net.minecraft.world.InteractionHand hand,
                         net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,long due,int remaining,int cooldown) {}
    private StaffEvents() {}
    @SubscribeEvent public static void attributes(ItemAttributeModifierEvent event) {
        if(event.getItemStack().getItem() instanceof com.mcmagic.omnira.item.ArcaneArquebusItem) {
            event.replaceModifier(ModAttributes.SPELL_POWER,new AttributeModifier(
                    CastAttributes.ARQUEBUS_POWER,com.mcmagic.omnira.item.ArquebusPlugin.of(event.getItemStack()).powerBonus,AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND);
            return;
        }
        if(!(event.getItemStack().getItem() instanceof StaffItem)) return;
        var stats=StaffAssembly.of(event.getItemStack()).stats();
        if(stats.cooldownReduction()!=0) event.replaceModifier(ModAttributes.COOLDOWN_REDUCTION,new AttributeModifier(
                CastAttributes.STAFF_COOLDOWN,stats.cooldownReduction(),AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
        if(stats.power()!=0) event.replaceModifier(ModAttributes.SPELL_POWER,new AttributeModifier(
                CastAttributes.STAFF_POWER,stats.power(),AttributeModifier.Operation.ADD_MULTIPLIED_BASE),EquipmentSlotGroup.MAINHAND);
        if(stats.reduction()!=0) event.replaceModifier(ModAttributes.COST_REDUCTION,new AttributeModifier(
                CastAttributes.STAFF_DISCOUNT,stats.reduction(),AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.MAINHAND);
    }
    public static void queue(ServerPlayer player,ItemStack staff,int remaining,int cooldown) {
        BURSTS.remove(player.getUUID());
        if(remaining>0) BURSTS.put(player.getUUID(),new Burst(staff,staff.copy(),CrystalGridMenu.locate(player,-2),player.getOffhandItem()==staff?net.minecraft.world.InteractionHand.OFF_HAND:net.minecraft.world.InteractionHand.MAIN_HAND,
                player.level().dimension(),player.level().getGameTime()+SHOT_INTERVAL,remaining,cooldown));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        var burst=BURSTS.get(player.getUUID());if(burst==null) return;
        if(com.mcmagic.omnira.time.FleetingTime.bonus(player)){
            burst=new Burst(burst.staff,burst.snapshot,burst.grid,burst.hand,burst.dimension,burst.due-1,burst.remaining,burst.cooldown);
            BURSTS.put(player.getUUID(),burst);
        }
        // A queued shot must not borrow bonuses from a replacement staff or survive dimension changes.
        if(!player.isAlive() || player.isSpectator() || player.getItemInHand(burst.hand)!=burst.staff
                || !ItemStack.matches(burst.snapshot,burst.staff) || !player.level().dimension().equals(burst.dimension)
                || player.containerMenu instanceof CrystalGridMenu || CrystalGridMenu.locate(player,-2)!=burst.grid) {
            BURSTS.remove(player.getUUID());return;
        }
        if(player.level().getGameTime()<burst.due) return;
        BURSTS.remove(player.getUUID());
        if(StaffItem.castSequence(player,burst.staff,1)>0 && burst.remaining>1)
            BURSTS.put(player.getUUID(),new Burst(burst.staff,burst.snapshot,burst.grid,burst.hand,burst.dimension,
                    player.level().getGameTime()+SHOT_INTERVAL,burst.remaining-1,burst.cooldown));
        else player.getCooldowns().addCooldown(burst.staff.getItem(),burst.cooldown);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {BURSTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {BURSTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {BURSTS.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stop(ServerStoppedEvent event) {BURSTS.clear();}
}
