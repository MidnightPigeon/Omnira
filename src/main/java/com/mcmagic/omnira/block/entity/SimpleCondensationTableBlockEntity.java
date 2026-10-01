package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.menu.SimpleCondensationTableMenu;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class SimpleCondensationTableBlockEntity extends CrystalProcessingTableBlockEntity {
    public SimpleCondensationTableBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.SIMPLE_CONDENSATION_TABLE.get(),pos,state,1);}
    protected SimpleCondensationTableBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state) {super(type,pos,state,1);}
    @Override public int getContainerSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return false;}
    @Override protected Component getDefaultName() {return Component.translatable("container.omnira.simple_condensation_table");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {return new SimpleCondensationTableMenu(id,inventory,this);}
}
