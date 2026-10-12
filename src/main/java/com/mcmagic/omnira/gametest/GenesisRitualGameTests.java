package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_genesis")
@PrefixGameTestTemplate(false)
public final class GenesisRitualGameTests {
    private static RitualEnergyCoreBlockEntity setup(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(5,3,5));var level=h.getLevel();
        level.setBlockAndUpdate(pos,ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get().defaultBlockState());
        int i=7;
        for(var offset:DreamRitual.ANCHORS){
            var at=pos.offset(offset[0],0,offset[1]);level.setBlockAndUpdate(at,ModBlocks.CRYSTAL_PEDESTAL.get().defaultBlockState());
            ((CrystalPedestalBlockEntity)level.getBlockEntity(at)).setItem(0,new ItemStack(GenesisRitual.ingredients().get(i--)));
        }
        return (RitualEnergyCoreBlockEntity)level.getBlockEntity(pos);
    }
    private static FluidStack fluid(int amount){return new FluidStack(AlchemyMaterials.REFINED.source.get(),amount);}
    private static CrystalPedestalBlockEntity first(GameTestHelper h,RitualEnergyCoreBlockEntity core){
        return (CrystalPedestalBlockEntity)h.getLevel().getBlockEntity(core.getBlockPos().offset(0,0,-3));
    }
    @GameTest(template="spell_arena") public static void unorderedValidationSimulationAndAllFaces(GameTestHelper h){
        var core=setup(h);var input=core.genesisOffering;
        h.assertTrue(GenesisRitual.pedestals(h.getLevel(),core.getBlockPos()).size()==8,"Unordered microcores rejected");
        h.assertTrue(input.fill(fluid(12000),FluidAction.SIMULATE)==10000 && core.genesisFluid()==0,"Simulation altered offering");
        h.assertTrue(input.fill(new FluidStack(Fluids.WATER,1000),FluidAction.EXECUTE)==0,"Accepted wrong liquid");
        for(var side:Direction.values()){
            var port=h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,core.getBlockPos(),side);
            h.assertTrue(port!=null && port.fill(fluid(1),FluidAction.EXECUTE)==1,"Face did not accept ritual offering");
        }
        h.assertTrue(input.getFluidInTank(0).isEmpty() && input.drain(10000,FluidAction.EXECUTE).isEmpty(),"Ritual became drainable fluid storage");
        var pedestal=first(h,core);var old=pedestal.getItem(0).copy();pedestal.setItem(0,new ItemStack(ModItems.EARTH_MICROCORE.get()));
        h.assertTrue(core.genesisFluid()==0 && input.fill(fluid(1000),FluidAction.EXECUTE)==0,"Duplicate offering accepted or partial liquid refunded");
        pedestal.setItem(0,old);h.assertTrue(input.fill(fluid(1000),FluidAction.EXECUTE)==1000,"Rebuilt ritual did not restart");
        h.getLevel().setBlockAndUpdate(core.getBlockPos(),ModBlocks.RITUAL_ENERGY_CORE.get().defaultBlockState());
        h.assertTrue(h.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,core.getBlockPos(),Direction.UP)==null,"Basic core exposed ritual fluid input");h.succeed();
    }
    @GameTest(template="spell_arena") public static void bucketsReturnAndCompleteExactlyOnce(GameTestHelper h){
        var core=setup(h);var player=h.makeMockPlayer(GameType.SURVIVAL);
        for(int i=0;i<10;i++){
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(AlchemyMaterials.REFINED.bucket.get()));
            h.assertTrue(FluidUtil.interactWithFluidHandler(player,InteractionHand.MAIN_HAND,core.genesisOffering),"Bucket interaction failed");
            h.assertTrue(player.getMainHandItem().is(Items.BUCKET) && core.genesisFluid()==(i+1)*1000,"Bucket lost or incorrect liquid debit");
        }
        h.assertTrue(core.genesisOffering.fill(fluid(1),FluidAction.EXECUTE)==0,"Overfilled ring");
        for(int i=0;i<60;i++)core.tickGenesis();
        h.assertTrue(core.genesisFluid()==0 && first(h,core).getItem(0).isEmpty(),"Completion did not consume offerings");
        for(int i=0;i<80;i++)core.tickGenesis();
        var drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(core.getBlockPos()).inflate(3));
        h.assertTrue(drops.stream().filter(d->d.getItem().is(ParadoxContent.GENESIS.get())).mapToInt(d->d.getItem().getCount()).sum()==1,"Duplicated or missing genesis output");h.succeed();
    }
    @GameTest(template="spell_arena") public static void portableOrbAndPipeDebitRealVolume(GameTestHelper h){
        var core=setup(h);var level=h.getLevel();var at=core.getBlockPos().offset(4,0,0);
        level.setBlockAndUpdate(at,ModBlocks.LIQUID_CRYSTAL_BALL.get().defaultBlockState());
        var orb=(LiquidCrystalBallBlockEntity)level.getBlockEntity(at);
        orb.upgrades.insertItem(0,new ItemStack(ModItems.SPACETIME_STABILIZATION_UPGRADE.get()),false);orb.tank.setFluid(fluid(6500));
        var stack=new ItemStack(ModItems.LIQUID_CRYSTAL_BALL.get());
        stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(orb.saveWithFullMetadata(level.registryAccess())));
        var player=h.makeMockPlayer(GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        h.assertTrue(FluidUtil.interactWithFluidHandler(player,InteractionHand.MAIN_HAND,core.genesisOffering),"Portable orb did not offer fluid");
        h.assertTrue(core.genesisFluid()==6500 && player.getMainHandItem().getCapability(Capabilities.FluidHandler.ITEM).getFluidInTank(0).isEmpty(),"Portable orb volume was duplicated");
        var pipe=new net.neoforged.neoforge.fluids.capability.templates.FluidTank(10000);pipe.setFluid(fluid(5000));
        var port=level.getCapability(Capabilities.FluidHandler.BLOCK,core.getBlockPos(),Direction.WEST);
        h.assertTrue(FluidUtil.tryFluidTransfer(port,pipe,5000,true).getAmount()==3500 && pipe.getFluidAmount()==1500,"Pipe over-drained source");
        h.assertTrue(FluidUtil.tryFluidTransfer(port,pipe,1000,true).isEmpty() && pipe.getFluidAmount()==1500,"Full ring still accepted fluid");h.succeed();
    }
    @GameTest(template="spell_arena") public static void reloadPreservesProgressAndInterruptionLosesOnlyFluid(GameTestHelper h){
        var core=setup(h);core.genesisOffering.fill(fluid(7200),FluidAction.EXECUTE);
        var saved=core.saveWithFullMetadata(h.getLevel().registryAccess());core.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(core.genesisFluid()==7200,"Reload lost offering");
        var pedestal=first(h,core);var item=pedestal.removeItem(0,1);pedestal.setChanged();
        h.assertTrue(core.genesisFluid()==0 && item.getCount()==1,"Interruption consumed microcore or retained ring");
        pedestal.setItem(0,item);core.genesisOffering.fill(fluid(10000),FluidAction.EXECUTE);
        for(int i=0;i<30;i++)core.tickGenesis();
        core.loadWithComponents(core.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());
        h.assertTrue(core.absorption()==10,"Reload lost absorption progress");
        for(int i=0;i<29;i++)core.tickGenesis();h.assertTrue(core.genesisFluid()==10000,"Completed early after reload");
        core.tickGenesis();h.assertTrue(core.genesisFluid()==0,"Did not complete after reload");h.succeed();
    }
    @GameTest(template="spell_arena") public static void pedestalBreakCancelsEvenDuringAbsorption(GameTestHelper h){
        var core=setup(h);core.genesisOffering.fill(fluid(10000),FluidAction.EXECUTE);core.tickGenesis();
        h.getLevel().removeBlock(first(h,core).getBlockPos(),false);core.tickGenesis();
        h.assertTrue(core.genesisFluid()==0 && GenesisRitual.pedestals(h.getLevel(),core.getBlockPos()).isEmpty(),"Broken ritual retained liquid");
        int remaining=0;for(var a:DreamRitual.ANCHORS)if(h.getLevel().getBlockEntity(core.getBlockPos().offset(a[0],0,a[1])) instanceof CrystalPedestalBlockEntity p)remaining+=p.getItem(0).getCount();
        h.assertTrue(remaining==7,"Interruption consumed untouched offerings");h.succeed();
    }
}
