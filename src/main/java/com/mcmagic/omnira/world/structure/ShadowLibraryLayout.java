package com.mcmagic.omnira.world.structure;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** Seeded tree of rooms; every new room has exactly one constructed connection. */
public final class ShadowLibraryLayout {
    public record Room(String kind,int x,int z,int parent) {
        public int half() {return kind.equals("research")?5:4;}
        public BlockPos origin() {return new BlockPos(x*16-half(),0,z*16-half());}
    }
    public final List<Room> rooms;
    public final boolean waymark,analysisTable,enchantingTable;
    public ShadowLibraryLayout(long seed) {
        var random=RandomSource.create(net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(seed));
        int count=4+random.nextInt(4);
        var list=new ArrayList<Room>();list.add(new Room("stacks",0,0,-1));
        int research=1+random.nextInt(count-1),sealed;
        do {sealed=1+random.nextInt(count-1);}while(sealed==research);
        while(list.size()<count) {
            var choices=new ArrayList<int[]>();
            for(int parent=0;parent<list.size();parent++)for(var d:Direction.Plane.HORIZONTAL) {
                if(parent==0 && d==Direction.WEST)continue; // Keep the entrance ladder clear.
                int x=list.get(parent).x()+d.getStepX(),z=list.get(parent).z()+d.getStepZ();
                if(Math.abs(x)>2 || Math.abs(z)>2 || list.stream().anyMatch(r->r.x()==x && r.z()==z))continue;
                choices.add(new int[]{x,z,parent});
            }
            var next=choices.get(random.nextInt(choices.size()));int i=list.size();
            list.add(new Room(i==research?"research":i==sealed?"sealed":"stacks",next[0],next[1],next[2]));
        }
        rooms=List.copyOf(list);waymark=random.nextFloat()<.2F;
        analysisTable=random.nextBoolean();enchantingTable=random.nextBoolean();
    }
    public BoundingBox bounds(BlockPos origin,int surfaceY) {
        int minX=0,maxX=0,minZ=0,maxZ=0;
        for(var r:rooms) {
            minX=Math.min(minX,r.x()*16-r.half());maxX=Math.max(maxX,r.x()*16+r.half());
            minZ=Math.min(minZ,r.z()*16-r.half());maxZ=Math.max(maxZ,r.z()*16+r.half());
        }
        return new BoundingBox(origin.getX()+minX,origin.getY(),origin.getZ()+minZ,
                origin.getX()+maxX,surfaceY,origin.getZ()+maxZ);
    }
    /** Horizontal footprints include the full corridor shell, not the gaps between rooms. */
    public Set<BlockPos> footprint() {
        var result=new HashSet<BlockPos>();
        for(var r:rooms) {
            for(int x=-r.half();x<=r.half();x++)for(int z=-r.half();z<=r.half();z++)
                result.add(new BlockPos(r.x()*16+x,0,r.z()*16+z));
            if(r.parent()<0)continue;
            var a=rooms.get(r.parent());int dx=Integer.signum(r.x()-a.x()),dz=Integer.signum(r.z()-a.z());
            for(int t=a.half();t<=16-r.half();t++)for(int side=-2;side<=2;side++)
                result.add(new BlockPos(a.x()*16+dx*t+dz*side,0,a.z()*16+dz*t+dx*side));
        }
        return result;
    }
}
