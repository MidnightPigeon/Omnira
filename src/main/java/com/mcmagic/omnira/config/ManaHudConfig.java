package com.mcmagic.omnira.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ManaHudConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue OFFSET_X, OFFSET_Y;
    static {
        var spec=new ModConfigSpec.Builder();
        spec.push("manaBar");
        OFFSET_X=spec.comment("Default X in GUI pixels from the upper-left corner of the hotbar. Positive is right.")
                .defineInRange("offsetX",-111,-32768,32767);
        OFFSET_Y=spec.comment("Default Y in GUI pixels from the upper-left corner of the hotbar. Positive is down.")
                .defineInRange("offsetY",4,-32768,32767);
        spec.pop();
        SPEC=spec.build();
    }
    private ManaHudConfig(){}
}
