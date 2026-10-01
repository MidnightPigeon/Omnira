package com.mcmagic.omnira.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> TYPES=DeferredRegister.create(Registries.PARTICLE_TYPE,"omnira");
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> NAIL_FOCUS=TYPES.register("nail_focus",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> NEEDLE_FOCUS=TYPES.register("needle_focus",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> PARADOX_SPARK=TYPES.register("paradox_spark",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> TIME_WARP_SPARK=TYPES.register("time_warp_spark",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> TIMEFLOW_RIPPLE=TYPES.register("timeflow_ripple",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> TIMEFLOW_BUBBLE=TYPES.register("timeflow_bubble",()->new SimpleParticleType(false));
    private ModParticles() {}
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> PEACEFUL_MEMORY_GLYPH=TYPES.register("peaceful_memory_glyph",()->new SimpleParticleType(false));
    public static final DeferredHolder<ParticleType<?>,SimpleParticleType> CORRUPTED_MEMORY_GLYPH=TYPES.register("corrupted_memory_glyph",()->new SimpleParticleType(false));
}
