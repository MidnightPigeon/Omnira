package com.mcmagic.omnira.event;

import com.mcmagic.omnira.entity.DreamMirror;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class MirrorProjectileEvents {
    private static boolean recent(Projectile projectile,DreamMirror mirror) {
        var data=projectile.getPersistentData();
        return data.hasUUID("MirrorBounce") && data.getUUID("MirrorBounce").equals(mirror.getUUID())
                && projectile.level().getGameTime()-data.getLong("MirrorBounceTime")<8;
    }
    public static boolean bounce(Projectile projectile,DreamMirror mirror,Vec3 hit) {
        if(projectile.level().isClientSide || recent(projectile,mirror)) return false;
        var source=projectile instanceof ThrownTrident trident?projectile.damageSources().trident(trident,projectile.getOwner())
                :projectile instanceof AbstractArrow arrow?projectile.damageSources().arrow(arrow,projectile.getOwner())
                :projectile.damageSources().mobProjectile(projectile,projectile.getOwner() instanceof net.minecraft.world.entity.LivingEntity living?living:null);
        return bounce(projectile,mirror,hit,source,physicalImpact(projectile,mirror,source));
    }
    private static float physicalImpact(Projectile projectile,DreamMirror mirror,DamageSource source) {
        if(projectile instanceof AbstractArrow arrow) {
            float base=projectile instanceof ThrownTrident?8:(float)arrow.getBaseDamage();
            if(arrow.getWeaponItem()!=null && projectile.level() instanceof ServerLevel server)
                base=EnchantmentHelper.modifyDamage(server,arrow.getWeaponItem(),mirror,source,base);
            if(projectile instanceof ThrownTrident) return base;
            int damage=Mth.ceil(Mth.clamp((float)projectile.getDeltaMovement().length()*(double)base,0,2147483647));
            if(arrow.isCritArrow()) damage=(int)Math.min(Integer.MAX_VALUE,(long)damage+mirror.getRandom().nextInt(damage/2+2));
            return damage;
        }
        if(projectile instanceof ShulkerBullet) return 4;
        if(projectile instanceof LlamaSpit || projectile instanceof net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge) return 1;
        return 0;
    }
    public static boolean bounce(Projectile projectile,DreamMirror mirror,Vec3 hit,DamageSource source,float amount) {
        if(projectile.level().isClientSide || recent(projectile,mirror)) return false;
        var velocity=projectile.getDeltaMovement();
        if(velocity.lengthSqr()<1.0E-8) return false;
        var owner=projectile.getOwner();
        var incoming=projectile;
        if(projectile instanceof net.minecraft.world.entity.projectile.ShulkerBullet) {
            var replacement=new net.minecraft.world.entity.projectile.ShulkerBullet(projectile.level(),mirror,owner,net.minecraft.core.Direction.Axis.Y);
            replacement.setPos(hit.subtract(velocity.normalize().scale(.6)));
            replacement.setDeltaMovement(velocity.scale(-1));
            if(!projectile.level().addFreshEntity(replacement)) return false;
            projectile.discard();projectile=replacement;
        } else {
            projectile.deflect((p,e,r)->{
                p.setDeltaMovement(velocity.scale(-1));p.setYRot(p.getYRot()+180);p.yRotO+=180;
            },mirror,mirror,false);
            projectile.setPos(hit.subtract(velocity.normalize().scale(.6)));
        }
        projectile.hasImpulse=true;projectile.hurtMarked=true;
        var data=projectile.getPersistentData();
        data.putUUID("MirrorBounce",mirror.getUUID());data.putLong("MirrorBounceTime",projectile.level().getGameTime());
        applyNonphysicalEffects(incoming,mirror,owner);
        if(source.is(DreamMirror.PHYSICAL)) mirror.returnPhysicalDamage(owner,source,amount);
        return true;
    }
    private static void applyNonphysicalEffects(Projectile projectile,DreamMirror mirror,net.minecraft.world.entity.Entity owner) {
        // Preserve vanilla secondary effects without invoking onHit, which would consume the projectile.
        if(projectile instanceof Arrow arrow) {
            var contents=arrow.getPickupItemStackOrigin().getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS,
                    net.minecraft.world.item.alchemy.PotionContents.EMPTY);
            contents.potion().ifPresent(potion->{
                for(var effect:potion.value().getEffects()) mirror.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        effect.getEffect(),Math.max(effect.mapDuration(ticks->ticks/8),1),effect.getAmplifier(),
                        effect.isAmbient(),effect.isVisible()),owner);
            });
            for(var effect:contents.customEffects()) mirror.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect),owner);
        } else if(projectile instanceof ShulkerBullet) {
            mirror.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION,200),owner);
        } else if(projectile instanceof SpectralArrow) {
            var tag=projectile.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            mirror.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,tag.getInt("Duration")),owner);
        }
        if(projectile.isOnFire()) mirror.igniteForSeconds(5);
    }
    @SubscribeEvent public static void beforeTick(EntityTickEvent.Pre event) {
        if(!(event.getEntity() instanceof Projectile projectile) || projectile.level().isClientSide) return;
        var from=projectile.position();var to=from.add(projectile.getDeltaMovement());
        if(from.distanceToSqr(to)<1.0E-8) return;
        var wall=projectile.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,projectile));
        var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(projectile.level(),projectile,from,wall.getLocation(),
                projectile.getBoundingBox().expandTowards(to.subtract(from)).inflate(.2),
                e->e instanceof DreamMirror m && m.isAlive() && projectile.getOwner()!=m && !recent(projectile,m),.1F);
        if(hit!=null && bounce(projectile,(DreamMirror)hit.getEntity(),hit.getLocation())) event.setCanceled(true);
    }
    @SubscribeEvent public static void impact(ProjectileImpactEvent event) {
        if(event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof DreamMirror mirror
                && !event.getProjectile().level().isClientSide) {
            if(recent(event.getProjectile(),mirror) || bounce(event.getProjectile(),mirror,hit.getLocation())) event.setCanceled(true);
        }
    }
}
