package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Five thick strata separated by single sediment layers. */
public final class EpochalTerrain {
    public static final int BASE=com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE;
    public record Column(int surface,int top,boolean exposure){}
    private static double fold(int x,int z){return Math.sin((x+22*Math.sin(z/73.0))/49.0)+.28*Math.sin(z/37.0);}
    public static Column column(int x,int z){
        double d=Math.abs(fold(x,z)),edge=Math.min(1,StrataBiomeSource.sample(x,z).edgeDistance()/24.0);
        int top=227+(int)Math.round(3*Math.sin(x/91.0)*Math.cos(z/107.0));
        double cut=d<.30?49:d<.40?49*(.40-d)/.10:0;
        return new Column((int)Math.round(224+(top-cut-224)*edge),top,edge>.8&&d>=.29&&d<=.44);
    }
    /** Top-to-bottom index, or -1 for the separator. */
    public static int era(int y,Column c){
        int sep=1,available=c.top-BASE-4*sep;
        int cursor=c.top;
        for(int era=0;era<5;era++){
            int thickness=available/5+(era<available%5?1:0);
            if(y<=cursor&&y>cursor-thickness)return era;
            cursor-=thickness;
            if(era<4){if(y<=cursor&&y>cursor-sep)return -1;cursor-=sep;}
        }
        return -1;
    }
    public static int hash(int x,int y,int z){int h=x*73428767^y*9123671^z*1274126177;h=(h^(h>>>16))*0x7feb352d;return h^(h>>>15);}
    public static BlockState block(int x,int y,int z){
        var c=column(x,z);if(y>c.surface)return Blocks.AIR.defaultBlockState();
        int era=era(y,c),h=Math.floorMod(hash(x,y,z),100);
        if(era<0)return (h<25?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get().defaultBlockState();
        var aquifer=EpochalAquifers.block(x,y,z,era);if(aquifer!=null)return aquifer;
        if(h<10)return ArchaeologyContent.SUSPICIOUS.get().defaultBlockState().setValue(TimeSandBlock.ERA,era);
        if(h<14)return ArchaeologyContent.relic(era).defaultBlockState();
        return ArchaeologyContent.SAND.get().defaultBlockState();
    }
    private EpochalTerrain(){}
}
