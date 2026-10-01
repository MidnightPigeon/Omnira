package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.menu.AnalysisArtisanTableMenu;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

public final class AnalysisArtisanTableBlockEntity extends CrystalProcessingTableBlockEntity {
    public AnalysisArtisanTableBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntityTypes.ANALYSIS_ARTISAN_TABLE.get(), pos, state, 3); }
    @Override public int getContainerSize() { return 3; }
    @Override protected Component getDefaultName() { return Component.translatable("container.omnira.analysis_artisan_table"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return new AnalysisArtisanTableMenu(id, inventory, this); }
    @Override public boolean canPlaceItem(int slot, net.minecraft.world.item.ItemStack stack) { return slot == 0; }
}
