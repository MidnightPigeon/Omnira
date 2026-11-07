package com.mcmagic.omnira.mana;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record AffinityState(int kind,int penaltyTicks,boolean perfected) {
    public static final AffinityState NONE=new AffinityState(0,0);
    public static final Codec<AffinityState> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,com.mcmagic.omnira.mana.Affinity.values().length-1).fieldOf("kind").forGetter(AffinityState::kind),
            Codec.intRange(0,2400).fieldOf("penalty").forGetter(AffinityState::penaltyTicks),
            Codec.BOOL.optionalFieldOf("perfected",false).forGetter(AffinityState::perfected)
    ).apply(i,AffinityState::new));
    public AffinityState(int kind,int penaltyTicks){this(kind,penaltyTicks,false);}
    public Affinity affinity(){return Affinity.byId(kind);}
    public Affinity active(){return penaltyTicks>0?Affinity.NONE:affinity();}
}
