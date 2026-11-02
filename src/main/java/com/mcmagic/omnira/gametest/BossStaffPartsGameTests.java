package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.staff.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.combat.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_boss_parts") @PrefixGameTestTemplate(false)
public class BossStaffPartsGameTests {
    private static ItemStack staff(Item shaft,Item reinforcement){return new StaffAssembly(new ItemStack(shaft),new ItemStack(reinforcement),new ItemStack(ModItems.SPIRITUAL_CRYSTAL_TIP.get()),List.of()).create();}
    @GameTest(template="spell_arena") public static void statsAndNativeAttributes(GameTestHelper h){
        var beast=staff(ModItems.MONSTROUS_STAFF_SHAFT.get(),ModItems.GUARD_REINFORCEMENT.get());var a=StaffAssembly.of(beast);
        h.assertTrue(a.valid()&&a.slotCapacity(StaffPart.SlotKind.ENHANCEMENT)==2&&a.stats().power()==.2&&a.stats().reduction()==10,"Monstrous stats or slots");
        var remnant=StaffPart.of(new ItemStack(ModItems.REMNANT_STAFF_SHAFT.get()));h.assertTrue(remnant.enhancementSlots()==2&&remnant.power()==0&&remnant.regeneration()==.3&&remnant.reduction()==10,"Remnant stats");
        var wither=StaffPart.of(new ItemStack(ModItems.WITHERITE_REINFORCEMENT.get()));h.assertTrue(wither.power()==.1&&wither.reduction()==10&&wither.cooldownReduction()==.1,"Witherite stats");
        var fire=StaffPart.of(new ItemStack(ModItems.IGNITIUM_REINFORCEMENT.get()));h.assertTrue(fire.power()==.3&&fire.reduction()==5,"Ignitium stats");
        var curse=StaffPart.of(new ItemStack(ModItems.CURSIUM_REINFORCEMENT.get()));h.assertTrue(curse.power()==.15&&curse.reduction()==0,"Cursium stats");
        var mods=beast.getAttributeModifiers().modifiers();
        h.assertTrue(mods.stream().anyMatch(m->m.attribute().equals(ModAttributes.DAMAGE_DEALT)&&m.modifier().amount()==.1),"Missing native damage attribute");
        h.assertTrue(mods.stream().anyMatch(m->m.attribute().equals(ModAttributes.DAMAGE_TAKEN)&&m.modifier().amount()==-.1),"Missing native protection attribute");
        var leech=staff(ModItems.WOODEN_STAFF_SHAFT.get(),ModItems.CURSIUM_REINFORCEMENT.get());
        h.assertTrue(leech.getAttributeModifiers().modifiers().stream().anyMatch(m->m.attribute().equals(ModAttributes.LIFESTEAL)&&m.modifier().amount()==.1),"Missing native leech attribute");h.succeed();
    }
    @GameTest(template="spell_arena") public static void handsDoNotStackOrLeak(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"parts"));
        var s=staff(ModItems.MONSTROUS_STAFF_SHAFT.get(),ModItems.GUARD_REINFORCEMENT.get());
        p.setItemInHand(InteractionHand.MAIN_HAND,s);p.setItemInHand(InteractionHand.OFF_HAND,s.copy());BossStaffParts.refresh(p);
        h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.DAMAGE_DEALT)-1.1)<.00001&&Math.abs(p.getAttributeValue(ModAttributes.DAMAGE_TAKEN)-.9)<.00001,"Held benefits stacked");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);BossStaffParts.refresh(p);
        h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.DAMAGE_DEALT)-1.1)<.00001,"Offhand benefit lost");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);BossStaffParts.refresh(p);
        h.assertTrue(p.getAttributeValue(ModAttributes.DAMAGE_DEALT)==1&&p.getAttributeValue(ModAttributes.DAMAGE_TAKEN)==1,"Benefits leaked after removal");h.succeed();
    }
    @GameTest(template="spell_arena") public static void leechAppliesToOrdinaryMeleeAndAdds(GameTestHelper h){
        var p=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"leech"));
        var s=staff(ModItems.WOODEN_STAFF_SHAFT.get(),ModItems.CURSIUM_REINFORCEMENT.get());
        p.setItemInHand(InteractionHand.OFF_HAND,s);p.setItemInHand(InteractionHand.MAIN_HAND,s.copy());
        p.getAttribute(ModAttributes.LIFESTEAL).addTransientModifier(new AttributeModifier(ResourceLocation.parse("omnira:test_parts_leech"),.25,AttributeModifier.Operation.ADD_VALUE));
        h.assertTrue(Math.abs(HeldCombatAttributes.lifeSteal(p,p.damageSources().playerAttack(p))-.35)<.00001,"Melee leech restricted or duplicate");
        h.assertTrue(Math.abs(HeldCombatAttributes.lifeSteal(p,p.damageSources().magic())-.35)<.00001,"Unrelated magic excluded");
        var cow=h.spawn(EntityType.COW,2,2,2);p.setHealth(10);cow.hurt(p.damageSources().playerAttack(p),4);
        h.assertTrue(p.getHealth()>11.39&&p.getHealth()<11.41,"Ordinary melee did not heal additively");
        p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
        h.assertTrue(Math.abs(HeldCombatAttributes.lifeSteal(p,p.damageSources().magic())-.25)<.00001,"Removal lost unrelated leech or retained staff leech");h.succeed();
    }
    @GameTest(template="spell_arena") public static void sixCentralMaterialRecipes(GameTestHelper h){
        String[][] entries={{"monstrous_staff_shaft","monstrous_horn"},{"remnant_staff_shaft","remnant_skull"},{"ignitium_reinforcement","ignitium_ingot"},{"witherite_reinforcement","witherite_ingot"},{"cursium_reinforcement","cursium_ingot"},{"guard_reinforcement","gauntlet_of_guard"}};
        for(var e:entries){
            var recipe=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:assembly/"+e[0]));
            if(!net.neoforged.fml.ModList.get().isLoaded("cataclysm")){h.assertTrue(recipe.isEmpty(),"Optional recipe leaked");continue;}
            h.assertTrue(recipe.isPresent(),"Missing recipe: "+e[0]);var r=(AssemblyRecipe)recipe.orElseThrow().value();var c=new SimpleContainer(7);
            c.setItem(6,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("cataclysm:"+e[1]))));
            h.assertTrue(r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Central ingredient rejected: "+e[0]);
            h.assertTrue(r.result().getCount()==1&&r.result().is(BuiltInRegistries.ITEM.get(ResourceLocation.parse("omnira:"+e[0]))),"Incorrect output");
            c.setItem(0,new ItemStack(Items.STONE));h.assertTrue(!r.matches(new AssemblyRecipe.Input(c),h.getLevel()),"Extra material accepted");
        }h.succeed();
    }
}
