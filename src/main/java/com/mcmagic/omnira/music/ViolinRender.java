package com.mcmagic.omnira.music;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ViolinRender {
    public static ModelResourceLocation model(boolean bow){return ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath("omnira","item/violin/"+(bow?"bow":"body")));}
    private static void part(boolean bow,PoseStack pose,MultiBufferSource buffers,int light){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().render(new ItemStack(ModItems.VOYAGER_VIOLIN.get()),ItemDisplayContext.NONE,false,pose,buffers,
                light,OverlayTexture.NO_OVERLAY,mc.getModelManager().getModel(model(bow)));
    }
    public static void thirdPerson(Player player,HumanoidModel<?> body,PoseStack pose,MultiBufferSource buffers,int light){
        int side=player.getMainArm()==HumanoidArm.RIGHT?1:-1;
        pose.pushPose();pose.translate(side*.27,.06,-.45);
        pose.mulPose(Axis.YP.rotationDegrees(side*35));pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(-side*90));pose.scale(.95F,.95F,.95F);
        part(false,pose,buffers,light);pose.popPose();
        // Anchor the bow at the actual hand and aim it through the bridge throughout the stroke.
        var handPose=new PoseStack();body.translateToHand(player.getMainArm(),handPose);
        var wrist=handPose.last().pose().transformPosition(new org.joml.Vector3f(0,.625F,0));
        var direction=new org.joml.Vector3f(side*.136F,-.041F,-.356F).sub(wrist).normalize();
        var across=new org.joml.Vector3f(direction).cross(0,-1,0).normalize();
        var normal=new org.joml.Vector3f(across).cross(direction).normalize();
        var basis=new org.joml.Matrix3f().setColumn(0,across).setColumn(1,direction).setColumn(2,normal);
        pose.pushPose();pose.translate(wrist.x,wrist.y,wrist.z);pose.mulPose(new org.joml.Quaternionf().setFromNormalized(basis));
        pose.scale(.72F,.72F,.72F);pose.translate(0,.46875,0);
        part(true,pose,buffers,light);pose.popPose();
    }
    @SubscribeEvent public static void firstPerson(net.neoforged.neoforge.client.event.RenderHandEvent e){
        var mc=Minecraft.getInstance();var p=mc.player;if(p==null||!ViolinClient.active(p))return;
        e.setCanceled(true);
        if(e.getHand()!=InteractionHand.MAIN_HAND)return;
        int side=p.getMainArm()==HumanoidArm.RIGHT?1:-1;
        var local=new org.joml.Matrix4f().translation(-side*.06F,-.37F,-.75F)
                .rotateX((float)Math.toRadians(-30)).rotateZ((float)Math.toRadians(side*70)).scale(.65F);
        var bridge=local.transformPosition(new org.joml.Vector3f(0,-.171875F,.10625F));
        var grip=local.transformPosition(new org.joml.Vector3f(0,.25F,.0625F));
        float stroke=(float)Math.sin((p.tickCount+e.getPartialTick())*.18);
        var wrist=new org.joml.Vector3f(side*(.3F+stroke*.07F),-.59F-stroke*.04F,-.67F);
        var pose=e.getPoseStack();pose.pushPose();pose.mulPose(local);part(false,pose,e.getMultiBufferSource(),e.getPackedLight());pose.popPose();
        var direction=new org.joml.Vector3f(bridge).sub(wrist).normalize();
        pose.pushPose();pose.translate(wrist.x,wrist.y,wrist.z);pose.mulPose(basis(direction,new org.joml.Vector3f(0,0,1)));
        pose.scale(.72F,.72F,.72F);pose.translate(0,.46875,0);part(true,pose,e.getMultiBufferSource(),e.getPackedLight());pose.popPose();
        hand(p,p.getMainArm(),wrist,pose,e.getMultiBufferSource(),e.getPackedLight());
        hand(p,p.getMainArm().getOpposite(),grip,pose,e.getMultiBufferSource(),e.getPackedLight());
    }
    private static org.joml.Quaternionf basis(org.joml.Vector3f direction,org.joml.Vector3f normal){
        var across=new org.joml.Vector3f(direction).cross(normal).normalize();
        normal=new org.joml.Vector3f(across).cross(direction).normalize();
        return new org.joml.Quaternionf().setFromNormalized(new org.joml.Matrix3f().setColumn(0,across).setColumn(1,direction).setColumn(2,normal));
    }
    private static void hand(net.minecraft.client.player.AbstractClientPlayer player,HumanoidArm arm,org.joml.Vector3f wrist,
                             PoseStack pose,MultiBufferSource buffers,int light){
        if(player.isInvisible()||!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer))return;
        var model=renderer.getModel();boolean right=arm==HumanoidArm.RIGHT;int side=right?1:-1;
        pose.pushPose();pose.translate(wrist.x,wrist.y,wrist.z);
        pose.mulPose(basis(new org.joml.Vector3f(-side*.3F,.75F,-.5F).normalize(),new org.joml.Vector3f(0,0,1)));
        pose.translate(side/16.0,-.625,0);
        for(var part:new net.minecraft.client.model.geom.ModelPart[]{right?model.rightArm:model.leftArm,right?model.rightSleeve:model.leftSleeve}){
            var saved=part.storePose();part.setPos(0,0,0);part.setRotation(0,0,0);
            part.render(pose,buffers.getBuffer(net.minecraft.client.renderer.RenderType.entityCutoutNoCull(player.getSkin().texture())),light,OverlayTexture.NO_OVERLAY);
            part.loadPose(saved);
        }
        pose.popPose();
    }
}
