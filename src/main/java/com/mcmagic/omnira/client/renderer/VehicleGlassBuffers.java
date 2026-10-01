package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/** Foreground vehicle glass is drawn after translucent terrain, with normal depth testing. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class VehicleGlassBuffers {
    private static MultiBufferSource.BufferSource deferred;

    private static MultiBufferSource.BufferSource buffers(){
        if(deferred==null){
            var fixed=new java.util.LinkedHashMap<RenderType,ByteBufferBuilder>();
            fixed.put(CrystalGlassLayer.ATLAS,new ByteBufferBuilder(16384));
            fixed.put(CrystalGlassLayer.COLOR,new ByteBufferBuilder(1536));
            deferred=MultiBufferSource.immediateWithBuffers(fixed,new ByteBufferBuilder(1536));
        }
        return deferred;
    }

    public static MultiBufferSource wrap(MultiBufferSource original){
        // Reflections have their own target and lifetime; never retain their transformed vertices.
        if(MirrorScene.rendering())return original;
        return type->(type==CrystalGlassLayer.ATLAS || type==CrystalGlassLayer.COLOR
                ?buffers():original).getBuffer(type);
    }

    @SubscribeEvent public static void finishWorld(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS && deferred!=null)
            deferred.endBatch();
    }
}
