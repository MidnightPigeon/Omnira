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
    @GameTest(template="spell_arena") public static void portalBackingIsSolidAndRendered(GameTestHelper h){
        for(var block:new net.minecraft.world.level.block.Block[]{LabyrinthContent.PORTAL.get(),LabyrinthContent.LONG_RANGE_PORTAL.get()})for(var direction:Direction.values()){
            var state=block.defaultBlockState().setValue(VoidPortalBlock.FACING,direction);
            h.assertTrue(state.getRenderShape()==net.minecraft.world.level.block.RenderShape.MODEL,"Portal backing is invisible");
            h.assertTrue(state.canOcclude()&&state.getOcclusionShape(h.getLevel(),BlockPos.ZERO)==net.minecraft.world.phys.shapes.Shapes.block(),"Portal backing does not seal the wall");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void roomDisplaySightPreservesTransparentAndPartialViews(GameTestHelper h){
        var level=h.getLevel();var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var camera=net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(new BlockPos(5,3,2)));
        var target=new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(5,3,6))).inflate(.2);
        try{
            for(var material:new net.minecraft.world.level.block.Block[]{ModBlocks.PURE_SOLIDIFIED_SPACETIME.get(),Blocks.GLASS,Blocks.WATER,DreamContent.DREAM_CRYSTAL.get()}){
                for(int x=2;x<=8;x++)for(int y=1;y<=6;y++){
                    var p=h.absolutePos(new BlockPos(x,y,4));saved.putIfAbsent(p,level.getBlockState(p));level.setBlock(p,material.defaultBlockState(),2);
                }
                h.assertTrue(LabyrinthSight.visible(level,camera,target)==(material!=ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Opaque maze wall or transparent display sight is wrong");
            }
            for(int x=2;x<=8;x++)for(int y=1;y<=6;y++)level.setBlock(h.absolutePos(new BlockPos(x,y,4)),ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState(),2);
            level.setBlock(h.absolutePos(new BlockPos(5,3,4)),Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(LabyrinthSight.visible(level,camera,target),"Open sight was incorrectly culled");
            level.setBlock(h.absolutePos(new BlockPos(5,3,4)),ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState(),2);
            var wide=new net.minecraft.world.phys.AABB(h.absolutePos(new BlockPos(4,3,6))).expandTowards(3,0,0);
            level.setBlock(h.absolutePos(new BlockPos(6,3,4)),Blocks.AIR.defaultBlockState(),2);
            h.assertTrue(!LabyrinthSight.clear(level,camera,wide.getCenter())&&LabyrinthSight.visible(level,camera,wide),"Partly visible rim was incorrectly culled");
            h.assertTrue(LabyrinthSight.visible(level,target.getCenter(),target),"Camera inside display was culled");
        }finally{saved.forEach((p,s)->level.setBlock(p,s,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void paletteContactWorksAcrossSectionsWithoutLoadingChunks(GameTestHelper h){
        var level=h.getLevel();var saved=new java.util.HashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var origin=h.absolutePos(new BlockPos(5,3,5));
        try{
            for(var direction:Direction.values()){
                var p=new BlockPos((origin.getX()&~15)+15,Math.floorDiv(origin.getY(),16)*16+15,(origin.getZ()&~15)+15);
                var state=LabyrinthContent.PORTAL.get().defaultBlockState().setValue(VoidPortalBlock.FACING,direction);
                saved.putIfAbsent(p,level.getBlockState(p));level.setBlock(p,state,2);
                var center=VoidPortalBlock.bounds(state,p).getCenter();
                var contact=LabyrinthTransit.contact(level,new net.minecraft.world.phys.AABB(center,center).inflate(.05));
                h.assertTrue(contact!=null&&contact.pos().equals(p)&&contact.outward()==direction,"Section-edge contact lost a portal");
            }
            int cx=1_800_000,cz=1_800_000;
            h.assertTrue(level.getChunkSource().getChunkNow(cx,cz)==null,"Unexpected distant test chunk");
            var far=new net.minecraft.world.phys.AABB(cx*16+4,30,cz*16+4,cx*16+5,31,cz*16+5);
            h.assertTrue(LabyrinthTransit.contact(level,far)==null&&level.getChunkSource().getChunkNow(cx,cz)==null,"Contact scan loaded a distant chunk");
        }finally{saved.forEach((p,s)->level.setBlock(p,s,2));}
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void roomDimensionsRoofsAndUniformThemes(GameTestHelper h){
        int[] kinds=new int[14];int roofs=0;
        for(int x=-40;x<=40;x++)for(int z=-40;z<=40;z++){
            var room=new LabyrinthTerrain.Room(x,2,z);kinds[LabyrinthTerrain.theme(room)]++;
            if(LabyrinthTerrain.entryRoof(room))roofs++;
        }
        h.assertTrue(roofs>1100&&roofs<1500,"Roof access is not approximately one fifth");
        for(int i=0;i<kinds.length;i++)h.assertTrue(i==6?kinds[i]>270&&kinds[i]<390:kinds[i]>370&&kinds[i]<590,"Room theme weights differ from five percent heron and evenly split remainder");
        var room=site();var plan=LabyrinthTerrain.plan(room);var o=room.origin();
        h.assertTrue(plan.doors().size()==5&&!plan.doors().containsKey(Direction.DOWN),"Lower room has an invalid exit through the base");
        for(int x=1;x<=5;x++)for(int z=1;z<=5;z++){
            h.assertTrue(plan.blocks().get(o.offset(x,0,z)).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())||x==3&&z==3,"Room floor missing");
            for(int y=1;y<=4;y++)h.assertTrue(plan.blocks().containsKey(o.offset(x,y,z)),"Missing interior cell");
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
            h.assertTrue(direction==Direction.DOWN?stable.isEmpty():stable.size()==1&&stable.getFirst().equals(room.adjacent(direction)),"Compass does not reach a valid adjacent room");
            var random=LabyrinthTransit.candidates(room,direction,false);
            h.assertTrue(random.size()>10,"Random mapping has too few destinations");
            for(var target:random)h.assertTrue(!target.equals(room)&&Math.abs(target.x()-room.x())<=2&&Math.abs(target.y()-room.y())<=2&&Math.abs(target.z()-room.z())<=2&&LabyrinthTerrain.plan(target).doors().containsKey(direction.getOpposite()),"Invalid destination");
        }
        h.assertTrue(LabyrinthTransit.candidates(new LabyrinthTerrain.Room(room.x(),2,room.z()),Direction.UP,true).isEmpty(),"Compass escapes through surface");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=200) public static void equalHeightClearCrossAndInactiveHeronDisplays(GameTestHelper h){
        var base=site();boolean foundHeron=false,foundCruise=false;int themes=0,total=0;
        for(int x=-100;x<=100;x++)for(int z=-100;z<=100;z++){
            var r=new LabyrinthTerrain.Room(x,1,z);
            total++;if(LabyrinthTerrain.theme(r)==6)themes++;
        }
        h.assertTrue(themes>total*.045&&themes<total*.055,"Heron room chance not approximately five percent");
        for(int dx=-6;dx<=6;dx++)for(int dz=-6;dz<=6;dz++)for(int layer=1;layer<=2;layer++){
            var r=new LabyrinthTerrain.Room(base.x()+dx,layer,base.z()+dz);if(!LabyrinthTerrain.exists(r))continue;
            var plan=LabyrinthTerrain.plan(r);var o=r.origin();
            h.assertTrue(r.ceiling()-r.floor()-1==4,"Room clear height is not four");
            h.assertTrue(LabyrinthTerrain.room(o.offset(3,1,3)).equals(r),"Vertical room lookup mismatched");
            for(var cell:plan.blocks().entrySet()){
                var p=cell.getKey();var s=cell.getValue();
                h.assertTrue(!s.is(ModBlocks.NIGHT_HERON_STATUE.get()),"Maze generated an active milk-producing statue");
                int x=p.getX()-o.getX(),z=p.getZ()-o.getZ();
                if(p.getY()>r.floor()&&p.getY()<r.ceiling()&&x>=1&&x<=5&&z>=1&&z<=5&&(x==3||z==3))
                    h.assertTrue(s.isAir()||s.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock,"Room content obstructs the portal cross");
                if(s.is(ModBlocks.CRYSTAL_PEDESTAL.get())){
                    var pedestal=new com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity(p,s);LabyrinthDecoration.configure(pedestal,p);
                    var expected=plan.kind()==6?ModBlocks.NIGHT_HERON_STATUE.get().asItem():ModItems.CRUISE_ORB.get();
                    h.assertTrue(pedestal.getItem(0).is(expected),"Wrong pedestal exhibit");
                    if(plan.kind()==6)foundHeron=true;else foundCruise=true;
                }
            }
        }
        h.assertTrue(foundHeron&&foundCruise,"Display themes were not covered");
        for(int y=24;y<27;y++)h.assertTrue(LabyrinthTerrain.block(base.origin().getX()+3,y,base.origin().getZ()+3).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Truncated basement was not sealed");
        h.succeed();
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
