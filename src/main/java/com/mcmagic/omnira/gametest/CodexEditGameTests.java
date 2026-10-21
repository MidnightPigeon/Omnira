package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.edit.*;

import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Server engine regression tests, discovered with namespace omnira_codex_edit. */
@GameTestHolder("omnira_codex_edit")
@PrefixGameTestTemplate(false)
public final class CodexEditGameTests {
    @GameTest(template="spell_arena") public static void everyNativeEcologyBiomeUsesNativeTreasure(GameTestHelper h){
        var level=h.getLevel();var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        var regional=Map.of(
                "temporal_continent",Set.of("golden_throne_tower","kirisame_workshop","rusty_lake_mill_cellar",
                        "drilling_platform","looking_glass_garden_rabbit","doomsday_rite"),
                "dream_islands",Set.of("radiant_courtyard"));
        int count=0;
        for(var biome:registry.holders().toList()){
            var id=biome.key().location();if(!id.getNamespace().equals("omnira")||id.getPath().equals("spacetime_corridor"))continue;count++;
            var seen=new HashSet<String>();
            for(int i=0;i<256;i++){
                var table=EcologyLoot.table(level,biome);var location=table.location();
                h.assertTrue(location.getNamespace().equals("omnira"),"Native biome fell back to vanilla loot: "+id);
                h.assertTrue(level.getServer().reloadableRegistries().getLootTable(table)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                        "Missing ecology loot: "+table);
                String path=location.getPath().substring("chests/".length());seen.add(path);
                if(regional.containsKey(id.getPath()))h.assertTrue(regional.get(id.getPath()).contains(path),"Treasure crossed ecology regions: "+id);
                else h.assertTrue(Objects.equals(lootMappings().get(id.getPath()),path),"Exclusive biome treasure changed: "+id);
            }
            if(regional.containsKey(id.getPath()))h.assertTrue(seen.equals(regional.get(id.getPath())),"Regional treasure pool incomplete: "+id);
        }
        h.assertTrue(count==17,"Native ecology coverage changed; review treasure mappings");h.succeed();
    }

