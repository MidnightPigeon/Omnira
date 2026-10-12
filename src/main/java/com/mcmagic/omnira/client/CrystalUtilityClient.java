package com.mcmagic.omnira.client;

import com.mcmagic.omnira.workstation.*;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
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
public final class CrystalUtilityClient {
    private static ModelResourceLocation model(String name){return ModelResourceLocation.standalone(ResourceLocation.parse("omnira:block/utility/"+name));}
    private static void draw(String name,PoseStack p,MultiBufferSource buffers,int light,int overlay,boolean glass){
        var baked=Minecraft.getInstance().getModelManager().getModel(model(name));
        var out=buffers.getBuffer(glass?CrystalGlassLayer.ATLAS:Sheets.solidBlockSheet());
        for(var quad:baked.getQuads(null,null,net.minecraft.util.RandomSource.create(71)))out.putBulkData(p.last(),quad,1,1,1,glass?.8F:1,light,overlay);
    }
    private static void body(int index,String name,PoseStack p,MultiBufferSource buffers,int light,int overlay,boolean glass){
        p.pushPose();
        if(index%3==2){p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(90));p.translate(-.5,0,-.5);}
        draw(name,p,buffers,light,overlay,glass);p.popPose();
    }
    private static void render(int index,CrystalWorkstationEntity work,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
        p.pushPose();
        if(work!=null){p.translate(.5,0,.5);p.mulPose(Axis.YP.rotationDegrees(180-work.getBlockState().getValue(CrystalWorkstationBlock.FACING).toYRot()));p.translate(-.5,0,-.5);}
        String id=CrystalWorkstations.IDS[index];body(index,id+"_glass",p,b,light,overlay,true);body(index,id+"_frame",p,b,light,overlay,false);
        body(index,id+"_detail",p,b,light,overlay,false);
        var mc=Minecraft.getInstance();float time=mc.level==null?0:mc.level.getGameTime()+partial;
        if(index%3==1){
            p.pushPose();p.translate(.5,.625,.5);p.mulPose(Axis.XP.rotationDegrees(time*(index>=3?2:.4F)));p.translate(-.5,-.625,-.5);
            draw(id+"_wheel",p,b,light,overlay,true);p.popPose();
        }
        if(index==3||index==5){
            float speed=work==null?0:work.host instanceof WorkstationAccess access?access.shaftSpeed():work.rpm;
            float angle=time*speed*.3F;
            p.pushPose();p.translate(.5,.5,.5);p.mulPose(Axis.ZP.rotationDegrees(angle));p.translate(-.5,-.5,-.5);
            draw(id+"_shaft",p,b,work!=null&&work.electrical?LightTexture.FULL_BRIGHT:light,overlay,false);p.popPose();
        }
        if(work!=null&&index%3!=0&&(work.powered()||work.burn>0))body(index,id+"_light",p,b,LightTexture.FULL_BRIGHT,overlay,true);
        var displayed=work==null?ItemStack.EMPTY:!work.getItem(0).isEmpty()?work.getItem(0):index%3!=0?work.getItem(1):ItemStack.EMPTY;
        if(!displayed.isEmpty()){
            int kind=index%3;
            double height=kind==0?.48:kind==1?.63:1.12;
            float scale=kind==2?.4F:.32F;
            p.pushPose();p.translate(.5,height+Math.sin(time*.035)*.018,.5);
            p.mulPose(Axis.YP.rotationDegrees(time*.6F%360));p.scale(scale,scale,scale);
            mc.getItemRenderer().renderStatic(displayed,ItemDisplayContext.FIXED,light,overlay,p,
                    CrystalGlassLayer.contents(b),work.getLevel(),0);
            p.popPose();
        }
        p.popPose();
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){
        for(var id:CrystalWorkstations.IDS)for(var layer:new String[]{"glass","frame","detail","wheel","shaft","light"})e.register(model(id+"_"+layer));
    }
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(CrystalWorkstations.ENTITY.get(),context->(entity,partial,p,b,light,overlay)->{
            var work=CrystalWorkstations.work(entity);render(CrystalWorkstations.index(entity.getBlockState()),work,partial,p,b,light,overlay);
        });
    }
    @SubscribeEvent public static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(CrystalWorkstations.MENU.get(),com.mcmagic.omnira.client.screen.CrystalUtilityScreen::new);}
    @SubscribeEvent public static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext display,PoseStack p,MultiBufferSource b,int light,int overlay){
                        for(int i=0;i<CrystalWorkstations.BLOCKS.size();i++)if(stack.is(CrystalWorkstations.BLOCKS.get(i).get().asItem())){render(i,null,0,p,b,light,overlay);break;}
                    }
                };
                return renderer;
            }
        },CrystalWorkstations.BLOCKS.stream().map(x->x.get().asItem()).toArray(Item[]::new));
    }
}
