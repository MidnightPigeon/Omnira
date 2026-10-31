package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.FulfillmentStarItem;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.renderer.StellarVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.extensions.common.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** A world-space activation driven by the server timer, with the item mesh split into eight sectors. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class FulfillmentStarVisuals {
    private static final ModelResourceLocation BODY=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/fulfillment_star_body"));
    private static final int[] COLORS={0xFFFFD783,0xFF8BE2D4,0xFF9ABEFF,0xFFE8A2E5,0xFFC1A4F0,0xFFFFF1BE,0xFF97D9F4,0xFFD5B9FF,0xFFABEDB8};
    private static final ModelResourceLocation[] PARTS=new ModelResourceLocation[8];
    static {for(int i=0;i<8;i++)PARTS[i]=ModelResourceLocation.standalone(ResourceLocation.parse("omnira:item/fulfillment_star_fragment_"+i));}
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event){event.register(BODY);for(var part:PARTS)event.register(part);}
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private Renderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new Renderer();return renderer;}
        },ModItems.FULFILLMENT_STAR.get());
    }
    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        Renderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var mc=Minecraft.getInstance();double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(BODY),stack,light,overlay,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
            pose.pushPose();pose.translate(.5,.5,.5);motes(time,0,pose,buffers);pose.popPose();
        }
    }
    /** Nine bounded cubes share the existing glass-color batch; no particle entities accumulate. */
    private static void motes(double time,double broken,PoseStack pose,MultiBufferSource buffers){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<COLORS.length;i++){
            double a=time*.035+i*Math.PI*2/COLORS.length;
            double inclination=(i%3-1)*.72,r=.275+.015*Math.sin(time*.023+i);
            double x=Math.cos(a)*r,y=Math.sin(a)*r*Math.cos(inclination),z=Math.sin(a)*r*Math.sin(inclination);
            double expansion=1+broken*2;
            int color=((int)(255*(1-broken))<<24)|(COLORS[i]&0xFFFFFF);
            StellarVisuals.cube(pose,mesh,(float)(x*expansion),(float)(y*expansion-broken*broken*.8),(float)(z*expansion),.018F+(i%3)*.003F,color);
        }
    }
    @SubscribeEvent public static void hand(RenderHandEvent event){
        var player=Minecraft.getInstance().player;if(player==null)return;
        int timer=player.getData(ModAttachments.FULFILLMENT_ACTIVATION);
        if(timer!=0&&event.getHand()==(timer>0?net.minecraft.world.InteractionHand.MAIN_HAND:net.minecraft.world.InteractionHand.OFF_HAND))event.setCanceled(true);
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();var buffers=mc.renderBuffers().bufferSource();
        for(var player:mc.level.players()){
            int timer=player.getData(ModAttachments.FULFILLMENT_ACTIVATION);
            if(timer==0||!player.isAlive()||player.distanceToSqr(camera)>48*48)continue;
            double age=FulfillmentStarItem.ACTIVATION_TICKS-Math.abs(timer)+partial;
            double rise=Math.clamp(age/28,0,1);rise=rise*rise*(3-2*rise);
            double broken=Math.clamp((age-FulfillmentStarItem.SHATTER_TICK)/20,0,1);
            float yaw=net.minecraft.util.Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
            var forward=net.minecraft.world.phys.Vec3.directionFromRotation(0,yaw).scale(.65*(1-rise));
            var pos=player.getPosition(partial).add(forward).add(0,1+rise*(player.getBbHeight()-.35),0);
            pose.pushPose();pose.translate(pos.x-camera.x,pos.y-camera.y,pos.z-camera.z);
            pose.mulPose(Axis.YP.rotationDegrees(-yaw+(float)(age*1.7)));pose.scale(.8F,.8F,.8F);
            for(int part=0;part<8;part++){
                pose.pushPose();double angle=(part+.5)*Math.PI/4;
                if(broken>0){
                    pose.translate(Math.cos(angle)*broken*.9,Math.sin(angle)*broken*.45-broken*broken*.9,Math.sin(angle*3)*broken*.4);
                    pose.mulPose(Axis.ZP.rotationDegrees((float)(broken*(part%2==0?85:-85))));
                    float scale=(float)(1-broken*.85);pose.scale(scale,scale,scale);
                }
                pose.translate(-.5,-.5,-.5);
                mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(PARTS[part]),ItemStack.EMPTY,LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
                pose.popPose();
            }
            motes(mc.level.getGameTime()+partial,broken,pose,buffers);
            pose.popPose();
        }
    }
    private FulfillmentStarVisuals(){}
}
