package com.mcmagic.omnira.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;

/** Detect recipe-relevant changes, including in-place mutations made by external automation.
 * Copies only changed values; progress/save/render notifications do not invalidate the gate.
 */
public final class RecipeRetryGate {
    private final java.util.ArrayList<Object> values=new java.util.ArrayList<>();
    private Object manager,recipes;
    private int cursor;
    private boolean changed,invalid=true;
    public void invalidate(){invalid=true;}
    public void begin(Level level){
        var next=level.getRecipeManager();var contents=next.getRecipes();
        changed=invalid||manager!=next||recipes!=contents;invalid=false;
        manager=next;recipes=contents;cursor=0;
    }
    private Object old(){return cursor<values.size()?values.get(cursor):null;}
    private void put(Object value){if(cursor==values.size())values.add(value);else values.set(cursor,value);changed=true;}
    public void item(ItemStack stack){
        if(!(old() instanceof ItemStack previous)||!ItemStack.matches(previous,stack))put(stack.copy());cursor++;
    }
    public void fluid(FluidStack stack){
        if(!(old() instanceof FluidStack previous)||previous.getAmount()!=stack.getAmount()
                ||!FluidStack.isSameFluidSameComponents(previous,stack))put(stack.copy());cursor++;
    }
    /** Only immutable values, e.g. block states, numbers and flags. */
    public void value(Object value){if(cursor>=values.size()||!java.util.Objects.equals(old(),value))put(value);cursor++;}
    public void items(net.neoforged.neoforge.items.IItemHandler inventory){for(int i=0;i<inventory.getSlots();i++)item(inventory.getStackInSlot(i));}
    public void items(net.minecraft.world.Container inventory){for(int i=0;i<inventory.getContainerSize();i++)item(inventory.getItem(i));}
    public void fluids(net.neoforged.neoforge.fluids.capability.IFluidHandler tanks){for(int i=0;i<tanks.getTanks();i++)fluid(tanks.getFluidInTank(i));}
    public boolean changed(){if(values.size()>cursor){values.subList(cursor,values.size()).clear();changed=true;}return changed;}
}
