package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.time.SundialBlockEntity;
import com.mcmagic.omnira.time.SundialContent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SundialClient {
    private static ModelResourceLocation model(String name){
        return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/sundial/"+name));
    }
    private static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,boolean glass,float alpha){
        var baked=Minecraft.getInstance().getModelManager().getModel(model(name));
        var out=buffers.getBuffer(glass?CrystalGlassLayer.ATLAS:Sheets.solidBlockSheet());
        for(var quad:baked.getQuads(null,null,net.minecraft.util.RandomSource.create(81)))
            out.putBulkData(pose.last(),quad,1,1,1,alpha,light,overlay);
    }
    private static double time(){var mc=Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:
            mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    private static void spinOnDial(PoseStack pose,float angle){
        pose.translate(.5,8.5/16,.5);
        pose.mulPose(Axis.XP.rotationDegrees(45));
        pose.mulPose(Axis.YP.rotationDegrees(angle));
        pose.mulPose(Axis.XP.rotationDegrees(-45));
        pose.translate(-.5,-8.5/16,-.5);
    }
    public static void render(SundialBlockEntity machine,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double t=time();boolean active=machine!=null&&machine.progress()>0;
        pose.pushPose();
        if(machine!=null){
            pose.translate(.5,0,.5);
            pose.mulPose(Axis.YP.rotationDegrees(180-machine.getBlockState().getValue(com.mcmagic.omnira.block.CrystalProcessingTableBlock.FACING).toYRot()));
            pose.translate(-.5,0,-.5);
        }
        draw("base",pose,buffers,light,overlay,false,1);
        draw("needle",pose,buffers,light,overlay,false,1);
        draw("face",pose,buffers,LightTexture.FULL_BRIGHT,overlay,true,.84F);
        pose.pushPose();spinOnDial(pose,(float)(t*(active?2:.35)%360));
        draw("sweep",pose,buffers,LightTexture.FULL_BRIGHT,overlay,true,active?.85F:.5F);pose.popPose();
        if(machine!=null)for(int i=0;i<32*machine.energy()/SundialBlockEntity.CAPACITY;i++)
            draw("charge_"+i,pose,buffers,LightTexture.FULL_BRIGHT,overlay,true,.95F);
        if(active){
            for(int side=0;side<2;side++){
                pose.pushPose();spinOnDial(pose,(float)((side==0?1:-1)*t*3.5+side*180)%360);
                draw("cutting_arc",pose,buffers,LightTexture.FULL_BRIGHT,overlay,true,.55F);
                pose.popPose();
            }
        }
        pose.popPose();
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            render(null,pose,buffers,light,overlay);
        }
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){
        for(var name:new String[]{"base","needle","face","sweep","cutting_arc"})event.register(model(name));
        for(int i=0;i<32;i++)event.register(model("charge_"+i));
    }
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){
        event.register(SundialContent.MENU.get(),com.mcmagic.omnira.client.screen.SundialScreen::new);
    }
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private ItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new ItemRenderer();return renderer;
            }
        },SundialContent.ITEM.get());
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(SundialContent.ENTITY.get(),context->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<SundialBlockEntity>(){
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(SundialBlockEntity machine){
                return new net.minecraft.world.phys.AABB(machine.getBlockPos()).inflate(.4);
            }
            @Override public void render(SundialBlockEntity machine,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                SundialClient.render(machine,pose,buffers,light,overlay);
            }
        });
    }
    private SundialClient(){}
}
