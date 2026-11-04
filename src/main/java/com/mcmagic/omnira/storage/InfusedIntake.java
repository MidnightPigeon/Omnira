package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.items.*;
import java.util.*;

/** A distinct storage upgrade: no machine processing, adjacency transfer or damage. */
public final class InfusedIntake {
    public static final int RADIUS=8,PERIOD=200;
    private static final List<BlockPos> CENTERED_OFFSETS=offsets(false),MOBILE_OFFSETS=offsets(true);
    private static List<BlockPos> offsets(boolean mobile){
        var result=new ArrayList<BlockPos>();
        // Preserve vanilla betweenClosed order: harvest priority must not change.
        for(var p:BlockPos.betweenClosed(-RADIUS,-RADIUS,-RADIUS,RADIUS,RADIUS,RADIUS)){
            double x=Math.max(0,Math.abs(p.getX())-(mobile?.5:0));
            double y=Math.max(0,Math.abs(p.getY())-(mobile?.5:0));
            double z=Math.max(0,Math.abs(p.getZ())-(mobile?.5:0));
            if(x*x+y*y+z*z<=RADIUS*RADIUS)result.add(p.immutable());
        }
        return List.copyOf(result);
    }
    private int elapsed;
    public void tick(ServerLevel level,Vec3 center,IItemHandler upgrades,IItemHandler storage){
        ItemStack plate=ItemStack.EMPTY;
        for(int i=0;i<upgrades.getSlots();i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==Kind.INFUSED_INTAKE){plate=upgrades.getStackInSlot(i);break;}
        if(plate.isEmpty()){elapsed=0;return;}
        if(++elapsed<PERIOD)return;elapsed=0;collect(level,center,plate,storage);
    }
    public static void collect(ServerLevel level,Vec3 center,ItemStack plate,IItemHandler storage){
        var tag=OrbUpgrades.settings(plate);boolean whitelist=tag.getBoolean("Whitelist");
        var samples=new ArrayList<ItemStack>();for(int i=0;i<3;i++)samples.add(ItemStack.parseOptional(level.registryAccess(),tag.getCompound("Filter"+i)));
        java.util.function.Predicate<ItemStack> allowed=stack->samples.stream().anyMatch(sample->OrbFilterMatcher.matches(level,sample,stack,null))==whitelist;
        var area=AABB.ofSize(center,RADIUS*2,RADIUS*2,RADIUS*2);
        for(var item:level.getEntitiesOfClass(ItemEntity.class,area,e->e.isAlive()&&e.position().distanceToSqr(center)<=RADIUS*RADIUS)){
            var stack=item.getItem();if(!allowed.test(stack))continue;
            var rest=ItemHandlerHelper.insertItemStacked(storage,stack,false);
            if(rest.isEmpty())item.discard();else if(rest.getCount()!=stack.getCount())item.setItem(rest);
        }
        var trial=new CapacityTrial(storage);
        var actor=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var origin=BlockPos.containing(center);var cursor=new BlockPos.MutableBlockPos();
        var offsets=center.equals(origin.getCenter())?CENTERED_OFFSETS:MOBILE_OFFSETS;
        for(var offset:offsets){
            var pos=cursor.setWithOffset(origin,offset);
            if(pos.getCenter().distanceToSqr(center)>RADIUS*RADIUS||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos))continue;
            var state=level.getBlockState(pos);BlockState next=harvested(state);if(next==null)continue;
            var drops=new ArrayList<>(Block.getDrops(state,level,pos,level.getBlockEntity(pos),actor,ItemStack.EMPTY));
            // Preserve a seed for replanted crops; berry bushes regrow from the existing bush.
            if(!(state.getBlock() instanceof SweetBerryBushBlock)){
                var seed=state.getBlock().getCloneItemStack(level,pos,state);boolean reserved=false;
                for(var drop:drops)if(drop.is(seed.getItem())&&!drop.isEmpty()){drop.shrink(1);reserved=true;break;}
                if(!reserved)continue;
            }
            drops.removeIf(ItemStack::isEmpty);
            if(drops.isEmpty()||drops.stream().anyMatch(allowed.negate())||!trial.fits(drops))continue;
            if(net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level,actor.gameMode.getGameModeForPlayer(),actor,pos,state).isCanceled())continue;
            if(!level.getBlockState(pos).equals(state)||!level.setBlock(pos,next,3))continue;
            for(var drop:drops)remainder(level,center,ItemHandlerHelper.insertItemStacked(storage,drop,false));
        }
        animals(level,center,area,storage,allowed,trial);
    }
    private static BlockState harvested(BlockState state){
        if(state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state))return crop.getStateForAge(0);
        if(state.getBlock() instanceof NetherWartBlock&&state.getValue(NetherWartBlock.AGE)==3)return state.setValue(NetherWartBlock.AGE,0);
        if(state.getBlock() instanceof CocoaBlock&&state.getValue(CocoaBlock.AGE)==2)return state.setValue(CocoaBlock.AGE,0);
        if(state.getBlock() instanceof SweetBerryBushBlock&&state.getValue(SweetBerryBushBlock.AGE)==3)return state.setValue(SweetBerryBushBlock.AGE,1);
        return null;
    }
    private static void animals(ServerLevel level,Vec3 center,AABB area,IItemHandler storage,java.util.function.Predicate<ItemStack> allowed,CapacityTrial trial){
        for(var cow:level.getEntitiesOfClass(net.minecraft.world.entity.animal.Cow.class,area,e->e.isAlive()&&!e.isBaby()&&e.position().distanceToSqr(center)<=RADIUS*RADIUS)){
            var milk=new ItemStack(com.mcmagic.omnira.registry.ModItems.MILK_SUSPENSION.get());
            if(allowed.test(milk)&&trial.fits(List.of(milk)))remainder(level,cow.position(),ItemHandlerHelper.insertItemStacked(storage,milk,false));
        }
        for(var sheep:level.getEntitiesOfClass(net.minecraft.world.entity.animal.Sheep.class,area,e->e.isAlive()&&e.readyForShearing()&&e.position().distanceToSqr(center)<=RADIUS*RADIUS)){
            var wool=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(sheep.getColor().getName()+"_wool")),3);
            if(!allowed.test(wool)||!trial.fits(List.of(wool)))continue;
            for(var drop:sheep.onSheared(null,new ItemStack(Items.SHEARS),level,sheep.blockPosition()))remainder(level,sheep.position(),ItemHandlerHelper.insertItemStacked(storage,drop,false));
        }
    }
    /** One scratch inventory per collection, refreshed against live contents before each transaction. */
    private static final class CapacityTrial extends ItemStackHandler {
        private final IItemHandler storage;
        CapacityTrial(IItemHandler storage){super(storage.getSlots());this.storage=storage;}
        @Override public int getSlotLimit(int slot){return storage.getSlotLimit(slot);}
        @Override public boolean isItemValid(int slot,ItemStack stack){return storage.isItemValid(slot,stack);}
        @Override protected int getStackLimit(int slot,ItemStack stack){
            var offered=stack.copyWithCount(storage.getSlotLimit(slot));var existing=storage.getStackInSlot(slot);
            return offered.getCount()-storage.insertItem(slot,offered,true).getCount()+(ItemStack.isSameItemSameComponents(existing,stack)?existing.getCount():0);
        }
        boolean fits(List<ItemStack> stacks){
            for(int i=0;i<storage.getSlots();i++){
                var actual=storage.getStackInSlot(i);
                if(!ItemStack.matches(getStackInSlot(i),actual))setStackInSlot(i,actual.copy());
            }
            // Only the scratch inventory is mutated; rejected harvests never touch storage.
            for(var stack:stacks)if(!ItemHandlerHelper.insertItemStacked(this,stack,false).isEmpty())return false;
            return true;
        }
    }
    private static void remainder(ServerLevel level,Vec3 pos,ItemStack stack){if(!stack.isEmpty())level.addFreshEntity(new ItemEntity(level,pos.x,pos.y,pos.z,stack));}
}
