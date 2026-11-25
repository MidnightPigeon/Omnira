package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.client.screen.UiScissor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class GuideIcon {
    private final ItemStack item;
    private final ResourceLocation texture, creature;
    private LivingEntity preview;

    private GuideIcon(ItemStack item, ResourceLocation texture, ResourceLocation creature) {
        this.item=item;
        this.texture=texture;
        this.creature=creature;
    }
    public static GuideIcon resolve(String id) {
        if (id.startsWith("omnira:entity/"))
            return new GuideIcon(ItemStack.EMPTY,null,ResourceLocation.fromNamespaceAndPath("omnira",id.substring("omnira:entity/".length())));
        if (id.endsWith(".png")) return new GuideIcon(ItemStack.EMPTY, ResourceLocation.parse(id),null);
        return new GuideIcon(GuideContents.icon(id), null,null);
    }
    public ItemStack item(){return item;}
    public boolean isEmpty() {
        return creature==null && texture == null && item.isEmpty();
    }
    public void draw(GuiGraphics graphics, int x, int y) {
        if(creature!=null)drawCreature(graphics,x,y);
        else if (texture == null) graphics.renderItem(item, x, y);
        else graphics.blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }
    private void drawCreature(GuiGraphics graphics,int x,int y){
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        if(preview==null || preview.level()!=level){
            var type=BuiltInRegistries.ENTITY_TYPE.getOptional(creature);
            if(type.isEmpty() || !(type.get().create(level) instanceof LivingEntity entity))return;
            preview=entity;
            preview.yBodyRot=preview.yBodyRotO=0;
            preview.yHeadRot=preview.yHeadRotO=0;
        }
        // The long fish and winged fauna extend beyond their collision boxes.
        float span=switch(creature.getPath()){
            case "timeflow_eel" -> 1.4F;
            case "chronal_blindfish","rift_moth","time_thief_mosquito" -> 1F;
            default -> Math.max(preview.getBbWidth(),preview.getBbHeight())*1.15F;
        };
        var view=new Quaternionf().rotationZ((float)Math.PI).rotateX(-.18F).rotateY(.96F);
        UiScissor.enable(graphics,x,y,x+16,y+16);
        try{
            InventoryScreen.renderEntityInInventory(graphics,x+8,y+8,14/span,
                    new Vector3f(0,preview.getBbHeight()*.5F,0),view,null,preview);
        }finally{graphics.disableScissor();}
    }
}
