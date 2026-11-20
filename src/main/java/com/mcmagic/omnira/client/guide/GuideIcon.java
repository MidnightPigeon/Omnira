package com.mcmagic.omnira.client.guide;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A guide symbol is either a standard item or a dedicated 16-pixel illustration. */
public record GuideIcon(ItemStack item, ResourceLocation texture) {
    public static GuideIcon resolve(String id) {
        if (id.endsWith(".png")) return new GuideIcon(ItemStack.EMPTY, ResourceLocation.parse(id));
        return new GuideIcon(GuideContents.icon(id), null);
    }

    public boolean isEmpty() {
        return texture == null && item.isEmpty();
    }

    public void draw(GuiGraphics graphics, int x, int y) {
        if (texture == null) graphics.renderItem(item, x, y);
        else graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }
}
