package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.*;
import top.theillusivec4.curios.api.*;

@GameTestHolder("omnira_primary_accessory")
@PrefixGameTestTemplate(false)
public final class PrimaryAccessoryGameTests {
    @GameTest(template="spell_arena") public static void uniqueBonusesAndEditing(GameTestHelper h){
        var p=new net.neoforged.neoforge.common.util.FakePlayer(h.getLevel(),new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"primary-test"));
        var inv=CuriosApi.getCuriosInventory(p).orElseThrow();
        var cores=inv.getStacksHandler("spell_core").orElseThrow().getStacks();cores.grow(2);
        var grids=inv.getStacksHandler("crystal_grid").orElseThrow().getStacks();grids.grow(2);
        var first=new ItemStack(ModItems.BASIC_CRYSTAL_GRID.get());var second=new ItemStack(ModItems.ELEMENTAL_CRYSTAL_GRID.get());
        p.getInventory().setItem(5,first.copy());p.getInventory().setItem(9,second.copy());
        h.assertTrue(CrystalGridMenu.editingSource(p).orElseThrow()==5 && CrystalGridMenu.locate(p,-2).isEmpty(),"Inventory grid is active or editing is not first-only");
        h.assertTrue(StaffItem.castSequence(p,com.mcmagic.omnira.item.staff.StaffAssembly.basic().create(),1)==0,"Inventory grid can cast");
        grids.setStackInSlot(0,first);grids.setStackInSlot(1,second);
        cores.setStackInSlot(0,new ItemStack(ModItems.TEST_SPELL_CORE.get()));cores.setStackInSlot(1,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));
        h.assertTrue(CrystalGridMenu.editingSource(p).orElseThrow()==-2 && CrystalGridMenu.locate(p,-2)==first,"Equipped grid does not take precedence");
        var id=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","audit");
        h.assertTrue(((CrystalGridItem)second.getItem()).getAttributeModifiers(new SlotContext("crystal_grid",p,1,false,true),id,second).isEmpty(),"Extra grid contributes bonuses");
        var core=cores.getStackInSlot(1);
        h.assertTrue(((SpellCoreItem)core.getItem()).getAttributeModifiers(new SlotContext("spell_core",p,1,false,true),id,core).isEmpty(),"Extra core contributes bonuses");
        var tick=new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p);
        PrimarySpellAccessory.tick(tick);double maximum=p.getAttributeValue(ModAttributes.MAX_MANA);
        PrimarySpellAccessory.tick(tick);h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==maximum,"Bonuses accumulate per tick");
        cores.setStackInSlot(0,ItemStack.EMPTY);grids.setStackInSlot(0,ItemStack.EMPTY);PrimarySpellAccessory.tick(tick);
        h.assertTrue(CrystalGridMenu.locate(p,-2)==second && p.getAttributeValue(ModAttributes.MAX_MANA)>maximum,"Next equipped item not promoted across empty slot");
        cores.setStackInSlot(1,ItemStack.EMPTY);grids.setStackInSlot(1,ItemStack.EMPTY);PrimarySpellAccessory.tick(tick);
        h.assertTrue(p.getAttributeValue(ModAttributes.MAX_MANA)==100 && CrystalGridMenu.editingSource(p).orElseThrow()==5,"Unequip left bonuses or wrong editor source");
        h.succeed();
    }
}
