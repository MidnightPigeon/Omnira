package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.mire.MireContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class MireLilyItemRenderer extends BlockEntityWithoutLevelRenderer {
    public MireLilyItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(boolean reborn){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/"+(reborn?"reborn":"decayed")+"_rottenleaf_lily"));}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();boolean reborn=stack.is(MireContent.REBORN_ITEM.get());
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(reborn)),stack,light,overlay,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
        double time=(mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false))*.035;
        TimePlantRenderer.lilyMotes(pose,buffers,time,reborn);
    }
}
