package com.mcmagic.omnira.paradox;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.registries.*;

public final class ParadoxContent {
    public static final DeferredItem<Item> MYSTERY=ModItems.ITEMS.registerSimpleItem("paradox_mystery");
    public static final DeferredItem<Item> ORE=ModItems.ITEMS.registerSimpleItem("paradox_ore");
    public static final DeferredItem<Item> BOTTLED_FLOW=ModItems.ITEMS.registerSimpleItem("encapsulated_paradox_flow");
    public static final DeferredItem<Item> UNSTABLE_SOURCE=ModItems.ITEMS.register("unstable_paradox_source",()->new com.mcmagic.omnira.spacetime.UnstableAggregateItem(new Item.Properties(),false));
    public static final DeferredItem<Item> STABLE_SOURCE=ModItems.ITEMS.registerSimpleItem("encapsulated_stable_paradox_source");
    public static final DeferredItem<Item> STABLE_AGGREGATE=ModItems.ITEMS.registerSimpleItem("encapsulated_stable_spacetime_aggregate");
    public static final DeferredItem<Item> GENESIS=ModItems.ITEMS.register("genesis_particles",()->new GenesisParticlesItem(new Item.Properties()));
    public static final DeferredHolder<EntityType<?>,EntityType<VoidAnvil>> ANVIL=ModEntityTypes.ENTITY_TYPES.register("void_anvil",()->
            EntityType.Builder.<VoidAnvil>of(VoidAnvil::new,MobCategory.MISC).sized(.98F,.98F).clientTrackingRange(8)
                    .updateInterval(1).fireImmune().noSummon().build("omnira:void_anvil"));
    public static void init(){}
    private ParadoxContent(){}
}
