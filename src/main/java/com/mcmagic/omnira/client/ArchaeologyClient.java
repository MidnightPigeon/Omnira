package com.mcmagic.omnira.client;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ArchaeologyClient {
    public static void hold(int id,boolean held){var level=Minecraft.getInstance().level;if(level!=null){var entity=level.getEntity(id);if(entity!=null)entity.getPersistentData().putBoolean("OmniraBrushHeld",held);}}
    static ModelResourceLocation model(String name){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/archaeology/"+name));}
    static double time(){var mc=Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float r,float g,float b){
        var model=Minecraft.getInstance().getModelManager().getModel(model(name));
        var vertices=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(42)))vertices.putBulkData(pose.last(),quad,r,g,b,1,light,overlay);
    }
    static void particles(PoseStack pose,MultiBufferSource buffers,int overlay,double t){
        for(int i=0;i<8;i++){
            double a=t*.055+i*Math.PI/4;pose.pushPose();pose.translate(.5+Math.cos(a)*.2,.55+Math.sin(a*.7+i)*.26,.5+Math.sin(a)*.2);
            draw("grain",pose,buffers,LightTexture.FULL_BRIGHT,overlay,i%3==0?1:.65F,1,i%2==0?1:.77F);pose.popPose();
        }
    }
    static void button(PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mesh=Minecraft.getInstance().getModelManager().getModel(model("button"));
        var vertices=buffers.getBuffer(Sheets.cutoutBlockSheet());
        for(var quad:mesh.getQuads(null,null,net.minecraft.util.RandomSource.create(42)))vertices.putBulkData(pose.last(),quad,1,1,1,1,light,overlay);
    }
    static void idleRig(PoseStack pose,MultiBufferSource buffers,int light,int overlay,int charge,double t){
        boolean ready=charge>=HyperDrillBlockEntity.CAPACITY;
        float wave=(float)(.5+.5*Math.sin(t*.035));
        float r=ready?.72F:.72F,g=ready?.88F+.12F*wave:.76F,b=ready?1-.16F*wave:.79F;
        draw(ready?"drill":"drill_depleted",pose,buffers,light,overlay,r,g,b);
        draw(ready?"rotor":"rotor_depleted",pose,buffers,light,overlay,r,g,b);
        button(pose,buffers,light,overlay);
    }
    static int aggregateIndex(ItemStack stack){
        for(int i=0;i<ArchaeologyContent.AGGREGATES.size();i++)if(stack.is(ArchaeologyContent.AGGREGATES.get(i).get()))return i;
        return -1;
    }
    static String aggregateModel(int i){return ArchaeologyContent.AGGREGATES.get(i).getId().getPath();}
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            double t=time();pose.pushPose();
            int aggregate=aggregateIndex(stack);
            if(aggregate>=0){
                var mesh=Minecraft.getInstance().getModelManager().getModel(model(aggregateModel(aggregate)));
                var vertices=buffers.getBuffer(Sheets.cutoutBlockSheet());
                for(var quad:mesh.getQuads(null,null,net.minecraft.util.RandomSource.create(42)))vertices.putBulkData(pose.last(),quad,1,1,1,1,light,overlay);
                float[][] colors={{1,.84F,.58F},{1,.4F,.5F},{1,.75F,.72F},{.87F,.95F,1},{.77F,1,.68F}};
                for(int i=0;i<3;i++){
                    double a=t*.025+i*Math.PI*2/3;pose.pushPose();pose.translate(.5+Math.cos(a)*.4,.5+Math.sin(a)*.4,.55);
                    float[] c=colors[aggregate];draw("grain",pose,buffers,LightTexture.FULL_BRIGHT,overlay,c[0],c[1],c[2]);pose.popPose();
                }
            }
            else if(stack.is(ArchaeologyContent.BRUSH.get())){draw("brush",pose,buffers,light,overlay,1,1,1);particles(pose,buffers,overlay,t);}
            else{pose.translate(1.0/3,0,1.0/3);pose.scale(1F/3,1F/3,1F/3);idleRig(pose,buffers,light,overlay,HyperDrillItem.charge(stack),t);}
            pose.popPose();
        }
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){
        for(String name:new String[]{"drill","rotor","drill_depleted","rotor_depleted","button","brush","grain"})event.register(model(name));
        for(int i=0;i<ArchaeologyContent.AGGREGATES.size();i++)event.register(model(aggregateModel(i)));
    }
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        var extension=new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private ItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ItemRenderer();return renderer;}
        };
        event.registerItem(extension,ArchaeologyContent.BRUSH.get(),ArchaeologyContent.DRILL_ITEM.get());
        for(var item:ArchaeologyContent.AGGREGATES)event.registerItem(extension,item.get());
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(ArchaeologyContent.BLINDFISH.get(),com.mcmagic.omnira.client.renderer.ChronalBlindfishRenderer::new);
        event.registerEntityRenderer(ArchaeologyContent.SEAT.get(),net.minecraft.client.renderer.entity.NoopRenderer::new);
        event.registerBlockEntityRenderer(ArchaeologyContent.DRILL_ENTITY.get(),context->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<HyperDrillBlockEntity>(){
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(HyperDrillBlockEntity be){return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(2);}
            @Override public void render(HyperDrillBlockEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            idleRig(pose,buffers,light,overlay,be.charge(),time());
        }});
        event.registerEntityRenderer(ArchaeologyContent.MOVING_RIG.get(),context->new net.minecraft.client.renderer.entity.EntityRenderer<MovingDrill>(context){
            @Override public ResourceLocation getTextureLocation(MovingDrill rig){return net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;}
            @Override public void render(MovingDrill rig,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
                pose.pushPose();pose.translate(-.5,0,-.5);
                float wave=(float)(.5+.5*Math.sin(time()*.035));draw("drill",pose,buffers,light,OverlayTexture.NO_OVERLAY,.72F,.88F+.12F*wave,1-.16F*wave);
                button(pose,buffers,light,OverlayTexture.NO_OVERLAY);
                pose.translate(.5,0,.5);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(time()*9%360)));pose.translate(-.5,0,-.5);
                draw("rotor",pose,buffers,light,OverlayTexture.NO_OVERLAY,.72F,.88F+.12F*wave,1-.16F*wave);pose.popPose();
            }
        });
    }
    private ArchaeologyClient(){}
}
