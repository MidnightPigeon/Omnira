package com.mcmagic.omnira.energy;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.capabilities.*;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class EnergyIntegration {
    private static RegisterCapabilitiesEvent registrations;
    private static volatile boolean available;
    private EnergyIntegration() {}
    public static boolean available() {
        if(registrations!=null) detect();
        return available;
    }
    @SubscribeEvent public static void register(RegisterCapabilitiesEvent event) {
        registrations=event;
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK,ModBlockEntityTypes.MANA_ENGINE.get(),
                (entity,side)->entity instanceof ManaEngineAccess engine?engine.engineState().energyPort(side):null);
    }
    private static boolean external(ResourceLocation id) {
        return !id.getNamespace().equals("omnira");
    }
    @SubscribeEvent public static void complete(FMLLoadCompleteEvent event) {
        event.enqueueWork(EnergyIntegration::detect);
    }
    private static synchronized void detect() {
            var registry=registrations;
            if(registry==null) return;
            // Inspect actual foreign providers after all mods have registered, not the always-present FE API.
            available=BuiltInRegistries.BLOCK.stream().anyMatch(block->external(BuiltInRegistries.BLOCK.getKey(block))
                    && registry.isBlockRegistered(Capabilities.EnergyStorage.BLOCK,block))
                    || BuiltInRegistries.ITEM.stream().anyMatch(item->external(BuiltInRegistries.ITEM.getKey(item))
                    && registry.isItemRegistered(Capabilities.EnergyStorage.ITEM,item))
                    || BuiltInRegistries.ENTITY_TYPE.stream().anyMatch(type->external(BuiltInRegistries.ENTITY_TYPE.getKey(type))
                    && registry.isEntityRegistered(Capabilities.EnergyStorage.ENTITY,type));
            registrations=null;
            Omnira.LOGGER.info("External FE providers detected: {}",available);
    }
}
