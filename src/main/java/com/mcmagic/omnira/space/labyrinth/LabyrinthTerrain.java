package com.mcmagic.omnira.space.labyrinth;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.TimeBiomeLayout;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;

/** The same clipped room plan drives terrain, decorations and portal target validation. */
public final class LabyrinthTerrain {
    public static final int SIZE=7,SURFACE=40,BASE=20,KINDS=14;
    public record Room(int x,int y,int z){
        public BlockPos origin(){return new BlockPos(x*SIZE,BASE+y*SIZE,z*SIZE);}
        public Room adjacent(Direction direction){return new Room(x+direction.getStepX(),y+direction.getStepY(),z+direction.getStepZ());}
        public int floor(){return Math.max(23,origin().getY());}
    }
    public record Plan(Map<BlockPos,BlockState> blocks,Map<Direction,BlockPos> doors,int kind){}
    private static final ThreadLocal<Map<Room,Plan>> CACHE=ThreadLocal.withInitial(()->new LinkedHashMap<>(256,.75F,true){
        @Override protected boolean removeEldestEntry(Map.Entry<Room,Plan> e){return size()>256;}
    });
    public static boolean biome(int x,int z){return TimeBiomeLayout.sample(x+8192,z-4096).region()==4;}
    public static Room room(BlockPos pos){return new Room(Math.floorDiv(pos.getX(),SIZE),Math.floorDiv(pos.getY()-BASE,SIZE),Math.floorDiv(pos.getZ(),SIZE));}
    public static int hash(Room room){int h=room.x*73428767^room.y*9123671^room.z*1274126177;h=(h^(h>>>16))*0x7feb352d;return h^(h>>>15);}
    public static boolean entryRoof(Room room){return Math.floorMod(hash(room)>>>4,5)==0;}
    private static boolean inside(int x,int z){return biome(x,z)&&biome(x-1,z)&&biome(x+1,z)&&biome(x,z-1)&&biome(x,z+1);}
    public static boolean exists(Room room){
        if(room.y<0||room.y>2)return false;
        var o=room.origin();
        for(int x=1;x<=5;x++)for(int z=1;z<=5;z++)if(inside(o.getX()+x,o.getZ()+z))return true;
        return false;
    }
    public static Plan plan(Room room){return CACHE.get().computeIfAbsent(room,LabyrinthTerrain::build);}
    public static BlockState block(int x,int y,int z){
        if(y>SURFACE)return y>=com.mcmagic.omnira.space.FoldedTerrain.roof(x,z)?DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState():Blocks.AIR.defaultBlockState();
        return plan(room(new BlockPos(x,y,z))).blocks.getOrDefault(new BlockPos(x,y,z),ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState());
    }
    private static Plan build(Room room){
        Map<BlockPos,BlockState> cells=new HashMap<>();Map<Direction,BlockPos> doors=new EnumMap<>(Direction.class);
        var o=room.origin();int kind=Math.floorMod(hash(room),KINDS),floor=room.floor(),ceiling=o.getY()+6;
        for(int x=0;x<SIZE;x++)for(int z=0;z<SIZE;z++)for(int y=Math.max(24,o.getY());y<=Math.min(SURFACE,ceiling);y++){
            var p=new BlockPos(o.getX()+x,y,o.getZ()+z);if(!biome(p.getX(),p.getZ()))continue;
            boolean wall=x==0||x==6||z==0||z==6||y==o.getY()||y==ceiling||!inside(p.getX(),p.getZ());
            cells.put(p,y==SURFACE?(entryRoof(room)?DreamContent.EXCITED_SPATIAL_CRYSTAL.get():ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()).defaultBlockState():wall?ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState():Blocks.AIR.defaultBlockState());
        }
        if(!exists(room))return new Plan(Map.copyOf(cells),Map.of(),kind);
        if(room.y==2&&!entryRoof(room)){
            BlockPos ceilingPortal=null;int distance=Integer.MAX_VALUE;
            for(int x=1;x<=5;x++)for(int z=1;z<=5;z++){
                int score=(x-3)*(x-3)+(z-3)*(z-3);var p=o.offset(x,6,z);
                if(score<distance&&inside(p.getX(),p.getZ())){ceilingPortal=p;distance=score;}
            }
            if(ceilingPortal!=null)cells.put(ceilingPortal,LabyrinthContent.LONG_RANGE_PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,Direction.UP));
        }
        for(var d:Direction.values()){
            if(!exists(room.adjacent(d)))continue;
            int midY=Math.max(floor+2,o.getY()+3);
            var p=new BlockPos(o.getX()+3,midY,o.getZ()+3);
            p=switch(d){case WEST->p.atY(midY).offset(-3,0,0);case EAST->p.offset(3,0,0);case NORTH->p.offset(0,0,-3);case SOUTH->p.offset(0,0,3);case DOWN->p.atY(o.getY());case UP->p.atY(ceiling);};
            if(p.getY()<=23||p.getY()>=SURFACE||!biome(p.getX(),p.getZ())||!inside(p.relative(d.getOpposite()).getX(),p.relative(d.getOpposite()).getZ()))continue;
            var state=LabyrinthContent.PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,d);
            cells.put(p,state);
            doors.put(d,p);
        }
        int ground=floor+1;
        // Only supported corners inside the clipped footprint receive the two-block torch.
        for(int x:new int[]{1,5})for(int z:new int[]{1,5}){
            var p=new BlockPos(o.getX()+x,ground,o.getZ()+z);
            if(air(cells,p)&&air(cells,p.above())&&inside(p.getX(),p.getZ())){
                var torch=DreamContent.LIGHT_CRYSTAL_TORCH.get().defaultBlockState().setValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.MOUNT,Direction.UP);
                cells.put(p,torch);cells.put(p.above(),torch.setValue(com.mcmagic.omnira.block.LightCrystalTorchBlock.HALF,DoubleBlockHalf.UPPER));
            }
        }
        var center=new BlockPos(o.getX()+3,ground,o.getZ()+3);
        switch(kind){
            case 1->put(cells,center,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
            case 2->put(cells,center,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
            case 3,4->{var fluid=(kind==3?com.mcmagic.omnira.space.sea.VoidSeaContent.LIQUID:com.mcmagic.omnira.mire.MireContent.LIQUID).get().defaultBlockState();
                for(int x=1;x<=5;x++)for(int z=1;z<=5;z++)put(cells,new BlockPos(o.getX()+x,ground,o.getZ()+z),fluid);
                for(var door:doors.values())if(door.getY()<=ground)cells.put(door,cells.get(door).setValue(VoidPortalBlock.FLUID,kind==3?2:3));}
            case 5->pile(cells,room,Blocks.CRYING_OBSIDIAN.defaultBlockState());
            case 6->{if(air(cells,center)&&air(cells,center.above())){
                var statue=ModBlocks.NIGHT_HERON_STATUE.get().defaultBlockState();cells.put(center,statue);cells.put(center.above(),statue.setValue(com.mcmagic.omnira.block.NightHeronStatueBlock.HALF,DoubleBlockHalf.UPPER));}}
            case 7->put(cells,center,ModBlocks.DREAM_SPELL_CORE.get().defaultBlockState());
            case 8->pile(cells,room,com.mcmagic.omnira.archaeology.ArchaeologyContent.SUSPICIOUS.get().defaultBlockState());
            case 9->{put(cells,center.west(),ModBlocks.CRYSTAL_BALL.get().defaultBlockState());put(cells,center.east(),ModBlocks.CRYSTAL_BALL.get().defaultBlockState());}
            case 10->{put(cells,center.west(),ModBlocks.CRYSTAL_BALL.get().defaultBlockState());put(cells,center.east(),ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());}
            case 11->pile(cells,room,DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE.get().defaultBlockState());
            case 12->put(cells,center,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            case 13->{Block[] matrices={Blocks.BUDDING_AMETHYST,DreamContent.DREAM_CRYSTAL_BEDROCK.get(),TimeNatureContent.SPATIAL_MATRIX.get(),com.mcmagic.omnira.space.geodes.DeflectionContent.MATRIX.get()};
                int i=0;for(int x:new int[]{2,4})for(int z:new int[]{2,4})put(cells,new BlockPos(o.getX()+x,ground,o.getZ()+z),matrices[i++].defaultBlockState());}
            default->{}
        }
        return new Plan(Map.copyOf(cells),Map.copyOf(doors),kind);
    }
    private static boolean air(Map<BlockPos,BlockState> cells,BlockPos p){return cells.containsKey(p)&&cells.get(p).isAir();}
    private static void put(Map<BlockPos,BlockState> cells,BlockPos p,BlockState s){if(air(cells,p))cells.put(p,s);}
    private static void pile(Map<BlockPos,BlockState> cells,Room room,BlockState state){
        var o=room.origin();int floor=room.floor()+1;
        for(int x:new int[]{2,4})for(int z:new int[]{2,4}){
            var p=new BlockPos(o.getX()+x,floor,o.getZ()+z);put(cells,p,state);
            if(((hash(room)>>>x+z)&1)==0)put(cells,p.above(),state);
        }
    }
    private LabyrinthTerrain(){}
}
