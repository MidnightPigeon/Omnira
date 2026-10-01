package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** The inventory, hand and dropped item share the placed plant's mesh and mote orbit. */
public final class TimePlantItemRenderer extends BlockEntityWithoutLevelRenderer {
    public TimePlantItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(boolean flower){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira",flower?"block/time_flower":"block/time_grass"));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();boolean flower=stack.is(TimeNatureContent.FLOWER.get().asItem());
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(flower)),stack,light,overlay,pose,
                buffers.getBuffer(Sheets.cutoutBlockSheet()));
        double time=(mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.035;
        TimePlantRenderer.motes(pose,buffers,time,flower);
    }
}
