package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.event.MilkSuspensionEvents;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_milk_burst")
@PrefixGameTestTemplate(false)
public final class MilkAndBurstGameTests {
    @GameTest(template="spell_arena")
    public static void milkTransfersExactlyOneBucket(GameTestHelper h) {
        var tank=new FluidTank(2000);
        var milk=new ItemStack(ModItems.MILK_SUSPENSION.get());
        h.assertTrue(MilkSuspensionEvents.injectBucket(tank,milk,false),"Milk transfer failed");
        h.assertTrue(milk.isEmpty() && tank.getFluidAmount()==1000 && tank.getFluid().getFluid()==NeoForgeMod.MILK.get(),"Wrong fluid, amount or item consumption");
        h.assertTrue(!MilkSuspensionEvents.injectBucket(tank,milk,false) && tank.getFluidAmount()==1000,"Consumed item reused");
        var bucket=FluidUtil.getFluidContained(new ItemStack(Items.MILK_BUCKET)).orElseThrow();
        h.assertTrue(FluidStack.isSameFluidSameComponents(bucket,tank.getFluid()),"Fluid differs from the vanilla milk bucket");
        var creative=new ItemStack(ModItems.MILK_SUSPENSION.get());
        h.assertTrue(MilkSuspensionEvents.injectBucket(tank,creative,true) && creative.getCount()==1 && tank.getFluidAmount()==2000,"Creative transfer semantics incorrect");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void rejectedMilkLeavesBothSidesUntouched(GameTestHelper h) {
        for(var tank:new FluidTank[]{new FluidTank(0),new FluidTank(1000),new FluidTank(2000,f->false)}) {
            if(tank.getCapacity()==1000) tank.setFluid(new FluidStack(Fluids.WATER,500));
            var before=tank.getFluid().copy();var milk=new ItemStack(ModItems.MILK_SUSPENSION.get());
            h.assertTrue(!MilkSuspensionEvents.injectBucket(tank,milk,false),"Invalid tank accepted milk");
            h.assertTrue(milk.getCount()==1 && FluidStack.matches(before,tank.getFluid()),"Rejected transfer changed fluid or item");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void partialMilkConsumesWholePortion(GameTestHelper h) {
        for(int capacity:new int[]{1,999,2000}) {
            var tank=new FluidTank(capacity);
            if(capacity==2000) tank.setFluid(new FluidStack(NeoForgeMod.MILK.get(),1001));
            var milk=new ItemStack(ModItems.MILK_SUSPENSION.get());
            h.assertTrue(MilkSuspensionEvents.injectBucket(tank,milk,false) && tank.getFluidAmount()==capacity && milk.isEmpty(),"Partial transfer must fill tank and consume portion");
            var extra=new ItemStack(ModItems.MILK_SUSPENSION.get());
            h.assertTrue(!MilkSuspensionEvents.injectBucket(tank,extra,false) && extra.getCount()==1,"Full tank consumed item");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void existingMilkComponentsArePreserved(GameTestHelper h) {
        var tank=new FluidTank(2000);
        var existing=new FluidStack(NeoForgeMod.MILK.get(),500);
        existing.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Existing milk"));
        tank.setFluid(existing.copy());
        var milk=new ItemStack(ModItems.MILK_SUSPENSION.get());
        h.assertTrue(MilkSuspensionEvents.injectBucket(tank,milk,false) && tank.getFluidAmount()==1500 && milk.isEmpty(),"Existing milk was not preferred");
        h.assertTrue(FluidStack.isSameFluidSameComponents(existing,tank.getFluid()),"Existing milk components changed");
        h.succeed();
    }
    @GameTest(template="spell_arena")
    public static void burstStillHarvestsSphere(GameTestHelper h) {CrystalRevisionGameTests.burstHarvestsWholeSphere(h);}
    @GameTest(template="spell_arena")
    public static void miningTiersUnchanged(GameTestHelper h) {UtilityGameTests.blockHarvestHonorsTier(h);}
}
