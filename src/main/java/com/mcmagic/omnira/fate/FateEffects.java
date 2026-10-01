package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.item.FateCurioItem.Kind;
import com.mcmagic.omnira.registry.ModEffects;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class FateEffects {
    private static final ThreadLocal<Boolean> REFLECTING=ThreadLocal.withInitial(()->false);
    private FateEffects() {}

    public static double spellPower(net.minecraft.world.entity.player.Player caster,double power) {
        if(caster instanceof ServerPlayer player && ManuscriptReward.equipped(player,"curse")==Kind.TZEENTCH
                && player.getRandom().nextFloat()<.2F) {
            player.setHealth(player.getHealth()*.5F);
            return power+1;
        }
        return power;
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if(!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive())return;
        Kind curse=ManuscriptReward.equipped(player,"curse");
        if(curse==Kind.NURGLE && player.tickCount%60==0)
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,100,0));
        if(curse==Kind.SLAANESH && player.tickCount%100==0)
            for(Animal animal:player.level().getEntitiesOfClass(Animal.class,player.getBoundingBox().inflate(5),
                    animal->animal.isAlive() && animal.distanceToSqr(player)<=25 && animal.getAge()==0 && animal.canFallInLove()))
                animal.setInLove(null);
        if(player.hasEffect(ModEffects.PLEASURABLE_AGONY)) {
            Vec3 velocity=player.getDeltaMovement();
            if(velocity.x!=0 || velocity.y>0 || velocity.z!=0)player.setDeltaMovement(0,Math.min(velocity.y,0),0);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent.Pre event) {
        if(REFLECTING.get())return;
        float bonus=0;
        if(event.getSource().getEntity() instanceof ServerPlayer attacker)
            bonus+=(float)(attacker.getAttributeValue(ModAttributes.DAMAGE_DEALT)-1);
        if(event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker!=event.getEntity() && com.mcmagic.omnira.throne.GoldenThrone.empowered(attacker))bonus+=1F;
        if(event.getSource().getEntity() instanceof ServerPlayer attacker
                && attacker!=event.getEntity() && com.mcmagic.omnira.throne.GoldenToilet.empowered(attacker))bonus+=.2F;
        if(event.getEntity() instanceof ServerPlayer victim)
            bonus+=(float)(victim.getAttributeValue(ModAttributes.DAMAGE_TAKEN)-1);
        if(bonus!=0)event.setNewDamage(event.getNewDamage()*(1+bonus));
    }

    @SubscribeEvent public static void afterDamage(LivingDamageEvent.Post event) {
        if(REFLECTING.get() || event.getNewDamage()<=0 || !(event.getEntity() instanceof ServerPlayer player))return;
        Kind curse=ManuscriptReward.equipped(player,"curse");
        if(curse==Kind.NURGLE && player.getRandom().nextFloat()<.2F) {
            EntityType<?>[] types={EntityType.ZOMBIE,EntityType.DROWNED,EntityType.HUSK};
            var mob=types[player.getRandom().nextInt(types.length)].create(player.level());
            if(mob!=null) {
                mob.moveTo(player.getX()+player.getRandom().nextInt(3)-1,player.getY(),player.getZ()+player.getRandom().nextInt(3)-1,
                        player.getRandom().nextFloat()*360,0);
                player.level().addFreshEntity(mob);
            }
        }
        if(curse!=Kind.SLAANESH && !player.hasEffect(ModEffects.PLEASURABLE_AGONY))return;
        if(curse==Kind.SLAANESH)player.addEffect(new MobEffectInstance(ModEffects.PLEASURABLE_AGONY,60,0));
        if(event.getSource().getEntity() instanceof LivingEntity attacker && attacker!=player) {
            REFLECTING.set(true);
            try {attacker.hurt(player.damageSources().thorns(player),event.getNewDamage());}
            finally {REFLECTING.set(false);}
        }
    }
}
