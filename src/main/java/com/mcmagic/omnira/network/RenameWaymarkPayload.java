package com.mcmagic.omnira.network;

import com.mcmagic.omnira.menu.WaymarkMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RenameWaymarkPayload(int menuId,String name) implements CustomPacketPayload {
    public static final Type<RenameWaymarkPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","rename_waymark"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RenameWaymarkPayload> CODEC=new StreamCodec<>() {
        @Override public RenameWaymarkPayload decode(RegistryFriendlyByteBuf buf) {return new RenameWaymarkPayload(buf.readVarInt(),buf.readUtf(32));}
        @Override public void encode(RegistryFriendlyByteBuf buf,RenameWaymarkPayload value) {buf.writeVarInt(value.menuId());buf.writeUtf(value.name(),32);}
    };
    @Override public Type<RenameWaymarkPayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(value,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player && player.containerMenu instanceof WaymarkMenu menu && menu.containerId==value.menuId())
                    menu.rename(player,value.name());
            }));
        }
    }
}
