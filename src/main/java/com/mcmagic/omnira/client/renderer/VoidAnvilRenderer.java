package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.paradox.VoidAnvil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class VoidAnvilRenderer extends EntityRenderer<VoidAnvil> {
    public static final ModelResourceLocation MODEL=CrystalGridRenderer.part("void_anvil","body");
    public VoidAnvilRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.4F;}
    @Override public void render(VoidAnvil entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.translate(-.5,0,-.5);
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(MODEL),ItemStack.EMPTY,
                light,OverlayTexture.NO_OVERLAY,pose,buffers.getBuffer(RenderType.cutout()));
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(VoidAnvil entity){return TextureAtlas.LOCATION_BLOCKS;}
}
