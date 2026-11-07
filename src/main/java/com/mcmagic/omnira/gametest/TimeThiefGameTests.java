package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.entity.TimeThiefMosquito;
import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.item.FateCurioItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("omnira_time_thief") @PrefixGameTestTemplate(false)
public final class TimeThiefGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"time_thief"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));return p;
    }
    private static FakePlayer awakened(GameTestHelper h){
        var p=player(h);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TIME_THIEVING_MONOCLE.get()));
        ModItems.TIME_THIEVING_MONOCLE.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);return p;
    }
    @GameTest(template="spell_arena") public static void grantConsumesAndCannotOverwrite(GameTestHelper h){
        var p=awakened(h);
        h.assertTrue(p.getMainHandItem().isEmpty(),"Successful activation did not consume mirror");
        h.assertTrue(ManuscriptReward.equipped(p,"talent")==FateCurioItem.Kind.TIME_THIEF&&ManuscriptReward.equipped(p,"curse")==FateCurioItem.Kind.AMON_GAZE,"Missing pair");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TIME_THIEVING_MONOCLE.get()));
        ModItems.TIME_THIEVING_MONOCLE.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1,"Rejected activation consumed mirror");
        var item=(FateCurioItem)ModItems.TIME_THIEF_TALENT.get();
        var modifiers=item.getAttributeModifiers(new top.theillusivec4.curios.api.SlotContext("talent",p,0,false,true),net.minecraft.resources.ResourceLocation.parse("omnira:test"),new ItemStack(item));
        h.assertTrue(modifiers.get(ModAttributes.LIFESTEAL).iterator().next().amount()==.2,"Wrong lifesteal");h.succeed();
    }
    @GameTest(template="spell_arena") public static void cleanseCurseKeepsTalentAndStopsWear(GameTestHelper h){
        var p=awakened(h);var a=new ItemStack(Items.IRON_SWORD);var b=new ItemStack(Items.SHIELD);
        p.setItemInHand(InteractionHand.MAIN_HAND,a);p.setItemInHand(InteractionHand.OFF_HAND,b);
        TimeThiefFate.wearHands(p);h.assertTrue(a.getDamageValue()==1&&b.getDamageValue()==1,"Both held stacks must wear once");
        ManuscriptReward.cleanseCurse(p);TimeThiefFate.wearHands(p);
        h.assertTrue(a.getDamageValue()==1&&b.getDamageValue()==1,"Cleansed curse still wears");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.TIME_THIEVING_MONOCLE.get()));
        ModItems.TIME_THIEVING_MONOCLE.get().use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().getCount()==1&&ManuscriptReward.equipped(p,"talent")==FateCurioItem.Kind.TIME_THIEF,"Empty curse bypassed talent exclusion");
        ManuscriptReward.cleanse(p);h.assertTrue(ManuscriptReward.equipped(p,"talent")==null&&ManuscriptReward.equipped(p,"curse")==null,"Cleansing retained fate");h.succeed();
    }
    @GameTest(template="spell_arena") public static void equipmentWearDoesNotDuplicateOrTouchBackpack(GameTestHelper h){
        var p=player(h);var sword=new ItemStack(Items.IRON_SWORD);var shield=new ItemStack(Items.SHIELD);var helmet=new ItemStack(Items.IRON_HELMET);
        p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.setItemInHand(InteractionHand.OFF_HAND,shield);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        var spare=new ItemStack(Items.IRON_PICKAXE);p.getInventory().setItem(8,spare);var backpack=new ItemStack(Items.IRON_AXE);p.getInventory().setItem(9,backpack);
        TimeThiefMosquito.erodeEquipment(p);
        for(var s:java.util.List.of(sword,shield,helmet,spare))h.assertTrue(s.getDamageValue()==(s.getMaxDamage()+9)/10,"Wrong 10% wear or duplicated mainhand");
        h.assertTrue(backpack.getDamageValue()==0,"Backpack was worn");sword.setDamageValue(sword.getMaxDamage()-1);TimeThiefMosquito.erodeEquipment(p);
        h.assertTrue(sword.isEmpty(),"Broken item remained");h.succeed();
    }
    @GameTest(template="spell_arena") public static void damageStealsThreeEffects(GameTestHelper h){
        var p=awakened(h);var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,4,3,4);
        cow.hurt(p.damageSources().playerAttack(p),2);
        for(var effect:java.util.List.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,net.minecraft.world.effect.MobEffects.WEAKNESS,net.minecraft.world.effect.MobEffects.WITHER)){
            var active=cow.getEffect(effect);h.assertTrue(active!=null&&active.getDuration()==200&&active.getAmplifier()==1,"Missing ten-second level-II effect");
        }
        for(var effect:java.util.List.of(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,net.minecraft.world.effect.MobEffects.DAMAGE_BOOST,net.minecraft.world.effect.MobEffects.REGENERATION)){
            var active=p.getEffect(effect);h.assertTrue(active!=null&&active.getDuration()==200&&active.getAmplifier()==1,"Missing corresponding self buff");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void immuneWitherDoesNotGrantRegeneration(GameTestHelper h){
        var p=awakened(h);var skeleton=h.spawn(net.minecraft.world.entity.EntityType.WITHER_SKELETON,4,3,4);
        skeleton.hurt(p.damageSources().playerAttack(p),2);
        h.assertTrue(!skeleton.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)&&!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Immune wither still granted regeneration");
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)&&p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),"One immunity blocked other pairs");h.succeed();
    }
    @GameTest(template="spell_arena") public static void rejectedDebuffsDoNotGrantCorrespondingBuffs(GameTestHelper h){
        var p=awakened(h);var cow=h.spawn(net.minecraft.world.entity.EntityType.COW,4,3,4);
        cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,400,2));
        cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS,400,2));
        cow.hurt(p.damageSources().playerAttack(p),2);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED)&&!p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST),"Rejected weaker effects granted buffs");
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Accepted wither failed to grant regeneration");h.succeed();
    }
    @GameTest(template="spell_arena") public static void spentMosquitoFadesWithoutDropsAfterReload(GameTestHelper h){
        var mosquito=ModEntityTypes.TIME_THIEF_MOSQUITO.get().create(h.getLevel());
        mosquito.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(5,3,5)));
        var data=new net.minecraft.nbt.CompoundTag();mosquito.addAdditionalSaveData(data);data.putBoolean("Spent",true);data.putInt("FadeTicks",1);mosquito.readAdditionalSaveData(data);
        h.getLevel().addFreshEntity(mosquito);h.assertTrue(!mosquito.attach(player(h)),"Spent mosquito reattached");
        for(int i=0;i<TimeThiefMosquito.FADE_TICKS;i++)mosquito.tick();
        h.assertTrue(mosquito.isRemoved(),"Spent mosquito did not dissolve");h.succeed();
    }
    @GameTest(template="spell_arena") public static void feedingWaitsFiveSecondsAndOnlySettlesOnce(GameTestHelper h){
        var p=player(h);h.getLevel().addNewPlayer(p);
        var mosquito=ModEntityTypes.TIME_THIEF_MOSQUITO.get().create(h.getLevel());mosquito.setPos(p.position());mosquito.setNoAi(true);
        var sword=new ItemStack(Items.IRON_SWORD);p.setItemInHand(InteractionHand.MAIN_HAND,sword);
        try {
            h.assertTrue(mosquito.attach(p),"Could not attach to player");
            for(int i=0;i<99;i++)mosquito.tick();h.assertTrue(sword.getDamageValue()==0,"Feeding settled too early");
            mosquito.tick();int wear=sword.getDamageValue();h.assertTrue(wear==(sword.getMaxDamage()+9)/10,"No five-second wear");
            for(int i=0;i<40;i++)mosquito.tick();h.assertTrue(sword.getDamageValue()==wear&&mosquito.isRemoved(),"Repeated feeding or missing fade");
        }finally{p.discard();mosquito.discard();}h.succeed();
    }
    @GameTest(template="spell_arena") public static void curseUsesVanillaUnbreaking(GameTestHelper h){
        var p=awakened(h);var sword=new ItemStack(Items.DIAMOND_SWORD);
        sword.enchant(h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),3);
        p.setItemInHand(InteractionHand.MAIN_HAND,sword);p.getRandom().setSeed(4201);
        for(int i=0;i<100;i++)TimeThiefFate.wearHands(p);
        h.assertTrue(sword.getDamageValue()>0&&sword.getDamageValue()<100,"Unbreaking was bypassed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void killedDropsPointsButSpentDoesNot(GameTestHelper h){
        var mosquito=h.spawn(ModEntityTypes.TIME_THIEF_MOSQUITO.get(),4,3,4);mosquito.setNoAi(true);
        var bounds=mosquito.getBoundingBox().inflate(2);mosquito.hurt(mosquito.damageSources().generic(),100);
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds);
        int points=drops.stream().filter(i->i.getItem().is(ModItems.TIME_WARP_POINT.get())).mapToInt(i->i.getItem().getCount()).sum();
        h.assertTrue(points>=3&&points<=5,"Killed mosquito missing 3-5 points");drops.forEach(net.minecraft.world.entity.Entity::discard);
        var spent=h.spawn(ModEntityTypes.TIME_THIEF_MOSQUITO.get(),4,3,4);var tag=new net.minecraft.nbt.CompoundTag();spent.addAdditionalSaveData(tag);tag.putBoolean("Spent",true);spent.readAdditionalSaveData(tag);
        spent.hurt(spent.damageSources().generic(),100);
        h.assertTrue(h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,bounds).isEmpty(),"Spent mosquito dropped loot");h.succeed();
    }
}
