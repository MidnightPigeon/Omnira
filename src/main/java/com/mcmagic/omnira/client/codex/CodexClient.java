package com.mcmagic.omnira.client.codex;

import com.mcmagic.omnira.codex.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CodexClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent e){
        e.register(MyriadCodexContent.MENU.get(),com.mcmagic.omnira.client.screen.MyriadCodexScreen::new);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional e){
        for(String path:new String[]{"block/myriad_codex","item/myriad_codex_book"})
            e.register(ModelResourceLocation.standalone(ResourceLocation.parse("omnira:"+path)));
    }
    @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){
        e.registerReloadListener((ResourceManagerReloadListener)manager->CodexVisuals.reload());
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Item e){
        e.register((stack,index)->CodexGeometry.color(index,time(),false),MyriadCodexContent.ITEM.get(),MyriadCodexContent.HEART_ITEM.get());
    }
    @SubscribeEvent public static void colors(RegisterColorHandlersEvent.Block e){
        e.register((state,level,pos,index)->CodexGeometry.color(index,time(),false),MyriadCodexContent.BLOCK.get());
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){
        e.registerBlockEntityRenderer(MyriadCodexContent.ENTITY.get(),context->new BlockEntityRenderer<net.minecraft.world.level.block.entity.BlockEntity>(){
            @Override public void render(net.minecraft.world.level.block.entity.BlockEntity entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                var be=MyriadCodexEntity.from(entity);if(be==null)return;
                double time=be.getLevel()==null?0:be.getLevel().getGameTime()+partial;
                var facing=be.getBlockState().getValue(MyriadCodexBlock.FACING);
                if(com.mcmagic.omnira.client.renderer.WorldGlassBuffers.visible(new AABB(be.getBlockPos()).inflate(.1).expandTowards(0,.7,0))){
                    float angle=net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.client.renderer.CreateEnginePhase.degrees(entity,facing.getAxis()):0;
                    angle*=switch(facing){case NORTH,WEST->-1;default->1;};
                    CodexVisuals.codex(facing,time,be.busy(),be.potential>=8192&&!be.busy()&&be.holder==null&&be.heartPos==null,pose,buffers,light,overlay,angle);
                    if(be.potential>0&&be.shaftSpeed()==0)CodexVisuals.input(facing.getOpposite(),time,pose,buffers,be.electrical?com.mcmagic.omnira.pulse.PulseOutput.ELECTRIC:com.mcmagic.omnira.pulse.PulseOutput.PULSE);
                }
                var target=CodexVisuals.target(be,partial);
                if(target!=null){
                    var local=target.subtract(Vec3.atLowerCornerOf(be.getBlockPos()));
                    int alpha=Math.round(76*CodexGeometry.opacity(be.fade));
                    CodexVisuals.dottedLink(new Vec3(.5,1.12,.5),local,time,alpha<<24|0xC9E5DF,pose,buffers);
                }
            }
            @Override public AABB getRenderBoundingBox(net.minecraft.world.level.block.entity.BlockEntity entity){
                var be=MyriadCodexEntity.from(entity);
                var box=new AABB(be.getBlockPos()).inflate(.1).expandTowards(0,.7,0);
                var target=CodexVisuals.target(be,1);return target==null?box:box.minmax(new AABB(target,target).inflate(.5));
            }
            @Override public int getViewDistance(){return 96;}
        });
        e.registerBlockEntityRenderer(MyriadCodexContent.HEART_ENTITY.get(),context->new BlockEntityRenderer<WorldHeartEntity>(){
            @Override public void render(WorldHeartEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                double time=be.getLevel()==null?0:be.getLevel().getGameTime()+partial;
                var source=CodexVisuals.origin(be.link);int fade=source==null?be.fade:source.fade;
                CodexVisuals.heart(time,fade,pose,buffers,overlay);
                CodexVisuals.sphere(source==null?be.diameter:source.diameter(),fade,
                        source==null?be.shape:source.mode==1?source.shape:0,source==null?be.axis:source.axis,pose,buffers);
            }
            @Override public AABB getRenderBoundingBox(WorldHeartEntity be){return new AABB(be.getBlockPos()).inflate(Math.max(0,(Math.clamp(be.diameter,1,15)-1)/2.0)+.1);}
            @Override public int getViewDistance(){return 96;}
        });
    }
    @SubscribeEvent public static void items(RegisterClientExtensionsEvent e){
        e.registerItem(new IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){
                if(renderer==null)renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                        double time=time();
                        if(stack.is(MyriadCodexContent.HEART_ITEM.get())){
                            var source=CodexVisuals.origin(HeartLink.of(stack));
                            float scale=context==ItemDisplayContext.GUI?2.4F:1.6F;
                            pose.pushPose();pose.translate(.5,.5,.5);pose.scale(scale,scale,scale);pose.translate(-.5,-.5,-.5);
                            CodexVisuals.heart(time,source==null?0:source.fade,pose,buffers,overlay);
                            pose.popPose();
                        }else CodexVisuals.codex(Direction.NORTH,time,false,true,pose,buffers,light,overlay);
                    }
                };return renderer;
            }
        },MyriadCodexContent.ITEM.get(),MyriadCodexContent.HEART_ITEM.get());
    }
    private static double time(){
        var mc=Minecraft.getInstance();return mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
    }
    private CodexClient(){}
}
