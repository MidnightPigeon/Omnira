package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class AffinityEffects {
    private static final ResourceLocation BONUS=ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"affinity");
    private AffinityEffects() {}

    public static void refresh(ServerPlayer player) {
        var affinity=player.getData(ModAttachments.AFFINITY).active();
        attribute(player,ModAttributes.MAX_MANA,affinity.capacity,AttributeModifier.Operation.ADD_VALUE);
        attribute(player,ModAttributes.MANA_REGEN,affinity.regeneration,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        attribute(player,ModAttributes.SPELL_POWER,affinity.power,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        attribute(player,ModAttributes.COST_REDUCTION,affinity.reduction,AttributeModifier.Operation.ADD_VALUE);
        attribute(player,ModAttributes.DAMAGE_TAKEN,affinity==Affinity.DREAM?.5:0,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        attribute(player,net.neoforged.neoforge.common.NeoForgeMod.CREATIVE_FLIGHT,affinity==Affinity.DREAM?1:0,AttributeModifier.Operation.ADD_VALUE);
        boolean dream=affinity==Affinity.DREAM;
        if(player.getData(ModAttachments.DREAM_AFFINITY)!=dream) {
            if(!dream && !player.mayFly() && player.getAbilities().flying) {
                player.getAbilities().flying=false;player.onUpdateAbilities();
            }
            player.setData(ModAttachments.DREAM_AFFINITY,dream);
        }
    }
    private static void attribute(ServerPlayer player,Holder<Attribute> type,double amount,AttributeModifier.Operation operation) {
        var attribute=player.getAttribute(type);
        if(attribute==null)return;
        var old=attribute.getModifier(BONUS);
        if(old!=null && old.amount()==amount && old.operation()==operation)return;
        if(old!=null)attribute.removeModifier(BONUS);
        if(amount!=0)attribute.addTransientModifier(new AttributeModifier(BONUS,amount,operation));
    }
    public static boolean spend(ServerPlayer player) {
        var mana=player.getData(ModAttachments.MANA);
        if(!mana.canSpend(20))return false;
        player.setData(ModAttachments.MANA,mana.spend(20));
        return true;
    }
    @SubscribeEvent public static void hit(LivingDamageEvent.Post event) {
        if(event.getNewDamage()<=0 || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || !player.isAlive())return;
        var affinity=player.getData(ModAttachments.AFFINITY).active();
        if((affinity!=Affinity.LIGHT && affinity!=Affinity.DARK) || !spend(player))return;
        if(affinity==Affinity.LIGHT)event.getEntity().igniteForSeconds(10);
        else event.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON,100,1));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void defend(LivingDamageEvent.Pre event) {
        if(event.getNewDamage()>0 && event.getEntity() instanceof ServerPlayer player
                && player.getData(ModAttachments.AFFINITY).active()==Affinity.ELEMENTAL && spend(player))
            event.setNewDamage(Math.max(0,event.getNewDamage()-2));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void died(LivingDeathEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && player.getData(ModAttachments.AFFINITY).penaltyTicks()>0) {
            player.setData(ModAttachments.AFFINITY,AffinityState.NONE);
            refresh(player);
        }
    }
    public static void tick(ServerPlayer player) {
        refresh(player);
        var state=player.getData(ModAttachments.AFFINITY);
        int remaining=state.penaltyTicks();
        if(remaining==0)return;
        var affinity=state.affinity();
        int elapsed=affinity.penaltyDuration()-remaining+1;
        // Short renewed effects survive reconnects without restarting the persisted penalty clock.
        if(elapsed%20==1) {
            if(affinity==Affinity.LIGHT)player.igniteForSeconds(Math.min(1,remaining/20F));
            else if(affinity==Affinity.DARK)player.addEffect(new MobEffectInstance(MobEffects.POISON,Math.min(40,remaining),1));
            else {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,Math.min(40,remaining),2));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,Math.min(40,remaining),2));
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,Math.min(40,remaining),0));
            }
        }
        if(affinity==Affinity.DARK && elapsed%100==0) {
            var cloud=new com.mcmagic.omnira.entity.ShadowMist(ModEntityTypes.SHADOW_MIST.get(),player.level());
            cloud.configureSpell(null,com.mcmagic.omnira.spell.SpellEffect.utility("dark_breath",0,0,0),1,1,false);
            cloud.setPos(player.position());player.level().addFreshEntity(cloud);
        } else if(affinity!=Affinity.DARK && elapsed%10==0) {
            player.hurt(penaltyDamage(player,affinity),1);
        }
        // A lethal tick clears the attachment in died(); do not resurrect that state here.
        if(player.isAlive())player.setData(ModAttachments.AFFINITY,remaining==1?AffinityState.NONE:new AffinityState(state.kind(),remaining-1));
    }
    private static net.minecraft.world.damagesource.DamageSource penaltyDamage(ServerPlayer player,Affinity affinity) {
        var type=(affinity==Affinity.LIGHT?player.damageSources().inWall():player.damageSources().magic()).typeHolder();
        // Fire and combat invulnerability frames must not swallow the scheduled half-second pulses.
        return new net.minecraft.world.damagesource.DamageSource(type) {
            @Override public boolean is(net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> tag) {
                return tag.equals(net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN) || super.is(tag);
            }
        };
    }
}
