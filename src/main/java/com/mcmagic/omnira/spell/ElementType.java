package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.serialization.Codec;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum ElementType implements StringRepresentable {
    EARTH("earth", ElementTier.PRIMARY, SpellTargetKeyword.GROUND, SpellShapeKeyword.BARRIER, ModItems.EARTH_MICROCORE),
    WATER("water", ElementTier.PRIMARY, SpellTargetKeyword.SELF, SpellShapeKeyword.ORBIT, ModItems.WATER_MICROCORE),
    FIRE("fire", ElementTier.PRIMARY, SpellTargetKeyword.TARGET, SpellShapeKeyword.BURST, ModItems.FIRE_MICROCORE),
    AIR("air", ElementTier.PRIMARY, SpellTargetKeyword.AIM, SpellShapeKeyword.PROJECTILE, ModItems.AIR_MICROCORE),
    LIGHT("light", ElementTier.SECONDARY, null, null, ModItems.LIGHT_MICROCORE),
    DARK("dark", ElementTier.SECONDARY, null, null, ModItems.DARK_MICROCORE),
    SPACE("space", ElementTier.ADVANCED, SpellTargetKeyword.AIM, SpellShapeKeyword.PROJECTILE, ModItems.SPACE_MICROCORE),
    TIME("time", ElementTier.ADVANCED, SpellTargetKeyword.SELF, SpellShapeKeyword.BURST, ModItems.TIME_MICROCORE),
    OMNI("omni", ElementTier.ULTIMATE, null, null, ModItems.OMNI_MICROCORE);

    public static final Codec<ElementType> CODEC = StringRepresentable.fromEnum(ElementType::values);

    private final String serializedName;
    private final ElementTier tier;
    private final SpellTargetKeyword targetKeyword;
    private final SpellShapeKeyword shapeKeyword;
    private final Supplier<? extends Item> microcoreItem;

    ElementType(
            String serializedName,
            ElementTier tier,
            SpellTargetKeyword targetKeyword,
            SpellShapeKeyword shapeKeyword,
            Supplier<? extends Item> microcoreItem
    ) {
        this.serializedName = serializedName;
        this.tier = tier;
        this.targetKeyword = targetKeyword;
        this.shapeKeyword = shapeKeyword;
        this.microcoreItem = microcoreItem;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }

    public ElementTier tier() {
        return tier;
    }

    public Optional<SpellTargetKeyword> targetKeyword() {
        return Optional.ofNullable(targetKeyword);
    }

    public Optional<SpellShapeKeyword> shapeKeyword() {
        return Optional.ofNullable(shapeKeyword);
    }

    public Supplier<? extends Item> microcoreItem() {
        return microcoreItem;
    }

    public boolean isPrimary() {
        return tier == ElementTier.PRIMARY;
    }

    public int tooltipColor() {
        return switch (this) {
            case EARTH -> 0x9A714A;
            case WATER -> 0x55AAFF;
            case FIRE -> 0xFF6A3D;
            case AIR -> 0x6EEDE4;
            case LIGHT -> 0xFFF49A;
            case DARK -> 0x7B5AA6;
            case SPACE -> 0x6F7CFF;
            case TIME -> 0x8FE08F;
            case OMNI -> 0xE9D5FF;
        };
    }

    public static Optional<ElementType> bySerializedName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        for (ElementType element : values()) {
            if (element.serializedName.equals(normalizedName)) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    public static Optional<ElementType> byMicrocore(ItemStack stack) {
        for (ElementType element : values()) {
            if (stack.is(element.microcoreItem.get())) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }
}
