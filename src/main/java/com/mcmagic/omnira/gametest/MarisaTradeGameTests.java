package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.shop.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.trading.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_marisa_trades")
@PrefixGameTestTemplate(false)
public final class MarisaTradeGameTests {
    @GameTest(template="spell_arena",templateNamespace="omnira_marisa_pool_revision")
    public static void revisedPoolsAndCurrencies(GameTestHelper h) throws java.io.IOException {
        var expected=java.util.Map.of(
                "common",Set.of("minecraft:golden_carrot","minecraft:experience_bottle","minecraft:redstone","minecraft:lapis_lazuli","minecraft:gunpowder","minecraft:book","omnira:time_sapling","omnira:resonance_crystal",
                        "omnira:arcane_dust","omnira:spell_ink","omnira:grid_frame","omnira:crude_light_core","omnira:shadow_sapling","omnira:light_condensate"),
                "rare",Set.of("omnira:analysis_crystal","omnira:paradox_dust","omnira:time_seed","omnira:time_flower","omnira:crystal_ball","minecraft:diamond","omnira:crystal_broom","omnira:infused_grimoire","omnira:primordial_spell_core","omnira:light_microcore","omnira:dark_microcore","omnira:damaged_research_facility","omnira:dream_rabbit_remains"),
                "epic",Set.of("minecraft:enchanted_golden_apple","omnira:dream_spell_core","omnira:time_microcore","omnira:space_microcore","omnira:cruise_orb"));
        try(var reader=h.getLevel().getServer().getResourceManager().getResourceOrThrow(ResourceLocation.parse("omnira:marisa_trades/default.json")).openAsReader()){
            var json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
            expected.forEach((tier,items)->{
                Set<String> actual=new HashSet<>();for(var row:json.getAsJsonArray(tier))actual.add(row.getAsJsonObject().get("item").getAsString());
                h.assertTrue(actual.equals(items)&&json.getAsJsonArray(tier).size()==items.size(),"Wrong revised pool: "+tier);
            });
        }
        for(int seed=0;seed<32;seed++){
            var offers=MarisaTrades.roll(h.getLevel(),seed,11);h.assertTrue(offers.size()==6,"Daily offer count changed");
            for(int i=0;i<6;i++){
                var offer=offers.get(i);String tier=i<3?"common":i<5?"rare":"epic";
                h.assertTrue(expected.get(tier).contains(BuiltInRegistries.ITEM.getKey(offer.getResult().getItem()).toString()),"Product in wrong tier");
                h.assertTrue(offer.getMaxUses()==(i<3?3:i<5?2:1),"Daily limits changed");
                h.assertTrue(offer.getCostA().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:infused_spiritual_crystal"))),"Wrong primary currency");
                h.assertTrue(i<3?offer.getCostB().isEmpty():offer.getCostB().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(i<5?"omnira:dream_crystal_shard":"omnira:spatial_crystal_shard"))),"Wrong tier currency");
            }
        }
        h.succeed();
    }
    private static MarisaOrbBlockEntity orb(GameTestHelper h,int x){
        var p=h.absolutePos(new BlockPos(x,3,5));h.getLevel().setBlockAndUpdate(p,KirisameContent.ORB.get().defaultBlockState());
        return (MarisaOrbBlockEntity)h.getLevel().getBlockEntity(p);
    }
    private static Player player(GameTestHelper h,MarisaOrbBlockEntity orb){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setUUID(UUID.randomUUID());p.setPos(orb.getBlockPos().getCenter());return p;
    }
    private static String signature(MerchantOffers offers){
        return offers.stream().map(o->BuiltInRegistries.ITEM.getKey(o.getResult().getItem()).toString()).reduce("",(a,b)->a+";"+b);
    }
    @GameTest(template="spell_arena")
    public static void tiersAndSeededPools(GameTestHelper h){
        Set<String> samples=new HashSet<>();
        for(int seed=0;seed<24;seed++){
            var offers=MarisaTrades.roll(h.getLevel(),seed,7);h.assertTrue(offers.size()==6,"Not six daily offers");
            h.assertTrue(signature(offers).equals(signature(MarisaTrades.roll(h.getLevel(),seed,7))),"Same seed/day rerolled");
            Set<Item> products=new HashSet<>();
            for(int i=0;i<6;i++){
                var o=offers.get(i);h.assertTrue(products.add(o.getResult().getItem()),"Duplicate daily product");
                h.assertTrue(o.getMaxUses()==(i<3?3:i<5?2:1),"Incorrect tier stock cap");
                h.assertTrue(o.getCostA().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:infused_spiritual_crystal"))),"Wrong primary currency");
                if(i<3)h.assertTrue(o.getCostB().isEmpty(),"Common offer has secondary currency");
                else h.assertTrue(o.getCostB().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse(i<5?"omnira:dream_crystal_shard":"omnira:spatial_crystal_shard"))),"Wrong secondary currency");
            }
            samples.add(signature(offers));
        }
        h.assertTrue(samples.size()>1,"Independent players cannot obtain different lists");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void playerStockSharedEverywhere(GameTestHelper h){
        var a=orb(h,4);var b=orb(h,8);var p=player(h,a);var q=player(h,a);
        var one=a.connect(p);var two=b.connect(p);var other=a.connect(q);
        h.assertTrue(one.getOffers()==two.getOffers(),"Different orbs own different stock");
        h.assertTrue(one.getOffers()!=other.getOffers(),"Different players share stock");
        h.assertTrue(one.getTradingPlayer()==p&&other.getTradingPlayer()==q,"One orb cannot serve two players");
        one.notifyTrade(one.getOffers().getFirst());
        h.assertTrue(two.getOffers().getFirst().getUses()==1&&other.getOffers().getFirst().getUses()==0,"Purchase counter leaked or split");
        var nether=h.getLevel().getServer().getLevel(Level.NETHER);
        h.assertTrue(MarisaTradeLedger.get(h.getLevel())==MarisaTradeLedger.get(nether),"Ledger is dimension-local");
        h.assertTrue(MarisaTradeLedger.get(nether).stock(nether,p.getUUID()).offers()==one.getOffers(),"Cross-dimension visit rerolls");
        one.close();two.close();other.close();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void saveReloadAndNextDay(GameTestHelper h){
        var level=h.getLevel();var id=UUID.randomUUID();var ledger=new MarisaTradeLedger();var stock=ledger.stock(level,id);
        stock.offers().getFirst().increaseUses();ledger.setDirty();var tag=ledger.save(new CompoundTag(),level.registryAccess());
        var restored=MarisaTradeLedger.load(tag,level.registryAccess());var same=restored.stock(level,id);
        h.assertTrue(same.offers().getFirst().getUses()==1&&signature(same.offers()).equals(signature(stock.offers())),"Reload loses stock or selection");
        tag.getList("Players",Tag.TAG_COMPOUND).getCompound(0).putLong("Day",MarisaTradeLedger.day(level)-1);
        var renewed=MarisaTradeLedger.load(tag,level.registryAccess()).stock(level,id);
        h.assertTrue(renewed.day()==MarisaTradeLedger.day(level)&&renewed.offers().stream().allMatch(o->o.getUses()==0),"New day did not reset stock");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void paidTradingLimitsAndAccess(GameTestHelper h){
        var orb=orb(h,5);var p=player(h,orb);var merchant=orb.connect(p);var menu=new MerchantMenu(7,p.getInventory(),merchant);p.containerMenu=menu;
        for(int i:new int[]{0,3,5}){
            var offer=merchant.getOffers().get(i);menu.setSelectionHint(i);
            for(int n=0;n<offer.getMaxUses();n++){
                menu.getSlot(0).set(offer.getCostA().copy());menu.getSlot(1).set(offer.getCostB().copy());
                h.assertTrue(!menu.quickMoveStack(p,2).isEmpty(),"Paid shift purchase failed");
                h.assertTrue(menu.getSlot(0).getItem().isEmpty()&&menu.getSlot(1).getItem().isEmpty(),"Payment not fully consumed");
            }
            menu.getSlot(0).set(offer.getCostA().copy());menu.getSlot(1).set(offer.getCostB().copy());
            h.assertTrue(menu.quickMoveStack(p,2).isEmpty()&&offer.isOutOfStock(),"Exceeded daily tier limit");
            menu.getSlot(0).set(ItemStack.EMPTY);menu.getSlot(1).set(ItemStack.EMPTY);
        }
        p.setPos(orb.getBlockPos().getCenter().add(20,0,0));h.assertTrue(!menu.stillValid(p),"Remote menu remains usable");
        p.setPos(orb.getBlockPos().getCenter());h.getLevel().removeBlock(orb.getBlockPos(),false);h.assertTrue(!menu.stillValid(p),"Removed orb still trades");h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void harvestCannotResetPlayerStock(GameTestHelper h){
        var orb=orb(h,5);var p=player(h,orb);var session=orb.connect(p);session.notifyTrade(session.getOffers().getFirst());
        var level=h.getLevel();var normal=Block.getDrops(orb.getBlockState(),level,orb.getBlockPos(),orb);
        h.assertTrue(normal.size()==1&&normal.getFirst().is(ModBlocks.CRYSTAL_BALL.get().asItem())&&!normal.getFirst().has(DataComponents.BLOCK_ENTITY_DATA),"Normal harvest not blank");
        var pick=new ItemStack(Items.DIAMOND_PICKAXE);pick.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SILK_TOUCH),1);
        var silk=Block.getDrops(orb.getBlockState(),level,orb.getBlockPos(),orb,null,pick);
        h.assertTrue(silk.size()==1&&silk.getFirst().is(KirisameContent.ORB.get().asItem()),"Silk lost special orb");
        level.removeBlock(orb.getBlockPos(),false);var replacement=orb(h,5);var reopened=replacement.connect(p);
        h.assertTrue(reopened.getOffers().getFirst().getUses()==1,"Replacing orb resets stock");
        var legacy=new CompoundTag();legacy.putLong("Day",0);legacy.put("Offers",new CompoundTag());replacement.loadWithComponents(legacy,level.registryAccess());
        h.assertTrue(replacement.connect(p).getOffers()==reopened.getOffers(),"Legacy orb NBT overrides player stock");replacement.close();h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void openMenuExpiresAtDayBoundary(GameTestHelper h){
        var orb=orb(h,5);var p=player(h,orb);var session=orb.connect(p);var menu=new MerchantMenu(7,p.getInventory(),session);p.containerMenu=menu;
        var world=h.getLevel().getServer().overworld();long now=world.getGameTime();
        var clock=(net.minecraft.world.level.storage.ServerLevelData)world.getLevelData();
        try{
            clock.setGameTime((Math.floorDiv(now,24000)+1)*24000);
            h.assertTrue(!menu.stillValid(p),"Yesterday's menu remains valid");orb.tick();
            var reopened=orb.connect(p);h.assertTrue(reopened.getOffers().stream().allMatch(o->o.getUses()==0),"Reopened menu carries yesterday's uses");reopened.close();
        }finally{clock.setGameTime(now);orb.close();}
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void showcasePacketAndMotion(GameTestHelper h){
        var level=h.getLevel();var stock=new MarisaTradeLedger().stock(level,UUID.randomUUID());
        var payload=new com.mcmagic.omnira.network.MarisaGoodsPayload(stock.day(),stock.offers().stream().map(o->o.getResult()).toList());
        var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
        try{
            com.mcmagic.omnira.network.MarisaGoodsPayload.CODEC.encode(buffer,payload);
            var decoded=com.mcmagic.omnira.network.MarisaGoodsPayload.CODEC.decode(buffer);
            h.assertTrue(decoded.day()==stock.day()&&decoded.goods().size()==6,"Incorrect showcase payload");
            for(int i=0;i<6;i++)h.assertTrue(ItemStack.matches(decoded.goods().get(i),stock.offers().get(i).getResult()),"Showcase differs from daily offer");
            h.assertTrue(!buffer.isReadable(),"Extra bytes left in showcase packet");
        }finally{buffer.release();}
        var center=new net.minecraft.world.phys.Vec3(.5,.58,.5);
        for(int t=0;t<2400;t+=7)for(int i=0;i<6;i++){
            var p=MarisaDisplayMotion.position(i,t+.5);
            h.assertTrue(p.distanceTo(center)<.192,"Orbit exits globe interior");
            for(int j=0;j<i;j++)h.assertTrue(p.distanceTo(MarisaDisplayMotion.position(j,t+.5))>.179,"Miniature orbits overlap");
        }
        h.assertTrue(MarisaDisplayMotion.position(0,0).distanceTo(MarisaDisplayMotion.position(0,40))>.05,"Showcase is static");h.succeed();
    }
}
