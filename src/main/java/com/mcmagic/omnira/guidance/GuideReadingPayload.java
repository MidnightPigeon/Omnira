package com.mcmagic.omnira.guidance;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

public record GuideReadingPayload(UUID bookId,InteractionHand hand,GuideBookmark bookmark) implements CustomPacketPayload {
    public static final Type<GuideReadingPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:guide_reading"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GuideReadingPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeUUID(p.bookId);b.writeEnum(p.hand);b.writeUtf(p.bookmark.destination(),96);b.writeVarInt(p.bookmark.page());},
            b->new GuideReadingPayload(b.readUUID(),b.readEnum(InteractionHand.class),new GuideBookmark(b.readUtf(96),b.readVarInt())));
    public Type<GuideReadingPayload> type(){return TYPE;}
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent
        public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event){
            event.registrar("1").playBidirectional(TYPE,CODEC,(p,c)->c.enqueueWork(()->{
                if(c.flow()==net.minecraft.network.protocol.PacketFlow.CLIENTBOUND)
                    com.mcmagic.omnira.client.GuideBookClient.open(p);
                else p.bookmark.save(c.player().getItemInHand(p.hand),p.bookId);
            }));
        }
    }
}
