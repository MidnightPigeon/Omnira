package com.mcmagic.omnira.client;

import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SuitcaseClient {
    private static ModelResourceLocation model(String name){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/"+name));}
    private static final ModelResourceLocation[][] HALVES=new ModelResourceLocation[2][4];
    private static final ModelResourceLocation HINGE=model("suitcase_hinge");
    private static final ModelResourceLocation SPARK=model("suitcase_spark");
    static {for(int side=0;side<2;side++)for(int phase=0;phase<4;phase++)HALVES[side][phase]=model("suitcase_"+(side==0?"front":"back")+"_"+phase);}
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){for(var row:HALVES)for(var m:row)e.register(m);e.register(HINGE);e.register(SPARK);}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ModBlockEntityTypes.SUITCASE.get(),ctx->new Renderer());}
    private static void draw(ModelResourceLocation id,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(id),ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
    }
    private static void drawCase(float angle,int phase,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        draw(HINGE,pose,buffers,light,overlay);
        for(int side=0;side<2;side++){
            pose.pushPose();pose.translate(.5,.125,.5);pose.mulPose(Axis.XP.rotationDegrees(side==0?-angle:angle));pose.translate(-.5,-.125,-.5);
            draw(HALVES[side][phase],pose,buffers,light,overlay);pose.popPose();
        }
    }
    @SubscribeEvent public static void activation(net.neoforged.neoforge.client.event.RenderLevelStageEvent event){
        if(event.getStage()!=net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        for(var player:mc.level.players()){
            if(!player.isAlive()||player.distanceToSqr(camera)>48*48)continue;
            for(var hand:net.minecraft.world.InteractionHand.values()){
                var stack=player.getItemInHand(hand);if(!stack.is(ModItems.RAIN_SOAKED_SUITCASE.get()))continue;
                double left=stack.getOrDefault(ModDataComponents.SUITCASE_OPEN_UNTIL,0L)-mc.level.getGameTime()-partial;
                if(left<=0||left>RainSoakedSuitcaseItem.ACTIVATION_TICKS)continue;
                double age=RainSoakedSuitcaseItem.ACTIVATION_TICKS-left;
                double progress=Math.min(1,Math.min(age/14,left/14));progress=progress*progress*(3-2*progress);
                float yaw=net.minecraft.util.Mth.rotLerp(partial,player.yRotO,player.getYRot());
                var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,yaw).scale(1.25);
                var pos=player.getPosition(partial).add(forward).add(0,.9+Math.sin(age*.09)*.04,0);
                pose.pushPose();pose.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);
                pose.mulPose(Axis.YP.rotationDegrees(-yaw));pose.scale(.8F,.8F,.8F);pose.translate(-.5,0,-.5);
                drawCase((float)(90*progress),(int)(age/4)%4,pose,buffers,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                pose.popPose();break;
            }
        }
    }
    private static final class Renderer implements BlockEntityRenderer<SuitcaseEntity> {
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(SuitcaseEntity be){return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(.25,0,.25).expandTowards(0,.7,0);}
        @Override public void render(SuitcaseEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(be.getLevel()==null)return;
            double t=be.getLevel().getGameTime()+partial;int phase=(int)(be.getLevel().getGameTime()/4%4);
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(SuitcaseBlock.FACING).toYRot()));pose.translate(-.5,.08,-.5);
            drawCase(60,phase,pose,buffers,light,overlay);pose.popPose();
            if(be.displayed().isEmpty())return;
            pose.pushPose();pose.translate(.5,1+Math.sin(t*.05)*.035,.5);
            if(be.musical()){
                pose.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(SuitcaseBlock.FACING).toYRot()));
                com.mcmagic.omnira.music.ViolinRender.showcase(t,pose,CrystalGlassLayer.contents(buffers),light);
            }else{
                pose.mulPose(Axis.YP.rotationDegrees((float)(t*1.5%360)));pose.scale(.6F,.6F,.6F);
                Minecraft.getInstance().getItemRenderer().renderStatic(be.displayed(),ItemDisplayContext.FIXED,light,overlay,pose,
                        CrystalGlassLayer.contents(buffers),be.getLevel(),0);
            }
            pose.popPose();
            if(be.musical())for(int i=0;i<6;i++){
                double a=t*.035+i*Math.PI/3;pose.pushPose();pose.translate(.5+Math.cos(a)*.38,.9+Math.sin(a*2)*.15,.5+Math.sin(a)*.38);
                draw(SPARK,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
            }
        }
    }
    private static final class Playback {Track sound;long performance=-1;}
    private static final Map<SuitcaseEntity,Playback> PLAYING=new WeakHashMap<>();
    public static void tick(SuitcaseEntity be){
        var mc=Minecraft.getInstance();boolean audible=be.musical()&&be.track()>=0&&be.track()<com.mcmagic.omnira.music.ViolinTracks.size()&&mc.player!=null&&mc.player.distanceToSqr(be.getBlockPos().getCenter())<=32*32;
        if(!audible){var old=PLAYING.remove(be);if(old!=null&&old.sound!=null)old.sound.finish();return;}
        var p=PLAYING.computeIfAbsent(be,b->new Playback());
        if(p.performance==be.performance())return;
        if(p.sound!=null)p.sound.finish();p.performance=be.performance();
        p.sound=new Track(be,be.track());mc.getSoundManager().play(p.sound);
    }
    private static final class Track extends AbstractTickableSoundInstance {
        private final net.minecraft.client.multiplayer.ClientLevel world;
        private final net.minecraft.core.BlockPos pos;
        private final java.lang.ref.WeakReference<SuitcaseEntity> source;
        Track(SuitcaseEntity be,int track){
            super(com.mcmagic.omnira.music.ViolinTracks.sound(track),net.minecraft.sounds.SoundSource.RECORDS,net.minecraft.util.RandomSource.create());
            world=(net.minecraft.client.multiplayer.ClientLevel)be.getLevel();pos=be.getBlockPos();source=new java.lang.ref.WeakReference<>(be);x=pos.getX()+.5;y=pos.getY()+1;z=pos.getZ()+.5;volume=1;looping=false;
        }
        void finish(){stop();}
        @Override public void tick(){var mc=Minecraft.getInstance();if(mc.level!=world||!world.hasChunkAt(pos)||!(world.getBlockEntity(pos) instanceof SuitcaseEntity be)||be!=source.get()||!be.musical()||mc.player==null||mc.player.distanceToSqr(pos.getCenter())>32*32)stop();}
    }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e){if(e.getLevel().isClientSide()){PLAYING.values().forEach(p->{if(p.sound!=null)p.sound.finish();});PLAYING.clear();}}
    private SuitcaseClient(){}
}
