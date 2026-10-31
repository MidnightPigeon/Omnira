package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.resources.model.*;
import org.joml.Vector3f;

/** Immutable local geometry only: pose, lighting, tint and output buffers remain per draw. */
public final class PreparedStaticMesh {
    private static final Map<BakedModel,List<Vertex>> CACHE=new WeakHashMap<>();
    private static final Map<BakedModel,List<Vertex>> UNCULLED=new WeakHashMap<>();
    private record Vertex(float x,float y,float z,int color,float u,float v,int light,
                          float nx,float ny,float nz,boolean bakedNormal,boolean tinted) {}
    public static void draw(BakedModel model,PoseStack pose,VertexConsumer out,int overlay){
        StaticModelQuads.draw(model,pose,out,LightTexture.FULL_BRIGHT,overlay);
    }
    /** Returns false for dynamic models, whose original rendering path must run unchanged. */
    public static boolean draw(BakedModel model,PoseStack pose,VertexConsumer out,int light,int overlay,
                               int tint,boolean allQuads,boolean readColor){
        if(model.getClass()!=SimpleBakedModel.class)return false;
        emit(CACHE.computeIfAbsent(model,m->prepare(StaticModelQuads.collect(m))),pose,out,light,overlay,tint,allQuads,readColor,1);
        return true;
    }
    /** Standalone machine parts that intentionally draw only their unculled quads. */
    public static void drawUnculled(BakedModel model,PoseStack pose,VertexConsumer out,int light,int overlay,int tint,float alpha,long seed){
        if(model.getClass()!=SimpleBakedModel.class){
            for(var quad:model.getQuads(null,null,net.minecraft.util.RandomSource.create(seed)))
                out.putBulkData(pose.last(),quad,(tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,alpha,light,overlay);
            return;
        }
        emit(UNCULLED.computeIfAbsent(model,m->prepare(m.getQuads(null,null,net.minecraft.util.RandomSource.create(seed)))),
                pose,out,light,overlay,tint,true,false,alpha);
    }
    private static void emit(List<Vertex> vertices,PoseStack pose,VertexConsumer out,int light,int overlay,
                             int tint,boolean allQuads,boolean readColor,float opacity){
        var position=new Vector3f();var normal=new Vector3f();var transform=pose.last();
        for(var v:vertices){
            int color=allQuads||v.tinted?tint:0xFFFFFF;
            float r=(color>>16&255)/255F,g=(color>>8&255)/255F,b=(color&255)/255F;
            int red,green,blue,alpha;
            if(readColor){
                red=(int)((v.color&255)*r);green=(int)((v.color>>8&255)*g);blue=(int)((v.color>>16&255)*b);
                alpha=(int)(((v.color>>>24)/255F)*255);
            }else{red=(int)(r*255);green=(int)(g*255);blue=(int)(b*255);alpha=(int)(opacity*255);}
            int combined=Math.max(light&65535,v.light&65535)|(Math.max(light>>>16,v.light>>>16)<<16);
            transform.pose().transformPosition(v.x,v.y,v.z,position);
            if(v.bakedNormal)normal.set(v.nx,v.ny,v.nz).mul(transform.normal());
            else transform.transformNormal(v.nx,v.ny,v.nz,normal);
            out.addVertex(position.x,position.y,position.z).setColor(red,green,blue,alpha).setUv(v.u,v.v)
                    .setOverlay(overlay).setLight(combined).setNormal(normal.x,normal.y,normal.z);
        }
    }
    private static List<Vertex> prepare(List<net.minecraft.client.renderer.block.model.BakedQuad> quads){
        var vertices=new ArrayList<Vertex>();
        for(var quad:quads){
            var direction=quad.getDirection().getNormal();float nx=direction.getX(),ny=direction.getY(),nz=direction.getZ();
            boolean baked=false;var data=quad.getVertices();
            for(int i=0;i<data.length;i+=8){
                int normal=data[i+7];byte x=(byte)normal,y=(byte)(normal>>8),z=(byte)(normal>>16);
                // Vanilla carries the previous normal if the next vertex has no baked normal.
                if(x!=0||y!=0||z!=0){nx=x/127F;ny=y/127F;nz=z/127F;baked=true;}
                vertices.add(new Vertex(Float.intBitsToFloat(data[i]),Float.intBitsToFloat(data[i+1]),Float.intBitsToFloat(data[i+2]),
                        data[i+3],Float.intBitsToFloat(data[i+4]),Float.intBitsToFloat(data[i+5]),data[i+6],nx,ny,nz,baked,quad.isTinted()));
            }
        }
        return List.copyOf(vertices);
    }
    public static void reset(){CACHE.clear();UNCULLED.clear();}
    private PreparedStaticMesh(){}
}
