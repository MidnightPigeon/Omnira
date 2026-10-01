package com.mcmagic.omnira.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import com.mcmagic.omnira.spell.SpellPattern;

public final class CompositeSlotGhost {
    private CompositeSlotGhost(){}
    static void render(GuiGraphics g,Slot first,Slot second,int left,int top){
        if(!SpellPattern.composite(first.getItem())||second.hasItem())return;
        int x=left+second.x,y=top+second.y;
        render(g,first.getItem(),x,y);
    }
    public static void render(GuiGraphics g,net.minecraft.world.item.ItemStack core,int x,int y){
        g.renderFakeItem(core,x,y);
        g.pose().pushPose();g.pose().translate(0,0,250);
        g.fill(x,y,x+16,y+16,0xAA53656B);
        g.fill(x+2,y+7,x+14,y+9,0x889FE9E0);
        g.pose().popPose();
    }
}
