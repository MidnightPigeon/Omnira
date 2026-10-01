package com.mcmagic.omnira.gametest;

import com.google.gson.*;
import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_mill_repair")
@PrefixGameTestTemplate(false)
public final class MillRepairGameTests {
    @GameTest(template="spell_arena",timeoutTicks=180)
    public static void dryCellarAndUnopenedLoot(GameTestHelper h)throws Exception {
        JsonObject plan;
        try(var r=new java.io.InputStreamReader(Objects.requireNonNull(MillRepairGameTests.class.getResourceAsStream("/data/omnira/structures/rusty_lake_mill_plan.json")),java.nio.charset.StandardCharsets.UTF_8)) {
            plan=JsonParser.parseReader(r).getAsJsonObject();
        }
        var level=h.getLevel();var origin=h.absolutePos(new BlockPos(3,4,3));int balls=0;
        for(var raw:plan.getAsJsonArray("cells")) {
            var c=raw.getAsJsonObject();int x=c.get("x").getAsInt(),y=c.get("y").getAsInt(),z=c.get("z").getAsInt();
            var props=new HashMap<String,String>();if(c.has("properties"))c.getAsJsonObject("properties").entrySet().forEach(e->props.put(e.getKey(),e.getValue().getAsString()));
            String name=c.get("name").getAsString();BlockState state=KirisameShopPlan.state(new KirisameShopPlan.Cell(BlockPos.ZERO,name,props,"",List.of(),"",false));
            if(x>=23&&x<=31&&z>=21&&z<=30&&y>=-2&&y<=3)level.setBlock(origin.offset(x-23,y+2,z-21),state,3);
            if(!name.equals("omnira:crystal_ball"))continue;
            h.assertTrue(c.has("loot"),"Sample ball has no loot table");
            var p=h.absolutePos(new BlockPos(15+balls,5,15));level.setBlock(p,state,3);
            var ball=(CrystalBallBlockEntity)level.getBlockEntity(p);
            ball.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse(c.get("loot").getAsString())),91L);
            var saved=ball.saveWithFullMetadata(level.registryAccess());ball.loadWithComponents(saved,level.registryAccess());
            h.assertTrue(ball.hasPendingLoot()&&ball.getUpdateTag(level.registryAccess()).getBoolean("PendingLoot"),"Loot mist lost on reload");
            var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);ball.createMenu(1,player.getInventory(),player);
            h.assertTrue(!ball.hasPendingLoot()&&!ball.isEmpty(),"Loot must resolve only when opened");balls++;
        }
        h.assertTrue(balls==2,"Expected two treasure balls");
        h.runAfterDelay(120,()->{
            for(int x=25;x<=29;x++)for(int z=24;z<=28;z++)for(int y=-1;y<=0;y++)
                h.assertTrue(level.getFluidState(origin.offset(x-23,y+2,z-21)).isEmpty(),"Cellar reflooded");
            h.succeed();
        });
    }
}
