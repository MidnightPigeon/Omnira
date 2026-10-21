package com.mcmagic.omnira.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class OmniraConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GIVE_STARTER_GUIDE;
    public static final ModConfigSpec.IntValue CRYSTAL_BALL_DISPLAY_MODE;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RELOAD_BLOCK_BLACKLIST;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> RELOAD_ENTITY_BLACKLIST;
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
        builder.pop(2);
        builder.push("spells").push("reload");
        RELOAD_BLOCK_BLACKLIST=builder.comment("Block IDs excluded as both reload targets and random results. Server-side rules; restart after editing.")
                .defineListAllowEmpty("blockBlacklist",java.util.List.of("minecraft:bedrock","omnira:pure_solidified_spacetime",
                        "create:wooden_bracket","create:copycat_base","create:metal_bracket","create:water_wheel_structure",
                        "create:copycat_bars","minecraft:frosted_ice","omnira:permanent_reinforced_void_crystal"),OmniraConfig::registryId);
        RELOAD_ENTITY_BLACKLIST=builder.comment("Entity type IDs excluded as both reload targets and random results. Does not affect World Rotation.")
                .defineListAllowEmpty("entityBlacklist",java.util.List.of("minecraft:ender_dragon"),OmniraConfig::registryId);
        builder.pop(2);SPEC=builder.build();
    }
    private static boolean registryId(Object value){return value instanceof String id&&id.contains(":")&&net.minecraft.resources.ResourceLocation.tryParse(id)!=null;}
    private OmniraConfig() {}
}
