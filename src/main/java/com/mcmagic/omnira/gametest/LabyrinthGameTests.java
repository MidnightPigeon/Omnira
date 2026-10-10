package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.labyrinth.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_labyrinth") @PrefixGameTestTemplate(false)
public final class LabyrinthGameTests {
    private static LabyrinthTerrain.Room site;
    private static LabyrinthTerrain.Room site(){
        if(site!=null)return site;
        for(int x=-100;x<=100;x++)for(int z=-100;z<=100;z++){
            boolean valid=true;
            for(int dx=-21;dx<=21&&valid;dx+=7)for(int dz=-21;dz<=21;dz+=7)
                if(!LabyrinthTerrain.biome(x*7+3+dx,z*7+3+dz)){valid=false;break;}
            if(valid)return site=new LabyrinthTerrain.Room(x,1,z);
        }
        throw new IllegalStateException("No complete maze region found");
    }
    @GameTest(template="spell_arena") public static void roomDimensionsRoofsAndUniformThemes(GameTestHelper h){
        int[] kinds=new int[14];int roofs=0;
        for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++){
            var room=new LabyrinthTerrain.Room(x,2,z);kinds[Math.floorMod(LabyrinthTerrain.hash(room),14)]++;
            if(LabyrinthTerrain.entryRoof(room))roofs++;
        }
        h.assertTrue(roofs>1100&&roofs<1500,"Roof access is not approximately one fifth");
        for(int count:kinds)h.assertTrue(count>350&&count<600,"Room themes not equally distributed");
        var room=site();var plan=LabyrinthTerrain.plan(room);var o=room.origin();
        h.assertTrue(plan.doors().size()==6,"Middle complete room lacks six directions");
        for(int x=1;x<=5;x++)for(int z=1;z<=5;z++){
            h.assertTrue(plan.blocks().get(o.offset(x,0,z)).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())||x==3&&z==3,"Room floor missing");
            for(int y=1;y<=5;y++)h.assertTrue(plan.blocks().containsKey(o.offset(x,y,z)),"Missing interior cell");
        }
        for(int x:new int[]{1,5})for(int z:new int[]{1,5}){
            var p=o.offset(x,1,z);h.assertTrue(plan.blocks().get(p).is(DreamContent.LIGHT_CRYSTAL_TORCH.get())&&plan.blocks().get(p.above()).is(DreamContent.LIGHT_CRYSTAL_TORCH.get()),"Corner torch incomplete");
        }
        var lower=new LabyrinthTerrain.Room(room.x(),0,room.z());
        h.assertTrue(LabyrinthTerrain.plan(lower).blocks().keySet().stream().allMatch(p->p.getY()>=24),"Bottom seal overwritten");h.succeed();
    }
    @GameTest(template="spell_arena") public static void randomAndCompassCandidates(GameTestHelper h){
        var room=site();
        for(var direction:Direction.values()){
            var stable=LabyrinthTransit.candidates(room,direction,true);
            h.assertTrue(stable.size()==1&&stable.getFirst().equals(room.adjacent(direction)),"Compass does not reach adjacent room");
            var random=LabyrinthTransit.candidates(room,direction,false);
            h.assertTrue(random.size()>10,"Random mapping has too few destinations");
            for(var target:random)h.assertTrue(!target.equals(room)&&Math.abs(target.x()-room.x())<=2&&Math.abs(target.y()-room.y())<=2&&Math.abs(target.z()-room.z())<=2&&LabyrinthTerrain.plan(target).doors().containsKey(direction.getOpposite()),"Invalid destination");
        }
        h.assertTrue(LabyrinthTransit.candidates(new LabyrinthTerrain.Room(room.x(),2,room.z()),Direction.UP,true).isEmpty(),"Compass escapes through surface");h.succeed();
    }
    @GameTest(template="spell_arena") public static void legacyBiomeSourceLoadsLabyrinth(GameTestHelper h)throws Exception{
        try(var in=LabyrinthGameTests.class.getResourceAsStream("/data/omnira/dimension/spacetime_corridor.json")){
            var json=com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("generator").get("biome_source");
            var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,h.getLevel().registryAccess());
            var source=com.mcmagic.omnira.spacetime.StrataBiomeSource.CODEC.codec().parse(ops,json).getOrThrow();var p=site().origin().offset(3,1,3);
            h.assertTrue(source.getNoiseBiome(Math.floorDiv(p.getX(),4),Math.floorDiv(p.getY(),4),Math.floorDiv(p.getZ(),4),h.getLevel().getChunkSource().randomState().sampler()).is(LabyrinthContent.BIOME),"Existing biome source omits maze");
            h.assertTrue(!source.getNoiseBiome(Math.floorDiv(p.getX(),4),56,Math.floorDiv(p.getZ(),4),h.getLevel().getChunkSource().randomState().sampler()).is(LabyrinthContent.BIOME),"Maze biome affects time continent");
        }
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,new net.minecraft.world.item.ItemStack(LabyrinthContent.COMPASS.get()));
        h.assertTrue(LabyrinthTransit.stable(player),"Off-hand compass ignored");
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,net.minecraft.world.item.ItemStack.EMPTY);
        h.assertTrue(!LabyrinthTransit.stable(player),"Unheld compass stabilized mapping");h.succeed();
    }
    @GameTest(template="spell_arena") public static void waterAndBothMagicLiquids(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var level=h.getLevel();var block=LabyrinthContent.PORTAL.get();
        var fluids=new net.minecraft.world.level.material.Fluid[]{net.minecraft.world.level.material.Fluids.WATER,com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get(),com.mcmagic.omnira.mire.MireContent.SOURCE.get()};
        for(int i=0;i<fluids.length;i++){
            var state=block.defaultBlockState();level.setBlock(pos,state,2);
            h.assertTrue(block.canPlaceLiquid(null,level,pos,state,fluids[i])&&block.placeLiquid(level,pos,state,fluids[i].defaultFluidState()),"Portal rejected supported fluid");
            h.assertTrue(level.getFluidState(pos).getType().isSame(fluids[i])&&level.getBlockState(pos).getValue(VoidPortalBlock.FLUID)==i+1,"Portal lost its fluid");
            h.assertTrue(!block.placeLiquid(level,pos,level.getBlockState(pos),fluids[i].defaultFluidState()),"Filled portal accepted another liquid");
        }
        h.assertTrue(!block.canPlaceLiquid(null,level,pos,block.defaultBlockState(),net.minecraft.world.level.material.Fluids.LAVA),"Unspecified lava accepted");
        for(var direction:Direction.values()){
            var box=VoidPortalBlock.bounds(block.defaultBlockState().setValue(VoidPortalBlock.FACING,direction),BlockPos.ZERO);
            h.assertTrue(Math.max(box.getXsize(),Math.max(box.getYsize(),box.getZsize()))>1,"Vortex smaller than one block");
        }
        level.removeBlock(pos,false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void liquidLandingAndBlockedDestination(GameTestHelper h){
        var room=site();var level=h.getLevel();var states=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        for(var cell:LabyrinthTerrain.plan(room).blocks().entrySet()){states.put(cell.getKey(),level.getBlockState(cell.getKey()));level.setBlock(cell.getKey(),cell.getValue(),2);}
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,5));pig.setNoAi(true);
        try{
            var point=LabyrinthTransit.arrival(level,pig,room,Direction.WEST);h.assertTrue(point!=null,"Horizontal exit unavailable");
            for(int x=1;x<=5;x++)for(int z=1;z<=5;z++)level.setBlock(room.origin().offset(x,1,z),Blocks.WATER.defaultBlockState(),2);
            h.assertTrue(LabyrinthTransit.arrival(level,pig,room,Direction.WEST)!=null,"Water blocked teleport landing");
            for(int x=1;x<=5;x++)for(int y=1;y<=5;y++)for(int z=1;z<=5;z++)level.setBlock(room.origin().offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
            h.assertTrue(LabyrinthTransit.arrival(level,pig,room,Direction.WEST)==null,"Teleport entered a solid room");
        }finally{pig.discard();states.forEach((p,s)->level.setBlock(p,s,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void unopenedTreasureAndPedestal(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(5,3,5));
        for(var block:new net.minecraft.world.level.block.Block[]{ModBlocks.CRYSTAL_BALL.get(),ModBlocks.LIQUID_CRYSTAL_BALL.get(),ModBlocks.CRYSTAL_PEDESTAL.get()}){
            level.setBlock(p,block.defaultBlockState(),2);var be=level.getBlockEntity(p);LabyrinthDecoration.configure(be,p);
            var tag=be.saveWithoutMetadata(level.registryAccess());
            if(block==ModBlocks.CRYSTAL_BALL.get())h.assertTrue(tag.getString("LootTable").equals("omnira:chests/topological_labyrinth"),"Wrong pending loot table");
            if(block==ModBlocks.LIQUID_CRYSTAL_BALL.get())h.assertTrue(tag.getBoolean("FluidTreasure"),"Liquid orb is not treasure");
            if(block==ModBlocks.CRYSTAL_PEDESTAL.get())h.assertTrue(((com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity)be).getItem(0).is(ModItems.CRUISE_ORB.get()),"Pedestal lacks cruise orb");
            level.removeBlock(p,false);
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void lootCompassChance(GameTestHelper h){
        var level=h.getLevel();var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse("omnira:chests/topological_labyrinth"));
        var table=level.getServer().reloadableRegistries().getLootTable(key);
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,net.minecraft.world.phys.Vec3.ZERO).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int compasses=0;
        for(int i=1;i<=1000;i++)for(var item:table.getRandomItems(params,i))if(item.is(LabyrinthContent.COMPASS.get()))compasses++;
        h.assertTrue(compasses>150&&compasses<250,"Compass chance or loot table is invalid: "+compasses);h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void touchTeleportsImmediatelyAndExitDoesNotRepeat(GameTestHelper h){
        var level=h.getLevel();var room=site();var source=LabyrinthTerrain.plan(room).doors().get(Direction.WEST);
        var candidates=LabyrinthTransit.candidates(room,Direction.WEST,false);var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var chunks=new java.util.HashSet<net.minecraft.world.level.ChunkPos>();
        var all=new java.util.ArrayList<>(candidates);all.add(room);
        for(var r:all)for(var cell:LabyrinthTerrain.plan(r).blocks().entrySet()){
            var p=cell.getKey();saved.putIfAbsent(p,level.getBlockState(p));level.setBlock(p,cell.getValue(),2);chunks.add(new net.minecraft.world.level.ChunkPos(p));
        }
        var biomes=new java.util.HashMap<net.minecraft.world.level.ChunkPos,net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>[]>();
        var maze=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(LabyrinthContent.BIOME);
        for(var c:chunks){
            var chunk=level.getChunk(c.x,c.z);int count=chunk.getSectionsCount()*64;
            @SuppressWarnings("unchecked") var old=(net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>[])new net.minecraft.core.Holder<?>[count];
            for(int i=0;i<count;i++)old[i]=chunk.getNoiseBiome(c.x*4+(i&3),level.getMinBuildHeight()/4+(i/16),c.z*4+((i/4)&3));
            biomes.put(c,old);chunk.fillBiomesFromNoise((x,y,z,s)->maze,level.getChunkSource().randomState().sampler());
        }
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,5));pig.setNoAi(true);pig.setNoGravity(true);
        try{
            var center=VoidPortalBlock.bounds(level.getBlockState(source),source).getCenter();pig.moveTo(center.x,center.y-.4,center.z);
            pig.fallDistance=12;var before=pig.position();
            LabyrinthTransit.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(pig));
            h.assertTrue(pig.position().distanceToSqr(before)>4&&pig.fallDistance==0,"Contact did not teleport immediately or reset fall damage");
            var arrived=pig.position();LabyrinthTransit.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(pig));
            h.assertTrue(pig.position().equals(arrived),"Exit immediately reteleported entity");
            var exit=BlockPos.of(pig.getPersistentData().getLong("OmniraVoidPortalExit"));var exitCenter=VoidPortalBlock.bounds(level.getBlockState(exit),exit).getCenter();
            pig.getPersistentData().putLong("OmniraVoidPortalCooldown",0);pig.moveTo(exitCenter.x,exitCenter.y-.4,exitCenter.z);
            var onExit=pig.position();LabyrinthTransit.tick(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(pig));
            h.assertTrue(pig.position().equals(onExit),"Continuously touching exit bypassed protection");
        }finally{
            pig.discard();saved.forEach((p,s)->level.setBlock(p,s,2));
            biomes.forEach((c,old)->level.getChunk(c.x,c.z).fillBiomesFromNoise((x,y,z,s)->old[((y-level.getMinBuildHeight()/4)*16)+Math.floorMod(z,4)*4+Math.floorMod(x,4)],level.getChunkSource().randomState().sampler()));
        }
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void mazeBoundaryIsSealedAndLevel(GameTestHelper h){
        int sites=0;
        search:for(int x=-512;x<=512;x+=8)for(int z=-512;z<=512;z+=8){
            if(!LabyrinthTerrain.biome(x,z)||com.mcmagic.omnira.space.CaveSurface.mazeDistance(x,z)!=0)continue;
            for(var d:Direction.Plane.HORIZONTAL){int nx=x+d.getStepX(),nz=z+d.getStepZ();
                if(!LabyrinthTerrain.biome(nx,nz)){
                    h.assertTrue(com.mcmagic.omnira.space.FoldedTerrain.floor(nx,nz)==40,"Adjacent floor not level");
                    for(int y=24;y<40;y++)h.assertTrue(!LabyrinthTerrain.block(x,y,z).isAir(),"Open maze boundary");
                }
            }
            h.assertTrue(com.mcmagic.omnira.spacetime.CorridorLayout.block(x,23,z).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Base seal missing");
            if(++sites==10)break search;
        }
        h.assertTrue(sites==10,"Not enough boundary samples");h.succeed();
    }
}
