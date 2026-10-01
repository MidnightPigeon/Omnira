package com.mcmagic.omnira.client;

import com.mcmagic.omnira.reversal.*;
import com.mcmagic.omnira.client.renderer.*;
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
public final class ReversalClient {
    private static final double ORB_CENTER_Y=11.5/16.0;
    private static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/reversal/"+part));}
    private static void draw(String name,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float r,float g,float b){
        var m=Minecraft.getInstance().getModelManager().getModel(model(name));var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(var quad:m.getQuads(null,null,net.minecraft.util.RandomSource.create(42)))out.putBulkData(pose.last(),quad,r,g,b,1,light,overlay);
    }
    private static double time(){var mc=Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);}
    private static void grain(PoseStack p,MultiBufferSource b,int o,double x,double y,double z,float r,float g,float blue){p.pushPose();p.translate(x,y,z);draw("grain",p,b,LightTexture.FULL_BRIGHT,o,r,g,blue);p.popPose();}
    public static void machine(ReversalBlockEntity be,PoseStack p,MultiBufferSource b,int light,int overlay){
        double t=time();boolean active=be!=null&&be.progress()>0;float wave=(float)(.5+.5*Math.sin(t*.07));
        draw("base",p,b,light,overlay,active?.55F:1,active?.78F+.22F*wave:1,active?1-.22F*wave:1);
        p.pushPose();p.translate(.5,ORB_CENTER_Y,.5);p.mulPose(Axis.YP.rotationDegrees((float)(t*.9%360)));
        draw("infinity",p,b,LightTexture.FULL_BRIGHT,overlay,1,1,1);
        for(int i=0;i<3;i++){
            double a=t*.04+i*Math.PI*2/3;
            grain(p,b,overlay,3.2*Math.cos(a)/16,1.7*Math.sin(2*a)/16,(Math.sin(a)+.6)/16,.92F,1,.95F);
        }
        p.popPose();
        for(int i=0;i<4+(be==null?0:be.energy()/2);i++){double a=t*.027+i*2.4;grain(p,b,overlay,.5+Math.sin(a)*.23,ORB_CENTER_Y+Math.sin(a*.67+i)*.20,.5+Math.cos(a)*.23,i%4==0?1:.55F,1,i%4==0?1:.65F);}
        if(be!=null)for(int i=0;i<2;i++){
            var item=be.getItem(i);if(item.isEmpty())continue;p.pushPose();double a=t*.02+i*Math.PI;
            p.translate(.5+Math.sin(a)*.24,ORB_CENTER_Y+Math.cos(a*.75)*.12,.5+Math.cos(a)*.21);p.mulPose(Axis.XP.rotationDegrees((float)t*1.4F));p.mulPose(Axis.YP.rotationDegrees((float)t*1.8F));p.mulPose(Axis.ZP.rotationDegrees((float)t*.7F));p.scale(.2F,.2F,.2F);
            MultiBufferSource contents=type->b.getBuffer(type==Sheets.translucentItemSheet()?CrystalGlassLayer.ATLAS:type);
            UnstableAggregateRenderer.inStorage(()->Minecraft.getInstance().getItemRenderer().renderStatic(item,ItemDisplayContext.GROUND,light,overlay,p,contents,be.getLevel(),0));p.popPose();
        }
        draw("shell",p,b,light,overlay,1,1,1);
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack s,ItemDisplayContext context,PoseStack p,MultiBufferSource b,int light,int overlay){
            if(s.is(ReversalContent.MACHINE_ITEM.get())){machine(null,p,b,light,overlay);return;}
            float r=s.is(ReversalContent.REX.get())?1:.35F,g=s.is(ReversalContent.PTERO.get())?1:.4F,blue=s.is(ReversalContent.MOSA.get())?1:.35F;
            p.pushPose();p.translate(.5,.5,.5);p.mulPose(Axis.YP.rotationDegrees((float)time()));draw("essence",p,b,light,overlay,r,g,blue);p.popPose();
            for(int i=0;i<9;i++){double a=time()*.07+i*2.1;grain(p,b,overlay,.5+Math.cos(a)*.26,.5+Math.sin(a*.73)*.24,.5+Math.sin(a)*.26,r,g,blue);}
        }
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){for(String name:new String[]{"base","shell","infinity","grain","essence"})e.register(model(name));}
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(ReversalContent.MENU.get(),com.mcmagic.omnira.client.screen.ReversalScreen::new);}
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){private ItemRenderer r;public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(r==null)r=new ItemRenderer();return r;}},ReversalContent.MACHINE_ITEM.get(),ReversalContent.REX.get(),ReversalContent.MOSA.get(),ReversalContent.PTERO.get());}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(ReversalContent.ENTITY.get(),context->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<ReversalBlockEntity>(){
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ReversalBlockEntity be){return new net.minecraft.world.phys.AABB(be.getBlockPos()).expandTowards(0,.3,0);}
            @Override public void render(ReversalBlockEntity be,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){machine(be,p,b,light,overlay);}
        });
    }
    private ReversalClient(){}
}
