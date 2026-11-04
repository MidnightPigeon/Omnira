package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.visual.MachineRangePreview;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_machine_range") @PrefixGameTestTemplate(false)
public final class MachineRangeGameTests {
    @GameTest(template="spell_arena") public static void timingAndActivation(GameTestHelper h){
        for(int r:new int[]{8,32,64}){
            float arrived=r/MachineRangePreview.SPEED;
            h.assertTrue(Math.abs(MachineRangePreview.extent(20,r)-3)<.0001,"Expansion is not three blocks per second");
            h.assertTrue(MachineRangePreview.extent(arrived+100,r)==r&&MachineRangePreview.alpha(arrived+100,r)==1,"Boundary hold changed");
            h.assertTrue(Math.abs(MachineRangePreview.alpha(arrived+110,r)-.5)<.0001&&MachineRangePreview.alpha(arrived+120,r)<.0001,"Fade timing changed");
        }
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        var anchors=com.mcmagic.omnira.space.anchor.AnchorContent.DORMANT.get();level.setBlockAndUpdate(pos,anchors.defaultBlockState());
        h.assertTrue(MachineRangePreview.find(level,pos)==null,"Dormant anchor exposes range");
        level.setBlockAndUpdate(pos,com.mcmagic.omnira.space.anchor.AnchorContent.ACTIVE.get().defaultBlockState());
        h.assertTrue(MachineRangePreview.find(level,pos).radius()==64,"Anchor radius incorrect");
        var orbBlock=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:wandering_star_cluster_orb"));
        level.setBlockAndUpdate(pos,orbBlock.defaultBlockState());
        var orb=(com.mcmagic.omnira.space.sea.StarClusterOrbEntity)level.getBlockEntity(pos);
        h.assertTrue(MachineRangePreview.find(level,pos)==null,"Unupgraded orb exposes range");
        orb.upgrades.setStackInSlot(0,new ItemStack(com.mcmagic.omnira.registry.ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));
        h.assertTrue(MachineRangePreview.find(level,pos).radius()==32,"Stabilized orb radius incorrect");
        orb.upgrades.setStackInSlot(0,ItemStack.EMPTY);h.assertTrue(MachineRangePreview.find(level,pos)==null,"Removed upgrade still exposes range");
        level.setBlockAndUpdate(pos,com.mcmagic.omnira.space.sea.StarlightContent.SOURCE.get().defaultBlockState());
        h.assertTrue(MachineRangePreview.find(level,pos).radius()==64,"Star source radius incorrect");
        level.setBlockAndUpdate(pos,net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:myriad_codex")).defaultBlockState());
        h.assertTrue(MachineRangePreview.find(level,pos).radius()==32,"Codex must show heart placement range");h.succeed();
    }
    @GameTest(template="spell_arena") public static void chunkStandAndEmptyHandGesture(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        var block=com.mcmagic.omnira.registry.ModBlocks.WONDERLAND_POKER_STAND.get();level.setBlock(pos,block.defaultBlockState(),2);
        level.setBlock(pos.above(),block.defaultBlockState().setValue(com.mcmagic.omnira.time.WonderlandPokerStandBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),2);
        var range=MachineRangePreview.find(level,pos.above());h.assertTrue(range!=null&&range.chunk()&&range.source().equals(pos),"Upper stand does not resolve chunk range");
        var player=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);player.setShiftKeyDown(true);
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
        var event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit);
        float health=player.getHealth();MachineRangePreview.use(event);
        h.assertTrue(event.isCanceled()&&player.getHealth()==health&&level.getBlockState(pos).is(block),"Range gesture failed or mutated world");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        event=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,net.minecraft.world.InteractionHand.MAIN_HAND,pos,hit);MachineRangePreview.use(event);
        h.assertTrue(!event.isCanceled(),"Held-item interaction intercepted");
        ((com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity)level.getBlockEntity(pos)).releaseForcedChunk();h.succeed();
    }

    @GameTest(template="spell_arena") public static void additionalRangesAndPlantShapes(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,3,4));
        h.assertTrue(MachineRangePreview.verticalAlpha(-64,-64,320)==1&&MachineRangePreview.verticalAlpha(128,-64,320)==.5F&&MachineRangePreview.verticalAlpha(320,-64,320)==0,"Chunk height fade incorrect");
        for(String id:new String[]{"time_warp_point","space_warp_point","spatial_remnant","dense_void_mass","crystal_anvil"}){
            var block=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse("omnira:"+id));
            h.assertTrue(block!=Blocks.AIR,"Missing test block "+id);level.setBlock(pos,block.defaultBlockState(),2);
            var range=MachineRangePreview.find(level,pos);h.assertTrue(range!=null&&range.radius()==5,"Range incorrect: "+id);
        }
        for(var block:com.mcmagic.omnira.space.geodes.DeflectionContent.stages()){
            level.setBlock(pos,block.defaultBlockState(),2);
            h.assertTrue(MachineRangePreview.find(level,pos).radius()==com.mcmagic.omnira.space.geodes.DeflectionContent.radius(block.defaultBlockState()),"Deflection stage mismatch");
        }
        var light=com.mcmagic.omnira.registry.DreamContent.LIGHT_CRYSTAL_CORE.get().defaultBlockState();level.setBlock(pos,light,2);
        h.assertTrue(MachineRangePreview.find(level,pos)==null,"Dormant light core exposes growth range");
        level.setBlock(pos,light.setValue(com.mcmagic.omnira.block.LightCrystalCoreBlock.GROWING,true),2);
        h.assertTrue(MachineRangePreview.find(level,pos).radius()==5,"Growing core range missing");
        for(var block:new Block[]{com.mcmagic.omnira.registry.TimeNatureContent.GRASS.get(),com.mcmagic.omnira.registry.TimeNatureContent.FLOWER.get()}){
            var state=block.defaultBlockState();var bounds=state.getShape(level,pos).bounds();
            h.assertTrue(bounds.maxY<1&&bounds.minX>0&&bounds.maxX<1,"Plant outline remains a full block");
            h.assertTrue(state.getCollisionShape(level,pos).isEmpty(),"Time plant blocks entities");
        }
        var phase=com.mcmagic.omnira.space.phase.PhaseContent.CLUSTER.get().defaultBlockState();
        h.assertTrue(!phase.getCollisionShape(level,pos).isEmpty()&&phase.getCollisionShape(level,pos).equals(phase.getShape(level,pos)),"Phase cluster collision mismatch");
        for(int age=0;age<3;age++)for(var face:Direction.Plane.HORIZONTAL){
            var berry=com.mcmagic.omnira.registry.DreamContent.SHADOW_BERRIES.get().defaultBlockState().setValue(CocoaBlock.AGE,age).setValue(CocoaBlock.FACING,face);
            h.assertTrue(!berry.getCollisionShape(level,pos).isEmpty()&&berry.getCollisionShape(level,pos).equals(berry.getShape(level,pos)),"Berry collision mismatch");
        }
        h.succeed();
    }
}
