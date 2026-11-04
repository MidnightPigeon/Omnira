package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

public record LootrMenuFluidPayload(int menuId,FluidStack fluid) implements CustomPacketPayload {
    public static final Type<LootrMenuFluidPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:lootr_menu_fluid"));
    public static final StreamCodec<RegistryFriendlyByteBuf,LootrMenuFluidPayload> CODEC=StreamCodec.composite(
            ByteBufCodecs.VAR_INT,LootrMenuFluidPayload::menuId,FluidStack.OPTIONAL_STREAM_CODEC,LootrMenuFluidPayload::fluid,LootrMenuFluidPayload::new);
    @Override public Type<LootrMenuFluidPayload> type(){return TYPE;}
}
