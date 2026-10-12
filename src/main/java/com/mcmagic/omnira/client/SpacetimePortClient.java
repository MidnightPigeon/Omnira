package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SpacetimePortClient {
    public static ModelResourceLocation model(String id){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","block/spacetime_port_"+id));}
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional e){for(String id:new String[]{"body","white","black"})e.register(model(id));}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(ModBlockEntityTypes.SPACETIME_PORT.get(),c->new Renderer());}
    @SubscribeEvent public static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){
        e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private BlockEntityWithoutLevelRenderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new ItemRenderer();return renderer;}
        },ModItems.SPACETIME_PORT.get());
    }
    private static void draw(String id,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(id)),ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private static Vec3 marker(SpacetimePortState p){return Vec3.atCenterOf(p.host.getBlockPos()).add(Vec3.atLowerCornerOf(p.face().getNormal()).scale(-3.0/16));}
    private static void quad(VertexConsumer out,PoseStack pose,Vec3 a,Vec3 b,Vec3 c,Vec3 d,int color){
        for(var v:new Vec3[]{a,b,c,d})out.addVertex(pose.last().pose(),(float)v.x,(float)v.y,(float)v.z).setColor(color);
    }
    private static final class Renderer implements BlockEntityRenderer<BlockEntity> {
        @Override public AABB getRenderBoundingBox(BlockEntity e){
            var p=((SpacetimePortHost)e).port();var bounds=new AABB(e.getBlockPos());
            var player=Minecraft.getInstance().player;
            return p.peer!=null&&p.white&&player!=null
                &&(player.getMainHandItem().is(com.mcmagic.omnira.storage.CrystalStorageContent.ROD.get())||player.getOffhandItem().is(com.mcmagic.omnira.storage.CrystalStorageContent.ROD.get()))
                ?bounds.minmax(new AABB(p.peer)):bounds;
        }
        @Override public int getViewDistance(){return 64;}
        @Override public void render(BlockEntity e,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var p=((SpacetimePortHost)e).port();
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(p.face().getRotation());pose.translate(-.5,-.5,-.5);
            draw("body",pose,buffers,light,overlay);
            if(p.peer!=null||p.selectedUntil>e.getLevel().getGameTime()){
                double time=e.getLevel().getGameTime()+partial;
                pose.translate(.5,5.0/16+Math.sin(time*.08)*.02,.5);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(time*1.2%360)));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(12));
                pose.translate(-.5,-3.0/16,-.5);
                draw(p.white||p.peer==null?"white":"black",pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            }
            pose.popPose();
            var player=Minecraft.getInstance().player;
            if(p.peer==null||!p.white||player==null||!(player.getMainHandItem().is(com.mcmagic.omnira.storage.CrystalStorageContent.ROD.get())||player.getOffhandItem().is(com.mcmagic.omnira.storage.CrystalStorageContent.ROD.get())))return;
            var start=marker(p).subtract(Vec3.atLowerCornerOf(e.getBlockPos()));var other=SpacetimePortState.at(e.getLevel(),p.peer);
            var end=(other==null?Vec3.atCenterOf(p.peer):marker(other)).subtract(Vec3.atLowerCornerOf(e.getBlockPos()));
            var delta=end.subtract(start);var side=delta.cross(new Vec3(0,1,0));if(side.lengthSqr()<.001)side=new Vec3(1,0,0);side=side.normalize().scale(.008);
            var vertical=new Vec3(0,.008,0);var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
            for(int i=0;i<2;i++){
                var a=start.add(delta.scale(i/2.0));var b=start.add(delta.scale((i+1)/2.0));int color=i==0?0xEBFFFFFF:0xEB181D29;
                quad(out,pose,a.subtract(side),b.subtract(side),b.add(side),a.add(side),color);
                quad(out,pose,a.subtract(vertical),b.subtract(vertical),b.add(vertical),a.add(vertical),color);
            }
        }
    }
    private static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack s,ItemDisplayContext c,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            // The placed plate is at y=0..1 pixels; item transforms rotate around y=8.
            pose.pushPose();pose.translate(0,7.5/16,0);
            draw("body",pose,buffers,light,overlay);pose.popPose();
        }
    }
}
