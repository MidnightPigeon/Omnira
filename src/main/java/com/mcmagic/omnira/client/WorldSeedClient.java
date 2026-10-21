package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.myriad.WorldSeedContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid = "omnira", value = Dist.CLIENT)
public final class WorldSeedClient {
    private static ModelResourceLocation model(boolean active) {
        return CrystalGridRenderer.part("world_seed", active ? "active" : "dormant");
    }

    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        event.register(model(false));
        event.register(model(true));
    }

    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private Renderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new Renderer();
                return renderer;
            }
        }, WorldSeedContent.DORMANT.get(), WorldSeedContent.ACTIVE.get());
    }

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private Renderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                          MultiBufferSource buffers, int light, int overlay) {
            var mc = Minecraft.getInstance();
            boolean active = stack.is(WorldSeedContent.ACTIVE.get());
            double time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
            pose.pushPose();
            pose.translate(.5, .5, .5);
            if (active) pose.mulPose(Axis.YP.rotationDegrees((float) (time * .35 % 360)));
            pose.pushPose();
            pose.translate(-.5, -.5, -.5);
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(active)), stack,
                    active ? LightTexture.FULL_BRIGHT : light, overlay, pose,
                    buffers.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)));
            pose.popPose();
            // Bounded mesh motes share the established, globally sorted crystal layer.
            if (active) {
                var mesh = buffers.getBuffer(CrystalGlassLayer.COLOR);
                for (int i = 0; i < 12; i++) {
                    double phase = (time / 90 + i / 12.0) % 1;
                    double y = -1 + 2 * (i + .5) / 12, angle = i * 2.399963 + time * .006;
                    double radius = .38 + phase * .18, horizontal = Math.sqrt(1 - y * y);
                    StellarVisuals.cube(pose, mesh, (float) (Math.cos(angle) * horizontal * radius),
                            (float) (y * radius), (float) (Math.sin(angle) * horizontal * radius),
                            .012F, i % 3 == 0 ? 0xDDF9FF : i % 3 == 1 ? 0x66DFAA : 0x67BFFF,
                            (float) (Math.sin(phase * Math.PI) * .9));
                }
            }
            pose.popPose();
        }
    }
    private WorldSeedClient() {}
}
