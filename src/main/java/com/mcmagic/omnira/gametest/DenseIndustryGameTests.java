package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.client.ReactionPoolPanelMesh;
import com.mcmagic.omnira.client.ResonanceNodeMotion;
import com.mcmagic.omnira.storage.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_dense_industry")
@PrefixGameTestTemplate(false)
public final class DenseIndustryGameTests {
    @GameTest(template="spell_arena") public static void spatialPhasesPreserveCadenceAndSpreadOneThousandNodes(GameTestHelper h){
        int[] transfers=new int[5],refreshes=new int[20];
        for(int x=0;x<40;x++)for(int z=0;z<25;z++){
            var pos=new BlockPos(x,5,z);int moved=0,refreshed=0;
            for(int t=0;t<20;t++){
                if(NodeSchedule.due(t,pos,5)){moved++;transfers[t%5]++;}
                if(NodeSchedule.due(t,pos,20)){refreshed++;refreshes[t]++;h.assertTrue(NodeSchedule.due(t,pos,5),"Refresh no longer coincides with transfer");}
            }
            h.assertTrue(moved==4&&refreshed==1,"Node cadence changed");
        }
        h.assertTrue(Arrays.stream(transfers).max().orElseThrow()==800&&Arrays.stream(transfers).min().orElseThrow()==800,"Transfer phases are concentrated");
        h.assertTrue(Arrays.stream(refreshes).max().orElseThrow()<=51,"Refresh phases are concentrated");h.succeed();
    }

    @GameTest(template="spell_arena",timeoutTicks=40) public static void staggeredNodesKeepTheirActualPerSecondThroughput(GameTestHelper h){
        var world=h.getLevel();var targets=new ArrayList<CrystalBallBlockEntity>();
        for(int row=0;row<3;row++){
            var a=h.absolutePos(new BlockPos(4+row,3,4+row*4));var b=a.east(4);
            for(var p:List.of(a,b)){world.setBlockAndUpdate(p,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());world.setBlockAndUpdate(p.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());}
            var source=(CrystalBallBlockEntity)world.getBlockEntity(a);var target=(CrystalBallBlockEntity)world.getBlockEntity(b);targets.add(target);
            source.setItem(0,new ItemStack(Items.STONE,64));
            h.assertTrue(NodeTransport.pair((ResonanceCoreBlockEntity)world.getBlockEntity(a.above()),(ResonanceCoreBlockEntity)world.getBlockEntity(b.above())),"Pair rejected");
        }
        h.runAfterDelay(20,()->{for(var target:targets)h.assertTrue(target.countItem(Items.STONE)==4,"Spatial phase changed four-item-per-second throughput");h.succeed();});
    }

    @GameTest(template="spell_arena") public static void sharedNodeTrajectoryIsBitExact(GameTestHelper h){
        for(int t=0;t<1000;t++){
            double time=t*.237;var points=ResonanceNodeMotion.ring(time);
            h.assertTrue(points==ResonanceNodeMotion.ring(time),"Same-frame ring allocated again");
            for(int i=0;i<14;i++){
                double a=time*.045+i*Math.PI*2/14;
                float[] old={.5F+(float)Math.cos(a)*.37F,.44F+(float)Math.sin(a*2+time*.025)*.045F,.5F+(float)Math.sin(a)*.37F};
                for(int axis=0;axis<3;axis++)h.assertTrue(Float.floatToIntBits(old[axis])==Float.floatToIntBits(points[i*3+axis]),"Node trajectory changed");
            }
        }h.succeed();
    }

    @GameTest(template="spell_arena") public static void cachedPanelRetainsAllPartialLiquidFaces(GameTestHelper h){
        double bottom=.75/16-1.95/16,radius=1.95/16;
        for(int i=0;i<100;i++){
            double low=bottom+2*radius*i/131,high=bottom+2*radius*(i+31)/131;
            var actual=ReactionPoolPanelMesh.faces(low,high);var expected=legacyPanel(low,high);
            h.assertTrue(actual==ReactionPoolPanelMesh.faces(low,high),"Same liquid range rebuilt panel geometry");
            h.assertTrue(actual.size()==expected.size(),"Panel face count changed");
            for(int n=0;n<actual.size();n++)h.assertTrue(Arrays.deepEquals(actual.get(n),expected.get(n)),"Panel partial-cell coordinates/winding changed");
        }
        ReactionPoolPanelMesh.reset();var first=ReactionPoolPanelMesh.faces(bottom,bottom+radius);
        for(int i=0;i<70;i++)ReactionPoolPanelMesh.faces(bottom+i*.0001,bottom+radius+i*.0001);
        h.assertTrue(first!=ReactionPoolPanelMesh.faces(bottom,bottom+radius),"Panel cache is unbounded");h.succeed();
    }
    private static List<float[][]> legacyPanel(double base,double top){
        double centerY=.75/16,centerZ=-.1/16,unit=1.95/4.1/16;
        int[][] neighbours={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}},faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
        var result=new ArrayList<float[][]>();
        for(int y=-4;y<4;y++)for(int x=-4;x<4;x++)for(int z=-4;z<4;z++){
            double yy=centerY+y*unit,lo=Math.max(base,yy),hi=Math.min(top,yy+unit);if(hi<=lo||!cell(x,y,z))continue;
            double a=x*unit,b=centerZ+z*unit,c=(x+1)*unit,d=centerZ+(z+1)*unit;
            float[][] v={{(float)a,(float)lo,(float)b},{(float)c,(float)lo,(float)b},{(float)c,(float)hi,(float)b},{(float)a,(float)hi,(float)b},
                    {(float)a,(float)lo,(float)d},{(float)c,(float)lo,(float)d},{(float)c,(float)hi,(float)d},{(float)a,(float)hi,(float)d}};
            for(int side=0;side<6;side++){
                var n=neighbours[side];if(cell(x+n[0],y+n[1],z+n[2])&&!(side==4&&hi==top)&&!(side==5&&lo==base))continue;
                var f=faces[side];result.add(new float[][]{v[f[0]],v[f[1]],v[f[2]],v[f[3]]});
            }
        }return result;
    }
    private static boolean cell(int x,int y,int z){return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5)<=4.1*4.1;}
}
