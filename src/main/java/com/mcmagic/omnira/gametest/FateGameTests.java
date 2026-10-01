package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.fate.FateEffects;
import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.item.FateCurioItem;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModAttributes;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;

@GameTestHolder("omnira_fate")
@PrefixGameTestTemplate(false)
public final class FateGameTests {
    @GameTest(template="spell_arena") public static void talentTooltipKeepsValuesWithoutCuriosHeader(GameTestHelper h) {
        var stack=new ItemStack(ModItems.PSYKER_TALENT.get());
        java.util.List<net.minecraft.network.chat.Component> lines=java.util.List.of(net.minecraft.network.chat.Component.empty(),
                net.minecraft.network.chat.Component.translatable("curios.modifiers.talent"),
                net.minecraft.network.chat.Component.literal("+20% mana regeneration"),
                net.minecraft.network.chat.Component.literal("+20% spell power"));
        var result=((FateCurioItem)stack.getItem()).getAttributesTooltip(lines,
                net.minecraft.world.item.Item.TooltipContext.EMPTY,stack);
        h.assertTrue(result.size()==3 && result.get(2)==lines.get(3) && !result.contains(lines.get(1)),
                "Talent tooltip lost an attribute or retained the Curios header");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void khorneTooltipUsesTwoAttributes(GameTestHelper h) {
        var stack=new ItemStack(ModItems.KHORNE_BLESSING.get());
        var item=(FateCurioItem)stack.getItem();
        var context=new top.theillusivec4.curios.api.SlotContext("curse",null,0,false,true);
        var modifiers=item.getAttributeModifiers(context,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","khorne_test"),stack);
        h.assertTrue(modifiers.get(ModAttributes.DAMAGE_DEALT).stream().anyMatch(modifier->modifier.amount()==.2),
                "Outgoing damage attribute missing");
        h.assertTrue(modifiers.get(ModAttributes.DAMAGE_TAKEN).stream().anyMatch(modifier->modifier.amount()==.5),
                "Incoming damage attribute missing");
        java.util.List<net.minecraft.network.chat.Component> lines=java.util.List.of(
                net.minecraft.network.chat.Component.translatable("curios.modifiers.curse"),
                net.minecraft.network.chat.Component.literal("+20% damage dealt"),
                net.minecraft.network.chat.Component.literal("+50% damage taken"));
        var result=item.getAttributesTooltip(lines,net.minecraft.world.item.Item.TooltipContext.EMPTY,stack);
        h.assertTrue(result.size()==2 && result.get(0)==lines.get(1) && result.get(1)==lines.get(2),
                "Curse header remained or damage attributes were hidden");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void manuscriptGrantsOnce(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"fate-test"));
        var curios=CuriosApi.getCuriosInventory(player).orElseThrow();
        h.assertTrue(curios.getStacksHandler("ring").orElseThrow().getSlots()>=2,"Missing generic ring slots");
        h.assertTrue(curios.getStacksHandler("charm").orElseThrow().getSlots()>=2,"Missing generic charm slots");
        h.assertTrue(curios.getStacksHandler("talent").orElseThrow().getSlots()==0,"Talent slot appeared before reward");
        h.assertTrue(curios.getStacksHandler("curse").orElseThrow().getSlots()==0,"Curse slot appeared before reward");
        ManuscriptReward.finish(player);
        h.assertTrue(curios.getStacksHandler("talent").orElseThrow().getSlots()==0,"Reward without manuscript");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(ModItems.TRAVELER_MANUSCRIPT.get()));
        ManuscriptReward.finish(player);
        var talent=curios.getStacksHandler("talent").orElseThrow();
        var curse=curios.getStacksHandler("curse").orElseThrow();
        h.assertTrue(talent.getSlots()==1 && talent.getStacks().getStackInSlot(0).is(ModItems.PSYKER_TALENT.get()),"Talent reward missing");
        h.assertTrue(curse.getSlots()==1 && !curse.getStacks().getStackInSlot(0).isEmpty(),"Curse reward missing");
        var first=curse.getStacks().getStackInSlot(0).getItem();
        ManuscriptReward.finish(player);
        h.assertTrue(curse.getSlots()==1 && curse.getStacks().getStackInSlot(0).is(first),"Rereading replaced the curse");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void tzeentchAppliesOnlyToOneCast(GameTestHelper h) {
        var player=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"tzeentch-test"));
        h.assertTrue(ManuscriptReward.grant(player,"curse",new ItemStack(ModItems.TZEENTCH_BLESSING.get())),"Could not create curse slot");
        boolean triggered=false;
        for(int i=0;i<100;i++) {
            player.setHealth(20);
            double power=FateEffects.spellPower(player,1.3);
            h.assertTrue(power==1.3 || power==2.3,"Incorrect temporary spell power");
            if(power==2.3) {
                h.assertTrue(player.getHealth()==10,"Life cost missing");
                triggered=true;
            } else h.assertTrue(player.getHealth()==20,"Life cost without bonus");
        }
        h.assertTrue(triggered,"Curse never triggered");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void khorneUsesDamagePipeline(GameTestHelper h) {
        var attacker=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"khorne-test"));
        var victim=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"victim-test"));
        h.assertTrue(ManuscriptReward.grant(attacker,"curse",new ItemStack(ModItems.KHORNE_BLESSING.get())),"Could not equip blessing");
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(attacker));
        h.assertTrue(Math.abs(attacker.getAttributeValue(ModAttributes.DAMAGE_DEALT)-1.2)<.001,
                "Equipped blessing did not apply outgoing attribute");
        var event=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(victim,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(victim.damageSources().playerAttack(attacker),10));
        FateEffects.damage(event);
        h.assertTrue(Math.abs(event.getNewDamage()-12)<.001,"Outgoing damage bonus missing");
        h.succeed();
    }

    @GameTest(template="spell_arena") public static void incomingAndTotalBonusesAdd(GameTestHelper h) {
        var victim=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"stacking-test"));
        h.assertTrue(ManuscriptReward.grant(victim,"curse",new ItemStack(ModItems.KHORNE_BLESSING.get())),"Could not equip blessing");
        victim.setData(com.mcmagic.omnira.registry.ModAttachments.AFFINITY,
                new com.mcmagic.omnira.mana.AffinityState(com.mcmagic.omnira.mana.Affinity.DREAM.ordinal(),0));
        com.mcmagic.omnira.mana.AffinityEffects.refresh(victim);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(victim));
        h.assertTrue(Math.abs(victim.getAttributeValue(ModAttributes.DAMAGE_TAKEN)-2)<.001,
                "Curse and affinity must add to the same damage attribute");
        var event=new net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre(victim,
                new net.neoforged.neoforge.common.damagesource.DamageContainer(victim.damageSources().magic(),10));
        FateEffects.damage(event);
        h.assertTrue(Math.abs(event.getNewDamage()-20)<.001,"Dream and Khorne should add to +100%, not multiply");
        h.succeed();
    }
}
