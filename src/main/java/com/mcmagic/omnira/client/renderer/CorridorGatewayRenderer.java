package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.spacetime.CorridorGatewayBlockEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Same mirror frame; the opaque pixel surface replaces reflection, not the surrounding world. */
public final class CorridorGatewayRenderer implements BlockEntityRenderer<CorridorGatewayBlockEntity> {
    private static final ResourceLocation FRAME=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/dream_mirror.png");
    private static final ResourceLocation GLASS=ResourceLocation.fromNamespaceAndPath("omnira","textures/block/mirror_rock.png");
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/solid_white.png");
    private final ModelPart frame=DreamMirrorRenderer.createFrame();
    public CorridorGatewayRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRenderOffScreen(CorridorGatewayBlockEntity gateway){return true;}
    @Override public void render(CorridorGatewayBlockEntity gateway,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,0,.5);
        var camera=net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
        var delta=camera.getPosition().subtract(net.minecraft.world.phys.Vec3.atCenterOf(gateway.getBlockPos()));
        float facing=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        pose.mulPose(Axis.YP.rotationDegrees(180-facing));
        draw(frame,pose,buffers,gateway.getLevel().getGameTime()+partial,gateway.progress(partial),light,
                MirrorScene.rendering()?null:MirrorScene.frozen(gateway.mirrorId()));
        pose.popPose();
    }
    public static void draw(ModelPart frame,PoseStack pose,MultiBufferSource buffers,float time,float progress,int light){
        draw(frame,pose,buffers,time,progress,light,null);
    }
    private static void draw(ModelPart frame,PoseStack pose,MultiBufferSource buffers,float time,float progress,int light,ResourceLocation frozen){
        pose.pushPose();pose.scale(1,1,.65F);
        frame.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(FRAME)),light,OverlayTexture.NO_OVERLAY);pose.popPose();
        if(progress<.33F){
            DreamMirrorRenderer.surface(pose,buffers,GLASS,.0624F,false,light);
            DreamMirrorRenderer.surface(pose,buffers,frozen==null?GLASS:frozen,-.0624F,true,LightTexture.FULL_BRIGHT);return;
        }
        var out=buffers.getBuffer(RenderType.entitySolid(WHITE));
        float opening=Math.clamp((progress-.66F)/.34F,0,1);
        for(int row=0;row<26;row++){
            float bottom=.1F+row*1.6F/26,top=.1F+(row+1)*1.6F/26;
            float half=Math.min(.43F,Math.min(row+1,26-row)*.43F/5);
            for(int col=0;col<14;col++){
                float left=Math.max(-half,-.43F+col*.86F/14),right=Math.min(half,-.43F+(col+1)*.86F/14);
                if(left>=right)continue;
                double wave=(Math.sin(row*.46+col*.62-time*.065)+1)*.5;
                boolean portal=Math.abs((left+right)*.5)<opening*.44F;
                int r=portal?(int)(65+wave*82):(int)(35+wave*210);
                int g=portal?(int)(40+wave*75):r,b=portal?(int)(140+wave*110):r;
                for(boolean front:new boolean[]{false,true}){
                    float[][] points=front?new float[][]{{right,bottom},{left,bottom},{left,top},{right,top}}:
                            new float[][]{{left,bottom},{right,bottom},{right,top},{left,top}};
                    for(var p:points)out.addVertex(pose.last(),p[0],p[1],front?-.063F:.063F).setColor(r,g,b,255)
                            .setUv(.5F,.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),0,0,front?-1:1);
                }
            }
        }
    }
}
