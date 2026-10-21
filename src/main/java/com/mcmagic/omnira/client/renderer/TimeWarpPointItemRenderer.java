package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The placed block uses world particles; an item needs its own animated grains. */
public final class TimeWarpPointItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final java.util.Map<String,ModelResourceLocation> MODELS=java.util.Map.of(
            "body",part("body"),"spatial_body",part("spatial_body"),"grain",part("grain"));
    public TimeWarpPointItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part){
        return java.util.Objects.requireNonNull(MODELS.get(part),part);
    }
    private static ModelResourceLocation part(String part){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/time_warp_point/"+part));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
                                       MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0
                :mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(1.25F,1.25F,1.25F);pose.translate(-.5,-.5,-.5);
        boolean spatial=stack.is(com.mcmagic.omnira.registry.ModItems.SPACE_WARP_POINT.get())||stack.is(com.mcmagic.omnira.registry.ModItems.SPATIAL_REMNANT.get());
        boolean mote=stack.is(com.mcmagic.omnira.registry.ModItems.TEMPORAL_MOTE.get())||stack.is(com.mcmagic.omnira.registry.ModItems.SPATIAL_REMNANT.get());
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.8)));
        pose.translate(-.5,-.5,-.5);
        if(!mote)ParticleModelQuads.tinted(StaticModelQuads.collect(mc.getModelManager().getModel(model(spatial?"spatial_body":"body"))),
                pose,out,LightTexture.FULL_BRIGHT,overlay,0xFFFFFF,1);
        pose.popPose();
        var grain=StaticModelQuads.collect(mc.getModelManager().getModel(model("grain")));
        for(int i=0;i<10;i++){
            double phase=(time/30+i*.137)%1;
            double y=1-2*(i+.5)/10,angle=i*2.3999632297+time*.045;
            double radius=mote?.08+.08*phase:.29+.16*phase,horizontal=Math.sqrt(1-y*y);
            pose.pushPose();pose.translate(.5+Math.cos(angle)*horizontal*radius,.5+y*radius,
                    .5+Math.sin(angle)*horizontal*radius);
            float scale=(float)(.65+.4*Math.sin(Math.PI*phase));pose.scale(scale,scale,scale);
            ParticleModelQuads.tinted(grain,pose,out,LightTexture.FULL_BRIGHT,overlay,
                    spatial?(i%4==0?0xE5EEFF:i%3==0?0x25283F:0x799EEE):(i%4==0?0xE8F8ED:i%3==0?0x9BDFC0:0x65C785),1);
            pose.popPose();
        }
        pose.popPose();
    }
}
