package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class ManaEngineRenderer implements BlockEntityRenderer<BlockEntity> {
    public static ModelResourceLocation model(String part) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","block/mana_engine_"+part));
    }
    public ManaEngineRenderer(BlockEntityRendererProvider.Context context) {}
    private static void part(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var minecraft=Minecraft.getInstance();
        minecraft.getItemRenderer().renderModelLists(minecraft.getModelManager().getModel(model(name)),ItemStack.EMPTY,
                light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private static void item(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,BlockEntity entity) {
        pose.pushPose();pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.22F,.22F,.22F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.FIXED,light,overlay,pose,buffers,entity.getLevel(),0);
        pose.popPose();
    }
    @Override public void render(BlockEntity entity,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!(entity instanceof ManaEngineAccess access)) return;
        var engine=access.engineState();
        float angle=engine.previousRotorAngle+(engine.rotorAngle-engine.previousRotorAngle)*partialTick;
        pose.pushPose();
        pose.translate(.5,0,.5);
        var facing=entity.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));
        // Create rotates around the positive world axis, not the shaft's local axis.
        float shaftAngle=(engine.kinetic && entity.getLevel()!=null && entity.getLevel().getBlockEntity(entity.getBlockPos())==entity
                ?CreateEnginePhase.degrees(entity):angle)*switch(facing) {
            case SOUTH,EAST -> -1;
            default -> 1;
        };
        pose.pushPose();pose.translate(0,.5,0);pose.mulPose(Axis.ZP.rotationDegrees(shaftAngle));pose.translate(-.5,-.5,-.5);
        part("shaft",pose,buffers,light,overlay);pose.popPose();
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        part("rotor",pose,buffers,light,overlay);
        double[][] points={{8,2},{13,11},{3,11}};
        for(int i=0;i<3;i++) {
            pose.pushPose();pose.translate(points[i][0]/16,14.9/16,points[i][1]/16);
            item(engine.inventory.getItem(i+1),pose,buffers,light,overlay,entity);pose.popPose();
        }
        pose.popPose();
        pose.translate(0,14.9/16,0);item(engine.inventory.getItem(0),pose,buffers,light,overlay,entity);
        pose.popPose();
    }
}
