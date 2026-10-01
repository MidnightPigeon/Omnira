package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.WaymarkBlock;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.RecallCrystalItem;
import com.mcmagic.omnira.mana.ManaState;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.travel.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_recall")
@PrefixGameTestTemplate(false)
public final class RecallGameTests {
    private static FakePlayer player(GameTestHelper h,Item item) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"recall-test")) {
            @Override public boolean isInvulnerableTo(DamageSource source) {return false;}
            // FakePlayer's network listener intentionally ignores teleport packets.
            @Override public boolean teleportTo(net.minecraft.server.level.ServerLevel level,double x,double y,double z,
                    java.util.Set<net.minecraft.world.entity.RelativeMovement> relative,float yaw,float pitch) {
                setServerLevel(level);setPos(x,y,z);return true;
            }
        };
        player.setPos(h.absoluteVec(new Vec3(1.5,2,1.5)));
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
        player.setData(ModAttachments.MANA,new ManaState(500,500));return player;
    }
    private static WaymarkBlockEntity mark(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(7,2,7));
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) level.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
        level.setBlock(pos,ModBlocks.WAYMARK.get().defaultBlockState(),2);
        level.setBlock(pos.above(),ModBlocks.WAYMARK.get().defaultBlockState().setValue(WaymarkBlock.HALF,DoubleBlockHalf.UPPER),3);
        var mark=(WaymarkBlockEntity)level.getBlockEntity(pos);mark.rename("Recall test");return mark;
    }
    @GameTest(template="spell_arena",timeoutTicks=90)
    public static void threeSecondsThenChargeOnce(GameTestHelper h) {
        var mark=mark(h);var player=player(h,ModItems.RECALL_CRYSTAL.get());var origin=player.position();
        mark.discover(player);
        h.assertTrue(RecallTravel.start(player,InteractionHand.MAIN_HAND,mark.id()),"Valid recall refused");
        h.runAfterDelay(59,()->{
            RecallTravel.advance(player);
            h.assertTrue(player.position().equals(origin) && player.getData(ModAttachments.MANA).current()==500,"Teleported or charged too early");
        });
        h.runAfterDelay(61,()->{
            RecallTravel.advance(player);
            h.assertTrue(player.position().distanceToSqr(mark.getBlockPos().getCenter())<30,"Did not arrive near waymark");
            h.assertTrue(player.getData(ModAttachments.MANA).current()==400,"Same dimension cost incorrect");
            h.assertTrue(player.getMainHandItem().is(ModItems.RECALL_CRYSTAL.get()),"Reusable crystal consumed");
            RecallTravel.advance(player);
            h.assertTrue(player.getData(ModAttachments.MANA).current()==400,"Charged twice");h.succeed();
        });
    }
    @GameTest(template="spell_arena")
    public static void interruptionsAndDestroyedWaymark(GameTestHelper h) {
        var mark=mark(h);var player=player(h,ModItems.RECALL_CRYSTAL.get());
        mark.discover(player);
        RecallTravel.start(player,InteractionHand.MAIN_HAND,mark.id());player.setPos(player.position().add(.1,0,0));RecallTravel.advance(player);
        h.assertTrue(player.getData(ModAttachments.RECALL_TICKS)==0 && player.getData(ModAttachments.MANA).current()==500,"Movement did not cancel without charge");
        RecallTravel.start(player,InteractionHand.MAIN_HAND,mark.id());
        player.hurt(player.damageSources().genericKill(),1);
        h.assertTrue(player.getData(ModAttachments.RECALL_TICKS)==0,"Damage did not interrupt");
        var id=mark.id();h.getLevel().destroyBlock(mark.getBlockPos().above(),false);
        h.assertTrue(!h.getLevel().getBlockState(mark.getBlockPos()).is(ModBlocks.WAYMARK.get()),"Bottom half survived removal of top");
        h.assertTrue(WaymarkDirectory.get(h.getLevel()).get(id)==null,"Destroyed waymark remained registered");
        h.assertTrue(!RecallTravel.start(player,InteractionHand.MAIN_HAND,id),"Destroyed destination accepted");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=90)
    public static void enhancedRecallIsFreeLocally(GameTestHelper h) {
        var mark=mark(h);var player=player(h,ModItems.ENHANCED_RECALL_CRYSTAL.get());
        mark.discover(player);
        player.setData(ModAttachments.MANA,new ManaState(0,500));
        h.assertTrue(RecallTravel.start(player,InteractionHand.MAIN_HAND,mark.id()),"Free recall required mana");
        h.runAfterDelay(61,()->{
            RecallTravel.advance(player);h.assertTrue(player.getData(ModAttachments.MANA).current()==0,"Free recall spent mana");
            h.assertTrue(player.position().distanceToSqr(mark.getBlockPos().getCenter())<30,"Free recall failed");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=110)
    public static void crossDimensionCosts(GameTestHelper h) {
        var destination=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        var pos=new BlockPos(32,100,32);destination.getChunkAt(pos);
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
            destination.setBlockAndUpdate(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState());
            for(int y=0;y<=3;y++) destination.setBlockAndUpdate(pos.offset(x,y,z),Blocks.AIR.defaultBlockState());
        }
        destination.setBlock(pos,ModBlocks.WAYMARK.get().defaultBlockState(),2);
        destination.setBlock(pos.above(),ModBlocks.WAYMARK.get().defaultBlockState().setValue(WaymarkBlock.HALF,DoubleBlockHalf.UPPER),3);
        var mark=(WaymarkBlockEntity)destination.getBlockEntity(pos);mark.rename("Cross-dimension test");
        var normal=player(h,ModItems.RECALL_CRYSTAL.get());var enhanced=player(h,ModItems.ENHANCED_RECALL_CRYSTAL.get());
        mark.discover(normal);mark.discover(enhanced);
        h.assertTrue(RecallTravel.start(normal,InteractionHand.MAIN_HAND,mark.id()),"Cross dimension start failed");
        h.assertTrue(RecallTravel.start(enhanced,InteractionHand.MAIN_HAND,mark.id()),"Enhanced cross dimension start failed");
        h.runAfterDelay(61,()->{
            RecallTravel.advance(normal);RecallTravel.advance(enhanced);
            h.assertTrue(normal.serverLevel()==destination && enhanced.serverLevel()==destination,"Wrong destination dimension");
            h.assertTrue(normal.getData(ModAttachments.MANA).current()==200,"Normal cross dimension cost is not 300");
            h.assertTrue(enhanced.getData(ModAttachments.MANA).current()==400,"Enhanced cross dimension cost is not 100");
            destination.destroyBlock(pos,false);h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void personalDiscoveryAndGapNames(GameTestHelper h) {
        var directory=WaymarkDirectory.get(h.getLevel());
        var first=player(h,ModItems.RECALL_CRYSTAL.get());
        var second=player(h,ModItems.RECALL_CRYSTAL.get());
        var mark=mark(h);
        h.assertTrue(directory.list(first.getUUID()).isEmpty(),"Chunk registration discovered a waymark");
        h.assertTrue(!RecallTravel.start(first,InteractionHand.MAIN_HAND,mark.id()),"Undiscovered recall accepted");
        mark.discover(first);
        h.assertTrue(mark.name().equals("失落点位1"),"First natural name is wrong");
        h.assertTrue(directory.list(first.getUUID()).size()==1 && directory.list(second.getUUID()).isEmpty(),"Discoveries leaked between players");
        var ids=new UUID[4];ids[0]=mark.id();
        for(int i=1;i<4;i++) {
            ids[i]=UUID.randomUUID();
            directory.put(new WaymarkDirectory.Entry(ids[i],mark.entry().dimension(),mark.getBlockPos().offset(i,0,0),directory.nextLostName(first.getUUID())));
            directory.discover(first.getUUID(),ids[i]);
        }
        directory.forget(first.getUUID(),ids[0]);directory.forget(first.getUUID(),ids[2]);
        h.assertTrue(directory.nextLostName(first.getUUID()).equals("失落点位1"),"Deleted first name was not reused");
        mark.discover(first);
        h.assertTrue(directory.nextLostName(first.getUUID()).equals("失落点位3"),"Deleted middle name was not reused");
        mark.rename("Home");
        h.assertTrue(directory.nextLostName(first.getUUID()).equals("失落点位1"),"Rename did not free number");
        mark.discover(second);directory.forget(first.getUUID(),mark.id());mark.register();
        h.assertTrue(!directory.knows(first.getUUID(),mark.id()) && directory.knows(second.getUUID(),mark.id()),"Forget affected another player or reload rediscovered the mark");
        h.assertTrue(directory.get(mark.id())!=null,"Forget destroyed global destination");
        var saved=directory.save(new net.minecraft.nbt.CompoundTag(),h.getLevel().registryAccess());
        h.assertTrue(!saved.getList("Discoveries",10).isEmpty(),"Discoveries were not persisted");
        var loaded=WaymarkDirectory.load(saved,h.getLevel().registryAccess());
        h.assertTrue(!loaded.knows(first.getUUID(),mark.id()) && loaded.knows(second.getUUID(),mark.id()),"Personal discovery state did not survive reload");
        mark.placedByPlayer();mark.rename("Player home");mark.discover(first);
        h.assertTrue(mark.name().equals("Player home"),"Player-placed waymark received a lost-location name");
        for(var id:ids) directory.remove(id);
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=230)
    public static void alternatingUpgradeAssembles(GameTestHelper h) {
        h.setBlock(4,2,4,ModBlocks.ARCANE_ASSEMBLY_TABLE.get());
        var table=((AssemblyAccess)h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(4,2,4)))).assembly();
        for(int parity=0;parity<2;parity++) {
            table.setItem(6,new ItemStack(ModItems.RECALL_CRYSTAL.get()));
            for(int i=0;i<6;i++) table.setItem(i,new ItemStack((i+parity)%2==0?Items.ENDER_EYE:Items.ENDER_PEARL));
            h.assertTrue(table.recipe()!=null && table.recipe().value().getResultItem(h.getLevel().registryAccess()).is(ModItems.ENHANCED_RECALL_CRYSTAL.get()),"Alternating ring not recognized");
        }
        var normal=(RecallCrystalItem)ModItems.RECALL_CRYSTAL.get();var enhanced=(RecallCrystalItem)ModItems.ENHANCED_RECALL_CRYSTAL.get();
        h.assertTrue(normal.baseCost(false)==100 && normal.baseCost(true)==300 && enhanced.baseCost(false)==0 && enhanced.baseCost(true)==100,"Cost table differs from design");
        var smith=player(h,ModItems.RECALL_CRYSTAL.get());smith.setData(ModAttachments.MANA,new ManaState(2000,2000));
        for(int i=0;i<20;i++) h.runAfterDelay(i*10+1,()->{if(!table.finished()) h.assertTrue(table.strike(smith),"Upgrade strike failed");});
        h.runAfterDelay(205,()->{
            h.assertTrue(table.finished() && table.getItem(6).is(ModItems.ENHANCED_RECALL_CRYSTAL.get()),"Upgrade did not finish");
            for(int i=0;i<6;i++) h.assertTrue(table.getItem(i).isEmpty(),"Upgrade left materials behind");h.succeed();
        });
    }
}
