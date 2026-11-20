package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.combat.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.UUID;

@GameTestHolder("omnira_unbound_core") @PrefixGameTestTemplate(false)
public final class UnboundCoreGameTests {
    private static FakePlayer player(GameTestHelper h){return new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"unbound-core"));}
    private static void equip(FakePlayer p,ItemStack stack){
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p).orElseThrow().setEquippedCurio("spell_core",0,stack);
        PrimarySpellAccessory.tick(new PlayerTickEvent.Post(p));
    }
    @GameTest(template="spell_arena") public static void ordinaryAssemblyAndAffinityOffering(GameTestHelper h){
        var r=h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:assembly/unbound_mana_core")).orElseThrow().value();
        h.assertTrue(r instanceof com.mcmagic.omnira.recipe.AssemblyRecipe,"Not ordinary assembly");
        var recipe=(com.mcmagic.omnira.recipe.AssemblyRecipe)r;var c=new net.minecraft.world.SimpleContainer(7);
        for(int i=0;i<6;i++)c.setItem(i,new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()));c.setItem(6,new ItemStack(ModItems.FULFILLMENT_STAR.get()));
        var input=new com.mcmagic.omnira.recipe.AssemblyRecipe.Input(c);
        h.assertTrue(recipe.matches(input,h.getLevel()) && recipe.assemble(input,h.getLevel().registryAccess()).is(ModItems.UNBOUND_MANA_CORE.get()),"Recipe layout or result incorrect");
        c.setItem(0,ItemStack.EMPTY);h.assertTrue(!recipe.matches(input,h.getLevel()),"Missing cluster accepted");
        h.assertTrue(Affinity.offering(new ItemStack(ModItems.UNBOUND_MANA_CORE.get()))==Affinity.CHAOS && Affinity.offering(new ItemStack(ModItems.UNORDERED_ELEMENT_CLUSTER.get()))==Affinity.NONE,"Chaos ritual retained obsolete offering");h.succeed();
    }
    @GameTest(template="spell_arena") public static void slotAttributesAndLifesteal(GameTestHelper h){
        var p=player(h);var stack=new ItemStack(ModItems.UNBOUND_MANA_CORE.get());
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
        h.assertTrue(!UnboundCoreEffects.equipped(p),"Held core applied drawbacks");equip(p,stack);
        PrimarySpellAccessory.tick(new PlayerTickEvent.Post(p));
        h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-1.5)<.001 && Math.abs(p.getAttributeValue(ModAttributes.MANA_REGEN)-1.25)<.001,"Core attributes missing or duplicated");
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100 && p.getAttributeValue(ModAttributes.COST_REDUCTION)==0,"Core inherited unwanted wearer stats");
        p.getAttribute(ModAttributes.LIFESTEAL).setBaseValue(1);
        h.assertTrue(HeldCombatAttributes.lifeSteal(p,p.damageSources().playerAttack(p))==0,"Melee lifesteal retained");
        h.assertTrue(HeldCombatAttributes.lifeSteal(p,p.damageSources().indirectMagic(p,p))==0,"Spell lifesteal retained");
        equip(p,ItemStack.EMPTY);
        h.assertTrue(p.getAttributeValue(ModAttributes.SPELL_POWER)==1 && HeldCombatAttributes.lifeSteal(p,p.damageSources().playerAttack(p))==1,"Removal retained effects or erased other lifesteal");h.succeed();
    }
    @GameTest(template="spell_arena") public static void actualSpellDamageRoundsUpWithoutRecursion(GameTestHelper h){
        var p=player(h);equip(p,new ItemStack(ModItems.UNBOUND_MANA_CORE.get()));p.getAttribute(ModAttributes.LIFESTEAL).setBaseValue(1);p.setHealth(10);
        var cow=h.spawn(EntityType.IRON_GOLEM,4,3,4);cow.setNoAi(true);
        try{
            cow.hurt(SpellDamageSource.of(cow,DamageTypes.INDIRECT_MAGIC,p,p,"spell"),5);
            h.assertTrue(p.getHealth()==8,"Spell recoil not ceil(5/4), recurred or healed via lifesteal: "+p.getHealth());
            OmniraDamage.trueDamage(cow,5,SpellDamageSource.physical(cow,p,p,"spell"));
            h.assertTrue(p.getHealth()==6,"True damage lost original spell attribution");
            cow.invulnerableTime=0;cow.hurt(p.damageSources().playerAttack(p),2);
            h.assertTrue(p.getHealth()==6,"Ordinary melee reflected or healed");
            AffinityCommands.apply(p,Affinity.CHAOS);p.setData(ModAttachments.MANA,new ManaState(100,100));ChaosBeam.backlash(p);
            h.assertTrue(p.getHealth()==6 && p.getData(ModAttachments.MANA).current()==90,"Core confused mana backlash with spell recoil");
            OmniraDamage.trueDamage(cow,4,SpellDamageSource.of(cow,DamageTypes.INDIRECT_MAGIC,p,p,"spell"));
            h.assertTrue(p.getHealth()==5,"Chaos affinity improperly immunized core recoil");
        }finally{cow.discard();}h.succeed();
    }
}
