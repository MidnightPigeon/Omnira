package com.mcmagic.omnira.client;

import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class MireClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(MireContent.SOURCE.get(),net.minecraft.client.renderer.RenderType.translucent());
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(MireContent.FLOWING.get(),net.minecraft.client.renderer.RenderType.translucent());
    });}
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.parse("omnira:block/decayed_timeflow_still");}
            @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.parse("omnira:block/decayed_timeflow_flow");}
            @Override public int getTintColor(){return 0xC8FFFFFF;}
        },MireContent.TYPE.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private MireLilyItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new MireLilyItemRenderer();return renderer;}
        },MireContent.DECAYED_ITEM.get(),MireContent.REBORN_ITEM.get());
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){event.register(MireLilyItemRenderer.model(false));event.register(MireLilyItemRenderer.model(true));}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(MireContent.EEL.get(),TimeflowEelRenderer::new);}
}
