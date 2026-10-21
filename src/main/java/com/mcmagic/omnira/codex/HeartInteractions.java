package com.mcmagic.omnira.codex;

import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;

/** A marker cannot be fed to another block's right-click insertion handler. */
@EventBusSubscriber(modid="omnira")
public final class HeartInteractions {
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void place(PlayerInteractEvent.RightClickBlock event){
        if(!event.getItemStack().is(MyriadCodexContent.HEART_ITEM.get()))return;
        if(event.getLevel().getBlockState(event.getPos()).is(MyriadCodexContent.BLOCK.get()))return;
        event.setCancellationResult(MyriadCodexContent.HEART_ITEM.get().useOn(
                new net.minecraft.world.item.context.UseOnContext(event.getEntity(),event.getHand(),event.getHitVec())));
        event.setCanceled(true);
    }
    @SubscribeEvent
    public static void close(PlayerContainerEvent.Close event){
        var player=event.getEntity();if(player.level().isClientSide)return;
        for(var slot:event.getContainer().slots){
            var link=HeartLink.of(slot.getItem());if(link==null)continue;
            if(link.matches(player.getMainHandItem()))continue;
            var codex=link.find(player.getServer());if(codex!=null)codex.recall();slot.set(net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
    private HeartInteractions(){}
}
