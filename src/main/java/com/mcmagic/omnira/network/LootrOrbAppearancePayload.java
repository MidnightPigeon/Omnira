package com.mcmagic.omnira.network;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Only a personal opened bit crosses the network; never another player's inventory. */
public record LootrOrbAppearancePayload(ResourceLocation dimension,BlockPos pos,java.util.UUID identity,boolean opened) implements CustomPacketPayload {
    public static final Type<LootrOrbAppearancePayload> TYPE=new Type<>(ResourceLocation.parse("omnira:lootr_orb_appearance"));
    public static final StreamCodec<RegistryFriendlyByteBuf,LootrOrbAppearancePayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeResourceLocation(p.dimension);b.writeBlockPos(p.pos);b.writeUUID(p.identity);b.writeBoolean(p.opened);},
            b->new LootrOrbAppearancePayload(b.readResourceLocation(),b.readBlockPos(),b.readUUID(),b.readBoolean()));
    @Override public Type<LootrOrbAppearancePayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira") public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->{
                var level=context.player().level();
                if(level.dimension().location().equals(payload.dimension)&&level.getBlockEntity(payload.pos) instanceof CrystalBallBlockEntity orb
                        &&orb.storageIdentity.equals(payload.identity))orb.lootrOpened=payload.opened;
            }));
        }
    }
}
