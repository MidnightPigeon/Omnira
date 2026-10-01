package com.mcmagic.omnira.event;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.config.OmniraConfig;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class StarterGuideEvents {
    private static final String CHECKED="omnira_initial_guide_checked";
    private StarterGuideEvents() {}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer player)) return;
        var persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(persisted.getBoolean(CHECKED)) return;
        persisted.putBoolean(CHECKED,true);
        player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        if(!OmniraConfig.GIVE_STARTER_GUIDE.get()) return;
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++) if(inventory.getItem(i).is(ModItems.GUIDE_BOOK.get())) return;
        var book=new ItemStack(ModItems.GUIDE_BOOK.get());
        if(player.getMainHandItem().isEmpty()) inventory.setItem(inventory.selected,book);
        else if(!inventory.add(book)) {
            var dropped=player.drop(book,false);
            if(dropped!=null) {dropped.setTarget(player.getUUID());dropped.setNoPickUpDelay();}
        }
        inventory.setChanged();player.inventoryMenu.broadcastChanges();
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event) {
        com.mcmagic.omnira.mire.MemoryCubeRewards.copyDailyLimits(event.getOriginal(),event.getEntity());
        var old=event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(old.getBoolean(CHECKED)) {
            var persisted=event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            persisted.putBoolean(CHECKED,true);
            event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        }
    }
}
