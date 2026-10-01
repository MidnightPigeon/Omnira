package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.PureVesselBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Break the existing one-pixel surface into drifting flakes, without changing its UV scale. */
public final class PureVesselRenderer implements BlockEntityRenderer<PureVesselBlockEntity> {
    public PureVesselRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(PureVesselBlockEntity vessel,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        var model=mc.getBlockRenderer().getBlockModel(vessel.getBlockState());
        var consumer=buffers.getBuffer(Sheets.cutoutBlockSheet());
        float progress=vessel.ritual.collapseProgress(partial);
        if(progress<=0) {
            mc.getItemRenderer().renderModelLists(model,ItemStack.EMPTY,light,overlay,pose,consumer);
            return;
        }
        var random=RandomSource.create(42);
        int index=0;
        for(int side=0;side<7;side++) {
            random.setSeed(42);
            for(var quad:model.getQuads(vessel.getBlockState(),side==6?null:Direction.values()[side],random)) {
                int hash=(++index*73428767)^0x59a71;
                float threshold=(hash & 1023)/1024F;
                if(progress>0 && progress>=threshold)continue;
                float drift=progress>0?Math.max(0,progress-threshold+.18F)/.18F:0;
                pose.pushPose();
                var normal=quad.getDirection();
                pose.translate(normal.getStepX()*drift*.12,-drift*drift*.13,normal.getStepZ()*drift*.12);
                mc.getItemRenderer().renderQuadList(pose,consumer,List.of(quad),ItemStack.EMPTY,light,overlay);
                pose.popPose();
            }
        }
    }
}
