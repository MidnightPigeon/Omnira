package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.reversal.*;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.archaeology.ArchaeologyContent;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("omnira_reversal")
@PrefixGameTestTemplate(false)
public final class ReversalGameTests {
    @GameTest(template="spell_arena") public static void repairFacility(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlockAndUpdate(pos,ReversalContent.MACHINE.get().defaultBlockState());
        var be=(ReversalBlockEntity)h.getLevel().getBlockEntity(pos);
        be.setItem(0,new ItemStack(ArchaeologyContent.DAMAGED_FACILITY.get()));be.setItem(2,new ItemStack(ModItems.SPACE_MICROCORE.get()));
        for(int i=0;i<100;i++)be.tick();
        h.assertTrue(be.getItem(0).isEmpty()&&be.getItem(1).is(ReversalContent.PART.get())&&be.energy()==7,"Facility reversal failed");h.succeed();
    }
    @GameTest(template="spell_arena") public static void attributeSentiment(GameTestHelper h){
        h.assertTrue(ModAttributes.DAMAGE_TAKEN.get().getStyle(true)==net.minecraft.ChatFormatting.RED,"Incoming damage increase must be red");
        h.assertTrue(ModAttributes.DAMAGE_TAKEN.get().getStyle(false)==net.minecraft.ChatFormatting.BLUE,"Incoming damage reduction must be beneficial");
        h.assertTrue(ModAttributes.DAMAGE_DEALT.get().getStyle(false)==net.minecraft.ChatFormatting.RED,"Outgoing damage reduction must be red");
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void chargingAndProcessing(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlockAndUpdate(p,ReversalContent.MACHINE.get().defaultBlockState());
        var be=(ReversalBlockEntity)h.getLevel().getBlockEntity(p);
        h.assertTrue(ReversalBlockEntity.fuel(new ItemStack(ModItems.TIME_WARP_POINT.get()))==4
                &&ReversalBlockEntity.fuel(new ItemStack(ModItems.TIME_MICROCORE.get()))==8
                &&ReversalBlockEntity.fuel(new ItemStack(ModItems.SPACE_MICROCORE.get()))==8,"Temporal fuel balance changed");
        be.setItem(0,new ItemStack(ModItems.TIME_WARP_POINT.get(),2));be.setItem(2,new ItemStack(ModItems.SPACE_MICROCORE.get(),8));
        for(int i=0;i<99;i++)be.tick();h.assertTrue(be.getItem(1).isEmpty(),"Processed before five seconds");be.tick();
        h.assertTrue(be.getItem(1).is(ModItems.TIME_MICROCORE.get())&&be.getItem(0).isEmpty()&&be.energy()==63,"Wrong recipe or energy");
        be.setItem(2,new ItemStack(ModItems.SPACE_MICROCORE.get()));be.tick();h.assertTrue(be.energy()==63&&be.getItem(2).getCount()==1,"Fuel overflow consumed");
        var saved=be.saveWithFullMetadata(h.getLevel().registryAccess());var copy=new ReversalBlockEntity(p,be.getBlockState());copy.loadWithComponents(saved,h.getLevel().registryAccess());h.assertTrue(copy.energy()==63,"Energy not saved");h.succeed();
    }
    @GameTest(template="spell_arena") public static void randomOutputBlocked(GameTestHelper h){
        var p=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(p,ReversalContent.MACHINE.get().defaultBlockState());var be=(ReversalBlockEntity)h.getLevel().getBlockEntity(p);
        be.setItem(0,new ItemStack(ArchaeologyContent.UNKNOWN_FOSSIL.get()));be.setItem(1,new ItemStack(ReversalContent.REX_REMAINS.get()));be.setItem(2,new ItemStack(ModItems.SPACE_MICROCORE.get()));
        for(int i=0;i<105;i++)be.tick();h.assertTrue(be.energy()==8&&be.getItem(0).getCount()==1&&be.progress()==0,"Blocked random recipe consumed resources");
        be.setItem(1,ItemStack.EMPTY);for(int i=0;i<100;i++)be.tick();h.assertTrue(be.energy()==7&&be.getItem(0).isEmpty()&&!be.getItem(1).isEmpty(),"Random recipe did not finish");h.succeed();
    }
    @GameTest(template="spell_arena") public static void wetlandStorage(GameTestHelper h){
        var inv=new net.neoforged.neoforge.items.ItemStackHandler(3);inv.setStackInSlot(0,new ItemStack(ArchaeologyContent.DAMAGED_FACILITY.get(),7));
        h.assertTrue(!(ArchaeologyContent.DAMAGED_FACILITY.get() instanceof BlockItem),"Damaged facility is placeable");
        h.assertTrue(WetlandRepair.repair(inv,false)&&inv.getStackInSlot(0).is(ArchaeologyContent.DAMAGED_FACILITY.get()),"Detection repaired early");
        WetlandRepair.repair(inv,true);h.assertTrue(inv.getStackInSlot(0).is(ReversalContent.PART.get())&&inv.getStackInSlot(0).getCount()==7,"Storage repair lost items");h.succeed();
    }
    @GameTest(template="spell_arena") public static void recipesAndTalents(GameTestHelper h){
        h.assertTrue(h.getLevel().getRecipeManager().getAllRecipesFor(ReversalContent.TYPE.get()).size()>=12,"Missing reversal recipes");
        h.assertTrue(ReversalBlockEntity.fuel(new ItemStack(ModItems.PEACEFUL_MEMORY.get()))==4&&ReversalBlockEntity.fuel(new ItemStack(com.mcmagic.omnira.mire.MireContent.EEL_ITEM.get()))==2,"Wrong fuels");
        var p=h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var curio=(com.mcmagic.omnira.item.FateCurioItem)ReversalContent.SEA.get();
        var context=new top.theillusivec4.curios.api.SlotContext("talent",p,0,false,true);
        var attrs=curio.getAttributeModifiers(context,net.minecraft.resources.ResourceLocation.parse("omnira:test_sea"),new ItemStack(curio));
        h.assertTrue(attrs.get(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).iterator().next().amount()==.2,"Sea health bonus");
        h.assertTrue(!curio.canUnequip(context,new ItemStack(curio)),"Talent removable by hand");h.succeed();
    }
}