    @GameTest(template="spell_arena") public static void ecologyShellIsOnlyTheClosedExteriorBoundary(GameTestHelper h){
        boolean reduced=false;var level=h.getLevel();var center=prepare(h);var biome=level.getBiome(center);
        for(int diameter=7;diameter<=15;diameter+=2){
            int radius=(diameter-1)/2,actual=0,old=0;
            for(int x=-radius;x<=radius;x++)for(int y=-radius;y<=radius;y++)for(int z=-radius;z<=radius;z++){
                int d=x*x+y*y+z*z;if(d>radius*radius)continue;
                if(d>(radius-1)*(radius-1))old++;
                boolean exterior=false;
                for(var direction:Direction.values()){
                    int xx=x+direction.getStepX(),yy=y+direction.getStepY(),zz=z+direction.getStepZ();
                    if(xx*xx+yy*yy+zz*zz>radius*radius)exterior=true;
                }
                boolean shell=EditGeometry.ecologyShell(radius,x,y,z);
                h.assertTrue(shell==exterior,"Ecology shell is not the face-adjacent exterior boundary");
                if(shell){actual++;h.assertTrue(d>(radius-1)*(radius-1),"New shell occupies former landscape");}
                else for(var direction:Direction.values()){
                    int xx=x+direction.getStepX(),yy=y+direction.getStepY(),zz=z+direction.getStepZ();
                    h.assertTrue(xx*xx+yy*yy+zz*zz<=radius*radius,"Interior leaks through shell");
                }
            }
            h.assertTrue(actual<=old&&actual>0,"Invalid shell count");if(actual<old)reduced=true;
            var result=CodexEditEngine.begin(level,center,diameter,Operation.ECOLOGY,Direction.Axis.Y,false,
                    ItemStack.EMPTY,ItemStack.EMPTY,biome,null,EcologySampler.palette(level,diameter,biome));
            h.assertTrue(result.accepted(),"Ecology boundary plan failed");
            var changes=result.job().save(level.registryAccess()).getList("Changes",net.minecraft.nbt.Tag.TAG_COMPOUND);
            int planned=0,loot=0;
            for(int i=0;i<changes.size();i++){
                var change=changes.getCompound(i);
                if(change.contains("Loot"))loot++;
                if(change.getCompound("After").getString("Name").equals("omnira:spiritual_crystal_block"))planned++;
            }
            h.assertTrue(planned==actual&&loot==1,"Ecology plan did not retain its boundary/unique treasure");
        }
        h.assertTrue(reduced,"Large spheres retain redundant radial layers");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void geometryMasksCoverEveryPowerAxisAndHollow(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        for (int diameter=7;diameter<=15;diameter+=2) {
            int radius=(diameter-1)/2;
            for (var operation:List.of(Operation.SHAPE,Operation.INSCRIBED,Operation.PLANE))
                for (var axis:Direction.Axis.values()) for (boolean hollow:new boolean[]{false,true}) {
                    var result=CodexEditEngine.begin(level,center,diameter,operation,axis,hollow,
                            ItemStack.EMPTY,new ItemStack(Items.GOLD_BLOCK),null,null);
                    h.assertTrue(result.accepted(),"Geometry preflight failed: "+result.error());
                    var cells=result.job().save(level.registryAccess()).getList("Changes",net.minecraft.nbt.Tag.TAG_COMPOUND);
                    var actual=new HashSet<BlockPos>();
                    double previous=-1;
                    for (int i=0;i<cells.size();i++) {
                        var pos=BlockPos.of(cells.getCompound(i).getLong("Pos"));
                        h.assertTrue(actual.add(pos),"Duplicate selected voxel");
                        double distance=pos.distSqr(center);
                        h.assertTrue(distance>=previous,"Placement is not a radial wave"); previous=distance;
                    }
                    for (int x=-radius-1;x<=radius+1;x++) for (int y=-radius-1;y<=radius+1;y++)
                        for (int z=-radius-1;z<=radius+1;z++) {
                            boolean expected=mask(operation,radius,axis,hollow,x,y,z);
                            h.assertTrue(actual.contains(center.offset(x,y,z))==expected,
                                    "Mask mismatch: "+diameter+"/"+operation+"/"+axis+"/"+hollow+" at "+x+","+y+","+z);
                        }
                }
        }
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void hollowShapesLeaveEveryUnselectedBlockUntouched(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        for (int diameter=7;diameter<=15;diameter+=2) {
            int radius=(diameter-1)/2;
            for (var operation:List.of(Operation.SHAPE,Operation.INSCRIBED,Operation.PLANE))
                for (var axis:operation==Operation.PLANE?Direction.Axis.values():new Direction.Axis[]{Direction.Axis.Y}) {
                    fill(h,center,radius+1,Blocks.OBSIDIAN.defaultBlockState());
                    var result=CodexEditEngine.begin(level,center,diameter,operation,axis,true,
                            ItemStack.EMPTY,new ItemStack(Items.GOLD_BLOCK),null,null);
                    h.assertTrue(result.accepted(),"Hollow preflight failed");
                    finish(h,result.job());
                    for (int x=-radius-1;x<=radius+1;x++) for (int y=-radius-1;y<=radius+1;y++)
                        for (int z=-radius-1;z<=radius+1;z++) {
                            var expected=mask(operation,radius,axis,true,x,y,z)?Blocks.GOLD_BLOCK:Blocks.OBSIDIAN;
                            h.assertTrue(level.getBlockState(center.offset(x,y,z)).is(expected),"Hollow touched wrong voxel");
                        }
                }
        }
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void matterOverwritesBedrockAndIncompleteContainers(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        fill(h,center,4,Blocks.OBSIDIAN.defaultBlockState());
        for (var offset:List.of(BlockPos.ZERO,new BlockPos(1,0,0),new BlockPos(3,3,3)))
            level.setBlock(center.offset(offset),Blocks.BEDROCK.defaultBlockState(),18);
        var result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.X,true,
                new ItemStack(Items.BEDROCK),new ItemStack(Items.GOLD_BLOCK),null,null);
        h.assertTrue(result.accepted(),"Bedrock rejected"); finish(h,result.job());
        h.assertTrue(level.getBlockState(center).is(Blocks.GOLD_BLOCK) &&
                level.getBlockState(center.east()).is(Blocks.GOLD_BLOCK),"Bedrock protected from replacement");
        h.assertTrue(level.getBlockState(center.offset(3,3,3)).is(Blocks.BEDROCK),"Matter escaped sphere");
        level.setBlock(center,Blocks.CHEST.defaultBlockState(),18);
        level.removeBlockEntity(center);
        result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.CHEST),new ItemStack(Items.STONE),null,null);
        h.assertTrue(result.accepted(),"Incomplete block entity rejected"); finish(h,result.job());
        h.assertTrue(level.getBlockState(center).is(Blocks.STONE) && level.getBlockEntity(center)==null,"Incomplete container survived");
        h.succeed();
    }

    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void savedManagerContinuesAfterActualSourceCodexIsOverwritten(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var codex=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(ResourceLocation.parse("omnira:myriad_codex"));
        h.assertTrue(codex!=Blocks.AIR,"Source Codex is not registered");
        level.setBlock(center,codex.defaultBlockState(),18);
        h.assertTrue(level.getBlockEntity(center)!=null,"Source Codex has no entity");
        var manager=CodexEdits.get(level);
        var submitted=manager.submit(center,15,Operation.SHAPE,Direction.Axis.Y,false,
                ItemStack.EMPTY,new ItemStack(Items.STONE),null,null);
        h.assertTrue(submitted.accepted(),"Managed job rejected: "+submitted.error());
        h.runAfterDelay(2,()->{
            h.assertTrue(level.getBlockState(center).is(Blocks.STONE) && level.getBlockEntity(center)==null,"Origin not overwritten");
            h.assertTrue(manager.active(submitted.jobId()),"Job died with source");
            var saved=manager.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess()).getList("Jobs",net.minecraft.nbt.Tag.TAG_COMPOUND);
            boolean found=false;
            for (int i=0;i<saved.size();i++) if (saved.getCompound(i).getUUID("Id").equals(submitted.jobId())) {
                var restored=EditJob.load(saved.getCompound(i),level.registryAccess());
                h.assertTrue(restored.active() && restored.cursor()>0,"Manager did not save live cursor"); found=true;
            }
            h.assertTrue(found,"Source-independent SavedData lost job");
            h.succeedWhen(()->{
                h.assertTrue(!manager.active(submitted.jobId()),"Managed job still executing");
                h.assertTrue(manager.progress(level,submitted.jobId()).orElseThrow().status()==EditJob.Status.COMPLETE,"Managed job failed");
                h.assertTrue(level.getBlockState(center.offset(7,0,0)).is(Blocks.STONE),"Managed job failed to finish edge");
            });
        });
    }

    @GameTest(template="spell_arena")
    public static void realLoadedEcologyPreservesBiomeAndResumesAcrossEveryPhase(GameTestHelper h) {
        var level=h.getLevel(); prepare(h);
        var source=h.absolutePos(new BlockPos(5,8,5)); var center=h.absolutePos(new BlockPos(16,14,16));
        for (int x=-3;x<=3;x++) for (int y=-3;y<=3;y++) for (int z=-3;z<=3;z++)
            level.setBlock(source.offset(x,y,z),(y<-1?Blocks.STONE:y==-1?Blocks.DIRT:y==0?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),18);
        level.setBlock(source.offset(1,1,0),Blocks.DANDELION.defaultBlockState(),18);
        level.setBlock(source.offset(-1,1,0),Blocks.CHEST.defaultBlockState(),18);
        var biome=level.getBiome(source);
        var sample=EcologySampler.capture(level,source,7,biome).orElseThrow(()->new AssertionError("Loaded sample rejected"));
        h.assertTrue(sample.at(1,1,0).is(Blocks.DANDELION) && sample.at(-1,1,0).isAir(),"Sample copied wrong landscape or container");
        fill(h,center,4,Blocks.BEDROCK.defaultBlockState());
        var biomes=new HashMap<BlockPos,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>>();
        for (int x=-4;x<=4;x++) for (int y=-4;y<=4;y++) for (int z=-4;z<=4;z++) {
            var pos=center.offset(x,y,z); biomes.put(pos,level.getBiome(pos));
        }
        var result=CodexEditEngine.begin(level,center,7,Operation.ECOLOGY,Direction.Axis.Y,false,
                ItemStack.EMPTY,ItemStack.EMPTY,biome,null,sample);
        h.assertTrue(result.accepted(),"Real ecology plan rejected");
        var job=result.job(); var seen=EnumSet.of(job.status()); int steps=0;
        while (job.active() && steps++<2000) {
            job.advance(level,1);
            if (seen.add(job.status())) {
                var saved=job.save(level.registryAccess()); job=EditJob.load(saved,level.registryAccess());
                h.assertTrue(job.save(level.registryAccess()).equals(saved),"Phase checkpoint changed snapshot");
            }
        }
        h.assertTrue(job.status()==EditJob.Status.COMPLETE && seen.containsAll(EnumSet.of(EditJob.Status.CLEARING,
                EditJob.Status.PLACING,EditJob.Status.UPDATING,EditJob.Status.COMPLETE)),"Not all ecology phases resumed");
        int shell=0,balls=0;
        for (int x=-4;x<=4;x++) for (int y=-4;y<=4;y++) for (int z=-4;z<=4;z++) {
            var pos=center.offset(x,y,z); var state=level.getBlockState(pos); int distance=x*x+y*y+z*z;
            if (distance>9) h.assertTrue(state.is(Blocks.BEDROCK),"Ecology modified outside sphere");
            else if (distance>4) {
                h.assertTrue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock())
                        .equals(ResourceLocation.parse("omnira:spiritual_crystal_block")),"Shell hole or wrong material"); shell++;
            }
            if (net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(ResourceLocation.parse("omnira:crystal_ball"))) balls++;
            h.assertTrue(level.getBiome(pos).equals(biomes.get(pos)),"Ecology mutated biome data");
        }
        h.assertTrue(shell==90 && balls==1,"Wrong actual shell/treasure count");
        h.assertTrue(level.getBlockState(center.offset(1,-1,0)).is(Blocks.GRASS_BLOCK) &&
                level.getBlockState(center.offset(1,0,0)).is(Blocks.DANDELION),"Sample not reproduced at lower-third surface");
        var ball=level.getBlockEntity(center);
        h.assertTrue(ball instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity,"Missing central loot ball");
        var loot=ball.saveWithFullMetadata(level.registryAccess());
        h.assertTrue(loot.getString("LootTable").equals(EcologyLoot.table(level,biome).location().toString()),"Wrong biome/dimension loot table");
        var restored=EditJob.load(job.save(level.registryAccess()),level.registryAccess()); restored.advance(level,256);
        h.assertTrue(ball.saveWithFullMetadata(level.registryAccess()).equals(loot),"Completed resume rerolled treasure");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void mixedDropStacksKeepComponentsCountsAndCollapsePerVoxel(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        addDrop(h,center,new ItemStack(Items.APPLE,53)); addDrop(h,center.east(),new ItemStack(Items.APPLE,19));
        var untouched=addDrop(h,center.offset(2,0,0),new ItemStack(Items.STICK,11));
        var outside=addDrop(h,center.offset(4,0,0),new ItemStack(Items.APPLE,5));
        var target=new ItemStack(Items.ENDER_PEARL);
        target.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Preserved sample"));
        var result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.APPLE),target,null,null);
        var job=result.job(); job.advance(level,2); job=EditJob.load(job.save(level.registryAccess()),level.registryAccess()); finish(h,job);
        var outputs=level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(3),drop->drop.getItem().is(Items.ENDER_PEARL));
        h.assertTrue(outputs.stream().mapToInt(drop->drop.getItem().getCount()).sum()==72,"Mixed drop totals changed");
        h.assertTrue(outputs.stream().allMatch(drop->drop.getItem().getCount()<=16 &&
                drop.getItem().getHoverName().getString().equals("Preserved sample")),"Wrong split/components");
        result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                target,new ItemStack(Items.DIAMOND_BLOCK),null,null); finish(h,result.job());
        h.assertTrue(level.getBlockState(center).is(Blocks.DIAMOND_BLOCK) && level.getBlockState(center.east()).is(Blocks.DIAMOND_BLOCK),"Drop-to-block missing voxel");
        h.assertTrue(outputs.stream().noneMatch(ItemEntity::isAlive),"Matched drop stacks remain");
        h.assertTrue(untouched.isAlive() && untouched.getItem().getCount()==11 && outside.isAlive() && outside.getItem().getCount()==5,
                "Changed unmatched or out-of-region drop");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void itemShapeDropsOneComponentPreservingItemPerRingVoxel(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h); fill(h,center,4,Blocks.OBSIDIAN.defaultBlockState());
        var target=new ItemStack(Items.APPLE,32);
        target.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Ring sample"));
        var result=CodexEditEngine.begin(level,center,7,Operation.PLANE,Direction.Axis.Z,true,ItemStack.EMPTY,target,null,null);
        finish(h,result.job());
        var drops=level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(4),drop->drop.getItem().is(Items.APPLE));
        h.assertTrue(drops.size()==24,"Wrong ring drop count");
        for (var drop:drops) h.assertTrue(drop.getItem().getCount()==1 && drop.getItem().getHoverName().getString().equals("Ring sample"),"Wrong voxel item/components");
        for (int x=-4;x<=4;x++) for (int y=-4;y<=4;y++) for (int z=-4;z<=4;z++)
            h.assertTrue(mask(Operation.PLANE,3,Direction.Axis.Z,true,x,y,z)?level.getBlockState(center.offset(x,y,z)).isAir():
                    level.getBlockState(center.offset(x,y,z)).is(Blocks.OBSIDIAN),"Item shape touched wrong voxel");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void snapshotResumesAfterSourceRemoval(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var outside=level.getBlockState(center.offset(3,3,3));
        level.setBlock(center,Blocks.BEDROCK.defaultBlockState(),2);
        var result=CodexEditEngine.begin(level,center,7,Operation.SHAPE,Direction.Axis.Y,false,
                ItemStack.EMPTY,new ItemStack(Items.STONE),null,null);
        h.assertTrue(result.accepted(),"Plan rejected");
        var job=result.job();
        h.assertTrue(job.advance(level,1)==1,"Budget exceeded");
        level.setBlock(center,Blocks.AIR.defaultBlockState(),2);
        var restored=EditJob.load(job.save(level.registryAccess()),level.registryAccess());
        h.assertTrue(restored.id().equals(job.id()) && restored.cursor()==job.cursor(),"Lost durable progress");
        int steps=0;
        while (restored.active() && steps++<100) h.assertTrue(restored.advance(level,16)<=16,"Action budget exceeded");
        h.assertTrue(!restored.active(),"Origin removal aborted job");
        h.assertTrue(level.getBlockState(center.offset(3,0,0)).is(Blocks.STONE),"Sphere missing edge");
        h.assertTrue(level.getBlockState(center.offset(3,3,3))==outside,"Changed outside sphere");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void matterDropsKeepCountsAndComponents(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var entity=new ItemEntity(level,center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.APPLE,13));
        level.addFreshEntity(entity);
        var target=new ItemStack(Items.STICK);
        target.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Codex sample"));
        var result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.APPLE),target,null,null);
        var job=result.job();
        while (job.active()) job.advance(level,32);
        h.assertTrue(entity.getItem().is(Items.STICK) && entity.getItem().getCount()==13,"Changed source count");
        h.assertTrue(entity.getItem().getHoverName().getString().equals("Codex sample"),"Lost exemplar components");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void pickedUpDropCannotCreateReplacement(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var entity=new ItemEntity(level,center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.APPLE,7));
        level.addFreshEntity(entity);
        var result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.APPLE),new ItemStack(Items.DIAMOND_BLOCK),null,null);
        entity.discard();
        var job=result.job(); while (job.active()) job.advance(level,32);
        h.assertTrue(!level.getBlockState(center).is(Blocks.DIAMOND_BLOCK),"Duplicated picked-up input");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void splitItemStacksResumeWithoutLosingCount(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var entity=new ItemEntity(level,center.getX()+0.5,center.getY()+0.5,center.getZ()+0.5,new ItemStack(Items.APPLE,64));
        level.addFreshEntity(entity);
        var result=CodexEditEngine.begin(level,center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.APPLE),new ItemStack(Items.IRON_SWORD),null,null);
        var job=result.job();
        h.assertTrue(job.advance(level,3)==3,"Split ignored action budget");
        job=EditJob.load(job.save(level.registryAccess()),level.registryAccess());
        while (job.active()) job.advance(level,8);
        var outputs=level.getEntitiesOfClass(ItemEntity.class,new net.minecraft.world.phys.AABB(center).inflate(1),
                drop->drop.getItem().is(Items.IRON_SWORD));
        h.assertTrue(outputs.stream().mapToInt(drop->drop.getItem().getCount()).sum()==64,"Lost split count");
        h.assertTrue(outputs.stream().allMatch(drop->drop.getItem().getCount()<=drop.getItem().getMaxStackSize()),"Overstacked output");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void ecologySnapshotHasOneTreasureAndThinShell(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var biome=level.getBiome(center);
        var sample=EcologySampler.palette(level,7,biome);
        var result=CodexEditEngine.begin(level,center,7,Operation.ECOLOGY,Direction.Axis.Y,false,
                ItemStack.EMPTY,ItemStack.EMPTY,biome,null,sample);
        h.assertTrue(result.accepted(),"Ecology rejected cached sample");
        var tag=result.job().save(level.registryAccess());
        var cells=tag.getList("Changes",net.minecraft.nbt.Tag.TAG_COMPOUND);
        int loot=0,shell=0;
        for (int i=0;i<cells.size();i++) {
            var cell=cells.getCompound(i);
            if (cell.contains("Loot")) loot++;
            if (cell.getCompound("After").getString("Name").equals("omnira:spiritual_crystal_block")) shell++;
        }
        h.assertTrue(loot==1 && shell==90,"Wrong treasure count or shell thickness");
        var restored=EditJob.load(tag,level.registryAccess());
        h.assertTrue(restored.save(level.registryAccess()).equals(tag),"Snapshot not stable");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void uniformPoolUsesAllRegisteredBiomesAndMultipartIsAccepted(GameTestHelper h) {
        var level=h.getLevel(); var center=prepare(h);
        var registry=level.registryAccess().registryOrThrow(Registries.BIOME);
        h.assertTrue(EcologySelection.candidates(registry,EcologySelection.Group.OMNI).size()==registry.size()-1,"OMNI must exclude only the spacetime corridor");
        var corridor=registry.getHolder(ResourceLocation.parse("omnira:spacetime_corridor")).orElseThrow();
        for(var group:EcologySelection.Group.values())h.assertTrue(!EcologySelection.matches(corridor,group),"Excluded corridor entered "+group);
        for (var group:EcologySelection.Group.values()) for (var holder:EcologySelection.candidates(registry,group))
            h.assertTrue(registry.getHolder(holder.key()).orElseThrow()==holder,"Fabricated biome holder");
        for (String id:List.of("mirror_dream_border","void_starsea","reversion_mire"))
            h.assertTrue(registry.containsKey(ResourceLocation.parse("omnira:"+id)),"Wrong native biome ID");
        for (var entry:lootMappings().entrySet()) {
            h.assertTrue(registry.containsKey(ResourceLocation.parse("omnira:"+entry.getKey())),"Mapped biome not registered");
            var table=net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                    ResourceLocation.parse("omnira:chests/"+entry.getValue()));
            h.assertTrue(level.getServer().reloadableRegistries().getLootTable(table)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,
                    "Mapped treasure table missing: "+entry.getValue());
        }
        var result=CodexEditEngine.begin(level,center,7,Operation.SHAPE,Direction.Axis.Y,true,
                ItemStack.EMPTY,new ItemStack(Items.OAK_DOOR),null,null);
        h.assertTrue(result.accepted(),"Incomplete target rejected");
        var codex=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:myriad_codex"));
        h.assertTrue(!CodexEditEngine.begin(level,center,7,Operation.SHAPE,Direction.Axis.Y,false,
                ItemStack.EMPTY,new ItemStack(codex),null,null).accepted(),"Codex target exclusion missing");
        var heart=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:myriad_heart"));
        for (var operation:Operation.values()) {
            var rejected=CodexEditEngine.begin(level,center,7,operation,Direction.Axis.Y,false,
                    new ItemStack(Items.STONE),new ItemStack(heart),level.getBiome(center),null);
            h.assertTrue(!rejected.accepted(),"Temporary heart accepted as target");
        }
        h.succeed();
    }

    @GameTest(template="spell_arena",timeoutTicks=200)
    public static void emptyFormClearsOnlySelectedShape(GameTestHelper h) {
        var center=prepare(h);
        for(var op:List.of(Operation.SHAPE,Operation.INSCRIBED,Operation.PLANE))
            for(var axis:Direction.Axis.values()) for(boolean hollow:new boolean[]{false,true}) {
                fill(h,center,3,Blocks.BEDROCK.defaultBlockState());
                var result=CodexEditEngine.begin(h.getLevel(),center,7,op,axis,hollow,
                        ItemStack.EMPTY,ItemStack.EMPTY,null,null);
                h.assertTrue(result.accepted(),"Empty form rejected");
                finish(h,EditJob.load(result.job().save(h.getLevel().registryAccess()),h.getLevel().registryAccess()));
                for(int x=-3;x<=3;x++) for(int y=-3;y<=3;y++) for(int z=-3;z<=3;z++) {
                    var state=h.getLevel().getBlockState(center.offset(x,y,z));
                    h.assertTrue(mask(op,3,axis,hollow,x,y,z)?state.isAir():state.is(Blocks.BEDROCK),
                            "Empty form cleared the wrong shape: "+op+"/"+axis+"/"+hollow);
                }
            }
        h.assertTrue(CodexEditEngine.begin(h.getLevel(),center,7,Operation.MATTER,Direction.Axis.Y,false,
                new ItemStack(Items.STONE),ItemStack.EMPTY,null,null).accepted(),"Matter rejected air as its target");
        h.succeed();
    }

    private static BlockPos prepare(GameTestHelper h) {
        var level=h.getLevel(); var center=h.absolutePos(new BlockPos(12,12,12));
        for (int x=(center.getX()-12)>>4;x<=((center.getX()+12)>>4);x++)
            for (int z=(center.getZ()-12)>>4;z<=((center.getZ()+12)>>4);z++) level.getChunk(x,z);
        return center;
    }

    private static boolean mask(Operation operation,int radius,Direction.Axis axis,boolean hollow,int x,int y,int z) {
        int distance=x*x+y*y+z*z;
        if (operation==Operation.INSCRIBED) {
            int half=(int)Math.floor(radius/Math.sqrt(3)); int edge=Math.max(Math.abs(x),Math.max(Math.abs(y),Math.abs(z)));
            return edge<=half && (!hollow || edge==half);
        }
        if (operation==Operation.PLANE) {
            int normal=axis==Direction.Axis.X?x:axis==Direction.Axis.Y?y:z;
            int planar=distance-normal*normal;
            return normal==0 && planar<=radius*radius && (!hollow || planar>(radius-2)*(radius-2));
        }
        return distance<=radius*radius && (!hollow || distance>(radius-1)*(radius-1));
    }

    private static void fill(GameTestHelper h,BlockPos center,int radius,net.minecraft.world.level.block.state.BlockState state) {
        for (int x=-radius;x<=radius;x++) for (int y=-radius;y<=radius;y++) for (int z=-radius;z<=radius;z++)
            h.getLevel().setBlock(center.offset(x,y,z),state,18);
    }

    private static EditJob finish(GameTestHelper h,EditJob job) {
        int steps=0;
        while (job.active() && steps++<20000) h.assertTrue(job.advance(h.getLevel(),256)<=256,"Exceeded explicit action budget");
        h.assertTrue(job.status()==EditJob.Status.COMPLETE,"Job did not complete: "+job.error());
        return job;
    }

    private static ItemEntity addDrop(GameTestHelper h,BlockPos pos,ItemStack stack) {
        var entity=new ItemEntity(h.getLevel(),pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,stack,0,0,0);
        h.assertTrue(h.getLevel().addFreshEntity(entity),"Failed to create source drop"); return entity;
    }

    @SuppressWarnings("unchecked")
    private static Map<String,String> lootMappings() {
        try {
            var method=EcologyLoot.class.getDeclaredMethod("defaults");
            method.setAccessible(true);
            return (Map<String,String>)method.invoke(null);
        } catch (ReflectiveOperationException exception) { throw new AssertionError(exception); }
    }
    private CodexEditGameTests() {}
}
