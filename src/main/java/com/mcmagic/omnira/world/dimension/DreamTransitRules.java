package com.mcmagic.omnira.world.dimension;

import net.minecraft.world.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;

public final class DreamTransitRules {
    private DreamTransitRules(){}
    public static boolean allowed(Entity root){
        return root.getSelfAndPassengers().allMatch(DreamTransitRules::ordinaryEntity);
    }
    private static boolean ordinaryEntity(Entity entity){
        String namespace=BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
        if(namespace.equals("sable") || namespace.equals("simulated") || namespace.equals("aeronautics"))return false;
        // Optional integration without loading Create classes when Create is absent.
        for(Class<?> type=entity.getClass();type!=null;type=type.getSuperclass())
            if(type.getName().equals("com.simibubi.create.content.contraptions.AbstractContraptionEntity"))return false;
        return !entity.isSpectator() && entity.isAlive();
    }
}
