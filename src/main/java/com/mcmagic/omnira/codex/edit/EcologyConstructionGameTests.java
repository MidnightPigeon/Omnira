package com.mcmagic.omnira.codex.edit;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_codex_ecology")
@PrefixGameTestTemplate(false)
public final class EcologyConstructionGameTests {
    @GameTest(template="spell_arena") public static void unmappedTreasurePrefersItsNativeDimension(GameTestHelper h){
        var level=h.getLevel();var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var time=registry.getHolder(ResourceLocation.parse("omnira:stilled_wastes")).orElseThrow();
        var space=registry.getHolder(ResourceLocation.parse("omnira:folded_caverns")).orElseThrow();
        var pool=EcologyLoot.dimensionTables(level,ResourceLocation.parse("othermod:realm"),List.of(time,space,time));
        h.assertTrue(pool.size()==2&&pool.stream().map(table->table.location().toString()).collect(java.util.stream.Collectors.toSet())
                .equals(Set.of("omnira:chests/golden_throne_tower","omnira:chests/fold_observatory_lower")),
                "Dimension fallback lost native mapped treasures, duplicated entries or used destination loot");
        var namespaced=EcologyLoot.dimensionTables(level,ResourceLocation.parse("omnira:test_realm"),List.of());
        h.assertTrue(!namespaced.isEmpty()&&namespaced.stream().allMatch(table->table.location().getNamespace().equals("omnira")
                &&table.location().getPath().startsWith("chests/")),"Independent dimension fallback mixed namespaces or non-chest loot");
        h.assertTrue(EcologyLoot.dimensionTables(level,ResourceLocation.parse("missingmod:realm"),List.of()).isEmpty(),
                "Missing native treasure silently used destination-world loot");
        var overworld=EcologyLoot.dimensionTables(level,net.minecraft.world.level.Level.OVERWORLD.location(),List.of());
        h.assertTrue(overworld.size()>1&&overworld.stream().noneMatch(table->table.location().getPath().contains("bastion_")
                ||table.location().getPath().endsWith("nether_bridge")||table.location().getPath().endsWith("end_city_treasure")),
                "Unmapped Overworld biome lost its regional pool or mixed in Nether/End loot");
        var end=EcologyLoot.dimensionTables(level,net.minecraft.world.level.Level.END.location(),List.of());
        h.assertTrue(end.size()==1&&end.getFirst().location().toString().equals("minecraft:chests/end_city_treasure"),
                "Unmapped End biome used Overworld loot");h.succeed();
    }

