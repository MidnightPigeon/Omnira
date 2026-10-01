package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.world.dimension.CleansingRitual;
import com.mcmagic.omnira.world.dimension.DreamRitual;
import com.mcmagic.omnira.world.dimension.ResonanceRitual;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.util.FakePlayer;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_cleansing")
@PrefixGameTestTemplate(false)
public final class CleansingRitualGameTests {
    @GameTest(template="spell_arena") public static void clearsUnknownContentsAndSlotSources(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"cleanse-generic"));
        var inventory=CuriosApi.getCuriosInventory(player).orElseThrow();
        var otherId=net.minecraft.resources.ResourceLocation.parse("omnira:future_reward_source");
        for(String slot:java.util.List.of("talent","curse")) {
            inventory.addPermanentSlotModifier(slot,otherId,3,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        }
        inventory.processSlots();
        for(String slot:java.util.List.of("talent","curse")) {
            var handler=inventory.getStacksHandler(slot).orElseThrow();
            h.assertTrue(handler.getSlots()==3,"Fixture did not create extra slots");
            for(int i=0;i<3;i++)handler.getStacks().setStackInSlot(i,new ItemStack(net.minecraft.world.item.Items.STICK));
        }
        var ring=inventory.getStacksHandler("ring").orElseThrow();
        int rings=ring.getSlots();ring.getStacks().setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        com.mcmagic.omnira.fate.FateRuntime.data(player).putInt("FutureUnknownTimer",999);
        player.getPersistentData().putInt("OmniraDistortionTicks",1199);
        ManuscriptReward.cleanse(player);
        for(String slot:java.util.List.of("talent","curse")) {
            var handler=inventory.getStacksHandler(slot).orElseThrow();
            h.assertTrue(handler.getSlots()==0&&handler.getModifiers().isEmpty(),"Unknown source or item survived cleansing");
        }
        h.assertTrue(ring.getSlots()==rings&&ring.getStacks().getStackInSlot(0).is(net.minecraft.world.item.Items.DIAMOND),"Unrelated curio slot changed");
        h.assertTrue(!player.getPersistentData().contains("OmniraFateRuntime")&&!player.getPersistentData().contains("OmniraDistortionTicks"),"Runtime state survived");
        h.assertTrue(ManuscriptReward.grant(player,"talent",new ItemStack(ModItems.PSYKER_TALENT.get())),"Could not regain cleaned slot");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void basicCoreCleansesEveryNearbyPlayer(GameTestHelper h) {
        run(h,false);
    }
    @GameTest(template="spell_arena") public static void advancedCoreCleansesEveryNearbyPlayer(GameTestHelper h) {
        run(h,true);
    }
    private static void run(GameTestHelper h,boolean advanced) {
        var level=h.getLevel();
        var core=h.absolutePos(new BlockPos(6,2,6));
        level.setBlockAndUpdate(core,(advanced?ModBlocks.ADVANCED_RITUAL_ENERGY_CORE:ModBlocks.RITUAL_ENERGY_CORE).get().defaultBlockState());
        for(int[] offset:DreamRitual.ANCHORS) {
            var pos=core.offset(offset[0],0,offset[1]);
            level.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()));
        }
        var caster=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"cleanse-caster"));
        var nearby=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"cleanse-near"));
        var outside=new FakePlayer(level,new GameProfile(UUID.randomUUID(),"cleanse-far"));
        caster.setPos(core.getX()+.5,core.getY()+1,core.getZ()+.5);
        nearby.setPos(core.getX()+2.5,core.getY()+1,core.getZ()+.5);
        outside.setPos(core.getX()+4.5,core.getY()+1,core.getZ()+.5);
        nearby.getInventory().setItem(9,new ItemStack(ModItems.INFUSED_GRIMOIRE.get()));
        for(var player:java.util.List.of(caster,nearby,outside)) {
            player.setGameMode(GameType.SURVIVAL);
            player.setHealth(20);
            player.getFoodData().setFoodLevel(20);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,400));
            ManuscriptReward.grant(player,"talent",new ItemStack(ModItems.PSYKER_TALENT.get()));
            ManuscriptReward.grant(player,"curse",new ItemStack(ModItems.KHORNE_BLESSING.get()));
            level.players().add(player);
        }
        try {
            caster.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.SPACETIME_KNOT.get()));
            h.assertTrue(!CleansingRitual.activate(level,core,caster,InteractionHand.MAIN_HAND),"Wrong activation item triggered cleansing");
            h.assertTrue(!ResonanceRitual.activate(level,core,caster,InteractionHand.MAIN_HAND),"Cleansing offerings triggered resonance");
            caster.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.PARADOX_DUST.get(),2));
            h.assertTrue(CleansingRitual.activate(level,core,caster,InteractionHand.MAIN_HAND),"Valid cleansing ritual failed");
            h.assertTrue(caster.getMainHandItem().getCount()==1,"Wrong offering consumption");
            for(var player:java.util.List.of(caster,nearby)) {
                var curios=CuriosApi.getCuriosInventory(player).orElseThrow();
                h.assertTrue(player.getActiveEffects().isEmpty(),"Potion effects survived cleansing");
                h.assertTrue(player.getHealth()==1 && player.getFoodData().getFoodLevel()==1,"Health or hunger not reduced to 1");
                h.assertTrue(curios.getStacksHandler("talent").orElseThrow().getSlots()==0
                        && curios.getStacksHandler("curse").orElseThrow().getSlots()==0,"Earned Curios slots survived cleansing");
                h.assertTrue(Math.abs(player.getAttributeValue(ModAttributes.SPELL_POWER)-1)<.001,
                        "Talent attribute bonus survived cleansing");
            }
            h.assertTrue(outside.hasEffect(MobEffects.MOVEMENT_SPEED) && outside.getHealth()==20
                    && CuriosApi.getCuriosInventory(outside).orElseThrow().getStacksHandler("talent").orElseThrow().getSlots()==1,
                    "Player outside ritual area was affected");
            var converted=nearby.getInventory().getItem(9);
            h.assertTrue(converted.is(ModItems.SANCTIFIED_GRIMOIRE.get()) || converted.is(ModItems.CORRUPTED_GRIMOIRE.get()),
                    "Carried grimoire was not transformed");
            for(int[] offset:DreamRitual.ANCHORS)
                h.assertTrue(((CrystalPedestalBlockEntity)level.getBlockEntity(core.offset(offset[0],0,offset[1])))
                        .getItem(0).is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()),"Symbolic crystal consumed");
            h.assertTrue(ManuscriptReward.grant(caster,"talent",new ItemStack(ModItems.PSYKER_TALENT.get())),"Talent slot could not be earned again");
        } finally {
            level.players().remove(caster);
            level.players().remove(nearby);
            level.players().remove(outside);
        }
        h.succeed();
    }
}
