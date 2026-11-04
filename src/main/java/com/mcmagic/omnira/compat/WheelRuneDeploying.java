package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.item.IndustrialWheelItem;
import com.mcmagic.omnira.item.staff.StaffPart;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.simibubi.create.content.kinetics.deployer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import java.util.Optional;

public final class WheelRuneDeploying {
    private WheelRuneDeploying(){}
    static void search(DeployerRecipeSearchEvent event){
        var input=event.getInventory();
        if(input.getItem(0).getItem() instanceof IndustrialWheelItem)
            event.addRecipe(()->recipe(input.getItem(0),input.getItem(1)),110);
    }
    public static Optional<RecipeHolder<DeployerApplicationRecipe>> recipe(ItemStack wheel,ItemStack rune){
        if(!(wheel.getItem() instanceof IndustrialWheelItem)||wheel.has(ModDataComponents.WHEEL_RUNE))return Optional.empty();
        var part=StaffPart.of(rune);
        if(part==null||part.role()!=StaffPart.Role.UPGRADE||part.upgradeSlot()!=StaffPart.SlotKind.RUNE)return Optional.empty();
        var output=wheel.copyWithCount(1);
        output.set(ModDataComponents.WHEEL_RUNE,net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.List.of(rune.copyWithCount(1))));
        var key=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(rune.getItem());
        var id=ResourceLocation.fromNamespaceAndPath("omnira","deploy_wheel_rune/"+key.getNamespace()+"/"+key.getPath());
        var recipe=new ItemApplicationRecipe.Builder<DeployerApplicationRecipe>(DeployerApplicationRecipe::new,id)
                .require(wheel.getItem()).require(rune.getItem()).withSingleItemOutput(output).build();
        return Optional.of(new RecipeHolder<>(id,recipe));
    }
}
