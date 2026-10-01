package com.mcmagic.omnira.spell;

import net.minecraft.util.StringRepresentable;

public enum ElementTier implements StringRepresentable {
    PRIMARY("primary"),
    SECONDARY("secondary"),
    ADVANCED("advanced"),
    ULTIMATE("ultimate");

    private final String serializedName;

    ElementTier(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
