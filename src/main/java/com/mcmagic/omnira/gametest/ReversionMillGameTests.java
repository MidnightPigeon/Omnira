package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_mill")
@PrefixGameTestTemplate(false)
public final class ReversionMillGameTests {
    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void blueprintAndAccess(GameTestHelper h)throws Exception{
        JsonObject plan;
        try(var reader=new java.io.InputStreamReader(Objects.requireNonNull(ReversionMillGameTests.class.getResourceAsStream("/data/omnira/structures/rusty_lake_mill_plan.json")),java.nio.charset.StandardCharsets.UTF_8)){
            plan=JsonParser.parseReader(reader).getAsJsonObject();
        }
        Map<BlockPos,BlockState> states=new HashMap<>();int stairs=0,fluid=0,lilies=0;
        for(var raw:plan.getAsJsonArray("cells")){
            var c=raw.getAsJsonObject();var pos=new BlockPos(c.get("x").getAsInt(),c.get("y").getAsInt(),c.get("z").getAsInt());
            var props=new HashMap<String,String>();if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
            String name=c.get("name").getAsString();var cell=new KirisameShopPlan.Cell(pos,name,props,"",List.of(),"",false);
            var state=KirisameShopPlan.state(cell);h.assertTrue(states.put(pos,state)==null,"Duplicate blueprint position");
            h.assertTrue(pos.getY()>=-2&&pos.getY()<25&&pos.getX()>=0&&pos.getX()<49&&pos.getZ()>=0&&pos.getZ()<45,"Out of sample bounds");
            if(name.equals("omnira:decayed_timeflow"))fluid++;
            if(name.endsWith("rottenleaf_lily"))lilies++;
            h.assertTrue(!Set.of("omnira:temporal_silt","omnira:living_temporal_silt").contains(name),"Raw sedimenting soil in sample");
        }
        var l=h.getLevel();var at=h.absolutePos(new BlockPos(4,4,4));
        for(var entry:states.entrySet())if(entry.getValue().getBlock() instanceof StairBlock){
            var s=entry.getValue();l.setBlock(at,s,2);
            for(var d:Direction.values())l.setBlock(at.relative(d),states.getOrDefault(entry.getKey().relative(d),Blocks.AIR.defaultBlockState()),2);
            var updated=s.updateShape(Direction.NORTH,l.getBlockState(at.north()),l,at,at.north());
            h.assertTrue(s.getValue(StairBlock.SHAPE)==updated.getValue(StairBlock.SHAPE),"Stair seam: "+entry.getKey());stairs++;
        }
        h.assertTrue(stairs>200&&fluid>300&&lilies>=6,"Missing roof, pond or lilies: "+stairs+"/"+fluid+"/"+lilies);
        for(int n=0;n<5;n++)for(int x:new int[]{30,31,23,24}){
            int y=x>=30?3+n:8+n,z=x>=30?22+n:29-n;
            for(int dy=1;dy<=2;dy++)h.assertTrue(states.getOrDefault(new BlockPos(x,y+dy,z),Blocks.AIR.defaultBlockState()).isAir(),"Blocked stair headroom at "+x+","+(y+dy)+","+z);
        }
        for(var tree:plan.getAsJsonArray("trees")){
            var root=tree.getAsJsonObject().getAsJsonArray("root");var p=new BlockPos(root.get(0).getAsInt(),root.get(1).getAsInt(),root.get(2).getAsInt());
            h.assertTrue(states.get(p.below()).is(com.mcmagic.omnira.time.TemporalSoils.SILTS),"Tree has no temporal soil");
            h.assertTrue(states.get(p).is(com.mcmagic.omnira.registry.TimeNatureContent.LOG.get()),"Tree footprint root missing");
        }
        h.succeed();
    }
}
