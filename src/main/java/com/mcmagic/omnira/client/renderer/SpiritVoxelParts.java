package com.mcmagic.omnira.client.renderer;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

final class SpiritVoxelParts {
    private SpiritVoxelParts() {}
    static ModelPart sphere(float radius) {
        var cubes=new java.util.ArrayList<ModelPart.Cube>();
        int extent=(int)Math.ceil(radius);
        for(int x=-extent;x<extent;x++) for(int y=-extent;y<extent;y++) for(int z=-extent;z<extent;z++) {
            if(!inside(x,y,z,radius)) continue;
            var faces=java.util.EnumSet.noneOf(net.minecraft.core.Direction.class);
            for(var face:net.minecraft.core.Direction.values())
                if(!inside(x+face.getStepX(),y+face.getStepY(),z+face.getStepZ(),radius)) faces.add(face);
            if(!faces.isEmpty()) cubes.add(new ModelPart.Cube(0,0,x,y,z,1,1,1,0,0,0,false,64,32,faces));
        }
        return new ModelPart(cubes,java.util.Map.of());
    }
    private static boolean inside(int x,int y,int z,float radius) {
        return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=radius*radius;
    }
    static ModelPart boxes(float[]... boxes) {
        var mesh=new MeshDefinition();
        var cubes=CubeListBuilder.create();
        for(float[] b:boxes) cubes.texOffs(0,0).addBox(b[0],b[1],b[2],b[3],b[4],b[5]);
        mesh.getRoot().addOrReplaceChild("body",cubes,PartPose.ZERO);
        return LayerDefinition.create(mesh,64,32).bakeRoot();
    }
}
