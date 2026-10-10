package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** One quad sort for Omnira world glass, including colors and static entity textures. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class WorldGlassBuffers {
    private static final ResourceLocation WHITE=ResourceLocation.parse("omnira:entity/solid_white");
    private static ByteBufferBuilder memory;
    private static MultiBufferSource.BufferSource deferred;
    private static boolean collecting,pending;

    // Only the main world source is redirected; GUI, hand, previews and mirrors stay local.
    public static VertexConsumer redirect(MultiBufferSource.BufferSource source,RenderType type){
        if(!collecting || MirrorScene.rendering() || source!=Minecraft.getInstance().renderBuffers().bufferSource())return null;
        var texture=CrystalGlassLayer.atlasTexture(type);
        if(type!=CrystalGlassLayer.ATLAS && type!=CrystalGlassLayer.COLOR && texture==null)return null;
        if(deferred==null){memory=new ByteBufferBuilder(65536);deferred=MultiBufferSource.immediate(memory);}
        pending=true;
        var vertices=deferred.getBuffer(WorldGlassShader.layer());
        var atlas=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        if(type==CrystalGlassLayer.COLOR)return GlassVertexAdapter.unlit(vertices,atlas.apply(WHITE));
        return GlassVertexAdapter.textured(vertices,texture==null?null:atlas.apply(texture),type==CrystalGlassLayer.ATLAS);
    }

    @SubscribeEvent public static void stage(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY){
            // Never replay camera-relative vertices left by an aborted frame.
            if(pending)reset();
            collecting=true;
        }else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS){
            collecting=false;
            if(pending){deferred.endBatch();pending=false;}
        }else if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL){
            collecting=false;
            if(pending)reset();
        }
    }
    public static void reset(){
        collecting=false;pending=false;deferred=null;
        if(memory!=null){memory.close();memory=null;}
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){reset();}
    private WorldGlassBuffers(){}
}
