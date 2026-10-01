package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.mana.ManaState;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Omnira.MOD_ID);
    private static final Codec<ManaState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.doubleRange(0, 1_000_000).fieldOf("current").forGetter(ManaState::current),
            Codec.doubleRange(0.000001, 1_000_000).fieldOf("maximum").forGetter(ManaState::maximum),
            Codec.intRange(0,4).optionalFieldOf("affinity",0).forGetter(ManaState::affinity)
    ).apply(instance, ManaState::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, ManaState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, ManaState::current, ByteBufCodecs.DOUBLE, ManaState::maximum,
            ByteBufCodecs.VAR_INT,ManaState::affinity,ManaState::new);

    public static final DeferredHolder<AttachmentType<?>,AttachmentType<com.mcmagic.omnira.mana.AffinityState>> AFFINITY=TYPES.register(
            "affinity",()->AttachmentType.builder(()->com.mcmagic.omnira.mana.AffinityState.NONE)
                    .serialize(com.mcmagic.omnira.mana.AffinityState.CODEC).copyOnDeath().build());
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Boolean>> DREAM_AFFINITY=TYPES.register(
            "dream_affinity",()->AttachmentType.builder(()->false).sync(ByteBufCodecs.BOOL).build());
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Float>> DREAM_SOLIDIFY=TYPES.register(
            "dream_solidify",()->AttachmentType.builder(()->0F).sync(ByteBufCodecs.FLOAT).build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ManaState>> MANA = TYPES.register(
            "mana", () -> AttachmentType.builder(ManaState::initial).serialize(CODEC).copyOnDeath()
                    .sync((holder, recipient) -> holder == recipient, STREAM_CODEC).build());
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Integer>> RECALL_TICKS=TYPES.register(
            "recall_ticks",()->AttachmentType.builder(()->0).sync((holder,recipient)->holder==recipient,ByteBufCodecs.VAR_INT).build());
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<Integer>> RABBIT_TICKS=TYPES.register(
            "rabbit_ticks",()->AttachmentType.builder(()->0).sync((holder,recipient)->holder==recipient,ByteBufCodecs.VAR_INT).build());

    private ModAttachments() {}

    public static void register(IEventBus bus) {
        TYPES.register(bus);
    }
}
