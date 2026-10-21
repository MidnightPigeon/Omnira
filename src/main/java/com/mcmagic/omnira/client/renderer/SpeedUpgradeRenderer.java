package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.SpeedUpgrade;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class SpeedUpgradeRenderer extends BlockEntityWithoutLevelRenderer {
    public static final ModelResourceLocation MODEL=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/speed_upgrade_body"));
    public SpeedUpgradeRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(MODEL),stack,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
        var cores=SpeedUpgrade.cores(stack);
        for(int i=0;i<2;i++)if(!cores.get(i).isEmpty()){
            pose.pushPose();pose.translate((6.25+i*3.5)/16.0,3.875/16.0,6.8/16.0);pose.scale(.09F,.09F,.09F);
            mc.getItemRenderer().renderStatic(cores.get(i),ItemDisplayContext.NONE,light,overlay,pose,buffers,mc.level,0);pose.popPose();
        }
    }
}
