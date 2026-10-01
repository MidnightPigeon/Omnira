package com.mcmagic.omnira.mire;

/** Continuous flooded lowlands and winding channels, independent of chunk order. */
public final class MireTerrain {
    public static final int WATER_Y=223;
    private static final net.minecraft.world.level.levelgen.synth.SimplexNoise BASIN=new net.minecraft.world.level.levelgen.synth.SimplexNoise(net.minecraft.util.RandomSource.create(81637));
    private static final net.minecraft.world.level.levelgen.synth.SimplexNoise CHANNEL=new net.minecraft.world.level.levelgen.synth.SimplexNoise(net.minecraft.util.RandomSource.create(19753));
    public record Column(int floor,boolean pond,boolean shore){}
    private record Cached(int x,int z,Column column){}
    private static final ThreadLocal<Cached> CACHE=new ThreadLocal<>();
    public static Column column(int x,int z){
        var cache=CACHE.get();
        if(cache!=null&&cache.x()==x&&cache.z()==z)return cache.column();
        double wet=.62*BASIN.getValue(x/128.0,z/128.0)+.28*BASIN.getValue(x/48.0,z/48.0)+.10*BASIN.getValue(x/18.0,z/18.0);
        double channel=Math.abs(CHANNEL.getValue(x/210.0,z/210.0)+.35*CHANNEL.getValue(x/65.0,z/65.0));
        wet=Math.min(wet,(channel-.13)*2);
        // Dry transition prevents water spilling over neighbouring mountain slopes.
        wet+=.65*(1-Math.min(1,com.mcmagic.omnira.spacetime.TimeBiomeLayout.sample(x,z).edgeDistance()/24.0));
        var result=wet<0?new Column(222-(wet<-.22?1:0)-(wet<-.48?1:0),true,true)
                :new Column(wet>.38?225:224,false,wet<.18);
        CACHE.set(new Cached(x,z,result));
        return result;
    }
}
