package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.music.*;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.space.sea.VoidLighthousePiece;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_violin") @PrefixGameTestTemplate(false)
public final class VoyagerViolinGameTests {
    private static Player player(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(h.absolutePos(new BlockPos(4,3,4)).getCenter());
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.VOYAGER_VIOLIN.get()));return p;
    }
    @GameTest(template="spell_arena") public static void handAndToggleContract(GameTestHelper h){
        var p=player(h);var item=ModItems.VOYAGER_VIOLIN.get();
        item.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!VoyagerPerformance.playing(p),"Main hand started performance");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));
        item.use(h.getLevel(),p,InteractionHand.OFF_HAND);h.assertTrue(!VoyagerPerformance.playing(p),"Occupied main hand allowed");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        item.use(h.getLevel(),p,InteractionHand.OFF_HAND);h.assertTrue(VoyagerPerformance.playing(p),"Offhand start failed");
        h.assertTrue(p.getMainHandItem().isEmpty()&&p.getOffhandItem().getCount()==1,"Animation created inventory props");
        item.use(h.getLevel(),p,InteractionHand.OFF_HAND);h.assertTrue(!VoyagerPerformance.playing(p),"Second use did not stop");h.succeed();
    }
    @GameTest(template="spell_arena") public static void auraIsSphericalAndIncludesPerformer(GameTestHelper h){
        var p=player(h);
        var inside=EntityType.COW.create(h.getLevel());inside.setPos(p.position().add(10,0,0));h.getLevel().addFreshEntity(inside);
        var outside=EntityType.COW.create(h.getLevel());outside.setPos(p.position().add(8,0,8));h.getLevel().addFreshEntity(outside);
        try {
            VoyagerPerformance.toggle(p);VoyagerPerformance.advance(p);
            for(var e:new LivingEntity[]{p,inside}){
                h.assertTrue(e.hasEffect(MobEffects.REGENERATION)&&e.getEffect(MobEffects.REGENERATION).getAmplifier()==1,"Regen II missing");
                h.assertTrue(e.hasEffect(MobEffects.DAMAGE_RESISTANCE)&&e.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier()==1,"Resistance II missing");
            }
            h.assertTrue(!outside.hasEffect(MobEffects.REGENERATION),"Cube corner affected outside radius");
        }finally{VoyagerPerformance.stop(p);inside.discard();outside.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void repairIsFivePerSecondNotPerTick(GameTestHelper h){
        var p=player(h);var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.setDamageValue(14);p.getInventory().setItem(9,tool);
        var helmet=new ItemStack(Items.IRON_HELMET);helmet.setDamageValue(3);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        try {
            VoyagerPerformance.toggle(p);for(int i=0;i<19;i++)VoyagerPerformance.advance(p);
            h.assertTrue(tool.getDamageValue()==14,"Repaired before one second");VoyagerPerformance.advance(p);
            h.assertTrue(tool.getDamageValue()==9&&helmet.getDamageValue()==0,"Incorrect repair or negative damage");
            for(int i=0;i<20;i++)VoyagerPerformance.advance(p);h.assertTrue(tool.getDamageValue()==4,"Second repair wrong");
            VoyagerPerformance.stop(p);for(int i=0;i<20;i++)VoyagerPerformance.advance(p);
            h.assertTrue(tool.getDamageValue()==4,"Repair continued after stop");
        }finally{VoyagerPerformance.stop(p);}h.succeed();
    }
    @GameTest(template="spell_arena") public static void removalMainHandAndDimensionStop(GameTestHelper h){
        var p=player(h);
        VoyagerPerformance.toggle(p);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STICK));VoyagerPerformance.advance(p);
        h.assertTrue(!VoyagerPerformance.playing(p),"Main-hand replacement failed to stop");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);VoyagerPerformance.toggle(p);
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);VoyagerPerformance.advance(p);
        h.assertTrue(!VoyagerPerformance.playing(p),"Removing violin failed to stop");
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.VOYAGER_VIOLIN.get()));VoyagerPerformance.toggle(p);
        VoyagerPerformance.dimension(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent(
                p,net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER));
        h.assertTrue(!VoyagerPerformance.playing(p),"Dimension change retained performance");h.succeed();
    }
    @GameTest(template="spell_arena") public static void nestedInventoryRepair(GameTestHelper h){
        var p=player(h);var tool=new ItemStack(Items.IRON_PICKAXE);tool.setDamageValue(10);
        var box=new ItemStack(Items.SHULKER_BOX);box.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(tool)));
        p.getInventory().setItem(9,box);VoyagerPerformance.repairCarried(p);
        h.assertTrue(box.get(net.minecraft.core.component.DataComponents.CONTAINER).copyOne().getDamageValue()==5,"Nested carried item not repaired");h.succeed();
    }
    @GameTest(template="spell_arena") public static void tooltipUsesAlternatingItalicColors(GameTestHelper h){
        var lines=new java.util.ArrayList<net.minecraft.network.chat.Component>();
        ModItems.VOYAGER_VIOLIN.get().appendHoverText(new ItemStack(ModItems.VOYAGER_VIOLIN.get()),Item.TooltipContext.of(h.getLevel()),lines,TooltipFlag.NORMAL);
        h.assertTrue(lines.size()==2&&!lines.getFirst().getSiblings().isEmpty(),"Missing relic inscription or usage hint");int i=0;
        h.assertTrue(net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY).equals(lines.get(1).getStyle().getColor())&&!lines.get(1).getStyle().isItalic(),"Usage hint must be plain gray");
        for(var glyph:lines.getFirst().getSiblings()){
            var expected=net.minecraft.network.chat.TextColor.fromLegacyFormat(i++%2==0?net.minecraft.ChatFormatting.AQUA:net.minecraft.ChatFormatting.GOLD);
            h.assertTrue(glyph.getStyle().isItalic()&&expected.equals(glyph.getStyle().getColor()),"Incorrect tooltip style");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void lighthouseLootHasIndependentTenPercentRoll(GameTestHelper h){
        var balls=VoidLighthousePiece.CELLS.stream().filter(c->!c.loot().isEmpty()).toList();
        h.assertTrue(balls.size()==2&&balls.stream().allMatch(c->c.loot().equals("omnira:chests/void_lighthouse")),"Missing lighthouse loot orbs");
        var cell=balls.getFirst();h.assertTrue(cell.name().equals("omnira:crystal_ball"),"Not an item loot orb");
        var tag=com.mcmagic.omnira.shop.KirisameShopPlan.blockEntity(cell,BlockPos.ZERO,42);
        h.assertTrue(tag.getString("LootTable").equals(cell.loot()),"Loot assignment lost on placement");
        var pos=h.absolutePos(new BlockPos(5,3,5));h.getLevel().setBlockAndUpdate(pos,com.mcmagic.omnira.shop.KirisameShopPlan.state(cell));
        var ball=(com.mcmagic.omnira.block.entity.CrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);
        ball.loadWithComponents(com.mcmagic.omnira.shop.KirisameShopPlan.blockEntity(cell,pos,42),h.getLevel().registryAccess());
        h.assertTrue(ball.hasPendingLoot(),"Placed orb is not an unopened loot container");
        var table=h.getLevel().getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse(cell.loot())));
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(h.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p(h))
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int found=0;
        for(int i=0;i<2000;i++){
            var drops=table.getRandomItems(params,net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(i+1));
            int count=drops.stream().filter(s->s.is(ModItems.VOYAGER_VIOLIN.get())).mapToInt(ItemStack::getCount).sum();
            h.assertTrue(count<=1,"Multiple violins from a single roll");found+=count;
        }
        h.assertTrue(found>=150&&found<=250,"Violin chance not near ten percent: "+found);h.succeed();
    }
    private static net.minecraft.world.phys.Vec3 p(GameTestHelper h){return h.absolutePos(new BlockPos(2,2,2)).getCenter();}
}
