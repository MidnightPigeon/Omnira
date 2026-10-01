package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Omnira.MOD_ID);
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.throne.GoldenThroneSeat>> GOLDEN_THRONE_SEAT=
            ENTITY_TYPES.register("golden_throne_seat",()->EntityType.Builder.<com.mcmagic.omnira.throne.GoldenThroneSeat>of(
                    com.mcmagic.omnira.throne.GoldenThroneSeat::new,MobCategory.MISC).sized(.1F,.1F)
                    .noSave().noSummon().clientTrackingRange(6).build("omnira:golden_throne_seat"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.spacetime.TemporalAmber>> TEMPORAL_AMBER=
            ENTITY_TYPES.register("temporal_amber",()->EntityType.Builder.<com.mcmagic.omnira.spacetime.TemporalAmber>of(
                    com.mcmagic.omnira.spacetime.TemporalAmber::new,MobCategory.MISC).sized(.75F,1).clientTrackingRange(8).fireImmune().build("omnira:temporal_amber"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.spacetime.ThrownAggregate>> UNSTABLE_AGGREGATE=
            ENTITY_TYPES.register("unstable_aggregate",()->EntityType.Builder.<com.mcmagic.omnira.spacetime.ThrownAggregate>of(
                    com.mcmagic.omnira.spacetime.ThrownAggregate::new,MobCategory.MISC).sized(.3F,.3F).clientTrackingRange(8).updateInterval(1).fireImmune().build("omnira:unstable_aggregate"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.vehicle.CrystalBroomEntity>> CRYSTAL_BROOM=
            ENTITY_TYPES.register("crystal_broom",()->EntityType.Builder.<com.mcmagic.omnira.vehicle.CrystalBroomEntity>of(
                    com.mcmagic.omnira.vehicle.CrystalBroomEntity::new,MobCategory.MISC).sized(1.5F,.4F).clientTrackingRange(10).updateInterval(1).build("omnira:crystal_broom"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.vehicle.CruiseOrbEntity>> CRUISE_ORB=
            ENTITY_TYPES.register("cruise_orb",()->EntityType.Builder.<com.mcmagic.omnira.vehicle.CruiseOrbEntity>of(
                    com.mcmagic.omnira.vehicle.CruiseOrbEntity::new,MobCategory.MISC).sized(3,3).clientTrackingRange(10).updateInterval(1).build("omnira:cruise_orb"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.entity.DreamMirror>> DREAM_MIRROR=
            ENTITY_TYPES.register("dream_mirror",()->EntityType.Builder.<com.mcmagic.omnira.entity.DreamMirror>of(
                    com.mcmagic.omnira.entity.DreamMirror::new,MobCategory.CREATURE).sized(1,1.8F).eyeHeight(.9F)
                    .clientTrackingRange(8).build("omnira:dream_mirror"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.entity.ShadowMist>> SHADOW_MIST =
            ENTITY_TYPES.register("shadow_mist",()->EntityType.Builder.<com.mcmagic.omnira.entity.ShadowMist>of(
                    com.mcmagic.omnira.entity.ShadowMist::new,MobCategory.MISC).sized(1,.65F).clientTrackingRange(8)
                    .updateInterval(5).fireImmune().build("omnira:shadow_mist"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.mcmagic.omnira.entity.LightSpirit>> LIGHT_SPIRIT =
            ENTITY_TYPES.register("light_spirit", () -> EntityType.Builder.<com.mcmagic.omnira.entity.LightSpirit>of(
                    com.mcmagic.omnira.entity.LightSpirit::new, MobCategory.CREATURE)
                    .sized(.65F, .65F).eyeHeight(.4F).clientTrackingRange(8).build("omnira:light_spirit"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.mcmagic.omnira.entity.ShadowGhost>> SHADOW_GHOST =
            ENTITY_TYPES.register("shadow_ghost", () -> EntityType.Builder.<com.mcmagic.omnira.entity.ShadowGhost>of(
                    com.mcmagic.omnira.entity.ShadowGhost::new, MobCategory.MONSTER)
                    .sized(.6F, 1.6F).eyeHeight(1.35F).clientTrackingRange(8).build("omnira:shadow_ghost"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.item.bottle.ThrownPocketBottle>> POCKET_BOTTLE=
            ENTITY_TYPES.register("pocket_bottle",()->EntityType.Builder.<com.mcmagic.omnira.item.bottle.ThrownPocketBottle>of(
                    com.mcmagic.omnira.item.bottle.ThrownPocketBottle::new,MobCategory.MISC).sized(.25F,.25F).clientTrackingRange(8).updateInterval(1).build("omnira:pocket_bottle"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpellEntity>> SPELL = ENTITY_TYPES.register("spell_shape",
            () -> EntityType.Builder.<SpellEntity>of(SpellEntity::new, MobCategory.MISC)
                    .sized(.36F, .36F).clientTrackingRange(8).updateInterval(1)
                    .noSave().noSummon().fireImmune().build("omnira:spell_shape"));

    private ModEntityTypes() {
    }
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.ancient.ArchaeopteryxSpirit>> ARCHAEOPTERYX_SPIRIT=
            ENTITY_TYPES.register("archaeopteryx_spirit",()->EntityType.Builder.of(com.mcmagic.omnira.ancient.ArchaeopteryxSpirit::new,MobCategory.CREATURE)
                    .sized(.65F,.7F).clientTrackingRange(8).updateInterval(1).noSummon().build("omnira:archaeopteryx_spirit"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.ancient.Velociraptor>> VELOCIRAPTOR=
            ENTITY_TYPES.register("velociraptor",()->EntityType.Builder.of(com.mcmagic.omnira.ancient.Velociraptor::new,MobCategory.CREATURE)
                    .sized(.85F,1.2F).passengerAttachments(1.05F).clientTrackingRange(8).updateInterval(1).noSummon().build("omnira:velociraptor"));
    public static final DeferredHolder<EntityType<?>,EntityType<com.mcmagic.omnira.item.FlyingNeedle>> FLYING_NEEDLE=
            ENTITY_TYPES.register("flying_needle",()->EntityType.Builder.<com.mcmagic.omnira.item.FlyingNeedle>of(com.mcmagic.omnira.item.FlyingNeedle::new,MobCategory.MISC)
                    .sized(.2F,.2F).clientTrackingRange(8).updateInterval(1).noSave().noSummon().build("omnira:flying_needle"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
        eventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)-> {
            event.put(ARCHAEOPTERYX_SPIRIT.get(),com.mcmagic.omnira.ancient.ArchaeopteryxSpirit.attributes().build());
            event.put(VELOCIRAPTOR.get(),com.mcmagic.omnira.ancient.Velociraptor.attributes().build());
        });
        eventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->
                event.put(DREAM_MIRROR.get(),com.mcmagic.omnira.entity.DreamMirror.attributes().build()));
        eventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event)->
                event.register(DREAM_MIRROR.get(),net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        com.mcmagic.omnira.entity.DreamMirror::canSpawn,
                        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
        eventBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                { event.put(LIGHT_SPIRIT.get(), net.minecraft.world.entity.animal.allay.Allay.createAttributes()
                        .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,8).build());
                  event.put(SHADOW_GHOST.get(),com.mcmagic.omnira.entity.ShadowGhost.attributes().build()); });
        eventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) ->
                event.register(SHADOW_GHOST.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        com.mcmagic.omnira.entity.ShadowGhost::canSpawn,
                        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
        eventBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) ->
                event.register(LIGHT_SPIRIT.get(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
                        net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        com.mcmagic.omnira.entity.LightSpirit::canSpawn,
                        net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE));
    }
}
