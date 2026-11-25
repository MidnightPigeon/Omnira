package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.screen.GuideBookScreen;
import com.mcmagic.omnira.guidance.ConstructionPattern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import java.util.List;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class GuideProjection {
    private record Placement(String scene, ConstructionPattern pattern, BlockPos base,RitualLayout ritual) {}
    private static LocalPlayer owner;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static GuideBookScreen book;
    private static ConstructionPattern held;
    private static RitualLayout ritual;
    private static Placement placed;
    private static String heldScene;
    private static int heldSince;
    public static boolean isPlaced(String scene){return placed!=null && placed.scene().equals(scene);}
    public static void cancel(String scene){if(isPlaced(scene))clear();}
    private static List<ConstructionPattern.Cell> missing=List.of();
    private static long missingMask;
    private static boolean waitRelease;
    public static boolean controlsHand(){return held!=null || waitRelease;}
    public static boolean holding(net.minecraft.world.entity.LivingEntity entity){return held!=null && entity==owner;}

    public static void begin(GuideBookScreen source,String scene){
        var mc=Minecraft.getInstance();
        if(mc.player==null || !mc.player.isAlive())return;
        owner=mc.player;
        world=mc.level;
        book=source;
        ritual=RitualLayout.supports(scene)?new RitualLayout(scene,mc.level):null;
        held=ritual==null?ConstructionPattern.of(scene):RitualLayout.pattern(scene);
        heldScene=scene;
        heldSince=owner.tickCount;
        mc.setScreen(null);
    }

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void click(InputEvent.InteractionKeyMappingTriggered event){
        if(held==null && !waitRelease)return;
        if(!event.isUseItem() && held==null)return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if(!event.isUseItem() || waitRelease || event.getHand()!=InteractionHand.MAIN_HAND)return;
        var mc=Minecraft.getInstance();
        waitRelease=true;
        if(mc.hitResult instanceof BlockHitResult hit && hit.getType()==HitResult.Type.BLOCK){
            placed=new Placement(heldScene,held,hit.getBlockPos().above(),ritual);
            missingMask=-1L;
            refreshPlacement();
        }else if(mc.hitResult==null || mc.hitResult.getType()==HitResult.Type.MISS){
            mc.setScreen(book);
        }else return;
        held=null;
        book=null;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(owner==null)return;
        if(mc.player!=owner || !owner.isAlive() || world!=mc.level){clear();return;}
        if(!mc.options.keyUse.isDown())waitRelease=false;
        if(mc.screen!=null){held=null;book=null;}
        if(placed!=null && placed.base().closerToCenterThan(owner.position(),64))refreshPlacement();
        if(placed==null && held==null && !waitRelease)clear();
    }

    private static void refreshPlacement(){
        var cells=placed.pattern().cells();
        long mask=placed.pattern().missingMask(world,placed.base());
        if(mask!=missingMask){
            var visible=new java.util.ArrayList<ConstructionPattern.Cell>();
            for(int i=0;i<cells.size();i++)if((mask&(1L<<i))!=0)visible.add(cells.get(i));
            missing=visible;
            missingMask=mask;
        }
        var layout=placed.ritual();
        boolean done=mask==0;
        if(layout!=null){
            layout.update(world,placed.base());
            for(int i=0;i<cells.size()-1;i++)done &= layout.complete(i);
        }
        if(done)placed=null;
    }

    @SubscribeEvent public static void hand(RenderHandEvent event){
        if(held==null || event.getHand()!=InteractionHand.MAIN_HAND)return;
        event.setCanceled(true);
        var pose=event.getPoseStack();
        int side=owner.getMainArm()==HumanoidArm.RIGHT?1:-1;
        pose.pushPose();
        pose.translate(side*.37,-.34,-.68);
        pose.mulPose(Axis.YP.rotationDegrees(-25*side));
        pose.scale(.12F,.12F,.12F);
        drawHeld(pose,event.getMultiBufferSource(),event.getPartialTick());
        pose.popPose();
    }

    @SubscribeEvent public static void world(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES || placed==null)return;
        var camera=event.getCamera().getPosition();
        var base=placed.base();
        if(!base.closerToCenterThan(camera,64))return;
        var pose=event.getPoseStack();
        pose.pushPose();
        pose.translate(base.getX()+.5-camera.x,base.getY()-camera.y,base.getZ()+.5-camera.z);
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        draw(pose,buffers,missing,true);
        if(placed.ritual()!=null)offerings(pose,fade(buffers,.4F),placed.scene(),placed.ritual(),true);
        pose.popPose();
    }

    public static void thirdPerson(PoseStack pose,MultiBufferSource buffers,net.minecraft.client.model.ArmedModel model,float partial){
        pose.pushPose();
        model.translateToHand(owner.getMainArm(),pose);
        pose.translate(0,.55,-.12);
        pose.scale(.13F,-.13F,-.13F);
        drawHeld(pose,buffers,partial);
        pose.popPose();
    }

    private static MultiBufferSource fade(MultiBufferSource buffers,float opacity){
        if(opacity>=1)return buffers;
        return type->{
            if(type==Sheets.solidBlockSheet() || type==Sheets.cutoutBlockSheet() || type==Sheets.translucentItemSheet()
                    || type==Sheets.translucentCullBlockSheet() || type==CrystalGlassLayer.FORGE_MIST)type=CrystalGlassLayer.ATLAS;
            return new com.mcmagic.omnira.client.renderer.FadeVertices(buffers.getBuffer(type),opacity);
        };
    }

    private static void drawHeld(PoseStack pose,MultiBufferSource buffers,float partial){
        if(ritual!=null){
            draw(pose,buffers,held.cells(),false);
            offerings(pose,buffers,heldScene,ritual,false);
            return;
        }
        int elapsed=owner.tickCount-heldSince;
        float phase=(Math.floorMod(elapsed,200)+partial)%200;
        float blend=net.minecraft.util.Mth.clamp(((phase%100)-80)/20,0,1);
        blend=blend*blend*(3-2*blend);
        float assembled=phase<100?blend:1-blend;
        if(assembled<1)draw(pose,fade(buffers,1-assembled),held.cells(),false);
        if(assembled>0){
            pose.pushPose();
            if(!heldScene.equals("advanced_assembly_table"))
                pose.translate(-.5,heldScene.equals("advanced_crystal_aggregation_ring")?.5:0,-.5);
            MachineConstructionScene.drawAssembled(heldScene,elapsed+partial,pose,fade(buffers,assembled));
            pose.popPose();
        }
    }

    private static void offerings(PoseStack pose,MultiBufferSource buffers,String scene,RitualLayout layout,boolean placed){
        var offsets=RitualLayout.anchors(scene);
        var mc=Minecraft.getInstance();
        for(int i=0;i<offsets.length;i++){
            if(placed && layout.complete(i))continue;
            pose.pushPose();
            pose.translate(offsets[i][0],com.mcmagic.omnira.client.renderer.CrystalPedestalRenderer.DISPLAY_HEIGHT,offsets[i][1]);
            float size=com.mcmagic.omnira.client.renderer.CrystalPedestalRenderer.DISPLAY_SCALE;
            pose.scale(size,size,size);
            mc.getItemRenderer().renderStatic(layout.display(i,owner.tickCount),net.minecraft.world.item.ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);
            pose.popPose();
        }
    }
    private static void draw(PoseStack pose,MultiBufferSource buffers,List<ConstructionPattern.Cell> cells,boolean ghost){
        var mc=Minecraft.getInstance();
        MultiBufferSource layers=CrystalGlassLayer.contents(buffers);
        if(ghost){
            var vertices=new GhostVertices(buffers.getBuffer(CrystalGlassLayer.ATLAS));
            layers=type->vertices;
        }
        for(var cell:cells){
            var pos=cell.offset();
            pose.pushPose();
            pose.translate(pos.getX()-.5,pos.getY(),pos.getZ()-.5);
            // Leave a visible seam between adjacent ghost blocks, including at occupied incorrect positions.
            if(ghost){pose.translate(-.002,-.002,-.002);pose.scale(1.004F,1.004F,1.004F);}
            var state=cell.display(owner.tickCount);
            if(state.is(com.mcmagic.omnira.registry.ModBlocks.PURE_VESSEL.get()))
                com.mcmagic.omnira.client.renderer.PureVesselRenderer.drawIntact(state,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            else mc.getBlockRenderer().renderSingleBlock(state,pose,layers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }

    private record GhostVertices(VertexConsumer target) implements VertexConsumer {
        public VertexConsumer addVertex(float x,float y,float z){target.addVertex(x,y,z);return this;}
        public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r*3/4,g,b,a*2/5);return this;}
        public VertexConsumer setUv(float u,float v){target.setUv(u,v);return this;}
        public VertexConsumer setUv1(int u,int v){target.setUv1(u,v);return this;}
        public VertexConsumer setUv2(int u,int v){target.setUv2(u,v);return this;}
        public VertexConsumer setNormal(float x,float y,float z){target.setNormal(x,y,z);return this;}
    }
    private static void clear(){ritual=null;owner=null;world=null;book=null;held=null;placed=null;missing=List.of();waitRelease=false;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){clear();}
    private GuideProjection(){}
}
