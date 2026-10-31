package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.myriad.WorldComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira", value=Dist.CLIENT)
public final class WorldComponentClient {
    private static ModelResourceLocation model(WorldComponent component, String part) {
        return CrystalGridRenderer.part(component.id, part);
    }
    @SubscribeEvent public static void models(ModelEvent.RegisterAdditional event) {
        for (var component : WorldComponent.values()) for (var part : new String[]{"main","motion","glass"}) event.register(model(component,part));
    }
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private Renderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer==null) renderer=new Renderer(); return renderer;
            }
        }, java.util.Arrays.stream(WorldComponent.values()).map(c->c.item.get()).toArray(Item[]::new));
    }

    private static void part(WorldComponent component, String name, boolean glass, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); pose.translate(-.5,-.5,-.5);
        var mc=Minecraft.getInstance();
        com.mcmagic.omnira.client.renderer.StaticModelQuads.draw(mc.getModelManager().getModel(model(component,name)),pose,buffers.getBuffer(glass ? CrystalGlassLayer.ATLAS : RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS)),light,overlay);
        pose.popPose();
    }

    public static void draw(WorldComponent component, double time, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose(); pose.translate(.5,.5,.5);
        part(component,"main",false,pose,buffers,light,overlay);
        part(component,"glass",true,pose,buffers,light,overlay);
        pose.pushPose();
        if (component==WorldComponent.SKY) pose.mulPose(Axis.YP.rotationDegrees((float)(time*.5%360)));
        if (component==WorldComponent.HEAT) {
            float pulse=1+(float)Math.sin(time*.06)*.055F; pose.scale(pulse,pulse,pulse);
        }
        if (component==WorldComponent.TIME) pose.mulPose(Axis.YP.rotationDegrees((float)(time*.3%360)));
        if (component==WorldComponent.LIFE || component==WorldComponent.POSSIBILITY) {
            float growth=component==WorldComponent.LIFE ? .93F+(float)Math.sin(time*.025)*.07F : .15F+.85F*(float)(.5+.5*Math.sin(time*.025));
            float root=component==WorldComponent.LIFE ? -.0625F : -.125F;
            pose.translate(0,root,0); pose.scale(1,growth,1); pose.translate(0,-root,0);
        }
        boolean glass=component==WorldComponent.WATER||component==WorldComponent.SKY||component==WorldComponent.POSSIBILITY;
        part(component,"motion",glass,pose,buffers,component==WorldComponent.HEAT ? LightTexture.FULL_BRIGHT : light,overlay);
        pose.popPose();
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        if (component==WorldComponent.WATER) for(int i=0;i<5;i++) {
            double path=(time*.008+i*.8)%4; int edge=(int)path; float u=(float)(path-edge),r=.27F;
            float x=edge==0?-r+2*r*u:edge==1?r:edge==2?r-2*r*u:-r;
            float z=edge==0?-r:edge==1?-r+2*r*u:edge==2?r:r-2*r*u;
            StellarVisuals.cube(pose,mesh,x,.016F,z,.013F,0xD9F7FF,.85F);
        }
        if (component==WorldComponent.SKY) for(int i=0;i<4;i++) {
            double angle=time*.025+i*Math.PI/2;
            StellarVisuals.cube(pose,mesh,(float)Math.cos(angle)*.26F,.15F,(float)Math.sin(angle)*.26F,.012F,0xFFFFFF,.75F);
        }
        if (component==WorldComponent.TIME) for(int i=0;i<8;i++) {
            double angle=i*Math.PI/4+time*.3*Math.PI/180;
            float alpha=.15F+.7F*(float)Math.max(0,Math.cos(time*.04-i*Math.PI/4));
            StellarVisuals.cube(pose,mesh,(float)Math.cos(angle)*.3F,.04F,(float)Math.sin(angle)*.3F,.015F,0xDEFFC8,alpha);
        }
        if (component==WorldComponent.BOUNDARY) for(int i=0;i<4;i++) {
            float y=-.25F+(float)((time/120+i*.25)%1)*.5F;
            float x=i<2?-.316F:.316F,z=i%2==0?-.316F:.316F;
            StellarVisuals.cube(pose,mesh,x,y,z,.013F,0xE8FAFF,.6F);
        }
        pose.popPose();
    }

    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private Renderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            var component=WorldComponent.of(stack); if(component==null)return;
            var mc=Minecraft.getInstance();
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            draw(component,time,pose,buffers,light,overlay);
        }
    }
    private WorldComponentClient() {}
}
