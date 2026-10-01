package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.spell.SpellPattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Omnira.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> AGGREGATE_TICKS=
            DATA_COMPONENTS.registerComponentType("aggregate_ticks",b->b.persistent(com.mojang.serialization.Codec.intRange(0,9)));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Long>> AGGREGATE_REPAIRED_AT=
            DATA_COMPONENTS.registerComponentType("aggregate_repaired_at",b->b.persistent(com.mojang.serialization.Codec.LONG));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Long>> SPELL_READY_AT=
            DATA_COMPONENTS.registerComponentType("spell_ready_at",b->b.persistent(com.mojang.serialization.Codec.LONG));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Boolean>> ANCESTOR_LAUNCHER=
            DATA_COMPONENTS.registerComponentType("ancestor_launcher",b->b.persistent(com.mojang.serialization.Codec.BOOL));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<com.mcmagic.omnira.item.ArquebusPlugin>> ARQUEBUS_PLUGIN=
            DATA_COMPONENTS.registerComponentType("arquebus_plugin",b->b.persistent(com.mcmagic.omnira.item.ArquebusPlugin.CODEC));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<com.mcmagic.omnira.item.staff.StaffPart>> STAFF_PART=
            DATA_COMPONENTS.registerComponentType("staff_part",b->b.persistent(com.mcmagic.omnira.item.staff.StaffPart.CODEC));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<com.mcmagic.omnira.item.staff.StaffAssembly>> STAFF_ASSEMBLY=
            DATA_COMPONENTS.registerComponentType("staff_assembly",b->b.persistent(com.mcmagic.omnira.item.staff.StaffAssembly.CODEC));
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<java.util.UUID>> BOTTLE_CAPTURE=
            DATA_COMPONENTS.registerComponentType("bottle_capture",b->b.persistent(net.minecraft.core.UUIDUtil.CODEC));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SpellPattern>> SPELL_PATTERN =
            DATA_COMPONENTS.registerComponentType("spell_pattern", builder -> builder
                    .persistent(SpellPattern.CODEC)
                    .cacheEncoding());

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<com.mcmagic.omnira.spell.SpellPayload>> SPELL_PAYLOAD =
            DATA_COMPONENTS.registerComponentType("spell_payload",b->b.persistent(com.mcmagic.omnira.spell.SpellPayload.CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> GRID_CURSOR =
            DATA_COMPONENTS.registerComponentType("grid_cursor",b->b.persistent(com.mojang.serialization.Codec.intRange(0,11)));

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
