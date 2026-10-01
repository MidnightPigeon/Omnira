package com.mcmagic.omnira.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OmniraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GIVE_STARTER_GUIDE;
    public static final ModConfigSpec.IntValue CRYSTAL_BALL_DISPLAY_MODE;
    static {
        var builder=new ModConfigSpec.Builder();
        builder.push("guide");
        GIVE_STARTER_GUIDE=builder.comment("Give the guide once when a player first joins this world. Does not replace held items.")
                .define("giveOnFirstJoin",true);
        builder.pop();
        builder.push("rendering").push("crystalBall");
        CRYSTAL_BALL_DISPLAY_MODE=builder.comment("1 = roaming light motes; 2 = roaming miniature stored items (default).",
                "One display per occupied slot. Empty balls display no contents. This is a local visual preference.")
                .defineInRange("displayMode",2,1,2);
        builder.pop(2);SPEC=builder.build();
    }
    private OmniraConfig() {}
}
