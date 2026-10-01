package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.FlyingNeedle;
import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class FlyingNeedleRenderer extends EntityRenderer<FlyingNeedle> {
    public static final net.minecraft.client.resources.model.ModelResourceLocation MODEL=
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/arcane_needle_blade"));
    private final ItemRenderer items;
    public FlyingNeedleRenderer(EntityRendererProvider.Context c){super(c);items=c.getItemRenderer();}
    @Override public void render(FlyingNeedle needle,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        var velocity=needle.getDeltaMovement();
        pose.pushPose();pose.mulPose(Axis.YP.rotation((float)Math.atan2(velocity.x,velocity.z)));
        pose.mulPose(Axis.XP.rotation((float)(Math.PI/2-Math.atan2(velocity.y,velocity.horizontalDistance()))));pose.scale(.55F,.55F,.55F);
        items.render(new ItemStack(ModItems.ARCANE_NEEDLE.get()),ItemDisplayContext.NONE,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,
                net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(MODEL));pose.popPose();
    }
    @Override public ResourceLocation getTextureLocation(FlyingNeedle entity){return TextureAtlas.LOCATION_BLOCKS;}
}
