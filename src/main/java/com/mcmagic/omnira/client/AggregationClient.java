package com.mcmagic.omnira.client;

import com.mcmagic.omnira.aggregation.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.renderer.SpellCoreRenderer;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class AggregationClient {
    private static final String[] PARTS={"frame","node","orb","panel","button","crystal","mote"};
    private static ModelResourceLocation model(String name){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/aggregation/"+name));}
    private static double time(){var mc=Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    private static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,float alpha){
        var out=buffers.getBuffer(name.equals("button")?Sheets.solidBlockSheet():CrystalGlassLayer.ATLAS);
        for(var quad:Minecraft.getInstance().getModelManager().getModel(model(name)).getQuads(null,null,net.minecraft.util.RandomSource.create(42)))
            out.putBulkData(pose.last(),quad,((color>>16)&255)/255F,((color>>8)&255)/255F,(color&255)/255F,alpha,light,overlay);
    }
    private static void part(String name,double x,double y,double z,int color,float alpha,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(x-.5,y-.5,z-.5);draw(name,pose,buffers,light,overlay,color,alpha);pose.popPose();
    }
    private static void item(ItemStack stack,double x,double y,double z,float scale,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stack.isEmpty())return;
        pose.pushPose();pose.translate(x,y,z);pose.scale(scale,scale,scale);
        if(stack.is(com.mcmagic.omnira.registry.ModItems.SPELL_INK.get()))pose.translate(0,-1.0/16,0);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack,ItemDisplayContext.NONE,light,overlay,pose,buffers,Minecraft.getInstance().level,0);pose.popPose();
    }
    private static void mote(double x,double y,double z,int color,float size,float alpha,PoseStack pose,MultiBufferSource buffers,int overlay){
        pose.pushPose();pose.translate(x,y,z);pose.scale(size,size,size);
        part("mote",-.03125,-.03125,-.03125,color,alpha,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
    }
    private static void button(PoseStack pose,MultiBufferSource buffers,int light,int overlay,double depth){
        pose.pushPose();pose.translate(0,AggregationLayout.BUTTON_Y,AggregationLayout.BUTTON_Z);
        pose.mulPose(Axis.XP.rotationDegrees((float)AggregationLayout.BUTTON_TILT));
        part("panel",0,0,0,0xB4E1EF,1,pose,buffers,light,overlay);
        part("button",0,0,.055-depth,0xFFFFFF,1,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
    }
    public static void render(AggregationRingBlockEntity machine,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        double t=time();boolean powered=machine!=null&&machine.powered();
        pose.pushPose();pose.translate(.5,.5,.5);
        if(machine!=null)pose.mulPose(Axis.YP.rotationDegrees(-machine.getBlockState().getValue(AggregationRingBlock.FACING).toYRot()));
        pose.translate(0,AggregationLayout.bob(t,powered),0);
        button(pose,buffers,light,overlay,machine==null?0:machine.buttonDepth(t));
        part("frame",0,0,0,0xD7F4FA,.8F,pose,buffers,light,overlay);
        double progress=machine==null?0:machine.renderProgress(t);
        for(int i=0;i<8;i++){
            var xy=AggregationLayout.NODES[i];part("node",xy[0],xy[1],0,AggregationLayout.COLORS[i],1,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            if(machine!=null&&(progress==0||AggregationLayout.ingredient(progress,i)==null))item(machine.getItem(i),xy[0],xy[1],0,.28F,pose,buffers,light,overlay);
        }
        for(int i=0;i<2;i++){
            var p=AggregationLayout.orb(t,i,powered);
            part("orb",p.x,p.y,0,AggregationLayout.COLORS[8+i],.65F,pose,buffers,light,overlay);
            if(machine!=null)item(machine.getItem(8+i),p.x,p.y,0,.27F,pose,buffers,light,overlay);
        }
        if(powered)for(int i=0;i<10;i++){
            var p=AggregationLayout.decoration(t,i);
            int color=net.minecraft.util.Mth.hsvToRgb((float)((t*.002+i*.075)%1),.32F,1);
            mote(p.x,p.y,p.z,color,.6F,.9F,pose,buffers,overlay);
        }
        if(machine!=null){
            if(powered){
                pose.pushPose();pose.scale(.6F,.6F,.6F);pose.translate(-.5,-.5,-.5);
                SpellCoreRenderer.renderCore(machine.getItem(AggregationLayout.CORE),pose,buffers,LightTexture.FULL_BRIGHT,overlay,t);pose.popPose();
            }
            item(machine.getItem(AggregationLayout.OUTPUT),0,0,.4,.42F,pose,buffers,light,overlay);
            if(progress>0){
                for(int i=0;i<8;i++)if(!machine.getItem(i).isEmpty()){
                    var at=AggregationLayout.ingredient(progress,i);if(at==null)continue;
                    pose.pushPose();pose.translate(at.x,at.y,at.z);
                    pose.mulPose(Axis.ZP.rotationDegrees((float)(t*2.4+i*35)));
                    pose.mulPose(Axis.YP.rotationDegrees((float)(t*1.8+i*20)));
                    item(machine.getItem(i),0,0,0,.2F,pose,buffers,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
                    for(int j=1;j<=6;j++){
                        var tail=AggregationLayout.ingredient(Math.max(0,progress-j*.006),i);if(tail==null)continue;
                        mote(tail.x,tail.y,tail.z,AggregationLayout.COLORS[i],.5F-j*.045F,.85F-j*.1F,pose,buffers,overlay);
                    }
                }
            }
        }
        pose.popPose();
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            if(stack.is(AggregationContent.ITEM.get())){
                pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.45F,.45F,.45F);pose.translate(-.5,-.5,-.5);
                render(null,pose,buffers,light,overlay);pose.popPose();return;
            }
            draw("crystal",pose,buffers,light,overlay,0xFFFFFF,.9F);
            var pattern=stack.get(ModDataComponents.SPELL_PATTERN.get());double t=time();
            for(int i=0;i<4;i++){
                int color=pattern==null?0xADF0DE:(i%2==1&&pattern.shapeElement()!=null?pattern.shapeElement():pattern.targetElement()).tooltipColor();
                double a=t*.04+i*Math.PI/2;
                part("mote",.5+Math.cos(a)*.36,.5+Math.sin(a*1.5)*.28,.5+Math.sin(a)*.3,color,.9F,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            }
        }
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){for(var part:PARTS)e.register(model(part));}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(AggregationContent.MENU.get(),com.mcmagic.omnira.client.screen.AggregationRingScreen::new);}
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){private ItemRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ItemRenderer();return renderer;}
        },AggregationContent.ITEM.get(),AggregationContent.CRYSTAL.get());
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(AggregationContent.ENTITY.get(),c->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<AggregationRingBlockEntity>(){
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(AggregationRingBlockEntity b){return new net.minecraft.world.phys.AABB(b.getBlockPos()).inflate(1.5);}
            @Override public void render(AggregationRingBlockEntity b,float p,PoseStack pose,MultiBufferSource buffers,int light,int overlay){AggregationClient.render(b,pose,buffers,light,overlay);}
        });
    }
    private AggregationClient(){}
}
