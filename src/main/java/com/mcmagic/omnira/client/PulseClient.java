package com.mcmagic.omnira.client;

import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class PulseClient {
    private static final net.minecraft.client.resources.model.ModelResourceLocation SHAFT_MODEL=
            net.minecraft.client.resources.model.ModelResourceLocation.standalone(net.minecraft.resources.ResourceLocation.parse("omnira:block/mana_engine_shaft"));
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(PulseContent.MENU.get(),com.mcmagic.omnira.client.screen.PulseOrbScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(PulseContent.ENTITY.get(),c->new BlockEntityRenderer<net.minecraft.world.level.block.entity.BlockEntity>(){
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(net.minecraft.world.level.block.entity.BlockEntity host){
                var orb=PulseOrbEntity.from(host);
                return new net.minecraft.world.phys.AABB(host.getBlockPos()).inflate(orb!=null&&orb.resonant()?.3:0);
            }
            @Override public void render(net.minecraft.world.level.block.entity.BlockEntity host,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var orb=PulseOrbEntity.from(host);if(orb==null)return;
            pose.pushPose();pose.translate(.5,.5,.5);
            pose.mulPose(com.mcmagic.omnira.block.OrbMount.rotation(host.getBlockState().getValue(PulseOrbBlock.MOUNT)));
            pose.translate(-.5,-.5,-.5);
            boolean kinetic=net.neoforged.fml.ModList.get().isLoaded("create");
            float angle=kinetic&&PulseOrbShaftState.mode(orb.shaftState,orb.inlet())==PulseOutput.ROTATION?PulseOrbShaftPhase.degrees(orb,orb.inlet()):0;
            orb((orb.potential+orb.occupied)/(double)orb.capacity(),orb.occupied/(double)orb.capacity(),orb.upgrade.getStackInSlot(0),pose,buffers,orb.getLevel().getGameTime()+partial,overlay,angle);
            if(orb.resonant())ResonanceSatellites.render(orb.frequency,pose,buffers,orb.getLevel().getGameTime()+partial,overlay);
            pose.popPose();
            for(var face:Direction.values())if(PulseOrbShaftState.connected(orb.shaftState,face)){
                int mode=PulseOrbShaftState.mode(orb.shaftState,face);
                if(face!=orb.inlet()&&mode!=PulseOutput.PULSE){
                    pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(com.mcmagic.omnira.block.OrbMount.rotation(face.getOpposite()));pose.translate(-.5,-.5,-.5);
                    axle(pose,buffers,overlay,kinetic&&mode==PulseOutput.ROTATION?PulseOrbShaftPhase.degrees(orb,face):0);pose.popPose();
                }
                if(orb.potential+orb.occupied>0){
                    if(PulseOrbShaftState.incoming(orb.shaftState,face))shaft(pose,buffers,face,orb.getLevel().getGameTime()+partial,false,mode);
                    if(PulseOrbShaftState.outgoing(orb.shaftState,face))shaft(pose,buffers,face,orb.getLevel().getGameTime()+partial,true,mode);
                }
            }
            }
        });
        e.registerBlockEntityRenderer(PulseContent.WIRE_ENTITY.get(),c->(wire,partial,pose,buffers,light,overlay)->wire(wire,pose,buffers,wire.getLevel().getGameTime()+partial));
    }
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){orb(.5,0,ItemStack.EMPTY,pose,buffers,net.minecraft.Util.getMillis()/50.0,overlay,0);}
                };return renderer;
            }
        },PulseContent.ORB_ITEM.get(),PulseContent.RESONANT_ITEM.get());
    }
    private static void orb(double fraction,double occupied,ItemStack upgrade,PoseStack pose,MultiBufferSource buffers,double time,int overlay,float angle){
        var mc=Minecraft.getInstance();int count=12+(int)(Math.sqrt(Math.clamp(fraction,0,1))*36);
        StaticModelQuads.draw(mc.getModelManager().getModel(CrystalBallRenderer.model("pulse_base")),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),LightTexture.FULL_BRIGHT,overlay);
        axle(pose,buffers,overlay,angle);
        pose.pushPose();pose.translate(0,.125,0);
        double speed=.006+.035*Math.clamp(fraction,0,1);
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<count;i++){
            double a=time*speed+i*2.39996,r=.15+.12*(i%5)/4.0;
            float y=.5F+(float)Math.sin(i*1.71+time*speed*.7)*.22F;
            int color=fraction<=0?0x506574:i<count*occupied/Math.max(.0001,fraction)?0xF1C76B:0x81D4FA;
            StellarVisuals.cube(pose,mesh,.5F+(float)(Math.cos(a)*r),y,.5F+(float)(Math.sin(a)*r),.012F,color,.9F);
        }
        StellarVisuals.centeredItem(upgrade,pose,buffers,overlay);
        StaticModelQuads.draw(mc.getModelManager().getModel(CrystalBallRenderer.model("shell")),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),LightTexture.FULL_BRIGHT,overlay);
        pose.popPose();
    }
    private static void axle(PoseStack pose,MultiBufferSource buffers,int overlay,float angle){
        var mc=Minecraft.getInstance();pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(angle));pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));
        pose.scale(1,1,.5F);pose.translate(-.5,-.5,0);
        StaticModelQuads.draw(mc.getModelManager().getModel(SHAFT_MODEL),pose,buffers.getBuffer(CrystalGlassLayer.ATLAS),LightTexture.FULL_BRIGHT,overlay);pose.popPose();
    }
    private static void wire(PulseWireEntity wire,PoseStack pose,MultiBufferSource buffers,double time){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);boolean active=wire.getBlockState().getValue(PulseWireBlock.POWERED);
        int color=active?0xDFC1FF:0x8F62BB;
        for(int i=0;i<5;i++)StellarVisuals.cube(pose,mesh,.5F+(i%3-1)*.045F,.5F+(i%2==0?.024F:-.024F),.5F+(i/3F-.7F)*.035F,.027F,i%2==0?color:0xA779CF,.95F);
        for(var face:Direction.values()){
            if(!PulseVisualConnections.wireConnects(wire.getLevel(),wire.getBlockPos(),face))continue;
            var lateral=face.getAxis()==Direction.Axis.X?Direction.UP:Direction.EAST;
            for(int i=1;i<=6;i++){
                float offset=i/12F,jitter=(i%2==0?1:-1)*.027F;
                StellarVisuals.cube(pose,mesh,.5F+face.getStepX()*offset+lateral.getStepX()*jitter,.5F+face.getStepY()*offset+lateral.getStepY()*jitter,.5F+face.getStepZ()*offset+lateral.getStepZ()*jitter,.029F,i%3==0?0xA779CF:color,.9F);
            }
            boolean incoming=(wire.visualFlow&(1<<face.ordinal()))!=0,outgoing=(wire.visualFlow&(1<<(face.ordinal()+6)))!=0;
            if(active&&(incoming||outgoing)){
                float phase=(float)((time*.04)%1),offset=(incoming?1-phase:phase)*.5F;
                StellarVisuals.cube(pose,mesh,.5F+face.getStepX()*offset,.5F+face.getStepY()*offset,.5F+face.getStepZ()*offset,.022F,0xFFFFFF,.95F);
            }
        }
    }
    public static void shaft(PoseStack pose,MultiBufferSource buffers,Direction face,double time,boolean outward,int mode){
        if(mode==PulseOutput.ROTATION)return;
        var mesh=buffers.getBuffer(CrystalGlassLayer.PULSE_RING);int color=mode==PulseOutput.ELECTRIC?0xF1C76B:0x81D4FA;
        var a=face.getAxis()==Direction.Axis.Y?Direction.EAST:Direction.UP;var b=face.getAxis()==Direction.Axis.Z?Direction.EAST:Direction.SOUTH;
        if(face.getAxis()==Direction.Axis.X)b=Direction.SOUTH;
        for(int ring=0;ring<2;ring++){
            var frame=PulseRingMotion.frame(time,ring,outward);
            for(int i=0;i<4;i++){
                float u=i==1||i==2?1:0,v=i>=2?1:0;
                float dx=(u-.5F)*PulseRingMotion.SIZE,dy=(v-.5F)*PulseRingMotion.SIZE;
                mesh.addVertex(pose.last().pose(),.5F+face.getStepX()*frame.offset()+a.getStepX()*dx+b.getStepX()*dy,
                        .5F+face.getStepY()*frame.offset()+a.getStepY()*dx+b.getStepY()*dy,
                        .5F+face.getStepZ()*frame.offset()+a.getStepZ()*dx+b.getStepZ()*dy)
                    .setColor(color>>16&255,color>>8&255,color&255,(int)(frame.alpha()*255)).setUv(u,v)
                    .setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(pose.last(),face.getStepX(),face.getStepY(),face.getStepZ());
            }
        }
    }
    private PulseClient(){}
}
