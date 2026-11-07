package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.*;
import com.mcmagic.omnira.item.FulfillmentStarItem;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.registry.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;

@GameTestHolder("omnira_fulfillment") @PrefixGameTestTemplate(false)
public final class FulfillmentStarGameTests {
    private static FakePlayer player(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"star-test"));
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;
    }
    private static ItemStack consume(FakePlayer p){
        var s=new ItemStack(ModItems.FULFILLMENT_STAR.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,s);
        s.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        for(int tick=0;tick<FulfillmentStarItem.ACTIVATION_TICKS;tick++)FulfillmentStarItem.advance(p);
        return s;
    }
    @GameTest(template="spell_arena") public static void activationIsDelayedAndCancellable(GameTestHelper h){
        var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.LIGHT.ordinal(),0));p.setHealth(4);
        var star=new ItemStack(ModItems.FULFILLMENT_STAR.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,star);
        star.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        for(int i=0;i<FulfillmentStarItem.SHATTER_TICK;i++)FulfillmentStarItem.advance(p);
        h.assertTrue(star.getCount()==1&&p.getHealth()==4&&!p.getData(ModAttachments.AFFINITY).perfected(),"Applied before shattering finished");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);FulfillmentStarItem.advance(p);
        h.assertTrue(star.getCount()==1&&p.getData(ModAttachments.FULFILLMENT_ACTIVATION)==0,"Cancellation consumed or stayed active");
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND,star);star.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.OFF_HAND);
        for(int i=0;i<FulfillmentStarItem.ACTIVATION_TICKS-1;i++)FulfillmentStarItem.advance(p);
        h.assertTrue(!star.isEmpty()&&p.getHealth()==4,"Applied before final tick");
        FulfillmentStarItem.advance(p);h.assertTrue(star.isEmpty()&&p.getHealth()==p.getMaxHealth(),"Offhand activation did not finish");h.succeed();
    }
    @GameTest(template="spell_arena") public static void qualificationsAreCapturedIndependentlyOnce(GameTestHelper h){
        var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.LIGHT.ordinal(),0));
        var star=new ItemStack(ModItems.FULFILLMENT_STAR.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,star);
        star.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.getData(ModAttachments.FULFILLMENT_EFFECTS)==1,"Affinity-only selection incorrect");
        // Becoming ineligible during the animation must not restart/refuse a previously accepted use.
        p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.LIGHT.ordinal(),0,true));
        ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.STORM_CURSE.get()));
        var repeated=star.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(repeated.getResult()!=net.minecraft.world.InteractionResult.FAIL&&p.getData(ModAttachments.FULFILLMENT_EFFECTS)==1,"Repeat click re-evaluated qualifications");
        for(int i=0;i<FulfillmentStarItem.ACTIVATION_TICKS;i++)FulfillmentStarItem.advance(p);
        h.assertTrue(star.isEmpty()&&ManuscriptReward.equipped(p,"curse")!=null,"Completion re-evaluated curse instead of using initial selection");
        var second=new ItemStack(ModItems.FULFILLMENT_STAR.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,second);
        second.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.getData(ModAttachments.FULFILLMENT_EFFECTS)==2,"Perfected affinity blocked eligible curse");
        for(int i=0;i<FulfillmentStarItem.ACTIVATION_TICKS;i++)FulfillmentStarItem.advance(p);
        h.assertTrue(second.isEmpty()&&ManuscriptReward.equipped(p,"curse")==null&&p.getData(ModAttachments.FULFILLMENT_EFFECTS)==0,"Curse-only completion or session cleanup failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void unstackableAndLegacyStackConsumesOnlyOne(GameTestHelper h){
        var p=player(h);var stack=new ItemStack(ModItems.FULFILLMENT_STAR.get(),2);
        h.assertTrue(stack.getMaxStackSize()==1,"Star remains stackable");
        p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.DARK.ordinal(),0));
        ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.STORM_CURSE.get()));
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);stack.getItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(p.getData(ModAttachments.FULFILLMENT_EFFECTS)==3,"Both effects not independently selected");
        for(int i=0;i<FulfillmentStarItem.ACTIVATION_TICKS+3;i++)FulfillmentStarItem.advance(p);
        h.assertTrue(stack.getCount()==1&&p.getData(ModAttachments.AFFINITY).perfected()&&ManuscriptReward.equipped(p,"curse")==null,"Legacy stack consumed twice or missed an effect");h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualShapelessRecipe(GameTestHelper h){
        String[] ids={"holy_halo","healing_dew","sharp_breath","infusion_bud","life_drain_origin","construction_matrix","lingering_thread","amplification_star","dissociation_thread"};
        var stacks=new java.util.ArrayList<ItemStack>();
        for(String id:ids)stacks.add(new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("omnira:"+id))));
        var input=net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks);
        var recipe=h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,h.getLevel()).orElseThrow();
        h.assertTrue(recipe.value().assemble(input,h.getLevel().registryAccess()).is(ModItems.FULFILLMENT_STAR.get()),"Nine materials did not craft the star");
        stacks.set(0,new ItemStack(ModItems.MINIATURE_WORLD.get()));
        h.assertTrue(!recipe.value().matches(net.minecraft.world.item.crafting.CraftingInput.of(3,3,stacks),h.getLevel()),"Reload material incorrectly accepted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void refusalAndRestoration(GameTestHelper h){
        var p=player(h);p.setHealth(4);p.getFoodData().setFoodLevel(2);
        var helmet=new ItemStack(Items.IRON_HELMET);helmet.setDamageValue(40);p.setItemSlot(EquipmentSlot.HEAD,helmet);
        var spare=new ItemStack(Items.IRON_SWORD);spare.setDamageValue(30);p.getInventory().setItem(9,spare);
        h.assertTrue(consume(p).getCount()==1&&p.getHealth()==4&&helmet.getDamageValue()==40,"Unqualified use restored or consumed");
        p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.LIGHT.ordinal(),0));
        h.assertTrue(consume(p).getCount()==0,"Qualified use did not consume exactly one");
        h.assertTrue(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20,"Health/hunger not restored");
        h.assertTrue(helmet.getDamageValue()==0&&spare.getDamageValue()==30,"Equipment repair reached inventory or missed armor");
        p.setHealth(4);h.assertTrue(consume(p).getCount()==1&&p.getHealth()==4,"Repeat use provided free restoration");h.succeed();
    }
    @GameTest(template="spell_arena") public static void affinityBonusesAndFlight(GameTestHelper h) throws Exception {
        for(var affinity:new Affinity[]{Affinity.LIGHT,Affinity.DARK,Affinity.ELEMENTAL,Affinity.DREAM,Affinity.ARCANE,Affinity.SPACETIME}){
            var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(affinity.ordinal(),0));consume(p);
            AffinityEffects.refresh(p);AffinityEffects.refresh(p);
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.MAX_MANA)-(200+affinity.capacity))<.001,"Mana bonus stacked or missing");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-(1.1+Math.max(0,affinity.power)))<.001,"Spell power incorrect");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.LIFESTEAL)-.1)<.001,"Life steal incorrect");
            h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.DAMAGE_TAKEN)-.9)<.001,"Vulnerability retained or reduction missing");
            if(affinity==Affinity.DREAM){
                p.getAbilities().flying=true;p.setSprinting(false);
                var method=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("getFlyingSpeed");method.setAccessible(true);
                h.assertTrue(Math.abs((float)method.invoke(p)-p.getAbilities().getFlyingSpeed())<.001,"Perfected flight still slowed");
            }
            p.setData(ModAttachments.AFFINITY,AffinityState.NONE);AffinityEffects.refresh(p);
            h.assertTrue(p.getAttributeValue(ModAttributes.LIFESTEAL)==0&&!p.getData(ModAttachments.PERFECTED_AFFINITY),"Removed affinity retained bonus");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void curseOnlyKeepsTalentAndCleansingStillWorks(GameTestHelper h){
        var p=player(h);
        h.assertTrue(ManuscriptReward.grant(p,"talent",new ItemStack(ModItems.ARCANIST_TALENT.get())),"Talent setup failed");
        h.assertTrue(ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.STORM_CURSE.get())),"Curse setup failed");
        var data=ArcanistFate.data(p);data.putString("Carrier","test");data.putInt("Casts",2);data.putInt("Rain",200);data.putInt("Clock",99);
        h.assertTrue(consume(p).getCount()==0,"Curse-only consumption failed");
        h.assertTrue(!ArcanistFate.storm(p)&&!data.contains("Rain")&&!data.contains("Clock"),"Storm survived");
        h.assertTrue(data.getInt("Casts")==2&&data.getString("Carrier").equals("test"),"Talent runtime lost");
        h.assertTrue(ManuscriptReward.equipped(p,"talent")!=null&&ManuscriptReward.equipped(p,"curse")==null,"Talent or curse slot incorrect");
        h.assertTrue(!com.mcmagic.omnira.reversal.TalentEssenceItem.eligible(p),"Removed curse bypassed talent exclusivity");
        h.assertTrue(!ManuscriptReward.grant(p,"talent",new ItemStack(ModItems.PSYKER_TALENT.get())),"Second talent granted");
        h.assertTrue(p.getAttributeValue(ModAttributes.LIFESTEAL)==0,"Curse-only use granted affinity benefits");
        ManuscriptReward.cleanse(p);
        h.assertTrue(ManuscriptReward.equipped(p,"talent")==null,"Cleansing failed to remove retained talent");h.succeed();
    }
    @GameTest(template="spell_arena") public static void combinedAndPersistent(GameTestHelper h){
        var p=player(h);p.setData(ModAttachments.AFFINITY,new AffinityState(Affinity.DARK.ordinal(),0));
        ManuscriptReward.grant(p,"curse",new ItemStack(ModItems.KHORNE_BLESSING.get()));
        h.assertTrue(consume(p).getCount()==0&&p.getData(ModAttachments.AFFINITY).perfected()&&ManuscriptReward.equipped(p,"curse")==null,"Combined use incomplete");
        var ops=com.mojang.serialization.JsonOps.INSTANCE;
        var encoded=AffinityState.CODEC.encodeStart(ops,p.getData(ModAttachments.AFFINITY)).getOrThrow();
        h.assertTrue(AffinityState.CODEC.parse(ops,encoded).getOrThrow().perfected(),"Perfection lost on serialization");
        var legacy=com.google.gson.JsonParser.parseString("{\"kind\":1,\"penalty\":0}");
        h.assertTrue(!AffinityState.CODEC.parse(ops,legacy).getOrThrow().perfected(),"Legacy affinity incorrectly perfected");h.succeed();
    }
}
