package com.mcmagic.omnira.client;

import com.mcmagic.omnira.engine.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SpacetimeEngineClient {
    private static final int[] ARRAY_FACES=SpacetimeEngineGeometry.ARRAY.stream().mapToInt(SpacetimeEngineClient::visibleArrayFaces).toArray();
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){e.register(SpacetimeEngineContent.MENU.get(),com.mcmagic.omnira.client.screen.SpacetimeEngineScreen::new);}
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(SpacetimeEngineContent.CONTROLLER.get(),context->new BlockEntityRenderer<SpacetimeEngineController>(){
        @Override public void render(SpacetimeEngineController be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(be.getBlockState().getValue(SpacetimeEngineBlock.PART)!=9||be.getLevel()==null)return;
            pose.pushPose();pose.translate(0,-1,0);draw(be,be.getLevel().getGameTime()+partial,pose,buffers,light,overlay);pose.popPose();
        }
        @Override public AABB getRenderBoundingBox(SpacetimeEngineController be){return new AABB(be.getBlockPos()).inflate(1.1,0,1.1).expandTowards(0,-1,0);}
    });}
    public static void draw(SpacetimeEngineController be,double ticks,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        for(var b:SpacetimeEngineGeometry.BASE)box(b,pose,buffers,light,overlay,63);
        // Square axles meet neighbouring shaft centres at Y=8, exactly at the outer faces.
        for(int side=0;side<4;side++){
            int color=SpacetimeEngineGeometry.outputColor(be==null?side:be.relativeSide(side));
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(-side*90));
            for(var b:SpacetimeEngineGeometry.DRIVE)box(b,pose,buffers,light,overlay,63);
            pose.translate(0,0,-1);pose.mulPose(Axis.ZP.rotationDegrees((float)(ticks*(be==null?16:be.generatedSpeed(side))*.3)));
            box(SpacetimeEngineGeometry.SHAFT.tint(color),pose,buffers,light,overlay,63);
            // Four rigid bearing lugs make the motor silhouette readable without a solid faceted shell.
            for(int a=0;a<4;a++){pose.pushPose();pose.mulPose(Axis.ZP.rotationDegrees(a*90));box(SpacetimeEngineGeometry.BEARING.tint(color),pose,buffers,light,overlay,63);pose.popPose();}
            pose.popPose();
        }
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotation((float)(ticks*.018)));
        for(var b:SpacetimeEngineGeometry.ROTOR)box(b,pose,buffers,light,overlay,63);
        pose.popPose();
        pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees((be==null?0:be.frontSide())*-90));
        for(var b:SpacetimeEngineGeometry.FRONT)box(b,pose,buffers,light,overlay,63);
        pose.popPose();
        pose.pushPose();pose.translate(.5,SpacetimeEngineGeometry.ARRAY_HEIGHT/16,.5);
        pose.mulPose(Axis.YP.rotation((float)SpacetimeEngineGeometry.arrayAngle(ticks)));
        for(int i=0;i<SpacetimeEngineGeometry.ARRAY.size();i++)box(SpacetimeEngineGeometry.ARRAY.get(i),pose,buffers,light,overlay,ARRAY_FACES[i]);
        for(int i=0;i<6;i++){
            double angle=i*Math.PI/3,x=Math.cos(angle)*SpacetimeEngineGeometry.ARRAY_RADIUS/16,z=Math.sin(angle)*SpacetimeEngineGeometry.ARRAY_RADIUS/16;
            pose.pushPose();pose.translate(x,0,z);
            box(SpacetimeEngineGeometry.NODE,pose,buffers,light,overlay,63);
            if(i%2==0&&be!=null)renderItem(be.grids.getStackInSlot(i/2),0,.17+Math.sin(ticks*.04+i)*.015,0,.32F,90,pose,buffers,light,overlay);
            else box(new SpacetimeEngineGeometry.Box(-.5,0,-.5,.5,1,.5,SpacetimeEngineGeometry.GREEN,true),pose,buffers,light,overlay,63);
            pose.popPose();
        }
        pose.popPose();
    }
    private static int visibleArrayFaces(SpacetimeEngineGeometry.Box a){
        int mask=63;
        for(var b:SpacetimeEngineGeometry.ARRAY){
            if(a.x()==b.x()&&a.xx()==b.xx()){if(a.z()==b.zz())mask&=~1;if(a.zz()==b.z())mask&=~2;}
            if(a.z()==b.z()&&a.zz()==b.zz()){if(a.x()==b.xx())mask&=~4;if(a.xx()==b.x())mask&=~8;}
        }return mask;
    }
    private static void renderItem(ItemStack stack,double x,double y,double z,float size,float pitch,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.isEmpty())return;pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.scale(size,size,size);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,CrystalGlassLayer.contents(buffers),Minecraft.getInstance().level,0);pose.popPose();
    }
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static void box(SpacetimeEngineGeometry.Box b,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int mask){
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("omnira:block/reaction_pool_"+(b.glow()?"glow":(b.color()>>>24)>=208?"frame":"glass")));
        float x=(float)b.x()/16,y=(float)b.y()/16,z=(float)b.z()/16,xx=(float)b.xx()/16,yy=(float)b.yy()/16,zz=(float)b.zz()/16;
        float[][] v={{x,y,z},{xx,y,z},{xx,yy,z},{x,yy,z},{x,y,zz},{xx,y,zz},{xx,yy,zz},{x,yy,zz}};
        for(int face=0;face<6;face++){
            if((mask&b.faces()&(1<<face))==0)continue;var f=FACES[face];var a=v[f[0]];var c=v[f[1]];var d=v[f[3]];
            var n=new org.joml.Vector3f(c[0]-a[0],c[1]-a[1],c[2]-a[2]).cross(new org.joml.Vector3f(d[0]-a[0],d[1]-a[1],d[2]-a[2])).normalize();
            var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
            for(int i=0;i<4;i++){
                var p=v[f[i]];float u=face<2?p[0]-x:face<4?p[2]-z:p[0]-x,w=face<4?p[1]-y:p[2]-z;
                out.addVertex(pose.last(),p[0],p[1],p[2]).setColor(b.color()).setUv(sprite.getU(u),sprite.getV(w)).setOverlay(overlay)
                        .setLight(b.glow()?LightTexture.FULL_BRIGHT:light).setNormal(pose.last(),n.x,n.y,n.z);
            }
        }
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent e){e.registerItem(new IClientItemExtensions(){private BlockEntityWithoutLevelRenderer renderer;
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
                pose.pushPose();pose.translate(.5,.18,.5);pose.scale(.29F,.29F,.29F);pose.translate(-.5,0,-.5);draw(null,time,pose,buffers,light,overlay);pose.popPose();
            }};return renderer;}
    },SpacetimeEngineContent.ITEM.get());}
    private SpacetimeEngineClient(){}
}
