package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/** UV1.x above the vanilla 0..15 overlay range carries per-face material flags. */
public final class GlassVertexAdapter implements VertexConsumer {
    public static final int CULL=32,UNLIT=64;
    private final VertexConsumer target;
    private final TextureAtlasSprite sprite;
    private final int flags;
    private GlassVertexAdapter(VertexConsumer target,TextureAtlasSprite sprite,int flags){
        this.target=target;this.sprite=sprite;this.flags=flags;
    }
    static VertexConsumer textured(VertexConsumer target,TextureAtlasSprite sprite,boolean cull){
        return new GlassVertexAdapter(target,sprite,cull?CULL:0);
    }
    static VertexConsumer unlit(VertexConsumer target,TextureAtlasSprite white){
        return new GlassVertexAdapter(target,white,UNLIT);
    }
    @Override public VertexConsumer addVertex(float x,float y,float z){
        target.addVertex(x,y,z);
        if(flags==UNLIT)target.setUv(sprite.getU(.5F),sprite.getV(.5F)).setUv1(UNLIT,OverlayTexture.v(false))
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0,1,0);
        return this;
    }
    @Override public VertexConsumer setColor(int r,int g,int b,int a){target.setColor(r,g,b,a);return this;}
    @Override public VertexConsumer setUv(float u,float v){
        if(flags!=UNLIT)target.setUv(sprite==null?u:sprite.getU(u),sprite==null?v:sprite.getV(v));return this;
    }
    @Override public VertexConsumer setUv1(int u,int v){if(flags!=UNLIT)target.setUv1(u+flags,v);return this;}
    @Override public VertexConsumer setUv2(int u,int v){if(flags!=UNLIT)target.setUv2(u,v);return this;}
    @Override public VertexConsumer setNormal(float x,float y,float z){if(flags!=UNLIT)target.setNormal(x,y,z);return this;}
}
