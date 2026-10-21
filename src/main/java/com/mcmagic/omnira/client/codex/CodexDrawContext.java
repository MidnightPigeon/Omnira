package com.mcmagic.omnira.client.codex;

import net.minecraft.client.renderer.MultiBufferSource;

/** Render-thread scratch; a nested item/mirror draw must not overwrite its caller. */
final class CodexDrawContext implements AutoCloseable {
    static final class Pool {
        private final CodexDrawContext[] free=new CodexDrawContext[4];
        private int count=free.length;
        Pool(){for(int i=0;i<free.length;i++)free[i]=new CodexDrawContext(this);}
        CodexDrawContext borrow(MultiBufferSource buffers,float u0,float u1,float v0,float v1,double time,boolean busy){
            CodexDrawContext draw;
            if(count==0)draw=new CodexDrawContext(this);
            else{draw=free[--count];free[count]=null;}
            draw.leased=true;draw.buffers=buffers;draw.u0=u0;draw.u1=u1;draw.v0=v0;draw.v1=v1;
            for(int i=0;i<draw.palette.length;i++)draw.palette[i]=CodexGeometry.color(i,time,busy);
            return draw;
        }
    }
    private final Pool pool;
    private final int[] palette=new int[11];
    private MultiBufferSource buffers;
    private float u0,u1,v0,v1;
    private boolean leased;
    private CodexDrawContext(Pool pool){this.pool=pool;}
    MultiBufferSource buffers(){return buffers;}
    float u0(){return u0;}
    float u1(){return u1;}
    float v0(){return v0;}
    float v1(){return v1;}
    int[] palette(){return palette;}
    @Override public void close(){
        if(!leased)throw new IllegalStateException("Codex draw context already released");
        leased=false;buffers=null;u0=u1=v0=v1=0;
        if(pool.count<pool.free.length)pool.free[pool.count++]=this;
    }
}
