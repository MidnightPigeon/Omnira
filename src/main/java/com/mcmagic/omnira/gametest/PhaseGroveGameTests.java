package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.phase.*;
import com.mcmagic.omnira.spacetime.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.alchemy.catalysis.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_phase") @PrefixGameTestTemplate(false)
public final class PhaseGroveGameTests {
    @GameTest(template="spell_arena") public static void physicalMotesStayBounded(GameTestHelper h){
        h.assertTrue(PhaseMoteLayout.CACHE_LIMIT==512&&PhaseMoteLayout.VISIBLE_LIMIT==128&&PhaseMoteLayout.RANGE==32,"Decoration has no fixed budget");
        for(int tick=0;tick<400;tick++)for(int i=0;i<3;i++){
            var inside=PhaseMoteLayout.sample(false,0,tick,i);float r=PhaseMoteLayout.RADIUS;
            h.assertTrue(inside.x()-r>0&&inside.x()+r<1&&inside.y()-r>0&&inside.y()+r<1&&inside.z()-r>0&&inside.z()+r<1,"Mote escapes crystal interior");
            var orbit=PhaseMoteLayout.sample(true,0,tick,i);
            double dx=orbit.x()-.5,dz=orbit.z()-.5;
            h.assertTrue(Math.abs(dx*dx+dz*dz-.49*.49)<.00001,"Cluster orbit changes size");
        }
        h.assertTrue(PhaseMoteLayout.sample(false,0,0,0).color()==0xCFA6FF&&PhaseMoteLayout.sample(false,1,0,0).color()==0x6E29C2,"Purple phase palette changed");
        h.succeed();
    }
    private static final class Roll extends net.minecraft.world.level.levelgen.LegacyRandomSource {
        final int result;int calls;Roll(int result){super(1);this.result=result;}
        @Override public int nextInt(int bound){if(bound!=5)throw new AssertionError("Not a 20% trial");calls++;return result;}
    }
    private static net.minecraft.util.RandomSource growthRoll(int value){return new net.minecraft.world.level.levelgen.LegacyRandomSource(1){
        @Override public int nextInt(int bound){if(bound!=4)throw new AssertionError("Not a 25% growth trial");return value;}
    };}
    private static java.util.List<BlockPos> line(GameTestHelper h,int z,int count){
        var cells=new java.util.ArrayList<BlockPos>();for(int i=0;i<count;i++){
            var p=h.absolutePos(new BlockPos(2+i,4,z));h.getLevel().setBlock(p,PhaseContent.BLOCK.get().defaultBlockState(),2);cells.add(p);
        }return cells;
    }
    @GameTest(template="spell_arena") public static void thresholdAndSingleTrial(GameTestHelper h){
        var level=h.getLevel();var nine=line(h,3,9);var fail=new Roll(0);
        for(var p:nine)PhaseNetworks.tick(level,p,fail);
        h.assertTrue(fail.calls==0&&nine.stream().noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)),"Nine blocks can invert");
        var ten=line(h,7,10);var success=new Roll(0);for(var p:ten)PhaseNetworks.tick(level,p,success);
        h.assertTrue(success.calls==1&&ten.stream().allMatch(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING)),"Component rolls independently per block");
        h.assertTrue(ten.stream().noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)),"Phase changes instantaneously");
        for(int i=0;i<16;i++)PhaseNetworks.advance(level,PhaseNetworks.component(level,ten.getFirst()));
        for(var p:ten){var s=level.getBlockState(p);h.assertTrue(s.getCollisionShape(level,p).isEmpty()&&s.getLightEmission()==15,"Inversion does not change collision/light");}
        h.assertTrue(PhaseNetworks.PERIOD==200&&level.getBlockTicks().hasScheduledTick(ten.getFirst(),PhaseContent.BLOCK.get()),"Periodic checks not scheduled");
        var solo=line(h,11,1);PhaseNetworks.tick(level,solo.getFirst(),new Roll(0));
        h.assertTrue(!level.getBlockState(solo.getFirst()).getValue(PhaseBlock.INVERTED),"Single block changes phase");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reversionAndFailedTrial(GameTestHelper h){
        var level=h.getLevel();var ten=line(h,5,10);var group=PhaseNetworks.component(level,ten.getFirst());PhaseNetworks.apply(level,group,true);
        PhaseNetworks.tick(level,ten.getFirst(),new Roll(0));
        h.assertTrue(level.getBlockState(ten.getFirst()).getValue(PhaseBlock.INVERTED),"Restoration is immediate");
        for(int i=0;i<16;i++)PhaseNetworks.advance(level,group);
        h.assertTrue(ten.stream().noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)),"Inverted network does not revert");
        for(var p:ten){var s=level.getBlockState(p);h.assertTrue(!s.getCollisionShape(level,p).isEmpty()&&s.getLightEmission()==12,"Normal collision/light incorrect");}
        var other=line(h,10,10);PhaseNetworks.tick(level,other.getFirst(),new Roll(4));
        h.assertTrue(other.stream().noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)),"Failed trial changes state");h.succeed();
    }
    @GameTest(template="spell_arena") public static void diagonalContactsDoNotJoinAndSplitsNormalize(GameTestHelper h){
        var level=h.getLevel();var group=line(h,6,10);PhaseNetworks.apply(level,PhaseNetworks.component(level,group.getFirst()),true);
        level.setBlock(group.get(4),Blocks.AIR.defaultBlockState(),2);
        var roll=new Roll(0);PhaseNetworks.tick(level,group.getFirst(),roll);PhaseNetworks.tick(level,group.getLast(),roll);
        for(int i=0;i<16;i++){
            PhaseNetworks.advance(level,PhaseNetworks.component(level,group.getFirst()));
            PhaseNetworks.advance(level,PhaseNetworks.component(level,group.getLast()));
        }
        h.assertTrue(roll.calls==0&&group.stream().filter(p->!level.getBlockState(p).isAir()).noneMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)),"Small split remains phased");
        var corner=group.getFirst().offset(-1,0,-1);level.setBlock(corner,PhaseContent.BLOCK.get().defaultBlockState(),2);
        h.assertTrue(PhaseNetworks.component(level,corner).blocks().size()==1,"Diagonal contact joins networks");h.succeed();
    }
    @GameTest(template="spell_arena") public static void dustGrowthIsAtomicAndDoesNotGrowNaturally(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(7,4,7));
        for(var offset:PhaseGrowth.form())level.setBlock(p.offset(offset),Blocks.AIR.defaultBlockState(),2);
        level.setBlockAndUpdate(p.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(p,PhaseContent.CLUSTER.get().defaultBlockState());
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);var dust=new ItemStack(ModItems.ARCANE_DUST.get(),4);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
        var context=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(p.getCenter(),Direction.UP,p,false));
        var blocked=p.above();level.setBlock(blocked,Blocks.BEDROCK.defaultBlockState(),2);
        h.assertTrue(!dust.getItem().useOn(context).consumesAction()&&dust.getCount()==4&&level.getBlockState(p).is(PhaseContent.CLUSTER.get()),"Obstructed overhead consumes or partially grows");
        level.setBlock(blocked,Blocks.STONE.defaultBlockState(),2);
        h.assertTrue(PhaseGrowth.grow(context,growthRoll(1)).consumesAction()&&dust.getCount()==3&&level.getBlockState(p).is(PhaseContent.CLUSTER.get()),"Failed growth does not consume one dust or grows anyway");
        var preserved=p.offset(4,4,4);level.setBlock(preserved,Blocks.BEDROCK.defaultBlockState(),2);
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(7,4,7));cow.setNoAi(true);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);cow.invulnerableTime=100;
        h.assertTrue(PhaseGrowth.grow(context,growthRoll(0)).consumesAction()&&dust.getCount()==2,"Dust does not grow cluster");
        h.assertTrue(cow.getHealth()==30&&level.getBlockState(blocked).is(PhaseContent.BLOCK.get()),"Growth does not pierce blocks or applies duplicate/non-true damage");cow.discard();
        h.assertTrue(level.getBlockState(preserved).is(Blocks.BEDROCK),"Growth destroys an off-center indestructible block");
        var group=PhaseNetworks.component(level,p);h.assertTrue(group.blocks().size()==PhaseGrowth.form().size()-1&&group.blocks().size()>9,"Large cluster not fully connected around obstacle");
        h.assertTrue(!PhaseContent.CLUSTER.get().defaultBlockState().isRandomlyTicking(),"Cluster grows naturally");
        for(var ground:new Block[]{Blocks.DIRT,Blocks.STONE,Blocks.GLASS,Blocks.OAK_PLANKS,Blocks.STONE_SLAB}){
            level.setBlock(p.below(),ground.defaultBlockState(),2);h.assertTrue(PhaseContent.CLUSTER.get().defaultBlockState().canSurvive(level,p),"Cluster rejects ground");
        }
        try{var json=new com.google.gson.JsonArray();for(var cell:PhaseGrowth.form()){var xyz=new com.google.gson.JsonArray();xyz.add(cell.getX());xyz.add(cell.getY());xyz.add(cell.getZ());json.add(xyz);}
            var path=java.nio.file.Path.of("build/phase-growth-form.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,json.toString());
        }catch(java.io.IOException e){throw new RuntimeException(e);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void solidificationImpalesOccupantsOnce(GameTestHelper h){
        var level=h.getLevel();var cells=line(h,6,10);var group=PhaseNetworks.component(level,cells.getFirst());PhaseNetworks.apply(level,group,true);
        var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,new BlockPos(4,4,6));cow.setNoAi(true);
        cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);cow.setHealth(40);cow.invulnerableTime=100;
        PhaseNetworks.begin(level,group,false);
        for(int i=0;i<15;i++)PhaseNetworks.advance(level,group);
        h.assertTrue(cow.getHealth()==40&&level.getBlockState(cells.getFirst()).getCollisionShape(level,cells.getFirst()).isEmpty(),"Restoration damages or collides before completion");
        PhaseNetworks.advance(level,group);h.assertTrue(cow.getHealth()==30,"Solidification misses occupants or damages once per block");
        PhaseNetworks.apply(level,group,false);h.assertTrue(cow.getHealth()==30,"Unchanged solids repeatedly hurt");
        PhaseNetworks.apply(level,group,true);h.assertTrue(cow.getHealth()==30,"Becoming passable hurts");cow.discard();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=120) public static void transitionUsesFourSeconds(GameTestHelper h){
        var level=h.getLevel();var cells=line(h,6,10);var group=PhaseNetworks.component(level,cells.getFirst());
        PhaseNetworks.begin(level,group,true);
        h.runAfterDelay(35,()->{
            var state=level.getBlockState(cells.getFirst());
            h.assertTrue(state.getValue(PhaseBlock.PHASE)>0&&state.getValue(PhaseBlock.PHASE)<16
                    &&!state.getCollisionShape(level,cells.getFirst()).isEmpty(),"Transition does not preserve collision and intermediate palette");
        });
        h.runAfterDelay(82,()->{
            var state=level.getBlockState(cells.getFirst());
            h.assertTrue(state.getValue(PhaseBlock.INVERTED)&&state.getValue(PhaseBlock.PHASE)==16
                    &&!state.getValue(PhaseBlock.TRANSITIONING),"Four-second transition does not complete");h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void catalysisHasSixteenYieldAndNoDryGrowth(GameTestHelper h){
        var level=h.getLevel();var soil=new ItemStack(DreamContent.EXCITED_SPATIAL_CRYSTAL.get());var seed=new ItemStack(PhaseContent.CLUSTER.get());
        var fluid=new net.neoforged.neoforge.fluids.FluidStack(com.mcmagic.omnira.alchemy.AlchemyMaterials.DILUTE.source.get(),500);
        var plan=CatalysisGrowth.resolve(level,soil,seed,fluid);
        h.assertTrue(plan!=null&&plan.stages()==4&&plan.product().is(PhaseContent.BLOCK_ITEM.get())&&plan.product().getCount()==16,"Phase catalysis missing or incorrect");
        h.assertTrue(CatalysisNaturalGrowth.resolve(level,soil,seed)==null,"Phase cluster grows without liquid");
        h.assertTrue(level.getRecipeManager().getRecipes().stream().anyMatch(r->r.id().toString().equals("omnira:phase_crystal_cluster")&&r.value().getResultItem(level.registryAccess()).is(PhaseContent.CLUSTER_ITEM.get())),"Four-block crafting recipe missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void forestIsSparseAndUsesSixthBiome(GameTestHelper h){
        int small=0,large=0;var centers=new java.util.ArrayList<BlockPos>();
        for(int x=-24;x<=24;x++)for(int z=-24;z<=24;z++){
            var p=PhaseTerrain.center(x,z);if(p==null)continue;centers.add(p);
            h.assertTrue(CorridorLayout.block(p.getX(),com.mcmagic.omnira.space.FoldedTerrain.floor(p.getX(),p.getZ()),p.getZ()).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),"Grove floor not excited crystal");
            if(PhaseTerrain.large(x,z)){large++;h.assertTrue(CorridorLayout.block(p.getX(),p.getY()+6,p.getZ()).is(PhaseContent.BLOCK.get()),"Large cluster absent");}
            else{small++;h.assertTrue(CorridorLayout.block(p.getX(),p.getY(),p.getZ()).is(PhaseContent.CLUSTER.get()),"Small cluster absent");}
        }
        h.assertTrue(small>10&&large>10,"Natural distribution missing a cluster form");
        for(var a:centers)for(var b:centers)if(a!=b)h.assertTrue(Math.max(Math.abs(a.getX()-b.getX()),Math.abs(a.getZ()-b.getZ()))>=17,"Natural clusters can connect");
        var ops=h.getLevel().registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
        var data=new com.google.gson.JsonObject();data.addProperty("time_biome","omnira:temporal_continent");data.addProperty("plain_biome","omnira:stilled_wastes");data.addProperty("corridor_biome","omnira:spacetime_corridor");
        var source=StrataBiomeSource.CODEC.codec().parse(ops,data).getOrThrow();var p=centers.getFirst();
        h.assertTrue(source.getNoiseBiome(p.getX()>>2,10,p.getZ()>>2,h.getLevel().getChunkSource().randomState().sampler()).is(PhaseContent.BIOME),"Old source does not expose phase biome");h.succeed();
    }
}
