package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spacetime.CorridorLayout;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spacetime_barrier")
@PrefixGameTestTemplate(false)
public final class SpacetimeBarrierGameTests {
    @GameTest(template="spell_arena") public static void mirrorShellAndSingleBiome(GameTestHelper h){
        for(int z=-8;z<=8;z++)for(int x=-2;x<=2;x++)for(int y=128;y<=132;y++){
            var state=CorridorLayout.block(x,y,z);
            if(y==128 || y==132 || Math.abs(x)==2)h.assertTrue(state.is(DreamContent.MIRROR_ROCK.get())
                    || state.is(DreamContent.ENGRAVED_MIRROR_ROCK.get()),"Non-mirror corridor shell");
            else h.assertTrue(state.isAir(),"Corridor interior changed");
        }
        var world=h.getLevel().getServer().getLevel(com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR);
        h.assertTrue(world.getChunkSource().getGenerator().getBiomeSource().possibleBiomes().size()==1,"Placeholder must have one biome");
        h.assertTrue(world.getBiome(new BlockPos(0,130,0)).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","spacetime_corridor")),"Biome identity changed");
        var settings=world.getBiome(new BlockPos(0,130,0)).value().getMobSettings();
        for(var category:net.minecraft.world.entity.MobCategory.values())
            h.assertTrue(settings.getMobs(category).isEmpty(),"Vanilla mobs can spawn naturally in the corridor dimension");
        h.assertTrue(!world.dimensionType().natural() && !world.dimensionType().hasSkyLight(),"Corridor dimension permits ordinary natural sky spawning");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void pistonAndSlimeCannotMoveSeparator(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
        var state=ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState();level.setBlockAndUpdate(pos,state);
        for(var direction:Direction.values())h.assertTrue(!net.minecraft.world.level.block.piston.PistonBaseBlock.isPushable(state,level,pos,direction,true,direction),"Piston accepted separator");
        level.setBlockAndUpdate(pos.west(),Blocks.SLIME_BLOCK.defaultBlockState());
        h.assertTrue(!new net.minecraft.world.level.block.piston.PistonStructureResolver(level,pos.west(2),Direction.EAST,true).resolve(),"Slime piston moved separator");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void createGlueAndContraptionRejection(GameTestHelper h)throws Exception{
        if(net.neoforged.fml.ModList.get().isLoaded("create"))CreateChecks.run(h);
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void sableCannotGatherSeparator(GameTestHelper h){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))SableChecks.run(h);
        h.succeed();
    }
    private static final class CreateChecks {
        static void run(GameTestHelper h)throws Exception{
            var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
            var state=ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState();
            level.setBlockAndUpdate(pos,state);level.setBlockAndUpdate(pos.east(),Blocks.STONE.defaultBlockState());
            var glue=new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(level,
                    com.simibubi.create.content.contraptions.glue.SuperGlueEntity.span(pos,pos.east()));level.addFreshEntity(glue);
            h.assertTrue(!com.simibubi.create.api.contraption.BlockMovementChecks.isMovementAllowed(state,level,pos),"Create can move separator");
            h.assertTrue(!com.simibubi.create.content.contraptions.glue.SuperGlueEntity.isValidFace(level,pos,Direction.EAST),"Glue placement accepted separator face");
            for(var from:new BlockPos[]{pos,pos.east()})h.assertTrue(!com.simibubi.create.content.contraptions.glue.SuperGlueEntity.isGlued(level,from,
                    from.equals(pos)?Direction.EAST:Direction.WEST,new java.util.HashSet<>()),"Area glue bonded a separator face");
            var contraption=new com.simibubi.create.content.contraptions.bearing.BearingContraption(false,Direction.UP);
            boolean rejected=false;
            try {rejected=!contraption.assemble(level,pos.below());}catch(com.simibubi.create.content.contraptions.AssemblyException expected){rejected=true;}
            h.assertTrue(rejected,"Bearing assembled separator");
            level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            h.assertTrue(com.simibubi.create.content.contraptions.glue.SuperGlueEntity.isGlued(level,pos,Direction.EAST,new java.util.HashSet<>()),"Normal stone bonding regressed");
            glue.discard();
        }
    }
    private static final class SableChecks {
        static void run(GameTestHelper h){
            var level=h.getLevel();var pos=h.absolutePos(new BlockPos(5,3,5));
            level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(pos.east(),ModBlocks.PURE_SOLIDIFIED_SPACETIME.get().defaultBlockState());
            var selection=java.util.Set.of(pos,pos.east());
            var result=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.gatherConnectedBlocks(pos,level,32,(a,b,c,d,e)->selection.contains(c));
            h.assertTrue(result.assemblyState()!=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.GatherResult.State.SUCCESS,"Sable gathered separator into physics structure");
            h.assertTrue(level.getBlockState(pos.east()).is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()),"Assembly removed separator");
        }
    }
}
