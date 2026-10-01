package com.mcmagic.omnira.network;
import com.mcmagic.omnira.spell.ConstructionLock;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record MineLockedEntityPayload(int entity) implements CustomPacketPayload {
    public static final Type<MineLockedEntityPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","mine_locked"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MineLockedEntityPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,MineLockedEntityPayload::entity,MineLockedEntityPayload::new);
    @Override public Type<MineLockedEntityPayload> type() {return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player && player.containerMenu==player.inventoryMenu)
                    ConstructionLock.mine(player,payload.entity);
            }));
        }
    }
}
