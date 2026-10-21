package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.myriad.MiniatureWorldContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.common.*;

/** Bounded physical grains, not a continuous source of particle entities. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ProbabilityCloudClient {
    public static final int GRAINS=180;
    @SubscribeEvent public static void extensions(RegisterClientExtensionsEvent event){
        event.registerItem(new IClientItemExtensions(){
            private Renderer renderer;
            public BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new Renderer();return renderer;}
        },MiniatureWorldContent.PROBABILITY_CLOUD.get());
    }
    public static void draw(double time,PoseStack pose,MultiBufferSource buffers){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<GRAINS;i++){
            double angle=i*2.399963+time*.007,latitude=1-2*(i+.5)/GRAINS;
            double radius=.10+.29*Math.cbrt(((i*73)%GRAINS+.5)/GRAINS),horizontal=Math.sqrt(1-latitude*latitude);
            double wave=1+.16*Math.sin(time*.045+i*.37);
            float x=(float)(Math.cos(angle)*radius*horizontal*wave),y=(float)(latitude*radius*.72+.035*Math.sin(time*.03+i)),z=(float)(Math.sin(angle)*radius*horizontal);
            float hue=(float)(.64+.17*(.5+.5*Math.sin(time*.014+i*.11)));
            int color=0xFF000000|Mth.hsvToRgb(hue,.45F+(i%4)*.12F,.66F+(i%3)*.16F);
            StellarVisuals.cube(pose,mesh,x,y,z,.025F+(i%3)*.004F,color);
        }
    }
    private static final class Renderer extends BlockEntityWithoutLevelRenderer {
        private Renderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            pose.pushPose();pose.translate(.5,.5,.5);draw(time,pose,buffers);pose.popPose();
        }
    }
}
