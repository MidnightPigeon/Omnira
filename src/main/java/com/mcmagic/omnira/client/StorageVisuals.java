package com.mcmagic.omnira.client;

import com.mcmagic.omnira.storage.CrystalStorageContent;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class StorageVisuals {
    public static int color(int status){return switch(status){case 1->0x80E9A3;case 2->0x69B8FF;case 3->0xF07285;default->0xBE90F3;};}
    public static ModelResourceLocation base(){return com.mcmagic.omnira.client.renderer.CrystalGridRenderer.part("resonant_crystal_ball","base");}
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){event.register(base());}
    @SubscribeEvent public static void blockColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Block event){
        event.register((state,level,pos,tint)->0x284E91,com.mcmagic.omnira.space.sea.StellarCraftContent.ORB.get());
        CrystalBallBlockEntity.refreshClientTint=ball->{
            var client=Minecraft.getInstance();
            if(client.level!=ball.getLevel())return;
            var pos=ball.getBlockPos();
            client.levelRenderer.setBlocksDirty(pos.getX()-1,pos.getY()-1,pos.getZ()-1,pos.getX()+1,pos.getY()+1,pos.getZ()+1);
        };
        event.register((state,level,pos,tint)->color(level!=null&&level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball?ball.networkStatus:0),CrystalStorageContent.ITEM_ORB.get(),CrystalStorageContent.FLUID_ORB.get());
    }
    @SubscribeEvent public static void itemColors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){
        event.register((stack,tint)->0xFF284E91,com.mcmagic.omnira.space.sea.StellarCraftContent.ORB_ITEM.get());
        event.register((stack,tint)->0xFF000000|color(0),CrystalStorageContent.ITEM.get(),CrystalStorageContent.FLUID.get());
    }
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private RodRenderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new RodRenderer();return renderer;}
        },CrystalStorageContent.ROD.get());
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(com.mcmagic.omnira.registry.ModBlockEntityTypes.RESONANCE_CORE.get(),context->new NodeRenderer());
    }
    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer mesh,PoseStack pose,float[][] vertices,int color){
        for(var v:vertices)mesh.addVertex(pose.last().pose(),v[0],v[1],v[2]).setColor(color);
    }
    private static void cube(com.mojang.blaze3d.vertex.VertexConsumer mesh,PoseStack pose,float x,float y,float z,float r,int color){
        com.mcmagic.omnira.client.renderer.StellarVisuals.cube(pose,mesh,x,y,z,r,color);
    }
    public static final class RodRenderer extends BlockEntityWithoutLevelRenderer {
        public RodRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            double time=net.minecraft.Util.getMillis()/50.0;
            var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
            // Six planar faces, 1.5 model units wide and 18 long; no handle.
            float radius=.047F,bottom=-.0625F,top=1.0625F;
            for(int band=0;band<9;band++){
                float y0=bottom+(top-bottom)*band/9,y1=bottom+(top-bottom)*(band+1)/9;
                int shade=(int)(28+220*(.5+.5*Math.sin(time*.08-band*.65)));
                int color=0xFF000000|shade<<16|shade<<8|shade;
                for(int i=0;i<6;i++){
                    double a=i*Math.PI/3,b=(i+1)*Math.PI/3;
                    float x0=.5F+(float)Math.cos(a)*radius,z0=.5F+(float)Math.sin(a)*radius;
                    float x1=.5F+(float)Math.cos(b)*radius,z1=.5F+(float)Math.sin(b)*radius;
                    int faceShade=(int)(shade*(.82+.18*Math.cos(a)));
                    int faceColor=0xFF000000|faceShade<<16|faceShade<<8|faceShade;
                    quad(mesh,pose,new float[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z1},{x1,y0,z1}},faceColor);
                    if(band==0)quad(mesh,pose,new float[][]{{.5F,y0,.5F},{x0,y0,z0},{x1,y0,z1},{.5F,y0,.5F}},color);
                    if(band==8)quad(mesh,pose,new float[][]{{.5F,y1,.5F},{x1,y1,z1},{x0,y1,z0},{.5F,y1,.5F}},color);
                }
            }
            for(int i=0;i<5;i++){
                double phase=time*.035+i*Math.PI*2/5;
                cube(mesh,pose,.5F+(float)Math.cos(phase)*.12F,.08F+i*.2F+(float)Math.sin(phase*1.3)*.035F,
                    .5F+(float)Math.sin(phase)*.12F,.015625F,0xDFC994FF);
            }
        }
    }
    private static final class NodeRenderer implements BlockEntityRenderer<ResonanceCoreBlockEntity> {
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ResonanceCoreBlockEntity node){
            var bounds=new net.minecraft.world.phys.AABB(node.getBlockPos()).inflate(.1);
            var player=Minecraft.getInstance().player;
            return node.peer!=null&&node.output&&player!=null
                &&(player.getMainHandItem().is(CrystalStorageContent.ROD.get())||player.getOffhandItem().is(CrystalStorageContent.ROD.get()))
                ?bounds.minmax(new net.minecraft.world.phys.AABB(node.peer).inflate(.1)):bounds;
        }
        public int getViewDistance(){return 96;}
        @Override public void render(ResonanceCoreBlockEntity node,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var player=Minecraft.getInstance().player;
            boolean selected=node.selectedUntil>node.getLevel().getGameTime();
            if(node.peer!=null||selected){
                var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);double time=node.getLevel().getGameTime()+partial;
                int color=selected||node.output?0xF5FFFFFF:0xF5201729;
                for(int i=0;i<14;i++){
                    double angle=time*.045+i*Math.PI*2/14;
                    cube(mesh,pose,.5F+(float)Math.cos(angle)*.37F,.44F+(float)Math.sin(angle*2+time*.025)*.045F,
                        .5F+(float)Math.sin(angle)*.37F,.025F,color);
                }
            }
            if(player==null||node.peer==null||!node.output||!(player.getMainHandItem().is(CrystalStorageContent.ROD.get())||player.getOffhandItem().is(CrystalStorageContent.ROD.get())))return;
            var delta=node.peer.subtract(node.getBlockPos());var vector=new Vector3f(delta.getX(),delta.getY(),delta.getZ());
            var side=vector.cross(new Vector3f(0,1,0),new Vector3f());
            if(side.lengthSquared()<.001F)side.set(1,0,0);side.normalize().mul(.012F);
            var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
            for(int i=0;i<2;i++){
                float a=i/2F,b=(i+1)/2F;int color=i==0?0xEFFFFFFF:0xEF211927;
                float x=.5F+delta.getX()*a,y=.45F+delta.getY()*a,z=.5F+delta.getZ()*a;
                float xx=.5F+delta.getX()*b,yy=.45F+delta.getY()*b,zz=.5F+delta.getZ()*b;
                quad(mesh,pose,new float[][]{{x-side.x,y,z-side.z},{xx-side.x,yy,zz-side.z},{xx+side.x,yy,zz+side.z},{x+side.x,y,z+side.z}},color);
                quad(mesh,pose,new float[][]{{x,y-.012F,z},{xx,yy-.012F,zz},{xx,yy+.012F,zz},{x,y+.012F,z}},color);
            }
        }
    }
}
