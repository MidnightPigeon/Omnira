package com.mcmagic.omnira.client;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID, value = Dist.CLIENT)
public final class SpellTrails {
    private SpellTrails() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.isPaused()) return;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof SpellEntity spell) || spell.isRemoved()) continue;
            if (spell.kind() != SpellEntity.Kind.PROJECTILE && spell.kind() != SpellEntity.Kind.FLYING_BLOCK
                    && spell.kind() != SpellEntity.Kind.SELF_FLIGHT) continue;
            if (client.player == null || client.player.distanceToSqr(spell) > 4096) continue;
            double height = spell.kind() == SpellEntity.Kind.SELF_FLIGHT ? .8 : spell.kind() == SpellEntity.Kind.FLYING_BLOCK ? .5 : 0;
            var movement = spell.getDeltaMovement();
            var particle = client.particleEngine.createParticle(ParticleTypes.FIREWORK,
                    spell.getX(), spell.getY() + height, spell.getZ(), -movement.x*.15, -movement.y*.15, -movement.z*.15);
            if (particle != null) {
                int rgb = spell.spellColor();
                particle.setColor(((rgb >> 16) & 255)/255F, ((rgb >> 8) & 255)/255F, (rgb & 255)/255F);
                particle.setLifetime(12);
                particle.scale(.6F);
            }
        }
    }
}
