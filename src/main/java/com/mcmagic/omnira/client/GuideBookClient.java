package com.mcmagic.omnira.client;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID,value=Dist.CLIENT)
public final class GuideBookClient {
    private GuideBookClient() {}
    public static void open() {Minecraft.getInstance().setScreen(new com.mcmagic.omnira.client.screen.GuideBookScreen());}
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if(event.getItemStack().is(ModItems.NIGHT_HERON_STATUE.get()))
            event.getToolTip().add(Component.translatable("tooltip.omnira.night_heron_statue.flavor")
                    .withStyle(Style.EMPTY.withColor(0x9ED7F7).withItalic(true)));
        if(event.getItemStack().is(ModItems.CRYSTAL_CASING.get()))
            event.getToolTip().add(Component.translatable("tooltip.omnira.crystal_casing.flavor")
                    .withStyle(Style.EMPTY.withColor(0x9ED7F7).withItalic(true)));
        if(event.getItemStack().is(ModItems.INFUSED_CRYSTAL_CASING.get()))
            event.getToolTip().add(Component.translatable("tooltip.omnira.infused_crystal_casing.flavor")
                    .withStyle(Style.EMPTY.withColor(0xFFFFFF).withItalic(true)));
        if(event.getItemStack().is(ModItems.TRAVELER_MANUSCRIPT.get()))
            event.getToolTip().add(Component.translatable("tooltip.omnira.traveler_manuscript"));
        if(event.getItemStack().is(ModItems.GOLDEN_TOILET.get())){
            event.getToolTip().add(Component.translatable("tooltip.omnira.golden_toilet.lore")
                    .withStyle(Style.EMPTY.withColor(0xFFD35A).withItalic(false)));
            event.getToolTip().add(Component.translatable("tooltip.omnira.golden_toilet.hint")
                    .withStyle(Style.EMPTY.withColor(0xFFD35A).withItalic(true)));
        }
    }
}
