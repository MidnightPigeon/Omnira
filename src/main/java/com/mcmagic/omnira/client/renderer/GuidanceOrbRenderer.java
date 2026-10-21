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
        int color=COLORS[Math.clamp(stage,0,4)];
        StaticModelQuads.drawTinted(model,pose,out,light,overlay,color,true);
        if(stage<5)StellarVisuals.baseStars(pose,buffers,time,true);
        pose.pushPose();pose.translate(0,.125+Math.sin(time*.035)*.018,0);
        if(stage==5)understanding(pose,buffers,time);else StellarVisuals.cluster(pose,buffers,time,true);
        StaticModelQuads.draw(mc.getModelManager().getModel(CrystalBallRenderer.model("shell")),pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS),light,overlay);pose.popPose();
    }
    private static void understanding(PoseStack pose,MultiBufferSource buffers,double time){
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        for(var v:UnderstandingVisual.CORE)out.addVertex(matrix,v.x(),v.y(),v.z()).setColor(v.shade(),v.shade(),v.shade(),1);
        for(int i=0;i<UnderstandingVisual.MOTES;i++){
            var p=UnderstandingVisual.mote(i,time);
            StellarVisuals.cube(pose,out,p.x(),p.y(),p.z(),p.size(),0xFFFFFF,p.alpha());
        }
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
            topButton(pose,buffers,orb.topLabelKey());
        }
        if(!orb.messageKey().isEmpty())message(pose,buffers,orb.messageKey());
    }
    private static void button(PoseStack pose,MultiBufferSource buffers,Direction face,String key){
        pose.pushPose();pose.translate(.5,.65,.5);
        pose.mulPose(Axis.YP.rotationDegrees(-face.toYRot()));pose.translate(0,0,.505);
        crystalButton(pose,buffers,GuidanceButtonMesh.PLATE);
        var font=Minecraft.getInstance().font;var text=Component.translatable(key);
        float scale=Math.min(.012F,.7F/Math.max(1,font.width(text)));
        pose.translate(0,0,1F/32+.001F);pose.scale(scale,-scale,scale);
        font.drawInBatch8xOutline(text.getVisualOrderText(),-font.width(text)/2F,-4,0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
    private static void topButton(PoseStack pose,MultiBufferSource buffers,String labelKey){
        pose.pushPose();pose.translate(.5,1.06,.5);pose.mulPose(Axis.XP.rotationDegrees(-90));
        crystalButton(pose,buffers,GuidanceButtonMesh.DISC);pose.popPose();
        var mc=Minecraft.getInstance();var text=Component.translatable(labelKey);
        float scale=Math.min(.012F,.58F/Math.max(1,mc.font.width(text)));
        pose.pushPose();pose.translate(.5,1.19,.5);pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        pose.scale(scale,-scale,scale);
        mc.font.drawInBatch8xOutline(text.getVisualOrderText(),-mc.font.width(text)/2F,-4,
                0xFFE7F2FF,0xFF23303A,pose.last().pose(),buffers,LightTexture.FULL_BRIGHT);pose.popPose();
    }
    private static void crystalButton(PoseStack pose,MultiBufferSource buffers,GuidanceButtonMesh mesh){
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        for(int i=0;i<mesh.size();i++){
            var v=mesh.vertex(i);
            var vertex=out.addVertex(matrix,v.x(),v.y(),v.z());
            if(v.rim())vertex.setColor(.64F,.82F,.9F,.65F);
            else vertex.setColor(.76F,.91F,.97F,.5F);
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
            int stage=stack.is(GuidanceContent.UNDERSTANDING_ITEM.get())?5:0;var state=stack.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if(state!=null)try{stage=Integer.parseInt(state.properties().getOrDefault("stage",Integer.toString(stage)));}catch(NumberFormatException ignored){}
            var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            drawOrb(stage,pose,buffers,time,light,overlay);
        }
    }
}
