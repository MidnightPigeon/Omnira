package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Head-top curios inherit the head pivot, but never its look rotation. */
public final class EquippedCrystalRenderer implements ICurioRenderer {
    @Override public <T extends LivingEntity,M extends EntityModel<T>> void render(ItemStack stack,SlotContext context,
            PoseStack pose,RenderLayerParent<T,M> parent,MultiBufferSource buffers,int light,
            float limbSwing,float limbSwingAmount,float partialTick,float age,float yaw,float pitch) {
        if(!context.visible() || context.entity().isInvisible() || !(parent.getModel() instanceof HumanoidModel<?> body)) return;
        pose.pushPose();
        boolean core=context.identifier().equals("spell_core");
        double time=context.entity().tickCount+partialTick;
        double bob=core?Math.sin(time*Math.PI*2/80)*.04:0;
        pose.translate(body.head.x/16.0,body.head.y/16.0-.72-bob,body.head.z/16.0);
        float size=core?.24F:.85F;
        pose.scale(size,-size,-size);
        if(!core) pose.mulPose(Axis.XP.rotationDegrees(90));
        // NONE bypasses inventory/frame display rotations so the grid stays horizontal.
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,OverlayTexture.NO_OVERLAY,
                pose,buffers,context.entity().level(),context.entity().getId());
        pose.popPose();
    }
}
