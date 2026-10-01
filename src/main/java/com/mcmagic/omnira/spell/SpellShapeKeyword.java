package com.mcmagic.omnira.spell;

import net.minecraft.util.StringRepresentable;

public enum SpellShapeKeyword implements StringRepresentable {
    BARRIER("barrier"),
    ORBIT("orbit"),
    BURST("burst"),
    PROJECTILE("projectile");

    private final String serializedName;

    SpellShapeKeyword(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
