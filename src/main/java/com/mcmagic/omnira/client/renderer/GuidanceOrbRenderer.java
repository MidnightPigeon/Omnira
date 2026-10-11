package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.guidance.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class GuidanceOrbRenderer implements BlockEntityRenderer<GuidanceOrbEntity> {
    private static final int[] COLORS={0x080808,0x454545,0x858585,0xC4C4C4,0xFFFFFF};
    public GuidanceOrbRenderer(BlockEntityRendererProvider.Context c){}
    public static void drawOrb(int stage,PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay){
        var mc=Minecraft.getInstance();var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        var model=mc.getModelManager().getModel(CrystalBallRenderer.model("guidance_base"));
        var random=net.minecraft.util.RandomSource.create(42);
        var faces=new java.util.ArrayList<Direction>(java.util.List.of(Direction.values()));faces.add(null);
        int color=COLORS[Math.clamp(stage,0,4)];
        for(var face:faces){random.setSeed(42);for(var quad:model.getQuads(null,face,random))
            out.putBulkData(pose.last(),quad,(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,1,light,overlay);}
        StellarVisuals.baseStars(pose,buffers,time,true);
        pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
        StellarVisuals.cluster(pose,buffers,time,true);
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(CrystalBallRenderer.model("shell")),ItemStack.EMPTY,
                light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
    }
    @Override public void render(GuidanceOrbEntity orb,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double time=orb.getLevel()==null?0:orb.getLevel().getGameTime()+partial;
        drawOrb(orb.stage(),pose,buffers,time,light,overlay);
        var projections=orb.projections();
        for(int i=0;i<projections.length;i++){
            pose.pushPose();pose.translate(.5+(i-(projections.length-1)*.5)*.32,.64+Math.sin(time*.035)*.018,.5);
            pose.mulPose(Axis.YP.rotationDegrees((float)time*.7F));pose.scale(.24F,.24F,.24F);
            Minecraft.getInstance().getItemRenderer().renderStatic(GuidanceOrbEntity.projection(projections[i]),ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,overlay,pose,CrystalGlassLayer.contents(buffers),orb.getLevel(),i);pose.popPose();
        }
        if(orb.buttonsVisible()){
            for(Direction face:Direction.Plane.HORIZONTAL){String target=orb.target(face);if(!target.isEmpty())button(pose,buffers,face,"guidance.omnira.button."+target);}
            topButton(pose,buffers);
        }
        if(!orb.messageKey().isEmpty())message(pose,buffers,orb.messageKey());
    }
    private static void button(PoseStack pose,MultiBufferSource buffers,Direction face,String key){
        pose.pushPose();pose.translate(.5,.65,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));pose.translate(0,0,.505);
        crystalPlate(pose,buffers,12,4);
        var font=Minecraft.getInstance().font;var text=Component.translatable(key);
        float scale=Math.min(.012F,.7F/Math.max(1,font.width(text)));
        pose.translate(0,0,1F/32+.001F);pose.scale(scale,-scale,scale);
        font.drawInBatch8xOutline(text.getVisualOrderText(),-font.width(text)/2F,-4,0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
    private static void topButton(PoseStack pose,MultiBufferSource buffers){
        pose.pushPose();pose.translate(.5,1.06,.5);pose.mulPose(Axis.XP.rotationDegrees(-90));
        crystalDisc(pose,buffers);pose.popPose();
        var mc=Minecraft.getInstance();var text=Component.translatable("guidance.omnira.deeper");
        float scale=Math.min(.012F,.58F/Math.max(1,mc.font.width(text)));
        pose.pushPose();pose.translate(.5,1.19,.5);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(scale,-scale,scale);
        mc.font.drawInBatch8xOutline(text.getVisualOrderText(),-mc.font.width(text)/2F,-4,
                0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private static void crystalDisc(PoseStack pose,MultiBufferSource buffers){
        int[][] edge={{-2,-4},{2,-4},{2,-3},{3,-3},{3,-2},{4,-2},{4,2},{3,2},{3,3},{2,3},
                {2,4},{-2,4},{-2,3},{-3,3},{-3,2},{-4,2},{-4,-2},{-3,-2},{-3,-3},{-2,-3}};
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();float z=1F/32;
        for(int i=0;i<edge.length;i++){
            float ax=edge[i][0]/16F,ay=edge[i][1]/16F,bx=edge[(i+1)%edge.length][0]/16F,by=edge[(i+1)%edge.length][1]/16F;
            for(var v:new float[][]{{ax,ay,-z},{bx,by,-z},{bx,by,z},{ax,ay,z}})
                out.addVertex(matrix,v[0],v[1],v[2]).setColor(.64F,.82F,.9F,.65F);
            for(float side:new float[]{-1,1}){
                float x0=side>0?ax:bx,y0=side>0?ay:by,x1=side>0?bx:ax,y1=side>0?by:ay;
                for(var v:new float[][]{{0,0,side*z},{x0,y0,side*z},{(x0+x1)/2,(y0+y1)/2,side*z},{x1,y1,side*z}})
                    out.addVertex(matrix,v[0],v[1],v[2]).setColor(.76F,.91F,.97F,.5F);
            }
        }
    }
    // One model-pixel thickness, with a continuous exterior and no buried shared faces.
    private static void crystalPlate(PoseStack pose,MultiBufferSource buffers,float width,float height){
        float x=width/32,y=height/32,c=1F/16,z=1F/32;
        float[][] edge={{-x+c,-y},{x-c,-y},{x,-y+c},{x,y-c},{x-c,y},{-x+c,y},{-x,y-c},{-x,-y+c}};
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        for(int i=0;i<edge.length;i++){
            var a=edge[i];var b=edge[(i+1)%edge.length];
            for(var v:new float[][]{{a[0],a[1],-z},{b[0],b[1],-z},{b[0],b[1],z},{a[0],a[1],z}})
                out.addVertex(matrix,v[0],v[1],v[2]).setColor(.64F,.82F,.9F,.65F);
        }
        int[][] caps={{0,1,2,7},{7,2,3,6},{6,3,4,5}};
        for(float side:new float[]{-1,1})for(var cap:caps)for(int i=0;i<4;i++){
            var v=edge[cap[side>0?i:3-i]];
            out.addVertex(matrix,v[0],v[1],side*z).setColor(.76F,.91F,.97F,.5F);
        }
    }
    private static void message(PoseStack pose,MultiBufferSource buffers,String key){
        var mc=Minecraft.getInstance();var lines=mc.font.split(Component.translatable(key),180);
        pose.pushPose();pose.translate(.5,1.35+lines.size()*.06,.5);
        pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());pose.scale(.012F,-.012F,.012F);
        for(int i=0;i<lines.size();i++)mc.font.drawInBatch8xOutline(lines.get(i),-mc.font.width(lines.get(i))/2F,i*11,
                0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(GuidanceOrbEntity orb){return new net.minecraft.world.phys.AABB(orb.getBlockPos()).inflate(1.5);}
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            int stage=0;var state=stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if(state!=null)try{stage=Integer.parseInt(state.properties().getOrDefault("stage","0"));}catch(NumberFormatException ignored){}
            var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            drawOrb(stage,pose,buffers,time,light,overlay);
        }
    }
}
