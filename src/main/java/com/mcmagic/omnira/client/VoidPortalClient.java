package com.mcmagic.omnira.client;

import com.mcmagic.omnira.space.labyrinth.*;
import com.mcmagic.omnira.client.renderer.CrystalBallRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class VoidPortalClient {
    public static void draw(PoseStack pose,MultiBufferSource buffers,Direction outward,double time){
        draw(pose,buffers,outward,time,false);
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,Direction outward,double time,boolean paleBlue){
        var inward=outward.getOpposite();pose.pushPose();pose.translate(.5+inward.getStepX()*.55,.5+inward.getStepY()*.55,.5+inward.getStepZ()*.55);
        pose.mulPose(outward.getRotation());pose.scale(2.5F,2.5F,2.5F);pose.translate(-.5,-.5,-.5);
        CrystalBallRenderer.vortex(pose,buffers,time,paleBlue);pose.popPose();
    }
    @SubscribeEvent public static void renderer(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(LabyrinthContent.ENTITY.get(),context->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<VoidPortalEntity>(){
            @Override public void render(VoidPortalEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(pose,buffers,be.getBlockState().getValue(VoidPortalBlock.FACING),be.getLevel().getGameTime()+partial,be.getBlockState().is(LabyrinthContent.LONG_RANGE_PORTAL.get()));}
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(VoidPortalEntity be){return VoidPortalBlock.bounds(be.getBlockState(),be.getBlockPos());}
        });
    }
    @SubscribeEvent public static void item(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                @Override public void renderByItem(ItemStack s,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                    pose.pushPose();pose.translate(0,0,-.55);draw(pose,buffers,Direction.NORTH,net.minecraft.Util.getMillis()/50.0,s.is(LabyrinthContent.LONG_RANGE_PORTAL_ITEM.get()));pose.popPose();
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },LabyrinthContent.PORTAL_ITEM.get(),LabyrinthContent.LONG_RANGE_PORTAL_ITEM.get());
    }
}
