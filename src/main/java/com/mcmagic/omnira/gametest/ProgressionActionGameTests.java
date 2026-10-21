package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.codex.edit.*;
import com.mcmagic.omnira.guidance.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_progression_actions") @PrefixGameTestTemplate(false)
public final class ProgressionActionGameTests {
    @GameTest(template="spell_arena",batch="progress_exploration")
    public static void explorationTicksRecordBothDreamAndSpacetimeHalves(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var level=h.getLevel();
        var chunk=level.getChunkAt(h.absolutePos(new BlockPos(8,4,8)));var cp=chunk.getPos();
        var old=new java.util.HashMap<BlockPos,Holder<net.minecraft.world.level.biome.Biome>>();
        for(int y=level.getMinBuildHeight()>>2;y<level.getMaxBuildHeight()>>2;y++)for(int z=0;z<4;z++)for(int x=0;x<4;x++){
            var at=new BlockPos((cp.x<<2)+x,y,(cp.z<<2)+z);old.put(at,chunk.getNoiseBiome(at.getX(),y,at.getZ()));
        }
        var sampler=level.getChunkSource().randomState().sampler();
        try{
            p.setPos(cp.getMiddleBlockX(),h.absolutePos(new BlockPos(8,4,8)).getY(),cp.getMiddleBlockZ());
            for(var id:new String[]{"dawn_crystal_fields","shadow_woodland"}){
                var biome=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,ResourceLocation.fromNamespaceAndPath("omnira",id)));
                chunk.fillBiomesFromNoise((x,y,z,s)->biome,sampler);
                com.mcmagic.omnira.progression.BranchProgress.explore(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            }
        }finally{chunk.fillBiomesFromNoise((x,y,z,s)->old.get(new BlockPos(x,y,z)),sampler);}
        var dream=p.server.getAdvancements().get(ResourceLocation.parse("omnira:branches/dream_duality"));
        for(var id:new String[]{"light","dark"})h.assertTrue(p.getAdvancements().getOrStartProgress(dream).getCriterion(id).isDone(),"Dream exploration missed "+id);
        var corridor=p.server.getLevel(com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR);
        h.assertTrue(corridor!=null,"Spacetime test dimension unavailable");
        var visitor=new net.minecraft.server.level.ServerPlayer(p.server,corridor,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"shore-audit"),net.minecraft.server.level.ClientInformation.createDefault());
        visitor.connection=new net.neoforged.neoforge.common.util.FakePlayer(corridor,visitor.getGameProfile()).connection;
        for(int y:new int[]{com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE,com.mcmagic.omnira.spacetime.CorridorLayout.SPACE_CEILING}){
            visitor.setPos(0,y,0);com.mcmagic.omnira.progression.BranchProgress.explore(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(visitor));
        }
        var shores=p.server.getAdvancements().get(ResourceLocation.parse("omnira:branches/spacetime_shores"));
        for(var id:new String[]{"time","space"})h.assertTrue(visitor.getAdvancements().getOrStartProgress(shores).getCriterion(id).isDone(),"Spacetime exploration missed "+id);
        h.succeed();
    }
    private static void recorded(GameTestHelper h,net.minecraft.server.level.ServerPlayer p,String id){
        h.assertTrue(BranchAdvancementGameTests.recorded(p,id),"Real action did not record "+id);
    }
    @GameTest(template="spell_arena") public static void engineWireAndOrbCreditActualSmelting(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var source=new BlockPos(4,3,2);
        h.setBlock(source,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        ((com.mcmagic.omnira.block.entity.ManaEngineAccess)h.getBlockEntity(source)).engineState().inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        h.setBlock(source.south(),com.mcmagic.omnira.pulse.PulseContent.WIRE.get());h.setBlock(source.south(2),com.mcmagic.omnira.pulse.PulseContent.ORB.get());
        h.setBlock(source.south(3),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var entity=h.getLevel().getBlockEntity(h.absolutePos(source.south(3)));
        com.mcmagic.omnira.progression.BranchProgress.operate(p,entity);
        var work=com.mcmagic.omnira.workstation.CrystalWorkstations.work(entity);work.setItem(0,new ItemStack(Items.COBBLESTONE));
        for(int tick=0;tick<100;tick++)work.tickDirect();
        h.assertTrue(work.getItem(2).is(Items.STONE),"Pulse-fed furnace did not finish actual recipe");
        recorded(h,p,"first_pulse");recorded(h,p,"branching_pulse");h.succeed();
    }
    @GameTest(template="spell_arena") public static void storageCollectionUsesInventoryCriteria(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);
        for(var block:java.util.List.of(ModBlocks.CRYSTAL_BALL.get(),ModBlocks.LIQUID_CRYSTAL_BALL.get())){
            var stack=new ItemStack(block);p.getInventory().setItem(0,stack);
            CriteriaTriggers.INVENTORY_CHANGED.trigger(p,p.getInventory(),stack);
        }
        var a=p.server.getAdvancements().get(ResourceLocation.parse("omnira:branches/sorted_storage"));
        for(var criterion:new String[]{"crystal_ball","liquid_crystal_ball"})
            h.assertTrue(p.getAdvancements().getOrStartProgress(a).getCriterion(criterion).isDone(),"Storage inventory criterion missing: "+criterion);
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualStaffCastsCompleteCycleAndOmniBranch(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);p.setPos(h.absolutePos(new BlockPos(5,4,5)).getCenter());p.setXRot(-90);
        p.setData(ModAttachments.MANA,com.mcmagic.omnira.mana.ManaState.initial());
        var grid=new ItemStack(ModItems.OMNI_CRYSTAL_GRID.get());
        var a=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        a.set(ModDataComponents.SPELL_PATTERN,new com.mcmagic.omnira.spell.SpellPattern(com.mcmagic.omnira.spell.ElementType.AIR,com.mcmagic.omnira.spell.ElementType.AIR));
        var b=a.copy();b.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Second spell"));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(java.util.List.of(a,b)));
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        var staff=com.mcmagic.omnira.item.staff.StaffAssembly.basic().create();p.setItemInHand(InteractionHand.MAIN_HAND,staff);
        h.assertTrue(com.mcmagic.omnira.item.StaffItem.castSequence(p,staff,2)==2,"Staff did not cast both slots");
        recorded(h,p,"spell_cycle");recorded(h,p,"twelvefold");h.succeed();
    }
    @GameTest(template="spell_arena") public static void captureAndReleaseRecordsCapturer(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var at=h.absolutePos(new BlockPos(6,4,6)).getCenter();p.setPos(at);
        var cow=EntityType.COW.create(h.getLevel());cow.setPos(at);h.getLevel().addFreshEntity(cow);
        var bottle=new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get());
        h.assertTrue(com.mcmagic.omnira.item.bottle.PocketBottleItem.capture(p,cow,bottle),"Capture failed");
        h.assertTrue(!BranchAdvancementGameTests.recorded(p,"pocket_traveler"),"Capture alone counted");
        h.assertTrue(com.mcmagic.omnira.item.bottle.PocketBottleItem.release(h.getLevel(),bottle,at),"Release failed");
        recorded(h,p,"pocket_traveler");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=180) public static void matterAndEcologyRecordOnlyAfterCompletion(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var at=h.absolutePos(new BlockPos(8,6,8));
        h.getLevel().setBlock(at,Blocks.STONE.defaultBlockState(),2);
        var edits=CodexEdits.get(h.getLevel());
        h.assertTrue(edits.submit(at,7,Operation.MATTER,Direction.Axis.Y,false,new ItemStack(Items.STONE),new ItemStack(Items.GOLD_BLOCK),null,p).accepted(),"Matter rejected");
        h.runAfterDelay(35,()->{
            recorded(h,p,"transmute_matter");
            h.assertTrue(edits.submit(at,7,Operation.ECOLOGY,Direction.Axis.Y,false,ItemStack.EMPTY,ItemStack.EMPTY,h.getLevel().getBiome(at),p).accepted(),"Ecology rejected");
            h.runAfterDelay(100,()->{recorded(h,p,"pocket_ecology");h.succeed();});
        });
    }
    @GameTest(template="spell_arena") public static void finalOrbUpgradeUsesActualRightClick(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);var at=h.absolutePos(new BlockPos(5,4,5));p.setPos(at.getCenter().add(0,0,-2));
        h.getLevel().setBlockAndUpdate(at,GuidanceContent.BLOCK.get().defaultBlockState().setValue(GuidanceOrbBlock.STAGE,4));
        var orb=(GuidanceOrbEntity)h.getLevel().getBlockEntity(at);
        orb.interact(ItemStack.EMPTY,Direction.NORTH);orb.interact(ItemStack.EMPTY,Direction.UP);
        var codex=new ItemStack(com.mcmagic.omnira.codex.MyriadCodexContent.ITEM.get());p.setItemInHand(InteractionHand.MAIN_HAND,codex);
        p.gameMode.useItemOn(p,h.getLevel(),codex,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.NORTH,at,false));
        h.assertTrue(h.getLevel().getBlockState(at).is(GuidanceContent.UNDERSTANDING.get()),"Final upgrade failed");
        recorded(h,p,"convergent_paths");h.succeed();
    }
    @GameTest(template="spell_arena") public static void manuscriptDiscoveryAlsoLightsIndependentRoot(GameTestHelper h){
        var p=BranchAdvancementGameTests.player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TRAVELER_MANUSCRIPT.get()));
        com.mcmagic.omnira.fate.ManuscriptReward.finish(p);
        for(var path:new String[]{"too_late_scholar","easter_eggs/root"}){
            var a=p.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("omnira",path));
            h.assertTrue(p.getAdvancements().getOrStartProgress(a).isDone(),"Discovery/root did not light without mainline: "+path);
        }h.succeed();
    }
}
