package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;

/** One draw's particle geometry; obtain fresh quads each frame for dynamic resource-pack models. */
public final class ParticleModelQuads {
    private static final Direction[] FACES=Direction.values();
    public static List<BakedQuad> collect(BakedModel model){
        var quads=new ArrayList<BakedQuad>();var random=RandomSource.create(42);
        // Match ItemRenderer's face order and independent seed for each direction.
        for(var face:FACES){
            random.setSeed(42);var directional=model.getQuads(null,face,random);
            if(!directional.isEmpty())quads.addAll(directional);
        }
        random.setSeed(42);var unculled=model.getQuads(null,null,random);
        // Authored particles usually have only unculled faces: keep that existing list intact.
        if(quads.isEmpty())return unculled;
        if(!unculled.isEmpty())quads.addAll(unculled);
        return quads;
    }
    public static void tinted(List<BakedQuad> quads,PoseStack pose,VertexConsumer out,int light,int overlay,int color,float alpha){
        float red=(color>>16&255)/255F,green=(color>>8&255)/255F,blue=(color&255)/255F;
        for(var quad:quads)out.putBulkData(pose.last(),quad,red,green,blue,alpha,light,overlay);
    }
    private ParticleModelQuads(){}
}
