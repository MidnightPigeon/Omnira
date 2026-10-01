package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class AdvancedCondensationTableBlockEntity extends SimpleCondensationTableBlockEntity {
    public AdvancedCondensationTableBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.ADVANCED_CONDENSATION_TABLE.get(),pos,state);}
    @Override public int getMaxStackSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {
        return level!=null && level.getRecipeManager().getAllRecipesFor(com.mcmagic.omnira.registry.ModRecipes.CONDENSATION_TYPE.get()).stream()
                .anyMatch(h->h.value().advanced() && h.value().ingredient().test(stack));
    }
    @Override protected Component getDefaultName() {return Component.translatable("container.omnira.advanced_condensation_table");}
}
