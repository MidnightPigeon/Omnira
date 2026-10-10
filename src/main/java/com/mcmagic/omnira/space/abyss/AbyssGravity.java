package com.mcmagic.omnira.space.abyss;

import com.mcmagic.omnira.spacetime.CorridorLayout;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Signed gravity: ordinary +1, weightless 0, ceiling -1. No equipment exemption. */
public final class AbyssGravity {
    private record Column(long tick,int low,int high){double middle(){return (low+high+4)/2.0;}}
    private static final Map<Level,Map<Long,Column>> COLUMNS=java.util.Collections.synchronizedMap(new WeakHashMap<>());
    public static int band(double height,double middle){return height<middle-2.5?1:height>=middle+2.5?-1:0;}
    public static int direction(Entity entity){return direction(entity.level(),entity.getBoundingBox().getCenter());}
    public static int direction(Level level,Vec3 position){
        var p=BlockPos.containing(position);
        if(!level.hasChunkAt(p)||!level.getBiome(p).is(AbyssContent.BIOME))return 1;
        if(level.dimension().location().toString().equals("omnira:spacetime_corridor"))
            return position.y<=CorridorLayout.SPACE_CEILING?band(position.y,AbyssTerrain.middle(p.getX(),p.getZ())):1;
        // Authored overworld samples and biome edits use their actual contiguous biome volume.
        long key=BlockPos.asLong(p.getX()>>2,0,p.getZ()>>2),now=level.getGameTime();
        var cache=COLUMNS.computeIfAbsent(level,l->new LinkedHashMap<>(64,.75F,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Long,Column> e){return size()>1024;}
        });
        var column=cache.get(key);
        if(column==null||now-column.tick()>20||p.getY()<column.low()||p.getY()>=column.high()+4){
            int y=(p.getY()>>2)<<2,low=y,high=y;
            while(low-4>=level.getMinBuildHeight()&&level.getNoiseBiome(p.getX()>>2,(low-4)>>2,p.getZ()>>2).is(AbyssContent.BIOME))low-=4;
            while(high+4<level.getMaxBuildHeight()&&level.getNoiseBiome(p.getX()>>2,(high+4)>>2,p.getZ()>>2).is(AbyssContent.BIOME))high+=4;
            column=new Column(now,low,high);cache.put(key,column);
        }
        return band(position.y,column.middle());
    }
    public static boolean inverted(Entity entity){return !entity.isSpectator()&&GravityTransition.inverted(entity);}
    public static double eyeOffset(Entity entity){return GravityTransition.eyeOffset(entity,0);}
    public static Vec3 input(Entity entity,Vec3 input,float speed){
        double length=input.length();if(length<1e-7)return Vec3.ZERO;
        var scaled=input.scale(speed/Math.max(1,length));
        double yaw=Math.toRadians(entity.getYRot());
        var forward=entity.getLookAngle();var side=new Vec3(Math.cos(yaw),0,Math.sin(yaw));
        double roll=Math.toRadians(GravityTransition.roll(entity,0));
        side=side.scale(Math.cos(roll)).add(forward.cross(side).scale(Math.sin(roll)));
        return forward.scale(scaled.z).add(side.scale(scaled.x)).add(0,scaled.y,0);
    }
    private AbyssGravity(){}
}
