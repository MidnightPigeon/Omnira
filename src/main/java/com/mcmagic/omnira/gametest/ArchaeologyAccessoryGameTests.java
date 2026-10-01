package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.DreamAffinityLoot;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.*;

@GameTestHolder("omnira_archaeology_accessory")
@PrefixGameTestTemplate(false)
public final class ArchaeologyAccessoryGameTests {
    private static FakePlayer player(GameTestHelper h){return new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"relic-test"));}
    @GameTest(template="spell_arena") public static void attributes(GameTestHelper h){
        var p=player(h);var id=ResourceLocation.parse("omnira:test");
        for(var entry:java.util.List.of(ArchaeologyContent.D_CLASS_PASS,ArchaeologyContent.C_CLASS_PASS,ArchaeologyContent.B_CLASS_PASS,ArchaeologyContent.A_CLASS_PASS)){
            var item=(ArchaeologyCurio)entry.get();var mods=item.getAttributeModifiers(new SlotContext("charm",p,0,false,true),id,new ItemStack(item));
            boolean low=entry==ArchaeologyContent.D_CLASS_PASS||entry==ArchaeologyContent.C_CLASS_PASS;
            h.assertTrue(mods.get(ModAttributes.DAMAGE_TAKEN).iterator().next().amount()==(low?-.1:-.2),"Wrong damage reduction");
            h.assertTrue(mods.containsKey(ModAttributes.SPELL_POWER)==(entry!=ArchaeologyContent.D_CLASS_PASS),"Wrong inherited spell power");
            h.assertTrue(mods.containsKey(ModAttributes.COST_REDUCTION)==!low,"Wrong inherited mana reduction");
        }
        var ring=(ArchaeologyCurio)ArchaeologyContent.JADE_RING.get();var stack=new ItemStack(ring);
        h.assertTrue(ring.getAttributeModifiers(new SlotContext("ring",p,0,false,true),id,stack).get(Attributes.MOVEMENT_SPEED).iterator().next().amount()==-.2,"Ring speed");
        h.assertTrue(ring.getAttributeModifiers(new SlotContext("charm",p,0,false,true),id,stack).isEmpty(),"Wrong slot grants modifiers");
        var psyker=(com.mcmagic.omnira.item.FateCurioItem)ModItems.PSYKER_TALENT.get();
        h.assertTrue(psyker.getAttributeModifiers(new SlotContext("talent",p,0,false,true),id,new ItemStack(psyker)).get(ModAttributes.COOLDOWN_REDUCTION).iterator().next().amount()==.1,"Psyker cooldown missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void timedEffects(GameTestHelper h){
        var p=player(h);var ring=(ArchaeologyCurio)ArchaeologyContent.JADE_RING.get();var context=new SlotContext("ring",p,0,false,true);
        p.tickCount=1;ring.curioTick(context,new ItemStack(ring));
        h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getDuration()==600&&p.getEffect(MobEffects.ABSORPTION).getAmplifier()==1,"Absorption II missing");
        p.getEffect(MobEffects.ABSORPTION).tick(p,()->{});p.tickCount=199;ring.curioTick(context,new ItemStack(ring));
        h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getDuration()==599,"Refreshed too early");
        p.tickCount=200;ring.curioTick(context,new ItemStack(ring));h.assertTrue(p.getEffect(MobEffects.ABSORPTION).getDuration()==600,"Ten-second refresh missing");
        var pass=(ArchaeologyCurio)ArchaeologyContent.A_CLASS_PASS.get();p.tickCount=600;
        pass.curioTick(new SlotContext("charm",p,0,false,true),new ItemStack(pass));
        h.assertTrue(p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.SATURATION),"A effects missing");h.succeed();
    }
    @GameTest(template="spell_arena") public static void additiveLoot(GameTestHelper h){
        var p=player(h);var inv=CuriosApi.getCuriosInventory(p).orElseThrow();
        inv.setEquippedCurio("charm",0,new ItemStack(ArchaeologyContent.ENHANCED_CARD.get()));
        h.assertTrue(DreamAffinityLoot.lootBonus(p)==2,"Enhanced loot bonus missing");
        var ench=h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        var fortune=ench.getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE);
        var tool=new ItemStack(Items.DIAMOND_PICKAXE);tool.enchant(fortune,3);
        h.assertTrue(DreamAffinityLoot.mining(p,()->net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(fortune,tool))==5,"Fortune not additive");
        h.assertTrue(tool.getEnchantmentLevel(fortune)==3&&!DreamAffinityLoot.mining(),"Context leaked");
        inv.setEquippedCurio("charm",0,ItemStack.EMPTY);h.assertTrue(DreamAffinityLoot.lootBonus(p)==0,"Unequipped bonus persisted");h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipes(GameTestHelper h){
        var recipes=h.getLevel().getRecipeManager();
        for(String id:java.util.List.of("assembly/enhanced_phantom_calling_card","advanced_assembly_table/b_class_credentials","spacetime_reversal/c_class_credentials","a_class_credentials"))
            h.assertTrue(recipes.byKey(ResourceLocation.parse("omnira:"+id)).isPresent(),"Missing recipe "+id);
        var input=new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(ArchaeologyContent.D_CLASS_PASS.get()));
        var recipe=recipes.getRecipeFor(com.mcmagic.omnira.reversal.ReversalContent.TYPE.get(),input,h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe.outputs(input.item()).getFirst().is(ArchaeologyContent.C_CLASS_PASS.get()),"D to C failed");h.succeed();
    }
}
