package com.mcmagic.omnira.network;

import com.mcmagic.omnira.item.StaffItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record OffhandStaffPayload() implements CustomPacketPayload {
    public static final Type<OffhandStaffPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","offhand_staff"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OffhandStaffPayload> CODEC=StreamCodec.unit(new OffhandStaffPayload());
    @Override public Type<OffhandStaffPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player && !player.isShiftKeyDown() && player.containerMenu==player.inventoryMenu
                        && player.getOffhandItem().getItem() instanceof StaffItem staff)
                    staff.castHeld(player,player.getOffhandItem());
            }));
        }
    }
}
