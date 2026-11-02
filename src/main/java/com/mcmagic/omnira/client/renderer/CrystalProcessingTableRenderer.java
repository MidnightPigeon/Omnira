package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.CrystalProcessingTableBlock;
import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class CrystalProcessingTableRenderer implements BlockEntityRenderer<CrystalProcessingTableBlockEntity> {
    // Centers of the nine colored regions in the original 16px tabletop texture.
    private static final float[][] SLOT_CENTERS = {
            {5, 1}, {11, 1}, {1, 8}, {5, 15}, {11, 15}, {15, 8},
            {6, 7}, {10, 7}, {8, 10}
    };
    private static final float ITEM_SCALE = 0.25F;
    private static final float[][] ASSEMBLY_CENTERS={{10,4},{12,7},{10,10},{6,10},{4,7},{6,4},{8,7}};
    private static final float[][] SINGLE_CENTER={{8,8}};
    private static final float[][] TRIPLE_CENTERS={{8,4},{6,10},{10,10}};
    private static final double ITEM_CENTER_Y = (CrystalProcessingTableBlock.TABLE_HEIGHT + 2.5D) / 16.0D;
    private static final double BOB_AMPLITUDE = 0.375D / 16.0D;
    private static final int BOB_PERIOD_TICKS = 120;
    private static final int ROTATION_PERIOD_TICKS = 240;
    private static final net.minecraft.client.resources.model.ModelResourceLocation HAMMER_MODEL=
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","item/assembly_hammer"));
    public static net.minecraft.client.resources.model.ModelResourceLocation hammerModel() {return HAMMER_MODEL;}
    private final ItemRenderer itemRenderer;

    public CrystalProcessingTableRenderer(BlockEntityRendererProvider.Context context) {
        itemRenderer = context.getItemRenderer();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(CrystalProcessingTableBlockEntity table){
        var bounds=BlockEntityRenderer.super.getRenderBoundingBox(table);
        return table instanceof com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity condensation && condensation.condensationVisible()
                ?bounds.expandTowards(0,.7,0):bounds;
    }

    @Override
    public void render(CrystalProcessingTableBlockEntity table, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light, int overlay) {
        if(table.getLevel()==null)return;
        double time=table.getLevel().getGameTime()+partialTick;
        renderPreview(table,pose,buffers,light,overlay,time,
                table instanceof com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity assembly?time-assembly.lastStrike():-1);
    }

    public void renderPreview(CrystalProcessingTableBlockEntity table,PoseStack pose,
                              MultiBufferSource buffers,int light,int overlay,double time,double strikeAge) {
        if (table.getLevel() == null) {
            return;
        }
        int displayLight = LightTexture.pack(
                Math.max(CrystalProcessingTableBlock.LIGHT_LEVEL, LightTexture.block(light)), LightTexture.sky(light));
        float rotation = (float)(time % ROTATION_PERIOD_TICKS) * (360.0F / ROTATION_PERIOD_TICKS);
        double bobPhase = (time % BOB_PERIOD_TICKS) * (Math.PI * 2.0D / BOB_PERIOD_TICKS);
        float[][] centers = switch(table.getContainerSize()){
            case 7 -> ASSEMBLY_CENTERS;
            case 1 -> SINGLE_CENTER;
            case 3 -> TRIPLE_CENTERS;
            default -> SLOT_CENTERS;
        };
        pose.pushPose();
        pose.translate(.5, 0, .5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - table.getBlockState().getValue(CrystalProcessingTableBlock.FACING).toYRot()));
        pose.translate(-.5, 0, -.5);
        if(table instanceof com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity condensation){
            if(condensation.condensing())CondensationWorkVisuals.render(time-condensation.condensationStarted(),Double.POSITIVE_INFINITY,pose,buffers);
            if(condensation.condensationTailVisible(time))CondensationWorkVisuals.render(time-condensation.condensationTailStarted(),
                    condensation.condensationTailStopped()-condensation.condensationTailStarted(),pose,buffers);
        }
        if(table instanceof com.mcmagic.omnira.block.entity.AdvancedCondensationTableBlockEntity) {
            AdvancedCondensationVisuals.render(time,pose,buffers,overlay);
        }
        if(table instanceof com.mcmagic.omnira.block.entity.ArcaneAssemblyTableBlockEntity assembly) {
            var motion=AssemblyHammerMotion.at(strikeAge);
            pose.pushPose();
            pose.translate(4.5/16,(13.5+motion.lift())/16,(13.5+motion.reach())/16);
            pose.mulPose(Axis.ZP.rotationDegrees((float)motion.swing()));
            pose.translate(-4.5/16,-13.5/16,-13.5/16);
            var mc=net.minecraft.client.Minecraft.getInstance();
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(hammerModel()),
                    ItemStack.EMPTY,displayLight,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
            pose.popPose();
        }
        var contents=CrystalGlassLayer.contents(buffers);
        for (int slot = 0; slot < centers.length; slot++) {
            ItemStack stack = table.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            double bob = Math.sin(bobPhase + slot * 0.7D) * BOB_AMPLITUDE;
            pose.pushPose();
            pose.translate((centers[slot][0]+(table.getContainerSize()==7?.5:0)) / 16.0D, (table.getContainerSize()==1?15.0/16:ITEM_CENTER_Y) + bob, (centers[slot][1]+(table.getContainerSize()==7?.5:0)) / 16.0D);
            // Independent phases keep the display natural; bobbing stays inside the height budget.
            pose.mulPose(Axis.YP.rotationDegrees(rotation + slot * 25.0F));
            float scale=table.getContainerSize()==7 && stack.getItem() instanceof com.mcmagic.omnira.item.StaffItem?.15F:ITEM_SCALE;
            pose.scale(scale,scale,scale);
            itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, displayLight, overlay,
                    pose, contents, table.getLevel(), (int) table.getBlockPos().asLong() + slot);
            pose.popPose();
        }
        pose.popPose();
    }
}
