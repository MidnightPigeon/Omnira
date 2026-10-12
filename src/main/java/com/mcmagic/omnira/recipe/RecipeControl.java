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
    public static boolean installed(IItemHandler upgrades) {
        for(int i=0;i<upgrades.getSlots();i++)if(OrbUpgrades.kind(upgrades.getStackInSlot(i))==Kind.RECIPE_CONTROL)return true;
        return false;
    }
    public static boolean allows(Level level,IItemHandler upgrades,List<ItemStack> products,FluidStack fluid) {
        for(int slot=0;slot<upgrades.getSlots();slot++) {
            var plate=upgrades.getStackInSlot(slot);
            if(OrbUpgrades.kind(plate)!=Kind.RECIPE_CONTROL)continue;
            var data=OrbUpgrades.settings(plate);boolean match=false;
            for(int i=0;i<FILTERS&&!match;i++) {
                var sample=ItemStack.parseOptional(level.registryAccess(),data.getCompound("Filter"+i));
                if(sample.isEmpty())continue;
                for(var product:products)if(OrbFilterMatcher.matches(level,sample,product,null)){match=true;break;}
                if(!match&&!fluid.isEmpty())match=OrbFilterMatcher.matches(level,sample,ItemStack.EMPTY,fluid);
            }
            if(match!=data.getBoolean("Whitelist"))return false;
        }
        return true;
    }
    private RecipeControl(){}
}
