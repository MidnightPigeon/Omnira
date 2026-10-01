package com.mcmagic.omnira.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

/** Sent only to the customer; the block entity never broadcasts another player's daily list. */
public record MarisaGoodsPayload(long day,List<ItemStack> goods) implements CustomPacketPayload {
    public MarisaGoodsPayload {
        if(goods.size()!=6)throw new IllegalArgumentException("Expected six daily goods");
        goods=goods.stream().map(ItemStack::copy).toList();
    }
    public static final Type<MarisaGoodsPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("omnira","marisa_goods"));
    public static final StreamCodec<RegistryFriendlyByteBuf,MarisaGoodsPayload> CODEC=StreamCodec.of(
            (buffer,payload)->{buffer.writeLong(payload.day);for(var stack:payload.goods)ItemStack.STREAM_CODEC.encode(buffer,stack);},
            buffer->{long day=buffer.readLong();List<ItemStack> goods=new ArrayList<>();for(int i=0;i<6;i++)goods.add(ItemStack.STREAM_CODEC.decode(buffer));return new MarisaGoodsPayload(day,goods);});
    @Override public Type<MarisaGoodsPayload> type(){return TYPE;}
    @EventBusSubscriber(modid="omnira")
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){
            event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->context.enqueueWork(()->
                    com.mcmagic.omnira.client.MarisaGoodsView.receive(payload)));
        }
    }
}
