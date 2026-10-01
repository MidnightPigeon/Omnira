package com.mcmagic.omnira.world.structure;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;

/** Palace shell and player-authored furnishings from the TEST overworld sample. */
public final class FrozenTerraPalacePlan {
    public static final int WIDTH=49, DEPTH=65, HEIGHT=24;
    public record Cell(String name,String half,String facing) {
        public Cell(String name){this(name,"","");}
        public Cell(String name,String half){this(name,half,"");}
    }
    private static final class Blocks {
        static final String AIR="minecraft:air",SOLIDIFIED_LIGHT_CORE="omnira:solidified_light_crystal_core",
                CHISELED_QUARTZ_BLOCK="minecraft:chiseled_quartz_block",POLISHED_BLACKSTONE_BRICKS="minecraft:polished_blackstone_bricks",
                QUARTZ_BRICKS="minecraft:quartz_bricks",QUARTZ_PILLAR="minecraft:quartz_pillar",
                SEA_LANTERN="minecraft:sea_lantern",SMOOTH_QUARTZ="minecraft:smooth_quartz";
    }
    private final Map<BlockPos,Cell> blocks=new LinkedHashMap<>();
    private FrozenTerraPalacePlan(){}
    private void set(int x,int y,int z,String block){blocks.put(new BlockPos(x,y,z),new Cell(block));}
    private void fill(int x,int y,int z,int xx,int yy,int zz,String block){
        for(int a=x;a<=xx;a++)for(int b=y;b<=yy;b++)for(int c=z;c<=zz;c++)set(a,b,c,block);
    }
    private void rim(int x,int z,int xx,int zz,int y,int height,String block){
        fill(x,y,z,xx,y+height-1,z,block);fill(x,y,zz,xx,y+height-1,zz,block);
        fill(x,y,z,x,y+height-1,zz,block);fill(xx,y,z,xx,y+height-1,zz,block);
    }
    private void tower(int cx,int cz){
        for(int y=2;y<=18;y++)for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++){
            int a=Math.abs(dx),b=Math.abs(dz);
            if(a+b>6)continue;
            boolean edge=a==4||b==4||a+b>=6;
            if(edge || y==2 || y==16)set(cx+dx,y,cz+dz,
                    y==2||y==16||y==18?Blocks.SOLIDIFIED_LIGHT_CORE:y%4==0?Blocks.CHISELED_QUARTZ_BLOCK:Blocks.QUARTZ_BRICKS);
            if(y==18 && ((dx+dz)&1)==0 && edge)set(cx+dx,y+1,cz+dz,Blocks.CHISELED_QUARTZ_BLOCK);
        }
        fill(cx-1,17,cz-1,cx+1,18,cz+1,Blocks.SOLIDIFIED_LIGHT_CORE);
        set(cx,19,cz,Blocks.SEA_LANTERN);
    }
    public static Map<BlockPos,Cell> create(){return create(true);}
    public static Map<BlockPos,Cell> create(boolean throne){
        var p=new FrozenTerraPalacePlan();
        // Quartz and golden light crystal recall monumental stone and gilded glass,
        // while the black plinth makes the intentionally vacant interior readable.
        p.fill(0,0,0,48,0,64,Blocks.POLISHED_BLACKSTONE_BRICKS);
        p.fill(1,1,1,47,1,63,Blocks.SMOOTH_QUARTZ);
        p.rim(3,3,45,61,2,10,Blocks.QUARTZ_BRICKS);
        for(int y=2;y<=10;y+=4)p.rim(3,3,45,61,y,1,Blocks.SOLIDIFIED_LIGHT_CORE);
        p.rim(2,2,46,62,12,1,Blocks.SOLIDIFIED_LIGHT_CORE);
        for(int z=6;z<=58;z+=4){
            p.set(2,13,z,Blocks.CHISELED_QUARTZ_BLOCK);p.set(46,13,z,Blocks.CHISELED_QUARTZ_BLOCK);
        }
        for(int x=6;x<=42;x+=4){
            p.set(x,13,2,Blocks.CHISELED_QUARTZ_BLOCK);p.set(x,13,62,Blocks.CHISELED_QUARTZ_BLOCK);
        }
        for(int cx:new int[]{5,43})for(int cz:new int[]{5,59})p.tower(cx,cz);
        // Processional approach, front portal, and two levels of colonnade.
        p.fill(18,2,61,30,10,62,Blocks.AIR);
        p.fill(19,1,62,29,1,64,Blocks.SOLIDIFIED_LIGHT_CORE);
        // A broad half-block ascent across the existing processional entrance.
        for(int x=19;x<=29;x++){
            p.set(x,0,64,"omnira:solidified_light_crystal_core_stairs");
            p.blocks.put(new BlockPos(x,0,64),new Cell("omnira:solidified_light_crystal_core_stairs","","north"));
            p.set(x,1,64,Blocks.AIR);
            p.blocks.put(new BlockPos(x,1,63),new Cell("omnira:solidified_light_crystal_core_stairs","","north"));
        }
        p.fill(20,2,61,28,10,63,Blocks.AIR);
        for(int x:new int[]{10,14,34,38})for(int z=18;z<=50;z+=8){
            p.fill(x,2,z,x,10,z,Blocks.QUARTZ_PILLAR);
            p.fill(x-1,11,z-1,x+1,11,z+1,Blocks.CHISELED_QUARTZ_BLOCK);
            p.set(x,12,z,Blocks.SOLIDIFIED_LIGHT_CORE);
        }
        // Sanctuary shell: a long clear nave with a broad, unobstructed centerline.
        p.rim(10,8,38,53,2,14,Blocks.QUARTZ_BRICKS);
        for(int y:new int[]{2,6,11,15})p.rim(10,8,38,53,y,1,Blocks.SOLIDIFIED_LIGHT_CORE);
        p.fill(20,2,52,28,12,54,Blocks.AIR);
        // A broad vestibule opens into the sanctuary, with narrow passages around it.
        p.fill(11,2,45,37,11,45,Blocks.QUARTZ_BRICKS);
        p.fill(20,2,45,28,10,45,Blocks.AIR);
        for(int x:new int[]{14,34})p.fill(x,2,45,x+1,8,45,Blocks.AIR);
        p.fill(11,1,46,37,1,52,Blocks.POLISHED_BLACKSTONE_BRICKS);
        p.fill(20,1,46,28,1,52,Blocks.SMOOTH_QUARTZ);
        for(int z=18;z<=50;z+=8){
            for(int x:new int[]{6,42})p.fill(x,2,z,x,8,z,Blocks.QUARTZ_PILLAR);
        }
        for(int z=18;z<=42;z+=8){
            p.fill(4,1,z,9,1,z,Blocks.SOLIDIFIED_LIGHT_CORE);
            p.fill(39,1,z,44,1,z,Blocks.SOLIDIFIED_LIGHT_CORE);
        }
        // A processional line directs the eye toward the otherwise undecorated throne dais.
        for(int z=18;z<=44;z++){
            p.set(24,1,z,Blocks.SOLIDIFIED_LIGHT_CORE);
            if(z%4==0){p.set(19,1,z,Blocks.CHISELED_QUARTZ_BLOCK);p.set(29,1,z,Blocks.CHISELED_QUARTZ_BLOCK);}
        }
        for(int z=14;z<=46;z+=8){
            for(int x:new int[]{10,38}){
                p.fill(x,7,z-1,x,10,z+1,Blocks.SEA_LANTERN);
                p.set(x,6,z,Blocks.CHISELED_QUARTZ_BLOCK);
                p.set(x,11,z,Blocks.CHISELED_QUARTZ_BLOCK);
            }
        }
        p.fill(10,16,8,38,16,53,Blocks.SMOOTH_QUARTZ);
        p.rim(9,7,39,54,17,1,Blocks.SOLIDIFIED_LIGHT_CORE);
        for(int x=9;x<=39;x+=2){p.set(x,18,7,Blocks.CHISELED_QUARTZ_BLOCK);p.set(x,18,54,Blocks.CHISELED_QUARTZ_BLOCK);}
        for(int z=9;z<=53;z+=2){p.set(9,18,z,Blocks.CHISELED_QUARTZ_BLOCK);p.set(39,18,z,Blocks.CHISELED_QUARTZ_BLOCK);}
        // Elevated dais and solitary Golden Throne at the rear of the sanctuary.
        p.fill(18,2,10,30,2,17,Blocks.SOLIDIFIED_LIGHT_CORE);
        p.fill(20,3,10,28,3,15,Blocks.SMOOTH_QUARTZ);
        p.fill(22,4,10,26,4,13,Blocks.SOLIDIFIED_LIGHT_CORE);
        p.fill(22,2,18,26,2,19,Blocks.SMOOTH_QUARTZ);
        p.fill(22,3,16,26,3,17,Blocks.SMOOTH_QUARTZ);
        p.fill(22,4,14,26,4,15,Blocks.SMOOTH_QUARTZ);
        if(throne){
            p.blocks.put(new BlockPos(24,5,11),new Cell("omnira:golden_throne","lower"));
            p.blocks.put(new BlockPos(24,6,11),new Cell("omnira:golden_throne","upper"));
        }
        for(int x:new int[]{18,30}){
            p.fill(x,3,10,x,13,10,Blocks.QUARTZ_PILLAR);
            p.set(x,14,10,Blocks.SOLIDIFIED_LIGHT_CORE);
        }
        // The two colonnades and four tower displays mirror the edited spawn-area sample.
        for(int x:new int[]{19,29})for(int z=20;z<=44;z+=4){
            p.blocks.put(new BlockPos(x,2,z),new Cell("omnira:light_crystal_torch","lower"));
            p.blocks.put(new BlockPos(x,3,z),new Cell("omnira:light_crystal_torch","upper"));
        }
        for(int x:new int[]{6,42})for(int z=18;z<=50;z+=8)
            p.set(x,9,z,"omnira:shadow_lantern");
        for(int x:new int[]{6,42})for(int z:new int[]{6,58}){
            p.set(x,2,z,Blocks.CHISELED_QUARTZ_BLOCK);
            p.blocks.put(new BlockPos(x,3,z),new Cell("omnira:crystal_ball","","south"));
        }
        return Map.copyOf(p.blocks);
    }
}
