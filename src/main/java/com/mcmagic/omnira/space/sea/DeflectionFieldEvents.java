package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid="omnira")
public final class DeflectionFieldEvents {
    public static ItemStack held(LivingEntity entity){
        if(entity.getMainHandItem().getItem() instanceof DeflectionFieldItem)return entity.getMainHandItem();
        if(entity.getOffhandItem().getItem() instanceof DeflectionFieldItem)return entity.getOffhandItem();
        return ItemStack.EMPTY;
    }
    public static boolean raised(LivingEntity entity){return entity.isUsingItem()&&entity.getUseItem().getItem() instanceof DeflectionFieldItem;}
    @SubscribeEvent public static void dodge(LivingIncomingDamageEvent event){
        if(!event.getEntity().level().isClientSide&&!held(event.getEntity()).isEmpty()&&event.getSource().getEntity()!=null
                &&event.getEntity().getRandom().nextFloat()<.25F)event.setCanceled(true);
    }
    @SubscribeEvent public static void shieldBlock(LivingShieldBlockEvent event){
        // This field reduces damage by 25%, not vanilla's complete frontal shield block.
        if(raised(event.getEntity()))event.setCanceled(true);
    }
    @SubscribeEvent public static void nonplayerReduction(LivingDamageEvent.Pre event){
        if(!(event.getEntity() instanceof net.minecraft.world.entity.player.Player)&&raised(event.getEntity()))event.setNewDamage(event.getNewDamage()*.75F);
    }
    public static boolean redirect(Entity shot,LivingEntity holder,Vec3 hit){
        var stack=held(holder);var velocity=shot.getDeltaMovement();
        if(stack.isEmpty()||!(shot.level() instanceof ServerLevel server)||velocity.lengthSqr()<1e-8)return false;
        var data=shot.getPersistentData();
        if(data.hasUUID("DeflectionHolder")&&data.getUUID("DeflectionHolder").equals(holder.getUUID())
                &&server.getGameTime()-data.getLong("DeflectionTick")<8)return false;
        Vec3 outgoing=raised(holder)?velocity.scale(-1):velocity.yRot((holder.getRandom().nextBoolean()?1:-1)
                *(float)Math.toRadians(35+holder.getRandom().nextInt(46))).xRot((float)Math.toRadians(holder.getRandom().nextInt(31)-15));
        if(shot instanceof SpellEntity spell){if(!spell.deflectFrom(holder,outgoing))return false;}
        else if(shot instanceof Projectile projectile){
            if(projectile instanceof ShulkerBullet){
                var replacement=new ShulkerBullet(server,holder,projectile.getOwner(),net.minecraft.core.Direction.Axis.Y);
                replacement.setPos(hit.add(outgoing.normalize().scale(.6)));replacement.setDeltaMovement(outgoing);
                if(!server.addFreshEntity(replacement))return false;
                projectile.discard();shot=replacement;data=shot.getPersistentData();
            }else projectile.deflect((p,e,r)->p.setDeltaMovement(outgoing),holder,holder,false);
        }else return false;
        // Vanilla arrows damp their velocity after deflect(); this field changes direction only.
        shot.setDeltaMovement(outgoing);shot.setPos(hit.add(outgoing.normalize().scale(.6)));shot.hasImpulse=true;shot.hurtMarked=true;
        data.putUUID("DeflectionHolder",holder.getUUID());data.putLong("DeflectionTick",server.getGameTime());
        DeflectionFieldItem.wear(holder,stack);
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,hit.x,hit.y,hit.z,6,.15,.15,.15,.02);
        return true;
    }
    @SubscribeEvent public static void intercept(EntityTickEvent.Pre event){
        Entity shot=event.getEntity();if(!(shot.level() instanceof ServerLevel server))return;
        if(!(shot instanceof Projectile)&&!(shot instanceof SpellEntity spell&&spell.deflectable()))return;
        Vec3 from=shot.getBoundingBox().getCenter(),movement=shot.getDeltaMovement(),to=from.add(movement);
        if(movement.lengthSqr()<1e-8)return;
        var wall=server.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,shot));
        var hit=ProjectileUtil.getEntityHitResult(server,shot,from,wall.getLocation(),shot.getBoundingBox().expandTowards(movement).inflate(.3),
            e->e instanceof LivingEntity living&&living.isAlive()&&!living.isSpectator()&&!held(living).isEmpty()
                &&!(shot instanceof Projectile p&&p.getOwner()==e)&&!(shot instanceof SpellEntity s&&s.ownedBy(e)),.2F);
        if(hit!=null&&redirect(shot,(LivingEntity)hit.getEntity(),hit.getLocation()))event.setCanceled(true);
    }
    @SubscribeEvent public static void impact(net.neoforged.neoforge.event.entity.ProjectileImpactEvent event){
        if(event.getRayTraceResult() instanceof EntityHitResult hit&&hit.getEntity() instanceof LivingEntity living
                &&redirect(event.getProjectile(),living,hit.getLocation()))event.setCanceled(true);
    }
    private DeflectionFieldEvents(){}
}
