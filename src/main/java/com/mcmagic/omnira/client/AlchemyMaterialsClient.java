package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class AlchemyMaterialsClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        for(var liquid:java.util.List.of(AlchemyMaterials.DILUTE,AlchemyMaterials.REFINED)){
            ItemBlockRenderTypes.setRenderLayer(liquid.source.get(),RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(liquid.flowing.get(),RenderType.translucent());
        }
    });}
    @SubscribeEvent public static void fluids(RegisterClientExtensionsEvent event){
        for(var liquid:java.util.List.of(AlchemyMaterials.DILUTE,AlchemyMaterials.REFINED))event.registerFluidType(new IClientFluidTypeExtensions(){
            public ResourceLocation getStillTexture(){return ResourceLocation.parse("omnira:block/"+liquid.id+"_still");}
            public ResourceLocation getFlowingTexture(){return ResourceLocation.parse("omnira:block/"+liquid.id+"_flow");}
            public int getTintColor(){return liquid==AlchemyMaterials.DILUTE?0xC8FFFFFF:0xECFFFFFF;}
        },liquid.type.get());
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerEntityRenderer(AlchemyMaterials.THROWN_WASTE.get(),ThrownItemRenderer::new);
    }
    private AlchemyMaterialsClient(){}
}
