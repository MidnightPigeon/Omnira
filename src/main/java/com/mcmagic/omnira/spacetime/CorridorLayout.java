package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/** Three continuous strata with random one-way rifts in the middle corridor. */
public final class CorridorLayout {
    public static final int FLOOR=128,SEPARATOR_BOTTOM=112,SEPARATOR_TOP=144;
    public static final int TIME_BASE=170,SPACE_CEILING=103,SPACE_ROOF=96;
    public static final int RIFT_RANGE=2048;
    public static final BlockPos TIME_ENTRANCE=new BlockPos(160,225,0),SPACE_ENTRANCE=new BlockPos(-160,41,0);
    private CorridorLayout(){}
    public static BlockPos entrance(boolean time){return time?TIME_ENTRANCE:SPACE_ENTRANCE;}
    public static int riftHash(int z){int n=Math.floorDiv(z,24)*73428767;n=(n^(n>>>16))*1274126177;return n^(n>>>15);}
    public static boolean corridorRift(int x,int z){
        int h=riftHash(z);return Math.floorMod(z,24)==12 && (h&3)!=0 && x==((h&4)==0?-2:2);
    }
    public static int riftBottom(int z){return FLOOR+1+((riftHash(z)&8)==0?0:1);}
    public static int timeSurface(int x,int z){
        if(StrataBiomeSource.epochalCliffs(x,z))return com.mcmagic.omnira.archaeology.EpochalTerrain.column(x,z).surface();
        if(StrataBiomeSource.reversionMire(x,z))return com.mcmagic.omnira.mire.MireTerrain.column(x,z).floor();
        if(!StrataBiomeSource.frozenTerra(x,z)&&!StrataBiomeSource.fleetingWoods(x,z))return 224;
        double ridge=Math.pow(Math.abs(Math.sin((x+.35*z)/78.0+.5*Math.sin(z/155.0))),1.3);
        double spur=.5+.5*Math.sin((z-.2*x)/105.0);
        double mountain=206+ridge*(17+7*spur)+1.5*Math.sin(x/29.0)*Math.cos(z/37.0);
        if(StrataBiomeSource.fleetingWoods(x,z))mountain=222+2*Math.sin(x/63.0)*Math.cos(z/75.0);
        double blend=Math.min(1,StrataBiomeSource.sample(x,z).edgeDistance()/32.0);
        return Math.clamp((int)Math.round(224+(mountain-224)*blend),204,230);
    }
    private static BlockState rift(boolean time,int y,int base,Direction direction){
        return ModBlocks.SPACETIME_RIFT.get().defaultBlockState().setValue(SpacetimeRiftBlock.TIME,time)
                .setValue(SpacetimeRiftBlock.FACING,direction).setValue(SpacetimeRiftBlock.HALF,y==base?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER);
    }
    public static BlockState block(int x,int y,int z){
        if(Math.abs(x)<=2 && y>=FLOOR && y<=FLOOR+4){
            if(y==FLOOR)return corridorRock(x,y,z,false);
            int bottom=riftBottom(z);
            if(y>=bottom && y<=bottom+1 && corridorRift(x,z))return rift(true,y,bottom,x<0?Direction.EAST:Direction.WEST);
            if(Math.abs(x)==2 || y==FLOOR+4)return corridorRock(x,y,z,y==FLOOR+4 || Math.floorMod(z,4)==0);
            return Blocks.AIR.defaultBlockState();
        }
        if(y>=SEPARATOR_BOTTOM && y<=SEPARATOR_TOP)return ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState();
        if(y==TIME_BASE||y==SPACE_CEILING)return ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState();
        if(y>TIME_BASE&&StrataBiomeSource.epochalCliffs(x,z))return com.mcmagic.omnira.archaeology.EpochalTerrain.block(x,y,z);
        if(y>=TIME_BASE&&y<=230&&StrataBiomeSource.reversionMire(x,z)){
            var mire=com.mcmagic.omnira.mire.MireTerrain.column(x,z);
            if(y>mire.floor())return mire.pond()&&y<=com.mcmagic.omnira.mire.MireTerrain.WATER_Y?com.mcmagic.omnira.mire.MireContent.LIQUID.get().defaultBlockState():Blocks.AIR.defaultBlockState();
            if(mire.shore()&&y>=mire.floor()-2)return com.mcmagic.omnira.mire.MireContent.SILT.get().defaultBlockState();
            return (Math.floorMod(hash(x,y,z),11)==0?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT).get().defaultBlockState();
        }
        if(y>=TIME_BASE&&y<=224&&com.mcmagic.omnira.time.RecurrencePonds.contains(x,z)){
            if(y>com.mcmagic.omnira.time.RecurrencePonds.FLOOR_Y)return y<=com.mcmagic.omnira.time.RecurrencePonds.WATER_Y
                    ?com.mcmagic.omnira.mire.MireContent.LIQUID.get().defaultBlockState():Blocks.AIR.defaultBlockState();
            return DreamContent.TEMPORAL_SILT.get().defaultBlockState();
        }
        if(y>TIME_BASE&&y<=timeSurface(x,z)&&StrataBiomeSource.eventideRuins(x,z)){
            if(com.mcmagic.omnira.time.EventideGround.masonry(x,y,z,timeSurface(x,z))){
                int variant=Math.floorMod(hash(x+17,y,z-31),20);
                return (variant==0?com.mcmagic.omnira.time.EventideMasonry.CRACKED
                        :variant==1?com.mcmagic.omnira.time.EventideMasonry.SANDBOUND
                        :com.mcmagic.omnira.time.EventideMasonry.BRICKS).get().defaultBlockState();
            }
            return com.mcmagic.omnira.mire.MireContent.SILT.get().defaultBlockState();
        }
        if(y>=TIME_BASE && y<=timeSurface(x,z)){
            boolean living=y==timeSurface(x,z)&&Math.floorMod(hash(x,0,z),11)==0;
            return (StrataBiomeSource.fleetingWoods(x,z)?(living?TimeNatureContent.LIVING_SILT:TimeNatureContent.SILT)
                    :(living?DreamContent.LIVING_TEMPORAL_SILT:DreamContent.TEMPORAL_SILT)).get().defaultBlockState();
        }
        if(y>=24 && y<=40 || y>=SPACE_ROOF && y<=SPACE_CEILING)return DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
        if(y>=41 && y<SPACE_ROOF){
            int cone=cone(x,y,z);
            if(cone!=0)return (Math.floorMod(hash(x,y,z),9)==0 && exposed(x,y,z)
                    ?DreamContent.EXCITED_SPATIAL_CRYSTAL:DreamContent.SPATIAL_CRYSTAL).get().defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static int hash(int x,int y,int z){
        int n=x*73428767 ^ y*9123671 ^ z*1274126177;
        n=(n^(n>>>16))*0x7feb352d;
        return n^(n>>>15);
    }

    private static BlockState corridorRock(int x,int y,int z,boolean engraved){
        boolean voidMarked=Math.floorMod(hash(x,y,z),11)==0;
        return (engraved?(voidMarked?DreamContent.VOID_ENGRAVED_MIRROR_ROCK:DreamContent.ENGRAVED_MIRROR_ROCK)
                :(voidMarked?DreamContent.VOID_MIRROR_ROCK:DreamContent.MIRROR_ROCK)).get().defaultBlockState();
    }

    private static int cone(int x,int y,int z){
        int cellX=Math.floorDiv(x,16),cellZ=Math.floorDiv(z,16);
        for(int cx=cellX-1;cx<=cellX+1;cx++)for(int cz=cellZ-1;cz<=cellZ+1;cz++){
            int seed=hash(cx,17,cz);
            if(Math.floorMod(seed,3)!=0)continue;
            int centerX=cx*16+Math.floorMod(seed>>>4,16);
            int centerZ=cz*16+Math.floorMod(seed>>>12,16);
            int height=7+Math.floorMod(seed>>>20,9);
            int radius=2+Math.floorMod(seed>>>8,3);
            int dx=x-centerX,dz=z-centerZ;
            for(int anchor:new int[]{40,SPACE_ROOF}){
                int distance=Math.abs(y-anchor);
                if(distance>height)continue;
                int width=Math.max(1,radius*(height-distance)/height);
                if(dx*dx+dz*dz<=width*width)return 1;
            }
        }
        return 0;
    }

    private static boolean exposed(int x,int y,int z){
        return (y>41 && cone(x,y-1,z)==0) || (y<SPACE_ROOF-1 && cone(x,y+1,z)==0)
                || cone(x+1,y,z)==0 || cone(x-1,y,z)==0 || cone(x,y,z+1)==0 || cone(x,y,z-1)==0;
    }
}
