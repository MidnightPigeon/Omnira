package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira", value=Dist.CLIENT)
public final class UnorderedClusterRenderer extends BlockEntityWithoutLevelRenderer {
    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/unordered_cluster_" + name));
    }
    private static final ModelResourceLocation CORE = part("core");
    private static final ModelResourceLocation INNER = part("inner");
    private static final ModelResourceLocation OUTER = part("outer");

    private UnorderedClusterRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        event.register(CORE);
        event.register(INNER);
        event.register(OUTER);
    }

    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private UnorderedClusterRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new UnorderedClusterRenderer();
                return renderer;
            }
        }, ModItems.UNORDERED_ELEMENT_CLUSTER.get());
    }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        double ticks = mc.level == null ? net.minecraft.Util.getMillis() / 50.0
                : mc.level.getGameTime() + (double) mc.getTimer().getGameTimeDeltaPartialTick(false);
        draw(CORE, stack, pose, buffers, overlay);
        orbit(INNER, 38, (float)((ticks % 120) * 3), stack, pose, buffers, overlay);
        orbit(OUTER, -49, (float)(-(ticks % 160) * 2.25), stack, pose, buffers, overlay);
    }

    private void orbit(ModelResourceLocation model, float tilt, float angle, ItemStack stack, PoseStack pose,
                       MultiBufferSource buffers, int overlay) {
        pose.pushPose();
        pose.translate(.5, .5, .5);
        pose.mulPose(Axis.ZP.rotationDegrees(tilt));
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.translate(-.5, -.5, -.5);
        draw(model, stack, pose, buffers, overlay);
        pose.popPose();
    }

    private void draw(ModelResourceLocation part, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int overlay) {
        var mc = Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(part), stack, LightTexture.FULL_BRIGHT,
                overlay, pose, buffers.getBuffer(Sheets.solidBlockSheet()));
    }
}
