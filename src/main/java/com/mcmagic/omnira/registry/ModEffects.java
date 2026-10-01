package com.mcmagic.omnira.registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.*;
import net.neoforged.neoforge.registries.*;
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,"omnira");
    public static final DeferredHolder<MobEffect,MobEffect> TIME_PASSAGE=EFFECTS.register("time_passage",com.mcmagic.omnira.mire.TimePassage::new);
    public static final DeferredHolder<MobEffect,MobEffect> TEMPORAL_DISLOCATION=EFFECTS.register("temporal_dislocation",com.mcmagic.omnira.time.TemporalDislocation::new);
    public static final DeferredHolder<MobEffect,MobEffect> CONSTRUCTION_LOCK=EFFECTS.register("construction_lock",()->new LockEffect());
    public static final DeferredHolder<MobEffect,MobEffect> PLEASURABLE_AGONY=EFFECTS.register("pleasurable_agony",()->new AgonyEffect());
    private static final class AgonyEffect extends MobEffect {
        AgonyEffect() {super(MobEffectCategory.HARMFUL,0xC869AE);
            addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","pleasurable_agony"),-1,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
    private static final class LockEffect extends MobEffect {
        LockEffect() {super(MobEffectCategory.HARMFUL,0xA0D8F0);
            addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","construction_lock"),-1,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        }
    }
}
