package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.DreamMirror;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

public final class DreamMirrorRenderer extends EntityRenderer<DreamMirror> {
    private static final ResourceLocation FRAME=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/dream_mirror.png");
    private static final ResourceLocation GLASS=ResourceLocation.fromNamespaceAndPath("omnira","textures/block/mirror_rock.png");
    private final ModelPart frame;
    public DreamMirrorRenderer(EntityRendererProvider.Context context) {
        super(context);shadowRadius=.25F;frame=createFrame();
    }
    public static ModelPart createFrame() {
        var cubes=new java.util.ArrayList<float[]>();
        // Follow the unchanged glass opening, reserving separate pixels outside it for the frame.
        for(int row=0;row<26;row++) {
            float y=1.6F+row*25.6F/26;
            float inner=Math.min(6.88F,Math.min(row+1,26-row)*6.88F/5);
            pair(cubes,inner+.04F,y,-2.65F,.55F,25.6F/26,.8F);
            pair(cubes,inner+.59F,y,-2.1F,1.35F,25.6F/26,4.2F);
        }
        // Raised corner settings and stepped scrolls keep the straight-sided hexagon readable.
        for(float y:new float[]{5.2F,22.3F}) {
            pair(cubes,7.15F,y,-3F,1.6F,1.3F,1.2F);
            pair(cubes,7.55F,y-.45F,-2.8F,.8F,2.2F,.8F);
        }
        for(float center:new float[]{10.5F,18.3F}) {
            pair(cubes,8.82F,center-1.5F,-1.6F,.7F,3,3.2F);
            pair(cubes,9.52F,center-.85F,-1.3F,.65F,1.7F,2.6F);
            pair(cubes,10.17F,center-.3F,-1F,.55F,.6F,2);
            pair(cubes,8.1F,center-.4F,-2.9F,.8F,.8F,.9F);
        }
        for(boolean top:new boolean[]{false,true}) {
            crown(cubes,top,-3.1F,27.2F,-2.1F,6.2F,.7F,4.2F);
            crown(cubes,top,-2.25F,27.9F,-2.3F,4.5F,.7F,4.6F);
            crown(cubes,top,-1.35F,28.6F,-2.5F,2.7F,.7F,5);
            crown(cubes,top,-.55F,29.3F,-2.1F,1.1F,.9F,4.2F);
            crown(cubes,top,-.55F,27.4F,-3.1F,1.1F,1.2F,.7F);
        }
        return SpiritVoxelParts.boxes(cubes.toArray(float[][]::new));
    }
    private static void pair(java.util.List<float[]> cubes,float x,float y,float z,float width,float height,float depth) {
        cubes.add(new float[]{x,y,z,width,height,depth});
        cubes.add(new float[]{-x-width,y,z,width,height,depth});
    }
    private static void crown(java.util.List<float[]> cubes,boolean top,float x,float y,float z,float width,float height,float depth) {
        cubes.add(new float[]{x,top?y:28.8F-y-height,z,width,height,depth});
    }
    @Override public void render(DreamMirror entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light) {
        // In another mirror, draw the body and plain glass only, never its live texture.
        var reflected=MirrorScene.rendering()?null:MirrorScene.texture(entity,partial,buffers);
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
        pose.pushPose();pose.scale(1,1,.65F);
        frame.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(FRAME)),light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        surface(pose,buffers,GLASS,.0624F,false,light);
        surface(pose,buffers,reflected==null?GLASS:reflected,-.0624F,true,LightTexture.FULL_BRIGHT);
        pose.popPose();super.render(entity,yaw,partial,pose,buffers,light);
    }
    static void surface(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,float z,boolean front,int light) {
        // The scene already contains lighting; applying entity diffuse light again darkens it.
        var consumer=buffers.getBuffer(front?RenderType.entityTranslucentEmissive(texture):RenderType.entitySolid(texture));
        // Horizontal strips give the elongated hexagon straight vertical sides and pixel-cut tips.
        for(int row=0;row<26;row++) {
            float bottom=.1F+row*1.6F/26,top=.1F+(row+1)*1.6F/26;
            float half=Math.min(.43F,Math.min(row+1,26-row)*.43F/5);
            float[][] points=front?new float[][]{{half,bottom},{-half,bottom},{-half,top},{half,top}}
                    :new float[][]{{-half,bottom},{half,bottom},{half,top},{-half,top}};
            for(var point:points) consumer.addVertex(pose.last(),point[0],point[1],z).setColor(-1)
                    .setUv((point[0]+.43F)/.86F,(point[1]-.1F)/1.6F).setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light).setNormal(pose.last(),0,0,front?-1:1);
        }
    }
    @Override public ResourceLocation getTextureLocation(DreamMirror entity) {return FRAME;}
}
