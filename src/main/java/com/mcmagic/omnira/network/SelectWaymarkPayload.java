package com.mcmagic.omnira.network;

import com.mcmagic.omnira.menu.WaymarkMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SelectWaymarkPayload(int menuId,int index) implements CustomPacketPayload {
    public static final Type<SelectWaymarkPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","select_waymark"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SelectWaymarkPayload> CODEC=new StreamCodec<>() {
        @Override public SelectWaymarkPayload decode(RegistryFriendlyByteBuf buf) {return new SelectWaymarkPayload(buf.readVarInt(),buf.readVarInt());}
        @Override public void encode(RegistryFriendlyByteBuf buf,SelectWaymarkPayload value) {buf.writeVarInt(value.menuId());buf.writeVarInt(value.index());}
    };
    @Override public Type<SelectWaymarkPayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(value,context)->context.enqueueWork(()->{
                var player=context.player();
                if(player.containerMenu instanceof WaymarkMenu menu && menu.containerId==value.menuId()) menu.clickMenuButton(player,value.index());
            }));
        }
    }
}
