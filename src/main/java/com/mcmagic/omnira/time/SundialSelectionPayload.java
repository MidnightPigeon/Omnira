package com.mcmagic.omnira.time;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record SundialSelectionPayload(int menu,ItemStack input,ItemStack target) implements CustomPacketPayload {
    public static final Type<SundialSelectionPayload> TYPE=new Type<>(ResourceLocation.parse("omnira:sundial_selection"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SundialSelectionPayload> CODEC=StreamCodec.of(
            (b,p)->{b.writeVarInt(p.menu);ItemStack.STREAM_CODEC.encode(b,p.input);ItemStack.STREAM_CODEC.encode(b,p.target);},
            b->new SundialSelectionPayload(b.readVarInt(),ItemStack.STREAM_CODEC.decode(b),ItemStack.STREAM_CODEC.decode(b)));
    @Override public Type<SundialSelectionPayload> type(){return TYPE;}
    public static boolean apply(net.minecraft.world.entity.player.Player player,SundialSelectionPayload p){
        return player.containerMenu instanceof SundialMenu m&&m.containerId==p.menu&&m.select(player,p.input,p.target);
    }
    @net.neoforged.fml.common.EventBusSubscriber(modid="omnira") public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e){
            e.registrar("1").playToServer(TYPE,CODEC,(p,c)->c.enqueueWork(()->apply(c.player(),p)));
        }
    }
}
