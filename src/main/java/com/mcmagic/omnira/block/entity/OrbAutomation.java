package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.*;

public final class OrbAutomation {
    public static void tick(CrystalBallBlockEntity ball) {
        tick(ball,true);
    }
    public static void tick(CrystalBallBlockEntity ball,boolean production) {
        if(ball.getLevel()==null || ball.getLevel().isClientSide)return;
        for(int slot=0;slot<3;slot++) {
            var upgrade=ball.upgrades.getStackInSlot(slot);var kind=OrbUpgrades.kind(upgrade);
            if(kind==Kind.LAVA && production)lava(ball,slot);
            if(kind==Kind.INTAKE || kind==Kind.OUTPUT)transfer(ball,slot,kind==Kind.INTAKE);
        }
    }
    private static void lava(CrystalBallBlockEntity ball,int slot) {
        if(ball instanceof LiquidCrystalBallBlockEntity liquid) {liquid.tank.fill(new FluidStack(Fluids.LAVA,100),FluidAction.EXECUTE);return;}
        var tag=OrbUpgrades.settings(ball.upgrades.getStackInSlot(slot));
        for(int i=0;i<12;i++) {
            var item=ball.getItem(i);if(item.isEmpty())continue;
            int destination=i;
            if(item.getCount()>1) {
                destination=-1;for(int j=0;j<12;j++)if(ball.getItem(j).isEmpty()){destination=j;break;}
                if(destination<0)continue;
            }
            var source=new FluidTank(1000);source.setFluid(new FluidStack(Fluids.LAVA,1000));
            if(!FluidUtil.tryFillContainer(item,source,1000,null,false).isSuccess())continue;
            int credit=Math.min(1000,tag.getInt("LavaCredit")+100);
            source.setFluid(new FluidStack(Fluids.LAVA,credit));
            var result=FluidUtil.tryFillContainer(item,source,credit,null,true);
            if(result.isSuccess()) {
                if(destination!=i)ball.removeItem(i,1);
                ball.setItem(destination,result.getResult());credit=source.getFluidAmount();
            }
            tag.putInt("LavaCredit",credit);ball.upgrades.update(slot,tag);return;
        }
    }
    public static Direction[] directions(CrystalBallBlockEntity ball,boolean input) {
        Direction front=ball.getBlockState().getValue(com.mcmagic.omnira.block.CrystalBallBlock.FACING);
        return input?new Direction[]{front,front.getOpposite(),front.getCounterClockWise(),front.getClockWise()}:new Direction[]{Direction.DOWN,Direction.UP};
    }
    private static void transfer(CrystalBallBlockEntity ball,int slot,boolean input) {
        var tag=OrbUpgrades.settings(ball.upgrades.getStackInSlot(slot));var directions=directions(ball,input);
        int mask=tag.contains("Directions")?tag.getInt("Directions"):1;
        int cursor=Math.floorMod(tag.getInt("Cursor"),directions.length);
        for(int n=0;n<directions.length;n++) {
            int index=(cursor+n)%directions.length;if((mask&(1<<index))==0)continue;
            var direction=directions[index];var pos=ball.getBlockPos().relative(direction);
            if(!ball.getLevel().hasChunkAt(pos))continue;
            boolean transferred=false;
            if(ball instanceof LiquidCrystalBallBlockEntity liquid) {
                var other=ball.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,pos,direction.getOpposite());
                if(other==null)continue;
                var source=input?other:liquid.fluidHandler;var target=input?liquid.fluidHandler:other;
                for(int tank=0;tank<source.getTanks();tank++) {
                    var fluid=source.getFluidInTank(tank);if(fluid.isEmpty() || !ball.upgrades.matches(slot,ItemStack.EMPTY,fluid))continue;
                    var offered=source.drain(fluid.copyWithAmount(500),FluidAction.SIMULATE);
                    int accepted=target.fill(offered,FluidAction.SIMULATE);if(accepted<=0)continue;
                    var taken=source.drain(offered.copyWithAmount(accepted),FluidAction.EXECUTE);
                    int filled=target.fill(taken,FluidAction.EXECUTE);
                    if(filled<taken.getAmount())source.fill(taken.copyWithAmount(taken.getAmount()-filled),FluidAction.EXECUTE);
                    transferred=filled>0;break;
                }
            } else {
                var other=ball.getLevel().getCapability(Capabilities.ItemHandler.BLOCK,pos,direction.getOpposite());if(other==null)continue;
                var source=input?other:ball.itemHandler;var target=input?ball.itemHandler:other;
                for(int i=0;i<source.getSlots();i++) {
                    var offered=source.extractItem(i,1,true);
                    if(offered.isEmpty() || !ball.upgrades.matches(slot,offered,null) || !ItemHandlerHelper.insertItemStacked(target,offered,true).isEmpty())continue;
                    var taken=source.extractItem(i,1,false);
                    var rest=ItemHandlerHelper.insertItemStacked(target,taken,false);
                    if(!rest.isEmpty()) {
                        rest=ItemHandlerHelper.insertItemStacked(source,rest,false);
                        if(!rest.isEmpty())net.minecraft.world.level.block.Block.popResource(ball.getLevel(),ball.getBlockPos(),rest);
                    }
                    transferred=!taken.isEmpty();break;
                }
            }
            if(transferred){tag.putInt("Cursor",(index+1)%directions.length);ball.upgrades.update(slot,tag);return;}
        }
    }
}
