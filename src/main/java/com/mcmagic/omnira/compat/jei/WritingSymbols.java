package com.mcmagic.omnira.compat.jei;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;

/** Display-only symbols: never recipe ingredients or transferable items. */
final class WritingSymbols {
    static void draw(GuiGraphics g,int x,int y,boolean element,int index){
        double time=net.minecraft.Util.getMillis()/1000.0+index*.43;
        double phase=time%3/3;
        float scale=(float)Math.sin(Math.PI*phase);
        if(scale<.15F)return;
        var choices=element?List.of(ModItems.SHARP_BREATH.get(),ModItems.HEALING_DEW.get(),ModItems.DISSOCIATION_THREAD.get(),ModItems.CONSTRUCTION_MATRIX.get(),ModItems.DARK_MICROCORE.get())
                :List.of(Items.CLOCK,Items.IRON_BLOCK,ModItems.SPIRITUAL_CRYSTAL.get(),ModItems.LIGHT_MICROCORE.get());
        var stack=new ItemStack(choices.get((int)(time/3)%choices.size()));
        g.pose().pushPose();g.pose().translate(x+8,y+8,0);g.pose().scale(scale,scale,1);g.renderFakeItem(stack,-8,-8);g.pose().popPose();
    }
    static List<Component> tooltip(boolean element){return List.of(Component.translatable(element?"jei.omnira.symbolic_element":"jei.omnira.symbolic_modifier"),Component.translatable("jei.omnira.symbolic_only"));}
    private WritingSymbols(){}
}
