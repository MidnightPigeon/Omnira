package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.*;

/** Item intake scans the base, output the middle. Fluid intake has exactly one physical port. */
public final class CatalysisAutomation {
    public static void tick(CatalysisBlockEntity be){
        var level=be.getLevel();var front=be.getBlockState().getValue(CatalysisBlock.FACING);
        for(int upgrade=0;upgrade<2;upgrade++){
            var plate=be.upgrades.getStackInSlot(upgrade);if(plate.isEmpty())continue;boolean intake=upgrade==0;
            var tag=OrbUpgrades.settings(plate);int mask=tag.contains("Directions")?tag.getInt("Directions"):1;
            Direction[] sides=intake?new Direction[]{front,front.getOpposite(),front.getCounterClockWise(),front.getClockWise(),Direction.DOWN,Direction.UP}
                    :new Direction[]{Direction.DOWN,Direction.UP,front,front.getOpposite(),front.getCounterClockWise(),front.getClockWise()};
            for(int n=0;n<sides.length;n++){
                if((mask&(1<<n))==0)continue;var side=sides[n];var at=be.getBlockPos().above(intake?0:1).relative(side);
                if(!level.hasChunkAt(at)||CatalysisBlock.controller(level,at)==be)continue;
                var other=level.getCapability(Capabilities.ItemHandler.BLOCK,at,side.getOpposite());if(other==null)continue;
                var source=intake?other:be.items;var target=intake?be.items:other;boolean moved=false;
                for(int slot=0;slot<source.getSlots();slot++){
                    var sample=source.extractItem(slot,1,true);if(sample.isEmpty()||!be.filter(upgrade,sample,null)||!ItemHandlerHelper.insertItemStacked(target,sample,true).isEmpty())continue;
                    var taken=source.extractItem(slot,1,false);var rest=ItemHandlerHelper.insertItemStacked(target,taken,false);
                    if(!rest.isEmpty()){rest=ItemHandlerHelper.insertItemStacked(source,rest,false);if(!rest.isEmpty())net.minecraft.world.level.block.Block.popResource(level,at,rest);}
                    moved=!taken.isEmpty();break;
                }if(moved)break;
            }
            if(intake&&(mask&2)!=0){
                var at=be.getBlockPos().relative(front.getOpposite());if(!level.hasChunkAt(at))continue;
                var other=level.getCapability(Capabilities.FluidHandler.BLOCK,at,front);if(other==null)continue;
                for(int i=0;i<other.getTanks();i++){
                    var sample=other.getFluidInTank(i);if(sample.isEmpty()||!be.filter(0,ItemStack.EMPTY,sample))continue;
                    var offered=other.drain(sample.copyWithAmount(500),FluidAction.SIMULATE);int n=be.tank.fill(offered,FluidAction.SIMULATE);if(n<=0)continue;
                    var taken=other.drain(offered.copyWithAmount(n),FluidAction.EXECUTE);int accepted=be.tank.fill(taken,FluidAction.EXECUTE);
                    if(accepted<taken.getAmount())other.fill(taken.copyWithAmount(taken.getAmount()-accepted),FluidAction.EXECUTE);break;
                }
            }
        }
    }
    private CatalysisAutomation(){}
}
