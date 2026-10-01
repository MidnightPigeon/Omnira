package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Evaluates implement bonuses without mutating equipment attributes or subtracting clamped values. */
public final class CastAttributes {
    public static final ResourceLocation STAFF_POWER = id("staff_power");
    public static final ResourceLocation STAFF_DISCOUNT = id("staff_discount");
    public static final ResourceLocation STAFF_COOLDOWN = id("staff_cooldown");
    public static final ResourceLocation ARQUEBUS_POWER = id("arquebus_power");

    public static double power(LivingEntity caster, double bonus) {
        return value(caster,ModAttributes.SPELL_POWER,STAFF_POWER,bonus,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
    public static double reduction(LivingEntity caster, double bonus) {
        return value(caster,ModAttributes.COST_REDUCTION,STAFF_DISCOUNT,bonus,AttributeModifier.Operation.ADD_VALUE);
    }
    public static double cooldownReduction(LivingEntity caster, double bonus) {
        return value(caster,ModAttributes.COOLDOWN_REDUCTION,STAFF_COOLDOWN,bonus,AttributeModifier.Operation.ADD_VALUE);
    }
    private static double value(LivingEntity caster, Holder<Attribute> attribute, ResourceLocation id,
                                double amount, AttributeModifier.Operation operation) {
        var source=caster.getAttribute(attribute);
        var snapshot=new AttributeInstance(attribute,ignored->{});
        snapshot.setBaseValue(source.getBaseValue());
        for(var modifier:source.getModifiers()) {
            if(!modifier.id().equals(id) && !modifier.id().equals(ARQUEBUS_POWER)) snapshot.addTransientModifier(modifier);
        }
        if(amount!=0) snapshot.addTransientModifier(new AttributeModifier(id,amount,operation));
        return snapshot.getValue();
    }
    private static ResourceLocation id(String path) {return ResourceLocation.fromNamespaceAndPath("omnira",path);}
    private CastAttributes() {}
}
