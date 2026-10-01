package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.EpochalTerrain;
import com.mcmagic.omnira.spacetime.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_epochal_height")
@PrefixGameTestTemplate(false)
public final class EpochalHeightGameTests {
    @GameTest(template="spell_arena") public static void expandedBand(GameTestHelper h)throws Exception{
        var field=CorridorGenerator.class.getDeclaredField("STRATA");field.setAccessible(true);
        var bands=(int[][])field.get(null);h.assertTrue(bands[2][0]==170&&bands[2][1]>=230&&bands[0][1]==103,"Generation bands do not match strata");
        try(var reader=new java.io.InputStreamReader(EpochalHeightGameTests.class.getResourceAsStream("/data/omnira/dimension_type/spacetime_corridor.json"))){
            var dim=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            h.assertTrue(dim.get("height").getAsInt()==256&&230+15<256,"Platform exceeds world height");
        }
        int checked=0;
        for(int x=-1024;x<=1024;x+=32)for(int z=-1024;z<=1024;z+=32){
            if(!StrataBiomeSource.epochalCliffs(x,z))continue;
            var c=EpochalTerrain.column(x,z);int[] counts=new int[5];int sep=0;
            for(int y=EpochalTerrain.BASE+1;y<=c.top();y++){int era=EpochalTerrain.era(y,c);if(era<0)sep++;else counts[era]++;}
            h.assertTrue(sep==4,"Separator thickness varies");for(int n:counts)h.assertTrue(n>=10,"Thin stratum");
            if(c.surface()>=224){h.assertTrue(!CorridorLayout.block(x,c.surface(),z).isAir(),"Surface missing");checked++;}
        }
        h.assertTrue(checked>0,"No intact cliff tested");
        for(int y:new int[]{170,103})h.assertTrue(CorridorLayout.block(0,y,0).getDestroySpeed(h.getLevel(),net.minecraft.core.BlockPos.ZERO)<0,"Missing relocated barrier");
        h.succeed();
    }
}
