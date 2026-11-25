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
public final class PortableTableRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation BODY = ModelResourceLocation.standalone(
            ResourceLocation.parse("omnira:item/portable_crystal_processing_table_body"));
    private static final ModelResourceLocation INLAYS = ModelResourceLocation.standalone(
            ResourceLocation.parse("omnira:item/portable_crystal_processing_table_inlays"));
    private static final double[][] OFFSETS = {{-.27,0},{.27,0},{0,.27},{0,-.27},{0,0}};

    private PortableTableRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        event.register(BODY);
        event.register(INLAYS);
    }

    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private PortableTableRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new PortableTableRenderer();
                return renderer;
            }
        }, ModItems.PORTABLE_TABLE.get());
    }

    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                       MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(BODY), stack, light, overlay, pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS));
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(INLAYS),stack,light,overlay,pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS));
        Integer left = stack.get(com.mcmagic.omnira.registry.ModDataComponents.PORTABLE_WORK);
        if (left == null) return;
        float t = net.minecraft.util.Mth.clamp((60-left+mc.getTimer().getGameTimeDeltaPartialTick(false))/60f,0,1);
        double spread = 1-t*t*(3-2*t);
        var slots = com.mcmagic.omnira.item.PortableTableItem.load(stack);
        for (int i = 0; i < OFFSETS.length; i++) {
            var ingredient = slots.getItem(i);
            if (ingredient.isEmpty() || ingredient.is(ModItems.PORTABLE_TABLE)) continue;
            pose.pushPose();
            pose.translate(.5,.67+.13*Math.sin(Math.PI*t),.5);
            pose.mulPose(Axis.YP.rotationDegrees(140*t));
            pose.translate(OFFSETS[i][0]*spread,0,OFFSETS[i][1]*spread);
            float size = (i==4 ? .23f : .21f*(1-t*.85f));
            pose.scale(size,size,size);
            mc.getItemRenderer().renderStatic(ingredient,ItemDisplayContext.FIXED,light,overlay,pose,CrystalGlassLayer.contents(buffers),mc.level,i);
            pose.popPose();
        }
    }
}
