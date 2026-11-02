package com.mcmagic.omnira.network;
import com.mcmagic.omnira.item.IndustrialWheelItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record IndustrialWheelPayload() implements CustomPacketPayload {
    public static final Type<IndustrialWheelPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:industrial_wheel"));
    public static final StreamCodec<RegistryFriendlyByteBuf,IndustrialWheelPayload> CODEC=StreamCodec.unit(new IndustrialWheelPayload());
    @Override public Type<IndustrialWheelPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToServer(TYPE,CODEC,(payload,c)->c.enqueueWork(()->{
            if(c.player() instanceof ServerPlayer p)IndustrialWheelItem.toggle(p,p.getMainHandItem().getItem() instanceof IndustrialWheelItem?InteractionHand.MAIN_HAND:InteractionHand.OFF_HAND);
        }));}
    }
}
