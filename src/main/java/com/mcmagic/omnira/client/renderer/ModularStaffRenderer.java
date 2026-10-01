package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.staff.StaffAssembly;
import com.mcmagic.omnira.item.staff.StaffPart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ModularStaffRenderer extends BlockEntityWithoutLevelRenderer {
    public ModularStaffRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    /** Accepts the full model path, e.g. omnira:item/staff_parts/wooden_staff_shaft. */
    public static ModelResourceLocation part(ResourceLocation model) {
        return ModelResourceLocation.standalone(model);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                             MultiBufferSource buffers, int light, int overlay) {
        if (stack.isEmpty()) return;
        var mc = Minecraft.getInstance();
        var components = StaffAssembly.of(stack).components();
        long runeCount = components.stream().map(StaffPart::of)
                .filter(p -> p != null && p.role() == StaffPart.Role.UPGRADE
                        && p.upgradeSlot() == StaffPart.SlotKind.RUNE
                        && !p.model().equals(StaffPart.EMPTY_MODEL)).count();
        double time = mc.level == null ? 0 : mc.level.getGameTime()
                + mc.getTimer().getGameTimeDeltaPartialTick(false);
        int runeIndex = 0;
        // ItemRenderer already applied the staff's display transform and origin offset.
        for (var partStack : components) {
            if (partStack.isEmpty()) continue;
            var definition = StaffPart.of(partStack);
            if (definition == null || definition.model().equals(StaffPart.EMPTY_MODEL)) continue;
            var model = mc.getModelManager().getModel(part(definition.model()));
            pose.pushPose();
            if (definition.role() == StaffPart.Role.UPGRADE
                    && definition.upgradeSlot() == StaffPart.SlotKind.RUNE) {
                // One shared ring and angular speed; N visible glyphs stay exactly 360/N degrees apart.
                float angle = (float)(time * .6 % 360 + 360.0 * runeIndex++ / runeCount);
                // The tip crystal spans model Y=13..23; orbit through its midpoint.
                pose.translate(.5, 18.0 / 16, .5);
                pose.mulPose(Axis.YP.rotationDegrees(angle));
                pose.translate(4.5 / 16, 0, 0);
                pose.mulPose(Axis.YP.rotationDegrees(90));
                pose.scale(.28F, .28F, .28F);
                pose.translate(-.5, -.5, -.5);
            }
            // Match the ordinary non-block item pipeline, including translucent model layers.
            for (var pass : model.getRenderPasses(partStack, true)) {
                for (var renderType : pass.getRenderTypes(partStack, true)) {
                    mc.getItemRenderer().renderModelLists(pass, partStack, light, overlay, pose,
                            buffers.getBuffer(renderType));
                }
            }
            pose.popPose();
        }
    }
}
