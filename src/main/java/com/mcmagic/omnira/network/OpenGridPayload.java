package com.mcmagic.omnira.network;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.CrystalGridItem;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record OpenGridPayload() implements CustomPacketPayload {
    public static final OpenGridPayload INSTANCE=new OpenGridPayload();
    public static final Type<OpenGridPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"open_grid"));
    public static final StreamCodec<RegistryFriendlyByteBuf,OpenGridPayload> CODEC=StreamCodec.unit(INSTANCE);
    @Override public Type<OpenGridPayload> type() {return TYPE;}

    @EventBusSubscriber(modid=Omnira.MOD_ID)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                var player=context.player();
                // Requests carry no client-chosen stack or index. Do not displace another open container.
                if(!player.isAlive() || player.isSpectator() || player.containerMenu!=player.inventoryMenu) return;
                CrystalGridMenu.editingSource(player).ifPresent(source->CrystalGridItem.open(player,source));
            }));
        }
    }
}
