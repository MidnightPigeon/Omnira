package com.mcmagic.omnira.client;

import com.mcmagic.omnira.space.labyrinth.*;
import com.mcmagic.omnira.client.renderer.CrystalBallRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class VoidPortalClient {
    private record Visibility(net.minecraft.world.phys.Vec3 camera,long tick,boolean visible){}
    private static final java.util.Map<VoidPortalEntity,Visibility> VISIBLE=new java.util.WeakHashMap<>();
    private static boolean visible(VoidPortalEntity be,net.minecraft.world.phys.Vec3 camera){
        var state=be.getBlockState();var center=VoidPortalBlock.bounds(state,be.getBlockPos()).getCenter();
        var inward=state.getValue(VoidPortalBlock.FACING).getOpposite();
        if(center.distanceToSqr(camera)>24*24||camera.subtract(center).dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(inward.getNormal()))<0)return false;
        long now=be.getLevel().getGameTime();var cached=VISIBLE.get(be);
        if(cached!=null&&now>=cached.tick()&&now-cached.tick()<10&&cached.camera().distanceToSqr(camera)<.25)return cached.visible();
        boolean clear=false;
        // Check the rim as well: a partly visible vortex must not disappear around a corner.
        for(int i=0;i<5&&!clear;i++){
            double a=i==1?-.45:i==2?.45:0,b=i==3?-.45:i==4?.45:0;
            var target=switch(inward.getAxis()){case X->center.add(0,a,b);case Y->center.add(a,0,b);case Z->center.add(a,b,0);};
            clear=LabyrinthSight.clear(be.getLevel(),camera,target);
        }
        VISIBLE.put(be,new Visibility(camera,now,clear));return clear;
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,Direction outward,double time){
        draw(pose,buffers,outward,time,false);
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,Direction outward,double time,boolean paleBlue){
        var inward=outward.getOpposite();pose.pushPose();pose.translate(.5+inward.getStepX()*.55,.5+inward.getStepY()*.55,.5+inward.getStepZ()*.55);
        pose.mulPose(outward.getRotation());pose.scale(2.5F,2.5F,2.5F);pose.translate(-.5,-.5,-.5);
        CrystalBallRenderer.vortex(pose,buffers,time,paleBlue);pose.popPose();
    }
    @SubscribeEvent public static void renderer(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(LabyrinthContent.ENTITY.get(),context->new net.minecraft.client.renderer.blockentity.BlockEntityRenderer<VoidPortalEntity>(){
            @Override public void render(VoidPortalEntity be,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){draw(pose,buffers,be.getBlockState().getValue(VoidPortalBlock.FACING),be.getLevel().getGameTime()+partial,be.getBlockState().is(LabyrinthContent.LONG_RANGE_PORTAL.get()));}
            @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(VoidPortalEntity be){return VoidPortalBlock.bounds(be.getBlockState(),be.getBlockPos());}
            @Override public boolean shouldRender(VoidPortalEntity be,net.minecraft.world.phys.Vec3 camera){return visible(be,camera);}
        });
    }
    @SubscribeEvent public static void item(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
                @Override public void renderByItem(ItemStack s,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                    pose.pushPose();pose.translate(0,0,-.55);draw(pose,buffers,Direction.NORTH,net.minecraft.Util.getMillis()/50.0,s.is(LabyrinthContent.LONG_RANGE_PORTAL_ITEM.get()));pose.popPose();
                }
            };
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}
        },LabyrinthContent.PORTAL_ITEM.get(),LabyrinthContent.LONG_RANGE_PORTAL_ITEM.get());
    }
}
