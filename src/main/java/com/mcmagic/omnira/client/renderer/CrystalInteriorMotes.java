package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;

/** Draw inside terrain glass before its depth-writing pass, but after opaque geometry. */
public final class CrystalInteriorMotes {
    private static ByteBufferBuilder memory;
    private static MultiBufferSource.BufferSource source;
    private static boolean pending;
    public static VertexConsumer vertices(){
        if(source==null){memory=new ByteBufferBuilder(16384);source=MultiBufferSource.immediate(memory);}
        pending=true;
        // A private source avoids the later WorldGlassBuffers redirection.
        return source.getBuffer(CrystalGlassLayer.COLOR);
    }
    public static void beginFrame(){if(pending)reset();}
    public static void flush(){
        if(pending){
            net.minecraft.client.Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
            source.endBatch();pending=false;
        }
    }
    public static void reset(){
        pending=false;source=null;
        if(memory!=null){memory.close();memory=null;}
    }
    private CrystalInteriorMotes(){}
}
