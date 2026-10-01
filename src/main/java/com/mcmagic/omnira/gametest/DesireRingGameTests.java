package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.archaeology.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.util.FakePlayer;
import top.theillusivec4.curios.api.SlotContext;

@GameTestHolder("omnira_desire_rings")
@PrefixGameTestTemplate(false)
public final class DesireRingGameTests {
    private static FakePlayer player(GameTestHelper h){var p=new FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"ring-test"));p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);return p;}
    private static void equip(FakePlayer p,net.minecraft.world.item.Item item){
        var curio=(ArchaeologyCurio)item;
        curio.getAttributeModifiers(new SlotContext("ring",p,0,false,true),ResourceLocation.parse("omnira:ring_test"),new ItemStack(item))
                .forEach((attribute,modifier)->p.getAttribute(attribute).addTransientModifier(modifier));
    }
    @GameTest(template="spell_arena") public static void meleeAndRangedLeech(GameTestHelper h){
        var p=player(h);equip(p,ArchaeologyContent.LUST_RING.get());p.setHealth(5);
        var cow=h.spawn(EntityType.COW,2,2,2);cow.hurt(p.damageSources().playerAttack(p),4);
        h.assertTrue(Math.abs(cow.getHealth()-5)<.001,"Melee was not increased by 25 percent");
        h.assertTrue(Math.abs(p.getHealth()-6.25)<.001,"Melee leech missing");
        var second=h.spawn(EntityType.COW,4,2,2);p.setHealth(5);
        var arrow=new net.minecraft.world.entity.projectile.Arrow(EntityType.ARROW,h.getLevel());
        second.hurt(p.damageSources().arrow(arrow,p),4);
        h.assertTrue(Math.abs(second.getHealth()-6)<.001,"Melee bonus leaked to arrow");
        h.assertTrue(Math.abs(p.getHealth()-6)<.001,"Arrow leech missing");
        var third=h.spawn(EntityType.COW,6,2,2);p.setHealth(5);
        third.hurt(p.damageSources().indirectMagic(arrow,p),4);
        h.assertTrue(Math.abs(p.getHealth()-6)<.001,"Magic leech missing");
        cow.discard();second.discard();third.discard();h.succeed();
    }
    @GameTest(template="spell_arena") public static void greedAndRecipes(GameTestHelper h){
        var p=player(h);equip(p,ArchaeologyContent.GREED_RING.get());
        p.getAttribute(ModAttributes.SPELL_POWER).addTransientModifier(new AttributeModifier(ResourceLocation.parse("omnira:other_power"),.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        h.assertTrue(Math.abs(p.getAttributeValue(ModAttributes.SPELL_POWER)-1.5)<.001,"Spell power not additive");
        for(String id:java.util.List.of("ring_of_lust","ring_of_greed"))h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("omnira:advanced_assembly_table/"+id)).isPresent(),"Missing ring recipe");h.succeed();
    }
}
