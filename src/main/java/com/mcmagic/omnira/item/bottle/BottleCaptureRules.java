package com.mcmagic.omnira.item.bottle;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.neoforged.neoforge.common.Tags;

public final class BottleCaptureRules {
    public static final TagKey<EntityType<?>> FORBIDDEN=TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath("omnira","bottle_capture_forbidden"));
    private BottleCaptureRules() {}
    /** Do not require the capturer to be the owner, or an owner to be currently online. */
    public static boolean tamed(Entity entity) {
        return entity instanceof LivingEntity && (entity instanceof TamableAnimal animal && animal.isTame()
                || entity instanceof net.minecraft.world.entity.animal.horse.AbstractHorse horse && horse.isTamed()
                || entity instanceof OwnableEntity owned && owned.getOwnerUUID()!=null);
    }
    /** Capture-only restriction: existing saves must remain releasable. */
    public static boolean hostileOrAngry(Entity entity) {
        if(entity instanceof NeutralMob neutral) {
            if(neutral.getRemainingPersistentAngerTime()>0 || neutral.getPersistentAngerTarget()!=null)return true;
        } else if(entity instanceof net.minecraft.world.entity.monster.Enemy
                || entity.getType().getCategory()==MobCategory.MONSTER)return true;
        if(entity instanceof Mob mob) {
            var brain=mob.getBrain();
            return mob.getTarget()!=null || mob.isAggressive()
                    || brain.hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                    || brain.hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT);
        }
        return false;
    }
    public static boolean forbidden(Entity entity) {
        if(entity instanceof net.neoforged.neoforge.entity.PartEntity<?>) return true;
        if(entity.getType().is(FORBIDDEN) || entity.getType().is(Tags.EntityTypes.BOSSES)
                || entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
                || entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss || entity.isMultipartEntity()) return true;
        // Some older mods expose a boss bar without participating in the common boss tag.
        for(Class<?> type=entity.getClass();type!=null && type!=Entity.class;type=type.getSuperclass())
            for(var field:type.getDeclaredFields())
                if(net.minecraft.world.BossEvent.class.isAssignableFrom(field.getType())) return true;
        return false;
    }
    /** Standalone capture is for creatures; structures use their separate serializer. */
    public static boolean unsupportedStandalone(Entity entity) {
        return !(entity instanceof LivingEntity) || entity instanceof net.minecraft.world.entity.player.Player
                || entity instanceof net.minecraft.world.entity.decoration.ArmorStand
                || forbidden(entity);
    }
}
