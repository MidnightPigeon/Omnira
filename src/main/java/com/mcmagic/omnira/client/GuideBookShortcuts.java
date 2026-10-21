package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.guide.*;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class GuideBookShortcuts {
    private static final GuideReferenceHold HOLD=new GuideReferenceHold();
    private static GuideReferenceIndex index;
    private static String language;
    private record Hover(Screen screen,int slot,Item item,String destination) {}
    private static GuideReferenceIndex index(){
        String current=Minecraft.getInstance().getLanguageManager().getSelected();
        if(index==null||!current.equals(language)){index=new GuideReferenceIndex(GuideContents.load());language=current;}
        return index;
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent event){
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->index=null);
    }
    private static Hover hovered(){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||mc.screen==null||mc.screen instanceof com.mcmagic.omnira.client.screen.GuideBookScreen||!mc.isWindowActive()||!GuideReferenceIndex.carriesBook(mc.player)
                ||!mc.player.containerMenu.getCarried().isEmpty())return null;
        ItemStack stack=ItemStack.EMPTY;int slot=-1;
        if(mc.screen instanceof AbstractContainerScreen<?> container){
            var hovered=container.getSlotUnderMouse();
            if(hovered!=null){stack=hovered.getItem();slot=hovered.index;}
        }
        if(stack.isEmpty()&&ModList.get().isLoaded("jei"))stack=com.mcmagic.omnira.compat.jei.OmniraJeiPlugin.itemUnderMouse();
        var destination=index().item(stack);
        return destination.isEmpty()?null:new Hover(mc.screen,slot,stack.getItem(),destination.get());
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var target=hovered();
        if(HOLD.tick(target,Screen.hasControlDown()))GuideBookClient.openReference(target.destination(),target.screen());
    }
    @SubscribeEvent public static void tooltip(RenderTooltipEvent.GatherComponents event){
        var target=hovered();if(target==null||!event.getItemStack().is(target.item()))return;
        int percent=HOLD.percent(target);
        event.getTooltipElements().add(Either.left(Component.translatable(percent>0?"guide.omnira.hold_ctrl_progress":"guide.omnira.hold_ctrl",percent)
                .withStyle(net.minecraft.ChatFormatting.GRAY)));
    }
    private static java.util.Optional<String> aimed(){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.screen!=null||!mc.isWindowActive()||!mc.player.getMainHandItem().is(ModItems.GUIDE_BOOK.get()))return java.util.Optional.empty();
        return index().target(mc.level,mc.player,mc.hitResult);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void use(InputEvent.InteractionKeyMappingTriggered event){
        if(!event.isUseItem()||Minecraft.getInstance().player==null||!Minecraft.getInstance().player.isShiftKeyDown())return;
        var target=aimed();if(target.isEmpty())return;
        event.setCanceled(true);event.setSwingHand(false);GuideBookClient.openReference(target.get(),null);
    }
    @SubscribeEvent public static void hint(RenderGuiEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.options.hideGui||aimed().isEmpty())return;
        var text=Component.translatable("guide.omnira.shift_use");var g=event.getGuiGraphics();
        g.drawString(mc.font,text,(g.guiWidth()-mc.font.width(text))/2,g.guiHeight()/2+16,0xD9F2FF,true);
    }
    private GuideBookShortcuts(){}
}
