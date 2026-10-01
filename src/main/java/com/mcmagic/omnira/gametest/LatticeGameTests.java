package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;
import java.util.UUID;

@GameTestHolder("omnira")
@PrefixGameTestTemplate(false)
public final class LatticeGameTests {
    private static FakePlayer player(GameTestHelper h) {
        FakePlayer player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"lattice-test"));
        player.setPos(h.absoluteVec(new Vec3(5,3,5)));
        player.setXRot(-90);
        player.setData(ModAttachments.MANA,ManaState.initial());
        return player;
    }
    private static ItemStack crystal() {
        ItemStack result=new ItemStack(ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
        result.set(ModDataComponents.SPELL_PATTERN,new SpellPattern(ElementType.AIR,ElementType.AIR));
        return result;
    }
    @GameTest(template="spell_arena")
    public static void unboundedComponentCosts(GameTestHelper h) {
        h.assertTrue(SpellPayload.componentCost(2)==20 && SpellPayload.componentCost(17)==170,"Cost is not component-count based");
        var payload=new SpellPayload(SpellPayload.componentCost(17),2);
        var encoded=SpellPayload.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,payload).getOrThrow();
        h.assertTrue(SpellPayload.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,encoded).getOrThrow().equals(payload),"Cost above 60 does not round-trip");
        h.assertTrue(SpellPayload.of(crystal()).baseCost()==20,"Legacy crystal cost changed");
        var player=player(h);
        player.getAttribute(ModAttributes.COST_REDUCTION).setBaseValue(15);
        h.assertTrue(ManaCosts.cost(player,payload.baseCost())==155,"Carrier cost reduction incorrect");
        h.assertTrue(ManaCosts.cost(player,10)==0,"Discount creates negative mana cost");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void slotsAndAttributes(GameTestHelper h) {
        var player=player(h);
        h.assertTrue(CuriosApi.getPlayerSlots(h.getLevel()).containsKey("crystal_grid"),"Missing lattice slot");
        h.assertTrue(CuriosApi.getPlayerSlots(h.getLevel()).containsKey("spell_core"),"Missing core slot");
        var grid=new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get());
        var context=new SlotContext("crystal_grid",player,0,false,true);
        h.assertTrue(CuriosApi.isStackValid(context,grid),"Lattice tag does not permit item");
        h.assertTrue(!CuriosApi.isStackValid(context,new ItemStack(ModItems.TEST_SPELL_CORE.get())),"Core fits lattice slot");
        var modifiers=((CrystalGridItem)grid.getItem()).getAttributeModifiers(context,ResourceLocation.fromNamespaceAndPath("omnira","test_grid"),grid);
        modifiers.forEach((attribute,modifier)->player.getAttribute(attribute).addTransientModifier(modifier));
        h.assertTrue(player.getAttributeValue(ModAttributes.MAX_MANA)==200,"Wrong maximum mana");
        h.assertTrue(Math.abs(player.getAttributeValue(ModAttributes.MANA_REGEN)-1.1)<.001,"Wrong regeneration");
        h.assertTrue(ManaCosts.cost(player,20)==10 && ManaCosts.cost(player,50)==40,"Discount is not shared");
        modifiers.forEach((attribute,modifier)->player.getAttribute(attribute).removeModifier(modifier.id()));
        h.assertTrue(player.getAttributeValue(ModAttributes.MAX_MANA)==100,"Unequip did not restore maximum");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void cycleWrapAndMana(GameTestHelper h) {
        var player=player(h);
        ItemStack grid=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());
        NonNullList<ItemStack> crystals=NonNullList.withSize(3,ItemStack.EMPTY);
        crystals.set(0,crystal());crystals.set(2,crystal());
        grid.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(crystals));
        grid.set(ModDataComponents.GRID_CURSOR,2);
        CuriosApi.getCuriosInventory(player).orElseThrow().setEquippedCurio("crystal_grid",0,grid);
        var staff=com.mcmagic.omnira.item.staff.StaffAssembly.basic().create();
        h.assertTrue(StaffItem.castSequence(player,staff,2)==2,"Multi-cast failed");
        h.assertTrue(CrystalGridMenu.next(grid,3)==2,"Last and first did not wrap correctly");
        h.assertTrue(player.getData(ModAttachments.MANA).current()==60,"Wrong multi-cast cost");
        NonNullList<ItemStack> remaining=NonNullList.withSize(3,ItemStack.EMPTY);
        grid.get(DataComponents.CONTAINER).copyInto(remaining);
        h.assertTrue(remaining.get(0).getCount()==1 && remaining.get(2).getCount()==1,"Staff consumed crystals");
        player.setData(ModAttachments.MANA,new ManaState(19,100));
        h.assertTrue(StaffItem.castSequence(player,staff,1)==0 && CrystalGridMenu.next(grid,3)==2,"Failed cast advanced cursor");
        var encoded=ItemStack.CODEC.encodeStart(h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),grid).getOrThrow();
        ItemStack decoded=ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE),encoded).getOrThrow();
        h.assertTrue(ItemStack.matches(grid,decoded),"Lattice contents did not persist");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void damageAndStrength(GameTestHelper h) {
        var player=player(h);
        SpellEntity spell=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,player.position());
        spell.configure(1.3,2);
        h.assertTrue(spell.duration()==520,"Strength did not scale lifetime");
        var target=h.spawn(net.minecraft.world.entity.EntityType.COW,8,3,8);
        float health=target.getHealth();
        spell.applyEffects(target);
        h.assertTrue(Math.abs(target.getHealth()-(health-2.6))<.01,"Strength did not scale damage");
        var empty=SpellEntity.spawn(h.getLevel(),player,SpellEntity.Kind.PROJECTILE,player.position());
        health=target.getHealth();
        empty.applyEffects(target);
        h.assertTrue(target.getHealth()==health,"Empty payload dealt damage");
        h.succeed();
    }
}
