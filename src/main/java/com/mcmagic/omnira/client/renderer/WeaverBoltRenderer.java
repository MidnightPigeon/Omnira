package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.weaver.WeaverBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class WeaverBoltRenderer extends EntityRenderer<WeaverBolt> {
    public WeaverBoltRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public void render(WeaverBolt entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();WeaverRunes.draw(entity.glyph(),pose,buffers,.024F);
        com.mcmagic.omnira.client.WeaverVisuals.volume(buffers.getBuffer(CrystalGlassLayer.COLOR),pose,
                com.mcmagic.omnira.spell.visual.SpellVolumeGeometry.ball(),.19F,0xBCEAFF,.65F);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(WeaverBolt entity){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
}
