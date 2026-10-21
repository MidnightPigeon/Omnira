package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.item.UpgradeCompatibility;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class UpgradeCompatibilityTooltip {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        if(event.getItemStack().getItem() instanceof OrbUpgradeItem plate)
            UpgradeCompatibility.appendTooltip(plate.kind,Screen.hasShiftDown(),event.getToolTip());
    }
    private UpgradeCompatibilityTooltip(){}
}
