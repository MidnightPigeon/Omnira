package com.mcmagic.omnira.spell;

import net.minecraft.util.StringRepresentable;

public enum SpellTargetKeyword implements StringRepresentable {
    GROUND("ground"),
    SELF("self"),
    TARGET("target"),
    AIM("aim");

    private final String serializedName;

    SpellTargetKeyword(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
