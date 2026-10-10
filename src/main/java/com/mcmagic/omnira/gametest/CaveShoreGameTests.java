package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.space.sea.*;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_cave_shore") @PrefixGameTestTemplate(false)
public final class CaveShoreGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200) public static void actualCoastsMeetAndContainFluid(GameTestHelper h){
        int sites=0,sources=0;
        search:for(int x=-512;x<=512;x+=8)for(int z=-512;z<=512;z+=8){
            if(!VoidSeaTerrain.biome(x,z)||CaveSurface.coastDistance(x,z)!=0)continue;
            h.assertTrue(VoidSeaTerrain.floor(x,z)==40,"Sea boundary not level with shore");
            for(Direction d:Direction.Plane.HORIZONTAL){
                int nx=x+d.getStepX(),nz=z+d.getStepZ();
                if(!VoidSeaTerrain.biome(nx,nz))h.assertTrue(FoldedTerrain.floor(nx,nz)==40,"Raised or leaking boundary");
            }
            for(int px=x-16;px<=x+16;px++)for(int pz=z-16;pz<=z+16;pz++){
                int floor=height(px,pz);
                h.assertTrue(Math.abs(floor-height(px+1,pz))<=2&&Math.abs(floor-height(px,pz+1))<=2,"Abrupt ground step");
                for(int y=25;y<=40;y++){
                    var state=CorridorLayout.block(px,y,pz);if(state.getFluidState().isEmpty())continue;
                    sources++;
                    for(Direction d:Direction.values()){
                        if(d==Direction.UP)continue;
                        var next=CorridorLayout.block(px+d.getStepX(),y+d.getStepY(),pz+d.getStepZ());
                        h.assertTrue(!next.isAir(),"Uncontained source at "+px+","+y+","+pz+" toward "+d);
                    }
                }
            }
            if(++sites==6)break search;
        }
        h.assertTrue(sites==6&&sources>0,"Missing coastal samples or fluids");h.succeed();
    }
    private static int height(int x,int z){return VoidSeaTerrain.biome(x,z)?VoidSeaTerrain.floor(x,z):FoldedTerrain.floor(x,z);}
}
