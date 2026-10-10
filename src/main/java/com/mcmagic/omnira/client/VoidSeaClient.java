package com.mcmagic.omnira.client;

import com.mcmagic.omnira.space.sea.VoidSeaContent;
import com.mcmagic.omnira.client.renderer.StarSeaCreatureRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class VoidSeaClient {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(VoidSeaContent.SOURCE.get(),net.minecraft.client.renderer.RenderType.translucent());
        net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(VoidSeaContent.FLOWING.get(),net.minecraft.client.renderer.RenderType.translucent());
    });}
    @SubscribeEvent public static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private com.mcmagic.omnira.client.renderer.StardustItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.StardustItemRenderer();return renderer;}
        },com.mcmagic.omnira.space.sea.StarlightContent.DUST_ITEM.get());
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
            private com.mcmagic.omnira.client.renderer.StarJellyfishItemRenderer renderer;
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer(){if(renderer==null)renderer=new com.mcmagic.omnira.client.renderer.StarJellyfishItemRenderer();return renderer;}
        },VoidSeaContent.JELLY_ITEM.get());
        event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions(){
            @Override public ResourceLocation getStillTexture(){return ResourceLocation.parse("omnira:block/void_starflow_still");}
            @Override public ResourceLocation getFlowingTexture(){return ResourceLocation.parse("omnira:block/void_starflow_flow");}
            @Override public int getTintColor(){return 0xC8FFFFFF;}
        },VoidSeaContent.TYPE.get());
    }
    @SubscribeEvent public static void models(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event){event.register(com.mcmagic.omnira.client.renderer.StarJellyfishItemRenderer.MODEL);event.register(com.mcmagic.omnira.client.renderer.StarlightRenderer.MODEL);event.register(com.mcmagic.omnira.client.renderer.StarlightRenderer.DUST);event.register(com.mcmagic.omnira.client.renderer.StardustItemRenderer.MODEL);event.register(com.mcmagic.omnira.client.renderer.StardustItemRenderer.MOTE);}
    @SubscribeEvent public static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(com.mcmagic.omnira.space.sea.StarlightContent.ENTITY.get(),com.mcmagic.omnira.client.renderer.StarlightRenderer::new);
        event.registerEntityRenderer(VoidSeaContent.JELLYFISH.get(),c->new StarSeaCreatureRenderer<>(c,true));
        event.registerEntityRenderer(VoidSeaContent.MANTA.get(),c->new StarSeaCreatureRenderer<>(c,false));
    }
}
