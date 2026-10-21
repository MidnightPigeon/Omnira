package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.SpeedUpgrade;
import com.mcmagic.omnira.menu.OrbUpgradeMenu;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_logistics_rate")
@PrefixGameTestTemplate(false)
public final class LogisticsRateGameTests {
    private static ItemStack speed(Item first,Item second){
        var plate=new ItemStack(ModItems.SPEED_UPGRADE.get());SpeedUpgrade.cores(plate,java.util.List.of(new ItemStack(first),new ItemStack(second)));return plate;
    }
    @GameTest(template="spell_arena") public static void allCorePairsPersistAndAdd(GameTestHelper h){
        var cores=java.util.List.of(ModItems.TEST_SPELL_CORE.get(),ModItems.DREAM_SPELL_CORE.get(),ModItems.LIGHT_DARK_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get());
        h.assertTrue(SpeedUpgrade.multiplier(new ItemStack(ModItems.SPEED_UPGRADE.get()))==2,"Empty plate lost base multiplier");
        for(var a:cores)for(var b:cores){
            var plate=speed(a,b);int expected=2+SpeedUpgrade.bonus(new ItemStack(a))+SpeedUpgrade.bonus(new ItemStack(b));
            var restored=ItemStack.parseOptional(h.getLevel().registryAccess(),(net.minecraft.nbt.CompoundTag)plate.save(h.getLevel().registryAccess()));
            h.assertTrue(SpeedUpgrade.multiplier(restored)==expected&&SpeedUpgrade.cores(restored).get(0).is(a)&&SpeedUpgrade.cores(restored).get(1).is(b),"Core configuration or additive rate lost");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void heldCoreSlotsAreRealAndSourceIsLocked(GameTestHelper h){
        var p=h.makeMockPlayer(GameType.SURVIVAL);var plate=new ItemStack(ModItems.SPEED_UPGRADE.get());p.getInventory().setItem(0,plate);
        var menu=new OrbUpgradeMenu(1,p.getInventory(),0,false);
        menu.setCarried(new ItemStack(ModItems.SPACETIME_SPELL_CORE.get()));menu.clicked(0,0,ClickType.PICKUP,p);
        h.assertTrue(menu.getCarried().isEmpty()&&SpeedUpgrade.multiplier(plate)==5,"Core was not consumed into a real slot");
        p.getInventory().setItem(9,new ItemStack(ModItems.DREAM_SPELL_CORE.get()));menu.quickMoveStack(p,2);
        h.assertTrue(p.getInventory().getItem(9).isEmpty()&&SpeedUpgrade.multiplier(plate)==7,"Shift insertion lost core or multiplier");
        h.assertTrue(!menu.slots.get(0).mayPlace(new ItemStack(Items.STONE))&&!menu.clickMenuButton(p,0),"Non-core or filter control accepted");
        menu.clicked(1,0,ClickType.SWAP,p);h.assertTrue(p.getMainHandItem()==plate&&SpeedUpgrade.multiplier(plate)==7,"Number-key swap moved bound plate");
        menu.quickMoveStack(p,0);h.assertTrue(SpeedUpgrade.multiplier(plate)==4&&p.getInventory().countItem(ModItems.SPACETIME_SPELL_CORE.get())==1,"Core shift removal duplicated or lost contents");
        menu.clicked(1,0,ClickType.PICKUP,p);h.assertTrue(menu.getCarried().is(ModItems.DREAM_SPELL_CORE.get())&&SpeedUpgrade.multiplier(plate)==2,"Core pickup not recoverable");
        p.getInventory().setItem(0,ItemStack.EMPTY);h.assertTrue(!menu.stillValid(p),"Detached plate remained editable");h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=45) public static void fluidMethodsHaveIndependentLimits(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,3,3));h.getLevel().setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var source=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);source.tank.setFluid(new FluidStack(Fluids.LAVA,24000));
        source.upgrades.setStackInSlot(0,speed(ModItems.SPACETIME_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get()));
        var below=pos.below();h.getLevel().setBlockAndUpdate(below,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var target=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(below);target.upgrades.setStackInSlot(0,source.upgrades.getStackInSlot(0).copy());
        h.runAfterDelay(22,()->{
            h.assertTrue(source.fluidHandler.drain(20000,FluidAction.SIMULATE).getAmount()==16000,"External fluid simulation has wrong 8x cap");
            h.assertTrue(source.fluidHandler.drain(20000,FluidAction.EXECUTE).getAmount()==16000&&source.fluidHandler.drain(1,FluidAction.EXECUTE).isEmpty(),"External repeated calls bypass limit");
            source.upgrades.setStackInSlot(1,new ItemStack(ModItems.OUTPUT_UPGRADE.get()));OrbAutomation.tick(source,false);
            h.assertTrue(target.tank.getFluidAmount()==4000&&source.tank.getFluidAmount()==4000,"Output upgrade incorrectly shares external pipe quota");
            var raw=source.transportFluids.drain(500,FluidAction.EXECUTE);h.assertTrue(raw.getAmount()==500,"Resonance path shares external quota");h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=25) public static void baseBucketTakesHalfSecond(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));h.getLevel().setBlockAndUpdate(pos,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var ball=(LiquidCrystalBallBlockEntity)h.getLevel().getBlockEntity(pos);ball.setItem(0,new ItemStack(Items.LAVA_BUCKET));
        h.runAfterDelay(8,()->h.assertTrue(ball.tank.isEmpty(),"Base bucket finished before half-second"));
        h.runAfterDelay(12,()->{h.assertTrue(ball.getItem(0).is(Items.BUCKET)&&ball.tank.getFluidAmount()==1000,"Base bucket failed half-second exchange");h.succeed();});
    }
    @GameTest(template="spell_arena") public static void intakeUsesReceiverRateAndNotPassiveSourceQuota(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(3,2,3));var world=h.getLevel();
        world.setBlockAndUpdate(pos,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        world.setBlockAndUpdate(pos.south(),ModBlocks.CRYSTAL_BALL.get().defaultBlockState());
        var source=(CrystalBallBlockEntity)world.getBlockEntity(pos);var receiver=(CrystalBallBlockEntity)world.getBlockEntity(pos.south());
        source.setItem(0,new ItemStack(Items.STONE,64));h.assertTrue(source.itemHandler.extractItem(0,64,false).getCount()==4,"Wrong passive item cap");
        receiver.upgrades.setStackInSlot(0,speed(ModItems.SPACETIME_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get()));
        receiver.upgrades.setStackInSlot(1,new ItemStack(ModItems.INTAKE_UPGRADE.get()));OrbAutomation.tick(receiver,false);
        h.assertTrue(receiver.countItem(Items.STONE)==8&&source.countItem(Items.STONE)==52,"Intake was limited by source instead of receiver");
        h.assertTrue(source.itemHandler.extractItem(0,1,false).isEmpty(),"Intake reset passive source quota");h.succeed();
    }
    @GameTest(template="spell_arena") public static void resonanceUsesSourceRateWithoutReceiverThrottle(GameTestHelper h){
        var world=h.getLevel();var a=h.absolutePos(new BlockPos(3,2,3));var b=a.east(4);
        for(var p:java.util.List.of(a,b)){world.setBlockAndUpdate(p,ModBlocks.CRYSTAL_BALL.get().defaultBlockState());world.setBlockAndUpdate(p.above(),ModBlocks.RESONANCE_CORE.get().defaultBlockState());}
        var source=(CrystalBallBlockEntity)world.getBlockEntity(a);var target=(CrystalBallBlockEntity)world.getBlockEntity(b);
        source.upgrades.setStackInSlot(0,speed(ModItems.SPACETIME_SPELL_CORE.get(),ModItems.SPACETIME_SPELL_CORE.get()));source.setItem(0,new ItemStack(Items.STONE,64));
        h.assertTrue(source.itemHandler.extractItem(0,64,false).getCount()==32,"Wrong accelerated external item cap");
        var left=(ResonanceCoreBlockEntity)world.getBlockEntity(a.above());var right=(ResonanceCoreBlockEntity)world.getBlockEntity(b.above());
        h.assertTrue(com.mcmagic.omnira.storage.NodeTransport.pair(left,right),"Node pair failed");com.mcmagic.omnira.storage.NodeTransport.tick(left);
        h.assertTrue(target.countItem(Items.STONE)==8&&source.countItem(Items.STONE)==24,"Node used receiver rate or shared external quota");h.succeed();
    }
}
