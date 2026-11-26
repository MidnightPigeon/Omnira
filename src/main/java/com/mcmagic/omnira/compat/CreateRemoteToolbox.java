package com.mcmagic.omnira.compat;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

/** ToolboxBlock supplies its menu and initialization data directly from its use interaction. */
public final class CreateRemoteToolbox {
    private CreateRemoteToolbox() {}
    public static MenuProvider provider(BlockEntity target){
        return target instanceof ToolboxBlockEntity toolbox?toolbox:null;
    }
    public static void write(MenuProvider provider,RegistryFriendlyByteBuf buffer){
        if(provider instanceof ToolboxBlockEntity toolbox)toolbox.sendToMenu(buffer);
    }
}
