package com.mcmagic.omnira.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AffinityState(int kind,int penaltyTicks) {
    public static final AffinityState NONE=new AffinityState(0,0);
    public static final Codec<AffinityState> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,4).fieldOf("kind").forGetter(AffinityState::kind),
            Codec.intRange(0,2400).fieldOf("penalty").forGetter(AffinityState::penaltyTicks)
    ).apply(i,AffinityState::new));
    public Affinity affinity(){return Affinity.byId(kind);}
    public Affinity active(){return penaltyTicks>0?Affinity.NONE:affinity();}
}
