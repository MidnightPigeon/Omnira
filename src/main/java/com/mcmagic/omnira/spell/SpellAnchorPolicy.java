package com.mcmagic.omnira.spell;

import net.minecraft.util.StringRepresentable;

public enum SpellAnchorPolicy implements StringRepresentable {
    CASTER("caster"),
    GROUND("ground"),
    SELF_PROJECTILE("self_projectile"),
    RAY_ENDPOINT("ray_endpoint"),
    REQUIRED_HIT("required_hit"),
    PROJECTILE("projectile");

    private final String serializedName;

    SpellAnchorPolicy(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
