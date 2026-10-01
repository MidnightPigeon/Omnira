package com.mcmagic.omnira.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> TYPES=DeferredRegister.create(Registries.SOUND_EVENT,"omnira");
    public static final java.util.function.Supplier<SoundEvent> NIGHT_HERON_CALL=TYPES.register("night_heron_call",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","night_heron_call")));
    private ModSounds() {}
}
