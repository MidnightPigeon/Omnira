package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class ThrownUnorderedCluster extends ThrowableItemProjectile {
    private static final int RADIUS = 5;
    public ThrownUnorderedCluster(EntityType<? extends ThrownUnorderedCluster> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }
    @Override protected Item getDefaultItem() { return ModItems.UNORDERED_ELEMENT_CLUSTER.get(); }

    @Override public void tick() {
        var velocity = getDeltaMovement();
        super.tick();
        if (!isRemoved()) {
            // Keep the vanilla swept collision and portal handling, without drag.
            if (getDeltaMovement().equals(velocity.scale(isInWater() ? .8F : .99F))) setDeltaMovement(velocity);
            if (!level().isClientSide && tickCount >= 1200) discard();
        }
    }

    @Override protected void onHit(HitResult hit) {
        if (!isRemoved() && level() instanceof ServerLevel server) {
            discard();
            burst(server, hit.getLocation(), getOwner(), this, 20);
        }
    }

    public static void burst(ServerLevel level, Vec3 center, Entity owner, Entity projectile, float damage) {
        SpellEntity.spawn(level, owner, SpellEntity.Kind.BURST, center).burstRadius(RADIUS);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 1, 1);
        var source = projectile == null ? level.damageSources().magic() : level.damageSources().indirectMagic(projectile, owner);
        var bounds = new AABB(center, center).inflate(RADIUS);
        for (var target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                e -> e.isAlive() && !e.isSpectator() && e.getBoundingBox().distanceToSqr(center) <= RADIUS * RADIUS)) {
            target.hurt(source, damage);
        }
    }
}
