package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.*;

/** Cache only vanilla immutable baked models; resource-pack dynamic models stay live. */
public final class StaticModelQuads {
    private static final Map<BakedModel,List<BakedQuad>> CACHE=new WeakHashMap<>();
    private static final float[] BRIGHTNESS={1,1,1,1};
    public static List<BakedQuad> collect(BakedModel model){
        return model.getClass()==SimpleBakedModel.class?CACHE.computeIfAbsent(model,ParticleModelQuads::collect):ParticleModelQuads.collect(model);
    }
    public static void draw(BakedModel model,PoseStack pose,VertexConsumer out,int light,int overlay){
        if(PreparedStaticMesh.draw(model,pose,out,light,overlay,0xFFFFFF,false,true))return;
        int[] lights={light,light,light,light};
        for(var quad:collect(model))out.putBulkData(pose.last(),quad,BRIGHTNESS,1,1,1,1,lights,overlay,true);
    }
    /** Matches the orb bases' tint path, which deliberately ignores baked vertex colors. */
    public static void drawTinted(BakedModel model,PoseStack pose,VertexConsumer out,int light,int overlay,int tint,boolean allQuads){
        if(PreparedStaticMesh.draw(model,pose,out,light,overlay,tint,allQuads,false))return;
        int[] lights={light,light,light,light};
        for(var quad:collect(model)){
            int color=allQuads||quad.isTinted()?tint:0xFFFFFF;
            out.putBulkData(pose.last(),quad,BRIGHTNESS,(color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,1,lights,overlay,false);
        }
    }
    public static void reset(){CACHE.clear();PreparedStaticMesh.reset();}
    private StaticModelQuads(){}
}
