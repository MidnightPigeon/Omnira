package com.mcmagic.omnira.recipe;

import com.mcmagic.omnira.item.StaffItem;
import com.mcmagic.omnira.item.staff.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.*;

/** Generic assembly operations: matching depends on component roles, never a variant catalogue. */
public final class StaffAssemblyRecipes {
    private StaffAssemblyRecipes() {}
    public static ItemStack assemble(AssemblyRecipe.Input input,String operation) {
        if(operation.equals("staff")) {
            for(int i:new int[]{1,2,4,5}) if(!input.getItem(i).isEmpty()) return ItemStack.EMPTY;
            if(!StaffPart.is(input.getItem(3),StaffPart.Role.SHAFT) || !StaffPart.is(input.getItem(6),StaffPart.Role.REINFORCEMENT)
                    || !StaffPart.is(input.getItem(0),StaffPart.Role.TIP)) return ItemStack.EMPTY;
            return new StaffAssembly(input.getItem(3),input.getItem(6),input.getItem(0),List.of()).create();
        }
        if(operation.equals("staff_upgrade") && input.getItem(6).getItem() instanceof StaffItem) {
            var upgrades=new ArrayList<ItemStack>();
            for(int i=0;i<6;i++) if(!input.getItem(i).isEmpty()) {
                if(!StaffPart.is(input.getItem(i),StaffPart.Role.UPGRADE)) return ItemStack.EMPTY;
                upgrades.add(input.getItem(i));
            }
            if(upgrades.isEmpty()) return ItemStack.EMPTY;
            var upgraded=StaffAssembly.of(input.getItem(6)).withUpgrades(upgrades);
            if(upgraded.isPresent()) {
                var result=input.getItem(6).copyWithCount(1);
                result.set(com.mcmagic.omnira.registry.ModDataComponents.STAFF_ASSEMBLY,upgraded.get());return result;
            }
        }
        return ItemStack.EMPTY;
    }
    public static List<ItemStack> parts(StaffPart.Role role) {
        var result=new ArrayList<ItemStack>();
        for(var item:BuiltInRegistries.ITEM) {
            var stack=item.getDefaultInstance();if(StaffPart.is(stack,role)) result.add(stack);
        }
        return List.copyOf(result);
    }
    public static List<Ingredient> displaySlots() {
        var slots=new ArrayList<Ingredient>(Collections.nCopies(7,Ingredient.EMPTY));
        for(var role:new StaffPart.Role[]{StaffPart.Role.SHAFT,StaffPart.Role.REINFORCEMENT,StaffPart.Role.TIP})
            slots.set(role.assemblySlot(),Ingredient.of(parts(role).stream()));
        return List.copyOf(slots);
    }
    /** JEI examples are derived from registered parts; actual crafting does not enumerate combinations. */
    public static List<AssemblyRecipe> previews() {
        var result=new ArrayList<AssemblyRecipe>();
        var shafts=parts(StaffPart.Role.SHAFT);var reinforcements=parts(StaffPart.Role.REINFORCEMENT);var tips=parts(StaffPart.Role.TIP);
        for(var shaft:shafts) for(var reinforcement:reinforcements) for(var tip:tips) {
            var slots=new ArrayList<Ingredient>(Collections.nCopies(7,Ingredient.EMPTY));
            slots.set(3,Ingredient.of(shaft));slots.set(6,Ingredient.of(reinforcement));slots.set(0,Ingredient.of(tip));
            result.add(new AssemblyRecipe(slots,new StaffAssembly(shaft,reinforcement,tip,List.of()).create()));
        }
        return List.copyOf(result);
    }
}
