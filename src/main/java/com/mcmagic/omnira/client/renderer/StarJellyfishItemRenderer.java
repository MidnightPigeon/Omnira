package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class StarJellyfishItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation MODEL=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/star_jellyfish_body"));
    private final StarSeaCreatureRenderer.Model<com.mcmagic.omnira.space.sea.StarJellyfish> model=new StarSeaCreatureRenderer.Model<>(true);
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/entity/star_jellyfish.png");
    public StarJellyfishItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        float age=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        model.animateJellyfish(age,0);
        pose.pushPose();
        // Entity mesh spans Y=8..23; flip its downward Y axis around the item center.
        pose.translate(.5,.5+15.5/16,.5);pose.scale(1,-1,-1);
        model.renderToBuffer(pose,buffers.getBuffer(CrystalGlassLayer.entity(TEXTURE)),light,overlay,-1);
        pose.popPose();
    }
}
