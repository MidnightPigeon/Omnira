package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.shop.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.StrataBiomeSource;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_kirisame")
@PrefixGameTestTemplate(false)
public final class KirisameGameTests {
    @GameTest(template="spell_arena",templateNamespace="omnira_landscape")
    public static void boundedBiomeRegions(GameTestHelper h){
        int size=StrataBiomeSource.CELL_SIZE;h.assertTrue(size==256,"Unexpected biome size");
        int[] counts=new int[6];
        for(int x=-30;x<30;x++)for(int z=-30;z<30;z++){
            int own=StrataBiomeSource.region(x*size,z*size);counts[own]++;
            h.assertTrue(own==StrataBiomeSource.region(x*size,z*size),"Unstable region sample");
        }
        for(int count:counts)h.assertTrue(count>450&&count<750,"Six planned shares drifted: "+count);
        int irregular=0;
        for(int x=-20;x<20;x++)for(int z=-20;z<20;z++)if(StrataBiomeSource.region(x*size,z*size)!=StrataBiomeSource.region(x*size+size-1,z*size+size-1))irregular++;
        h.assertTrue(irregular>800,"Biome boundaries still follow square tiles");h.succeed();
    }
    @GameTest(template="spell_arena",templateNamespace="omnira_kirisame_blueprint",timeoutTicks=100)
    public static void blueprintStatesAndStairJoins(GameTestHelper h){
        Map<BlockPos,BlockState> states=new HashMap<>();int traders=0,loot=0;
        for(var c:KirisameShopPlan.cells()){
            var p=c.pos();h.assertTrue(p.getX()>=0&&p.getX()<31&&p.getY()>=0&&p.getY()<22&&p.getZ()>=0&&p.getZ()<29,"Out of bounds");
            h.assertTrue(states.put(p,KirisameShopPlan.state(c))==null,"Duplicate blueprint cell");
            if(c.name().equals("omnira:marisa_crystal_ball"))traders++;
            if(!c.loot().isEmpty()){
                loot++;var key=net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse(c.loot()));
                h.assertTrue(h.getLevel().getServer().reloadableRegistries().getLootTable(key)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"Missing loot table");
            }
        }
        h.assertTrue(traders==1&&loot==2,"Wrong merchant/treasure count");
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(5,4,5));int stairs=0;
        for(var entry:states.entrySet())if(entry.getValue().getBlock() instanceof StairBlock){
            var s=entry.getValue();
            level.setBlock(at,s,2);
            for(var d:Direction.values())level.setBlock(at.relative(d),states.getOrDefault(entry.getKey().relative(d),Blocks.AIR.defaultBlockState()),2);
            var updated=s.updateShape(Direction.NORTH,level.getBlockState(at.north()),level,at,at.north());
            h.assertTrue(s.getValue(StairBlock.SHAPE)==updated.getValue(StairBlock.SHAPE),"Authored stair shape differs from vanilla: "+entry.getKey());stairs++;
        }
        h.assertTrue(stairs>300,"Roof stairs missing");
        h.assertTrue(states.values().stream().filter(s->s.is(Blocks.LADDER)).count()>=6,"Attic ladder missing");
        h.assertTrue(states.values().stream().anyMatch(s->s.is(TimeNatureContent.DOOR.get())),"Authored time door missing");
        int terrain=0,liquid=0;
        for(var c:KirisameShopPlan.cells()){
            h.assertTrue(!Set.of("minecraft:dirt","minecraft:grass_block").contains(c.name()),"Soil baked into template");
            if(c.terrain()){terrain++;h.assertTrue(c.name().equals("minecraft:air"),"Terrain placeholder is not air");}
            var tag=KirisameShopPlan.blockEntity(c,at,123);
            if(c.name().equals("omnira:liquid_crystal_ball")){liquid++;h.assertTrue(tag.getBoolean("FluidTreasure"),"Liquid treasure missing");}
            if(c.name().equals("omnira:spacetime_waymark")&&tag!=null)h.assertTrue(!tag.hasUUID("Id"),"Cloned waymark identity");
        }
        h.assertTrue(terrain==483&&liquid==1,"Authored terrain/liquid count changed");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void liquidTreasureOpensOnce(GameTestHelper h){
        var level=h.getLevel();var at=h.absolutePos(new BlockPos(5,4,5));
        level.setBlockAndUpdate(at,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var ball=(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity)level.getBlockEntity(at);
        var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("FluidTreasure",true);ball.loadWithComponents(tag,level.registryAccess());
        h.assertTrue(ball.hasPendingLoot()&&ball.tank.isEmpty(),"Treasure opened early");
        ball.unpackLootTable(null);h.assertTrue(ball.hasPendingLoot(),"Automation opened treasure");
        h.assertTrue(ball.fluidHandler.fill(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)==0,"Automation filled sealed treasure");
        ball.unpackLootTable(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
        var fluid=ball.tank.getFluid().copy();
        h.assertTrue(!ball.hasPendingLoot()&&fluid.getAmount()==ball.tank.getCapacity(),"Not full after opening");
        h.assertTrue(com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity.treasureFluids().contains(fluid.getFluid()),"Ineligible fluid");
        ball.unpackLootTable(h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));h.assertTrue(net.neoforged.neoforge.fluids.FluidStack.matches(fluid,ball.tank.getFluid()),"Treasure rerolled");
        var saved=ball.saveWithFullMetadata(level.registryAccess());ball.loadWithComponents(saved,level.registryAccess());
        h.assertTrue(!ball.hasPendingLoot()&&net.neoforged.neoforge.fluids.FluidStack.matches(fluid,ball.tank.getFluid()),"Fluid save lost");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void persistentPlantRenderAnchors(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));
        for(var block:new Block[]{TimeNatureContent.GRASS.get(),TimeNatureContent.FLOWER.get()}){
            level.setBlockAndUpdate(p.below(),TimeNatureContent.SILT.get().defaultBlockState());level.setBlockAndUpdate(p,block.defaultBlockState());
            h.assertTrue(level.getBlockEntity(p) instanceof com.mcmagic.omnira.time.TimePlantBlockEntity,"Plant animation anchor missing");
            h.assertTrue(level.getBlockState(p).getOffset(level,p).lengthSqr()==0,"Model and orbit anchors differ");
        }
        h.succeed();
    }
}
