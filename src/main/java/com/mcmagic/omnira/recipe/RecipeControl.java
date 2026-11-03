package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.block.entity.OrbFilterMatcher;
import com.mcmagic.omnira.block.entity.OrbUpgrades;
import com.mcmagic.omnira.item.OrbUpgradeItem.Kind;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import java.util.List;

/** Tests all possible products, before choosing a random product or consuming inputs. */
public final class RecipeControl {
    public static final int FILTERS=9;
    private record Filter(boolean whitelist,ItemStack[] samples){}
    private static final class TickFilters {
        long time=Long.MIN_VALUE;
        final java.util.IdentityHashMap<net.minecraft.world.item.component.CustomData,Filter> entries=new java.util.IdentityHashMap<>();
    }
    private static final java.util.Map<Level,TickFilters> FILTER_CACHE=new java.util.WeakHashMap<>();
    /** Decode only; matching remains live so tags, inventories and products are never cached. */
    private static Filter filter(Level level,ItemStack plate){
        var data=plate.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY);
        synchronized(FILTER_CACHE){
            var cache=FILTER_CACHE.computeIfAbsent(level,ignored->new TickFilters());
            if(cache.time!=level.getGameTime()){cache.entries.clear();cache.time=level.getGameTime();}
            var found=cache.entries.get(data);if(found!=null)return found;
            var tag=data.copyTag();var samples=new ItemStack[FILTERS];
            for(int i=0;i<FILTERS;i++)samples[i]=ItemStack.parseOptional(level.registryAccess(),tag.getCompound("Filter"+i));
            found=new Filter(tag.getBoolean("Whitelist"),samples);
            if(cache.entries.size()>=256)cache.entries.clear();
            cache.entries.put(data,found);return found;
        }
    }
    public static boolean installed(IItemHandler upgrades) {
        for(int i=0;i<upgrades.getSlots();i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==Kind.RECIPE_CONTROL)return true;
        return false;
    }
    public static boolean allows(Level level,IItemHandler upgrades,List<ItemStack> products,FluidStack fluid) {
        for(int slot=0;slot<upgrades.getSlots();slot++) {
            var plate=upgrades.getStackInSlot(slot);
            if(OrbUpgrades.kind(plate)!=Kind.RECIPE_CONTROL)continue;
            var filter=filter(level,plate);boolean match=false;
            for(int i=0;i<FILTERS&&!match;i++) {
                var sample=filter.samples()[i];
                if(sample.isEmpty())continue;
                for(var product:products)if(OrbFilterMatcher.matches(level,sample,product,null)){match=true;break;}
                if(!match&&!fluid.isEmpty())match=OrbFilterMatcher.matches(level,sample,ItemStack.EMPTY,fluid);
            }
            if(match!=filter.whitelist())return false;
        }
        return true;
    }
    private RecipeControl(){}
}
