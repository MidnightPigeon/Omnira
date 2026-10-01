package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.WeakHashMap;

/** Size the actual mesh, rather than stacking miniature and dropped-item transforms. */
final class CrystalBallItemShape {
    private static final Map<BakedModel, Bounds> CACHE=new WeakHashMap<>();
    private record Bounds(float x,float y,float z,float width,float height,float depth) {}

    static boolean apply(BakedModel model,PoseStack pose) {
        if(model.isCustomRenderer())return false;
        Bounds b=CACHE.computeIfAbsent(model,CrystalBallItemShape::measure);
        float longest=Math.max(b.width,Math.max(b.height,b.depth));
        if(longest<=0)return false;
        float size=.14F/longest;
        // Generated sprites already have textured edge faces. Stretch those faces too,
        // preserving their pixel colors, alpha cutouts and the item's tint animation.
        float thickness=longest*.18F;
        pose.scale(size*thicken(b.width,thickness),size*thicken(b.height,thickness),
                size*thicken(b.depth,thickness));
        // ItemRenderer translates by -0.5 before emitting model vertices.
        pose.translate(.5F-b.x,.5F-b.y,.5F-b.z);
        return true;
    }

    private static float thicken(float extent,float minimum) {
        return extent>0?Math.max(1,minimum/extent):1;
    }

    private static Bounds measure(BakedModel model) {
        float[] min={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY};
        float[] max={Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        var random=RandomSource.create(42);
        for(int side=0;side<=6;side++) {
            random.setSeed(42);
            for(var quad:model.getQuads(null,side==6?null:Direction.values()[side],random)) {
                int[] vertices=quad.getVertices();
                int stride=vertices.length/4;
                for(int vertex=0;vertex<4;vertex++)for(int axis=0;axis<3;axis++) {
                    float value=Float.intBitsToFloat(vertices[vertex*stride+axis]);
                    min[axis]=Math.min(min[axis],value);
                    max[axis]=Math.max(max[axis],value);
                }
            }
        }
        if(!Float.isFinite(min[0]))return new Bounds(0,0,0,0,0,0);
        return new Bounds((min[0]+max[0])/2,(min[1]+max[1])/2,(min[2]+max[2])/2,
                max[0]-min[0],max[1]-min[1],max[2]-min[2]);
    }
}
