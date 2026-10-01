package com.mcmagic.omnira.time;

import com.mcmagic.omnira.spacetime.AnchoringSigilItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

public final class TemporalCreatureTags {
    // Tag future creatures from the archaeological egg and spirit seed on registration.
    public static final TagKey<EntityType<?>> TIME=tag("time_creatures");
    public static final TagKey<EntityType<?>> DREAM=tag("dream_creatures");

    public static boolean timeImmune(Entity entity){
        return entity.getType().is(TIME)
                ||entity instanceof Player player&&AnchoringSigilItem.equipped(player);
    }

    private static TagKey<EntityType<?>> tag(String name){
        return TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("omnira",name));
    }
    private TemporalCreatureTags(){}
}
