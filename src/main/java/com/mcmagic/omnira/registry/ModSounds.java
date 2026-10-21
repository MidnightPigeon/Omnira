package com.mcmagic.omnira.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> TYPES=DeferredRegister.create(Registries.SOUND_EVENT,"omnira");
    public static final java.util.function.Supplier<SoundEvent> TIME_MUSIC=TYPES.register("music.time",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","music.time")));
    public static final java.util.function.Supplier<SoundEvent> LIU_NIAN_DISC=TYPES.register("music_disc.liu_nian",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","music_disc.liu_nian")));
    public static final java.util.function.Supplier<SoundEvent> MIRROR_DREAM_MUSIC=TYPES.register("music.mirror_dream",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","music.mirror_dream")));
    public static final java.util.function.Supplier<SoundEvent> MI_MENG_YE_DISC=TYPES.register("music_disc.mi_meng_ye",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","music_disc.mi_meng_ye")));
    public static final java.util.function.Supplier<SoundEvent> NIGHT_HERON_CALL=TYPES.register("night_heron_call",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","night_heron_call")));
    private ModSounds() {}
    public static final java.util.function.Supplier<SoundEvent> VIOLIN_ALLEMANDA=TYPES.register("violin_allemanda",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","violin_allemanda")));
    public static final java.util.function.Supplier<SoundEvent> VIOLIN_ADAGIO=TYPES.register("violin_adagio",
            ()->SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("omnira","violin_adagio")));
}
