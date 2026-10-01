package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.menu.ManaEngineMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;

public interface ManaEngineAccess extends MenuProvider {
    ManaEngineState engineState();
    @Override default Component getDisplayName() {return Component.translatable("block.omnira.mana_engine");}
    @Override default AbstractContainerMenu createMenu(int id,Inventory inventory,Player player) {
        return new ManaEngineMenu(id,inventory,engineState());
    }
}
