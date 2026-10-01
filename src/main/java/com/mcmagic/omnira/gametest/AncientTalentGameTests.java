package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.reversal.*;
import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.item.FateCurioItem.Kind;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;

@GameTestHolder("omnira_reversal_extra")
@PrefixGameTestTemplate(false)
public final class AncientTalentGameTests {
    @GameTest(template="spell_arena") public static void steadyNightVision(GameTestHelper h){
        var p=player(h);ManuscriptReward.grant(p,"talent",new ItemStack(ReversalContent.SKY.get()));
        var effect=net.minecraft.world.effect.MobEffects.NIGHT_VISION;
        p.tickCount=1;AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.getEffect(effect)!=null&&p.getEffect(effect).getDuration()==600,"Night vision must apply immediately for 30 seconds");
        p.removeEffect(effect);p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect,401,0));
        p.tickCount=199;AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.getEffect(effect).getDuration()==401,"Night vision refreshed before ten seconds");
        p.tickCount=200;AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.getEffect(effect).getDuration()==600,"Ten-second renewal failed");
        ManuscriptReward.cleanse(p);p.removeAllEffects();p.tickCount=400;
        AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(!p.hasEffect(effect),"Cleansed talent renewed night vision");h.succeed();
    }
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ancient-talent"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;}
    @GameTest(template="spell_arena") public static void grantRejectAndCleanse(GameTestHelper h){
        var p=player(h);var food=new ItemStack(ReversalContent.REX.get(),2);ReversalContent.REX.get().finishUsingItem(food,h.getLevel(),p);
        h.assertTrue(food.getCount()==1&&ManuscriptReward.equipped(p,"talent")==Kind.LAND_KING&&ManuscriptReward.equipped(p,"curse")==Kind.TIME_DISTORTION,"Grant not atomic or incorrect");
        ReversalContent.MOSA.get().finishUsingItem(food,h.getLevel(),p);h.assertTrue(food.getCount()==1&&ManuscriptReward.equipped(p,"talent")==Kind.LAND_KING,"Existing talent overwritten or item consumed");
        ManuscriptReward.cleanse(p);h.assertTrue(ManuscriptReward.equipped(p,"talent")==null&&ManuscriptReward.equipped(p,"curse")==null,"Cleanse missed ancient slots");
        var fluid=new ItemStack(ReversalContent.TALENT_FLUID.get());ReversalContent.TALENT_FLUID.get().finishUsingItem(fluid,h.getLevel(),p);
        h.assertTrue(fluid.isEmpty()&&ManuscriptReward.equipped(p,"talent")==Kind.PSYKER&&ManuscriptReward.equipped(p,"curse")==Kind.TIME_DISTORTION,"Psychic fluid failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void memoryBlockState(GameTestHelper h){
        var source=com.mcmagic.omnira.item.MemoryCubeBlockItem.withState(new ItemStack(ModItems.MEMORY_CUBE.get()),true);
        var r=h.getLevel().getRecipeManager().getRecipeFor(ReversalContent.TYPE.get(),new net.minecraft.world.item.crafting.SingleRecipeInput(source),h.getLevel()).orElseThrow().value();
        var output=r.outputs(source).getFirst();h.assertTrue(!com.mcmagic.omnira.item.MemoryCubeBlockItem.peaceful(output)&&com.mcmagic.omnira.item.MemoryCubeBlockItem.peaceful(source),"State reversal mutated input or failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void groundedRegeneration(GameTestHelper h){
        var p=player(h);ManuscriptReward.grant(p,"talent",new ItemStack(ReversalContent.LAND.get()));p.setOnGround(true);
        AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Grounded regeneration absent");p.setOnGround(false);AncientTalents.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION),"Airborne regeneration persisted");h.succeed();
    }
}
