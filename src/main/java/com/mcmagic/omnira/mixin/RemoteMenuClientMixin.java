package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mcmagic.omnira.client.RemoteContainerView;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value=net.neoforged.neoforge.network.handlers.ClientPayloadHandler.class,remap=false)
abstract class RemoteMenuClientMixin {
    @WrapMethod(method="createMenuScreen")
    private static void omnira$construct(Component title,MenuType<?> type,int id,RegistryFriendlyByteBuf data,Operation<Void> original){
        RemoteContainerView.begin(id);try{original.call(title,type,id,data);}finally{RemoteContainerView.end();}
    }
}
