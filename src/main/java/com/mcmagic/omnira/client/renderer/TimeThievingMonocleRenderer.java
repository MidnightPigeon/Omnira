package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

/** Opaque frame, independently sorted glass; worn monocle remains an open ring. */
public final class TimeThievingMonocleRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation FRAME=model("frame"),LENS=model("lens");
    private static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/time_thieving_monocle_"+part));}
    public TimeThievingMonocleRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var models=Minecraft.getInstance().getModelManager();
        StaticModelQuads.draw(models.getModel(FRAME),pose,buffers.getBuffer(Sheets.cutoutBlockSheet()),light,overlay);
        StaticModelQuads.draw(models.getModel(LENS),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),light,overlay);
    }
}
