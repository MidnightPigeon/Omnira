package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.space.sea.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class StarlightRenderer implements BlockEntityRenderer<StarlightEntity> {
    public static final ModelResourceLocation MODEL=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/star_source_orb"));
    public static final ModelResourceLocation DUST=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/stardust_scatter"));
    public StarlightRenderer(BlockEntityRendererProvider.Context c) {}
    @Override public void render(StarlightEntity e,float partial,PoseStack p,MultiBufferSource b,int light,int overlay) {
        double t=e.getLevel()==null?0:e.getLevel().getGameTime()+partial;
        boolean source=e.getBlockState().is(StarlightContent.SOURCE.get());
        if (source) {
            p.pushPose();
            p.translate(.5,.5+Math.sin(t*.04+e.getBlockPos().asLong()%31)*.025,.5);
            p.mulPose(Axis.YP.rotationDegrees((float)(t*.5)));p.translate(-.5,-.5,-.5);
        }
        var mc=Minecraft.getInstance();
        if(source)StaticModelQuads.draw(mc.getModelManager().getModel(MODEL),p,b.getBuffer(CrystalGlassLayer.ATLAS),LightTexture.FULL_BRIGHT,overlay);
        else PreparedStaticMesh.draw(mc.getModelManager().getModel(DUST),p,b.getBuffer(CrystalGlassLayer.ATLAS),overlay);
        if(source)p.popPose();
    }
}
