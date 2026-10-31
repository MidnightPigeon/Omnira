package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.progression.*;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.pulse.*;
import com.mcmagic.omnira.storage.*;
import com.mcmagic.omnira.codex.edit.*;
import com.mojang.authlib.GameProfile;
import java.util.*;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_branches") @PrefixGameTestTemplate(false)
public final class BranchAdvancementGameTests {
    static ServerPlayer player(GameTestHelper h){
        var profile=new GameProfile(UUID.randomUUID(),"branch-test");
        var player=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,ClientInformation.createDefault());
        player.connection=new FakePlayer(h.getLevel(),profile).connection;return player;
    }
    private static AdvancementHolder advancement(ServerPlayer p,String path){return p.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("omnira",path));}
    static boolean recorded(ServerPlayer p,String id){
        BranchProgress.reconcile(p);return p.getAdvancements().getOrStartProgress(advancement(p,"branches/"+id)).getCriterion("performed").isDone();
    }
    private static void parents(ServerPlayer p,AdvancementHolder child){
        child.value().parent().ifPresent(id->{var parent=p.server.getAdvancements().get(id);parents(p,parent);
            parent.value().criteria().keySet().stream().filter(k->!k.equals(AdvancementDependencies.PREVIOUS)).forEach(k->p.getAdvancements().award(parent,k));});
        AdvancementDependencies.sync(p);
    }
    @GameTest(template="spell_arena") public static void thirtyBranchesGateEarlyRecordsAndRevocation(GameTestHelper h){
        var all=h.getLevel().getServer().getAdvancements().getAllAdvancements().stream().filter(a->a.id().getNamespace().equals("omnira")&&a.id().getPath().startsWith("branches/")).toList();
        h.assertTrue(all.size()==30,"Expected all thirty approved branches");
        for(var branch:all){
            var p=player(h);h.assertTrue(!branch.value().display().orElseThrow().getIcon().isEmpty(),"Empty branch icon");
            h.assertTrue(branch.value().criteria().containsKey(AdvancementDependencies.PREVIOUS),"Missing strong prerequisite");
            for(var criterion:branch.value().criteria().keySet())if(!criterion.equals(AdvancementDependencies.PREVIOUS))p.getAdvancements().award(branch,criterion);
            h.assertTrue(!p.getAdvancements().getOrStartProgress(branch).isDone(),"Early action bypassed parent");
            parents(p,branch);h.assertTrue(p.getAdvancements().getOrStartProgress(branch).isDone(),"Early objective was lost");
            var parent=p.server.getAdvancements().get(branch.value().parent().orElseThrow());
            for(var criterion:parent.value().criteria().keySet())p.getAdvancements().revoke(parent,criterion);
            AdvancementDependencies.sync(p);h.assertTrue(!p.getAdvancements().getOrStartProgress(branch).isDone(),"Revoked prerequisite remained granted");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void automationRejectsSimulationAndKeepsOfflineOwner(GameTestHelper h){
        var p=player(h);h.setBlock(3,3,3,ModBlocks.CRYSTAL_BALL.get());var ball=(CrystalBallBlockEntity)h.getBlockEntity(new BlockPos(3,3,3));
        BranchProgress.operate(p,ball);ball.itemHandler.insertItem(0,new ItemStack(Items.DIAMOND),true);
        h.assertTrue(!recorded(p,"unseen_hands"),"Simulation counted as automation");
        BranchStorage.manually(()->ball.itemHandler.insertItem(0,new ItemStack(Items.DIAMOND),false));
        h.assertTrue(!recorded(p,"unseen_hands"),"Manual menu insertion counted as automation");
        ball.itemHandler.extractItem(0,1,false);h.assertTrue(recorded(p,"unseen_hands"),"Successful offline-owned automation not delivered");h.succeed();
    }
    private static CrystalBallBlockEntity orb(GameTestHelper h,int x,boolean liquid){
        var pos=new BlockPos(x,3,4);h.setBlock(pos,(liquid?CrystalStorageContent.FLUID_ORB:CrystalStorageContent.ITEM_ORB).get());return (CrystalBallBlockEntity)h.getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void sharedItemsRequireRealWithdrawalThroughOtherEnd(GameTestHelper h){
        var p=player(h);var a=orb(h,3,false);var b=orb(h,6,false);BranchProgress.operate(p,a);BranchProgress.operate(p,b);
        var marker=new ItemStack(Items.PAPER);marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(UUID.randomUUID().toString()));
        a.setFrequency(0,marker);a.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));b.setFrequency(0,marker);b.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));
        h.assertTrue(!recorded(p,"distant_echo"),"Frequency alone counted");a.setItem(0,new ItemStack(Items.EMERALD,2));
        b.setItem(0,new ItemStack(Items.EMERALD,3));
        b.itemHandler.extractItem(0,1,true);h.assertTrue(!recorded(p,"distant_echo"),"Simulated withdrawal counted");
        a.removeItem(0,1);h.assertTrue(!recorded(p,"distant_echo"),"Same endpoint counted");
        b.removeItem(0,1);h.assertTrue(recorded(p,"distant_echo"),"Other endpoint withdrawal not recorded");
        h.assertTrue(!recorded(p,"across_dimensions"),"Same dimension counted as cross-dimensional");a.detachNetwork();b.detachNetwork();h.succeed();
    }
    @GameTest(template="spell_arena") public static void sharedFluidReceiptSurvivesNetworkReload(GameTestHelper h){
        var p=player(h);var a=(LiquidCrystalBallBlockEntity)orb(h,3,true);var b=(LiquidCrystalBallBlockEntity)orb(h,6,true);BranchProgress.operate(p,b);
        var marker=new ItemStack(Items.PAPER);marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(UUID.randomUUID().toString()));a.setFrequency(0,marker);a.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));b.setFrequency(0,marker);b.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));
        a.tank.fill(new FluidStack(net.minecraft.world.level.material.Fluids.LAVA,1000),FluidAction.EXECUTE);
        var tag=CrystalStorageNetworks.get(h.getLevel()).save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.getLevel().getServer().overworld().getDataStorage().set("omnira_crystal_storage",CrystalStorageNetworks.load(tag,h.getLevel().registryAccess()));
        b.tank.drain(100,FluidAction.SIMULATE);h.assertTrue(!recorded(p,"distant_echo"),"Fluid simulation counted");
        b.tank.drain(100,FluidAction.EXECUTE);h.assertTrue(recorded(p,"distant_echo"),"Persisted fluid provenance lost");a.detachNetwork();b.detachNetwork();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=1200) public static void crossDimensionRequiresStabilizationAndWithdrawal(GameTestHelper h){
        var p=player(h);var a=orb(h,3,false);var level=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var pos=h.absolutePos(new BlockPos(6,3,4)).atY(80);level.getChunkAt(pos);var before=level.getBlockState(pos);
        level.setBlockAndUpdate(pos,CrystalStorageContent.ITEM_ORB.get().defaultBlockState());var b=(CrystalBallBlockEntity)level.getBlockEntity(pos);
        try{
            BranchProgress.operate(p,b);var marker=new ItemStack(Items.PAPER);marker.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal(UUID.randomUUID().toString()));
            a.setFrequency(0,marker);a.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));b.setFrequency(0,marker);b.setFrequency(1,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.QUARTZ));a.setItem(0,new ItemStack(Items.DIAMOND));
            h.assertTrue(b.removeItem(0,1).isEmpty()&&!recorded(p,"across_dimensions"),"Unstabilized cross-dimensional transfer counted");
            a.upgrades.setStackInSlot(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()));CrystalStorageNetworks.get(h.getLevel()).reconcile();
            h.assertTrue(!recorded(p,"across_dimensions"),"Installing upgrade alone counted");
            b.removeItem(0,1);
            h.assertTrue(recorded(p,"across_dimensions")&&recorded(p,"distant_echo"),"Successful cross-dimensional retrieval not credited");
        }finally{a.detachNetwork();b.detachNetwork();level.setBlockAndUpdate(pos,before);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void nodeTransferCountsOnlySuccessfulDelivery(GameTestHelper h){
        var p=player(h);var source=new BlockPos(3,3,3);var target=new BlockPos(8,3,3);
        h.setBlock(source,Blocks.CHEST);h.setBlock(target,Blocks.CHEST);h.setBlock(source.above(),ModBlocks.RESONANCE_CORE.get());h.setBlock(target.above(),ModBlocks.RESONANCE_CORE.get());
        var a=(ResonanceCoreBlockEntity)h.getBlockEntity(source.above());var b=(ResonanceCoreBlockEntity)h.getBlockEntity(target.above());BranchProgress.operate(p,a);
        h.assertTrue(NodeTransport.pair(a,b),"Node pair failed");NodeTransport.tick(a);h.assertTrue(!recorded(p,"unseen_hands"),"Empty node transfer counted");
        ((net.minecraft.world.Container)h.getBlockEntity(source)).setItem(0,new ItemStack(Items.DIAMOND));NodeTransport.tick(a);
        h.assertTrue(recorded(p,"unseen_hands"),"Actual resonance-node I/O not credited");NodeTransport.unpair(a);h.succeed();
    }
    @GameTest(template="spell_arena") public static void portSimulationDoesNotCountAsTransmission(GameTestHelper h){
        var p=player(h);var source=h.absolutePos(new BlockPos(3,4,3));var target=h.absolutePos(new BlockPos(8,4,8));
        h.getLevel().setBlockAndUpdate(source,Blocks.CHEST.defaultBlockState());h.getLevel().setBlockAndUpdate(target,Blocks.CHEST.defaultBlockState());
        var a=PortAutomationGameTests.port(h,source,Direction.DOWN);var b=PortAutomationGameTests.port(h,target,Direction.EAST);
        BranchProgress.operate(p,a.host);h.assertTrue(com.mcmagic.omnira.space.SpacetimePortState.pair(a,b),"Port pairing failed");
        var port=a.itemPort(a.face().getOpposite());port.insertItem(0,new ItemStack(Items.DIAMOND),true);
        h.assertTrue(!recorded(p,"adjacent_worlds")&&!recorded(p,"unseen_hands"),"Port simulation counted");
        port.insertItem(0,new ItemStack(Items.DIAMOND),false);h.assertTrue(recorded(p,"adjacent_worlds")&&recorded(p,"unseen_hands"),"Actual port transfer not credited");a.unlink();h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=180) public static void actualAnalysisAndWritingWaitForSuccessfulCompletion(GameTestHelper h){
        var p=player(h);var at=new BlockPos(4,3,4);h.setBlock(at,ModBlocks.ANALYSIS_ARTISAN_TABLE.get());
        p.moveTo(net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(at.south(2))));p.setData(ModAttachments.MANA,com.mcmagic.omnira.mana.ManaState.initial());
        var analysis=(AnalysisArtisanTableBlockEntity)h.getBlockEntity(at);analysis.setItem(0,new ItemStack(Items.DIRT,64));
        var menu=new com.mcmagic.omnira.menu.AnalysisArtisanTableMenu(1,p.getInventory(),analysis);
        h.assertTrue(menu.clickMenuButton(p,0)&&!recorded(p,"analysis"),"Analysis start counted as completion");
        h.runAfterDelay(45,()->{
            menu.broadcastChanges();h.assertTrue(recorded(p,"analysis"),"Analysis success not credited");menu.removed(p);
            h.setBlock(at,ModBlocks.CRYSTAL_PROCESSING_TABLE.get());var table=(CrystalProcessingTableBlockEntity)h.getBlockEntity(at);
            table.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get()));table.setItem(1,new ItemStack(ModItems.WATER_MICROCORE.get()));
            table.setItem(6,new ItemStack(Items.INK_SAC));table.setItem(7,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get()));
            p.setData(ModAttachments.MANA,com.mcmagic.omnira.mana.ManaState.initial());var writing=new com.mcmagic.omnira.menu.CrystalProcessingTableMenu(2,p.getInventory(),table,table);
            h.assertTrue(writing.clickMenuButton(p,0)&&!recorded(p,"spell_writing"),"Writing start counted as completion");
            h.runAfterDelay(65,()->{writing.broadcastChanges();h.assertTrue(recorded(p,"spell_writing"),"Writing success not credited");writing.removed(p);h.succeed();});
        });
    }
    @GameTest(template="spell_arena") public static void worldSpellsRequireRealChangesOrSuccessfulCast(GameTestHelper h){
        var p=player(h);var at=h.absolutePos(new BlockPos(4,3,4));p.moveTo(net.minecraft.world.phys.Vec3.atCenterOf(at.south()));
        var effect=com.mcmagic.omnira.spell.SpellEffect.utility("reload",0,0,0);
        h.getLevel().setBlockAndUpdate(at,Blocks.AIR.defaultBlockState());
        h.assertTrue(!com.mcmagic.omnira.spell.world.ReloadEffect.block(effect,p,at)&&!recorded(p,"rewrite_common_sense"),"Air counted as reload");
        h.getLevel().setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());
        h.assertTrue(com.mcmagic.omnira.spell.world.ReloadEffect.block(effect,p,at)&&recorded(p,"rewrite_common_sense"),"Actual reload not credited");
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(0,100));h.assertTrue(!com.mcmagic.omnira.spell.world.WorldRotation.cast(p)&&!recorded(p,"fleeting_world"),"Failed rotation counted");
        p.setData(ModAttachments.MANA,com.mcmagic.omnira.mana.ManaState.initial());h.assertTrue(com.mcmagic.omnira.spell.world.WorldRotation.cast(p)&&recorded(p,"fleeting_world"),"Successful rotation not credited");
        com.mcmagic.omnira.spell.world.WorldRotation.cancel(p.getUUID());h.succeed();
    }
    @GameTest(template="spell_arena") public static void poweredProcessingUsesSettledPathAndStopsOnDisconnect(GameTestHelper h){
        var p=player(h);BlockPos source=new BlockPos(4,3,2),wire=source.south(),relay=wire.south(),target=relay.south();
        h.setBlock(source,ModBlocks.MANA_ENGINE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        ((ManaEngineAccess)h.getBlockEntity(source)).engineState().inventory.setItem(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));
        h.setBlock(wire,PulseContent.WIRE.get());h.setBlock(relay,PulseContent.ORB.get());
        h.setBlock(target,ModBlocks.ARCANE_ASSEMBLY_TABLE.get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        BranchProgress.operate(p,h.getBlockEntity(target));
        h.assertTrue(!recorded(p,"first_pulse")&&!recorded(p,"branching_pulse"),"Connection alone granted processing");
        BranchProgress.processed(h.getBlockEntity(target));h.assertTrue(recorded(p,"first_pulse")&&recorded(p,"branching_pulse"),"Actual path was not credited");
        var other=player(h);BranchProgress.operate(other,h.getBlockEntity(target));h.setBlock(wire,Blocks.AIR);
        BranchProgress.processed(h.getBlockEntity(target));h.assertTrue(!recorded(other,"first_pulse")&&!recorded(other,"branching_pulse"),"Broken path still credited");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=150) public static void realPlayerChannelRequiresSuccessfulProcessing(GameTestHelper h){
        var at=new BlockPos(5,3,5);h.setBlock(at,PulseContent.WIRE.get());h.setBlock(at.south(),PulseContent.ORB.get());
        h.setBlock(at.south(2),com.mcmagic.omnira.workstation.CrystalWorkstations.BLOCKS.get(3).get().defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH));
        var p=player(h);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(h.absolutePos(at.north(2).below())));
        p.getAttribute(ModAttributes.MAX_MANA).setBaseValue(1024);p.getAttribute(ModAttributes.MANA_REGEN).setBaseValue(1);
        p.setData(ModAttachments.MANA,new com.mcmagic.omnira.mana.ManaState(100,1024));
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,net.minecraft.world.phys.Vec3.atCenterOf(h.absolutePos(at)));p.setOldPosAndRot();
        for(int tick=1;tick<=103;tick++)h.runAtTickTime(tick,()->PulseChanneling.input(p,true,h.absolutePos(at),Direction.NORTH));
        h.runAtTickTime(104,()->{
            h.assertTrue(PulseChanneling.active(p)&&!recorded(p,"personal_power"),"Charging alone counted as processing");
            var work=com.mcmagic.omnira.workstation.CrystalWorkstations.work(h.getLevel().getBlockEntity(h.absolutePos(at.south(2))));
            work.setItem(0,new ItemStack(Items.COBBLESTONE));for(int i=0;i<100;i++)work.tickDirect();
            h.assertTrue(work.getItem(2).is(Items.STONE)&&recorded(p,"personal_power"),"Completed personally powered smelting not credited");
            PulseChanneling.stop(p);h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void spellCycleUsesFullCrystalComponents(GameTestHelper h){
        var p=player(h);var grid=new ItemStack(ModItems.ARCANE_CRYSTAL_GRID.get());
        var a=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());var b=a.copy();b.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("different"));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(a,b)));
        BranchProgress.spell(p,grid,0,1);h.assertTrue(!recorded(p,"spell_cycle"),"Partial cycle counted");
        BranchProgress.spell(p,grid,1,0);h.assertTrue(recorded(p,"spell_cycle"),"Distinct component cycle failed");
        var other=player(h);BranchProgress.spell(other,grid,0,1);b.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("changed"));
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(List.of(a,b)));BranchProgress.spell(other,grid,1,0);
        h.assertTrue(!recorded(other,"spell_cycle"),"Edited grid retained old successful slots");h.succeed();
    }
    @GameTest(template="spell_arena") public static void codexAwardsOnlyCompletedNonemptyOperations(GameTestHelper h){
        var p=player(h);var center=h.absolutePos(new BlockPos(7,5,7));
        for(var at:BlockPos.betweenClosed(center.offset(-5,-5,-5),center.offset(5,5,5)))h.getLevel().setBlock(at,Blocks.AIR.defaultBlockState(),2);
        var edits=CodexEdits.get(h.getLevel());
        var shape=edits.submit(center,7,Operation.SHAPE,Direction.Axis.Y,false,ItemStack.EMPTY,new ItemStack(Items.STONE),null,p);
        h.assertTrue(shape.accepted()&&!recorded(p,"shape_at_will"),"Submission counted as completion");
        h.runAfterDelay(35,()->{
            h.assertTrue(recorded(p,"shape_at_will"),"Completed shape not credited");
            var clear=player(h);var job=edits.submit(center,7,Operation.SHAPE,Direction.Axis.Y,false,ItemStack.EMPTY,ItemStack.EMPTY,null,clear);
            h.assertTrue(job.accepted(),"Empty shape clearing rejected");
            h.runAfterDelay(35,()->{h.assertTrue(!recorded(clear,"shape_at_will"),"Air clearing granted material placement");h.succeed();});
        });
    }
}