    @GameTest(template="spell_arena") public static void everyBiomeConstructsAllFiveSizesWithoutTerrain(GameTestHelper h) {
        var level=h.getLevel();
        for (var biome:level.registryAccess().registryOrThrow(Registries.BIOME).holders().toList()) {
            for (int diameter=7;diameter<=15;diameter+=2) {
                var a=EcologyLandscape.build(level,diameter,biome,71);
                var b=EcologyLandscape.build(level,diameter,biome,71);
                h.assertTrue(a.states().equals(b.states()),"Construction is not seed deterministic: "+biome.key());
                h.assertTrue(a.biomeId().equals(biome.key().location()),"Construction rerolled its biome");
                h.assertTrue(a.states().size()==diameter*diameter*diameter,"Wrong detached volume");
                h.assertTrue(a.states().stream().noneMatch(s->s.hasBlockEntity()||CodexEditEngine.forbidden(s)),"Unsafe decoration");
                h.assertTrue(a.states().stream().noneMatch(s->s.is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())),"Restricted solidified spacetime remained");
                h.assertTrue(!a.at(0,0,0).isAir()&&a.at(0,1,0).isAir(),"Missing terrain or blocked treasure");
                h.assertTrue(a.surface().equals(BlockPos.ZERO),"Construction depends on a native chunk");
            }
        }
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void liquidsFillABasinAndSpaceHasItsOwnMaterials(GameTestHelper h) {
        var registry=h.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        for (String name:List.of("omnira:void_starsea","omnira:reversion_mire","minecraft:ocean")) {
            var biome=registry.getHolder(ResourceLocation.parse(name)).orElseThrow();
            var sample=EcologyLandscape.build(h.getLevel(),15,biome,19);
            h.assertTrue(!sample.at(0,0,0).getFluidState().isEmpty(),"Missing liquid surface: "+name);
            h.assertTrue(sample.at(0,-1,0).getFluidState().getType()==sample.at(0,0,0).getFluidState().getType(),"Liquid is only a surface film");
            h.assertTrue(sample.at(0,-2,0).getFluidState().isEmpty(),"Missing basin floor");
        }
        for (var biome:EcologySelection.candidates(registry,EcologySelection.Group.SPACE)) {
            var sample=EcologyLandscape.build(h.getLevel(),15,biome,19);
            h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(sample.at(0,0,0).getBlock())
                    .getNamespace().equals("omnira"),"Space landscape fell back to ordinary grass");
            boolean decorated=false;
            for(int x=-7;x<=7;x++)for(int y=1;y<=7;y++)for(int z=-7;z<=7;z++)
                if(!sample.at(x,y,z).isAir())decorated=true;
            h.assertTrue(decorated,"Space decoration missing");
        }
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void pendingOldSearchResumesAsOneConstruction(GameTestHelper h) {
        var level=h.getLevel();var owner=UUID.randomUUID();var id=UUID.randomUUID();
        var request=new EcologyRequest(id,BlockPos.ZERO,15,ResourceLocation.parse("omnira:phase_crystal_grove"),owner,123);
        var old=request.save();old.putLong("Checks",65_535);old.putString("Notice","sampling_retry:old");
        request=EcologyRequest.load(old,level);
        int[] dirty={0};request.construct(level,()->dirty[0]++);
        h.assertTrue(request.error.isEmpty()&&request.sample!=null,"Old sampling request did not migrate");
        h.assertTrue(request.id.equals(id)&&request.owner.equals(owner)&&request.seed==123,"Migration lost job identity");
        var saved=request.save();var loaded=EcologyRequest.load(saved,level);
        loaded.construct(level,()->dirty[0]++);
        h.assertTrue(dirty[0]==1&&loaded.sample.states().equals(request.sample.states()),"Saved sample regenerated or duplicated");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void removedBiomeFailsRatherThanRerolling(GameTestHelper h) {
        var request=new EcologyRequest(UUID.randomUUID(),BlockPos.ZERO,15,ResourceLocation.parse("omnira:missing_biome"),null,12);
        request.construct(h.getLevel(),()->{});
        h.assertTrue(request.sample==null&&request.error.equals("selected_biome_removed"),"Removed biome was silently replaced");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void terrainThenDecorationsThenUniqueTreasurePersist(GameTestHelper h) {
        var level=h.getLevel();var center=h.absolutePos(new BlockPos(8,8,8));
        for(int x=(center.getX()-8)>>4;x<=((center.getX()+8)>>4);x++)
            for(int z=(center.getZ()-8)>>4;z<=((center.getZ()+8)>>4);z++) level.getChunk(x,z);
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolder(ResourceLocation.parse("omnira:phase_crystal_grove")).orElseThrow();
        for(int diameter=7;diameter<=15;diameter+=2) {
            var sample=EcologyLandscape.build(level,diameter,biome,37);
            var result=CodexEditEngine.begin(level,center,diameter,Operation.ECOLOGY,Direction.Axis.Y,false,
                    ItemStack.EMPTY,ItemStack.EMPTY,biome,null,sample);
            h.assertTrue(result.accepted(),"Construction planning failed: "+result.error());
            var tag=result.job().save(level.registryAccess());var cells=tag.getList("Changes",Tag.TAG_COMPOUND);
            int treasure=0,phase=0,ground=-(diameter-1)/2+diameter/3;
            for(int i=0;i<cells.size();i++) {
                var cell=cells.getCompound(i);int next=cell.contains("Loot")?2:BlockPos.of(cell.getLong("Pos")).getY()<=center.getY()+ground?0:1;
                h.assertTrue(next>=phase,"Landscape placed before ground or treasure was not last");phase=next;
                if(cell.contains("Loot"))treasure++;
            }
            h.assertTrue(treasure==1&&cells.getCompound(cells.size()-1).contains("Loot"),"Treasure is not unique/last");
            h.assertTrue(EditJob.load(tag,level.registryAccess()).save(level.registryAccess()).equals(tag),"Constructed job is not resumable");
        }
        h.succeed();
    }
}
