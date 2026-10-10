package com.mcmagic.omnira.alchemy;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.*;

/** Authored mesh data and collider share the same local 3x3x2 coordinate frame. */
public final class ReactionPoolGeometry {
    public static final JsonObject DATA=load();
    public static final float LIQUID_BOTTOM=DATA.getAsJsonObject("liquid").get("bottom").getAsFloat()/16;
    public static final float LIQUID_TOP=DATA.getAsJsonObject("liquid").get("top").getAsFloat()/16;
    public static final float VESSEL_FLOOR=DATA.getAsJsonObject("liquid").get("wall_bottom").getAsFloat()/16;
    private static final VoxelShape[][] SHAPES=new VoxelShape[4][18];
    public static boolean panelHit(Direction facing,net.minecraft.world.phys.Vec3 relative){
        var right=ReactionPoolBlock.offset(facing,5);var back=ReactionPoolBlock.offset(facing,7);
        double dx=relative.x-.5,dz=relative.z-.5;
        double x=8+16*(dx*right.getX()+dz*right.getZ()),y=relative.y*16,z=8+16*(dx*back.getX()+dz*back.getZ());
        // Match the tablet's collision bounds, including its tilted front face.
        return x>=1.5&&x<=14.5&&y>=21.5&&y<=29.5&&z>=-17.75&&z<=-14.25;
    }
    private static JsonObject load(){
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(ReactionPoolGeometry.class.getResourceAsStream(
                "/assets/omnira/geometry/reaction_pool.json")),java.nio.charset.StandardCharsets.UTF_8)){
            return JsonParser.parseReader(reader).getAsJsonObject();
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot load reaction pool geometry",ex);}
    }
    public static synchronized VoxelShape shape(Direction facing,int part){
        int index=facing.get2DDataValue();if(SHAPES[index][part]!=null)return SHAPES[index][part];
        var offset=ReactionPoolBlock.offset(facing,part);
        var right=ReactionPoolBlock.offset(facing,5);var back=ReactionPoolBlock.offset(facing,7);
        var boxes=new ArrayList<VoxelShape>();
        for(var entry:DATA.getAsJsonArray("collision")){
            var box=entry.getAsJsonArray();double[] min={Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE},max={-Double.MAX_VALUE,-Double.MAX_VALUE,-Double.MAX_VALUE};
            for(int n=0;n<8;n++){
                double x=box.get((n&1)==0?0:3).getAsDouble()/16-.5,y=box.get((n&2)==0?1:4).getAsDouble()/16,z=box.get((n&4)==0?2:5).getAsDouble()/16-.5;
                double[] at={.5+right.getX()*x+back.getX()*z,y,.5+right.getZ()*x+back.getZ()*z};
                for(int a=0;a<3;a++){min[a]=Math.min(min[a],at[a]);max[a]=Math.max(max[a],at[a]);}
            }
            double[] low={offset.getX(),offset.getY(),offset.getZ()},high={low[0]+1,low[1]+1,low[2]+1};
            // Outermost cells own the unchanged protruding connector tips.
            if(offset.getX()<0)low[0]-=.25;if(offset.getX()>0)high[0]+=.25;
            if(offset.getZ()<0)low[2]-=.25;if(offset.getZ()>0)high[2]+=.25;
            boolean fits=true;for(int a=0;a<3;a++){min[a]=Math.max(min[a],low[a]);max[a]=Math.min(max[a],high[a]);fits&=max[a]-min[a]>.00001;}
            if(fits)boxes.add(Shapes.box(min[0]-offset.getX(),min[1]-offset.getY(),min[2]-offset.getZ(),
                    max[0]-offset.getX(),max[1]-offset.getY(),max[2]-offset.getZ()));
        }
        return SHAPES[index][part]=Shapes.or(Shapes.empty(),boxes.toArray(VoxelShape[]::new)).optimize();
    }
    public static double[] outputPoint(boolean fluid,double progress){
        var path=DATA.getAsJsonObject("output_paths").getAsJsonArray(fluid?"fluid":"item");
        double[] lengths=new double[path.size()-1];double total=0;
        for(int i=0;i<lengths.length;i++){
            double squared=0;for(int a=0;a<3;a++){double delta=path.get(i+1).getAsJsonArray().get(a).getAsDouble()-path.get(i).getAsJsonArray().get(a).getAsDouble();squared+=delta*delta;}
            total+=lengths[i]=Math.sqrt(squared);
        }
        double distance=Math.clamp(progress,0,1)*total;int segment=0;
        while(segment<lengths.length-1&&distance>lengths[segment])distance-=lengths[segment++];
        double t=lengths[segment]>0?distance/lengths[segment]:0;
        double[] result=new double[3];for(int a=0;a<3;a++)result[a]=(path.get(segment).getAsJsonArray().get(a).getAsDouble()*(1-t)
                +path.get(segment+1).getAsJsonArray().get(a).getAsDouble()*t)/16;
        return result;
    }
    private ReactionPoolGeometry(){}
}
