package com.mcmagic.omnira.combat;

import com.mcmagic.omnira.space.phase.PhaseConcealment;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Exact health damage; reductions and payment failures are deliberately separate operations. */
public final class OmniraDamage {
    public static final ResourceKey<DamageType> TRUE_DAMAGE=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("omnira:true_damage"));
    public static final ResourceKey<DamageType> OVERDRAW=ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("omnira:overdraw"));
    private static final class Source extends DamageSource {
        final DamageSource original;
        final String reason;
        final boolean payment;
        Source(Entity target,DamageSource original,String reason,boolean payment){
            super(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(reason.equals("overdraw")?OVERDRAW:TRUE_DAMAGE),
                    original==null?null:original.getDirectEntity(),original==null?null:original.getEntity());
            this.original=original;this.reason=reason;this.payment=payment;
        }
        @Override public boolean is(net.minecraft.tags.TagKey<DamageType> tag){
            return tag.equals(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)&&original!=null&&original.is(tag)||super.is(tag);
        }
        @Override public net.minecraft.world.phys.Vec3 getSourcePosition(){return original==null?super.getSourcePosition():original.getSourcePosition();}
        @Override public Component getLocalizedDeathMessage(LivingEntity victim){
            if(reason.equals("overdraw"))return Component.translatable("death.attack.omnira.overdraw",victim.getDisplayName());
            if(reason.equals("true_damage"))return Component.translatable("death.attack.omnira.true_damage",victim.getDisplayName());
            if(original instanceof com.mcmagic.omnira.spell.SpellDamageSource)return original.getLocalizedDeathMessage(victim);
            if(reason.equals("time_ending"))return Component.translatable("death.attack.omnira.time_ending",victim.getDisplayName());
            if(!reason.equals("true_damage"))return Component.translatable("death.attack.omnira."+reason+".unattributed",victim.getDisplayName());
            return super.getLocalizedDeathMessage(victim);
        }
    }
    public static DamageSource source(Entity target,String reason){return new Source(target,null,reason,false);}
    public static DamageSource source(Entity target,DamageSource original){
        return isTrue(original)||isOverdraw(original)?original:new Source(target,original,"true_damage",false);
    }
    public static boolean isTrue(DamageSource source){return source.is(TRUE_DAMAGE);}
    public static boolean isPayment(DamageSource source){return source instanceof Source s&&s.payment;}
    public static boolean isOverdraw(DamageSource source){return source.is(OVERDRAW);}
    public static boolean trueDamage(LivingEntity target,float amount,DamageSource source){
        return apply(target,amount,source);
    }
    public static boolean unavoidable(LivingEntity target,float amount,DamageSource source){return trueDamage(target,amount,source);}
    private static boolean apply(LivingEntity target,float amount,DamageSource source){
        if(!(target.level() instanceof ServerLevel server)||!target.isAlive()||!Float.isFinite(amount)||amount<=0)return false;
        source=source(target,source);
        if(!isPayment(source)&&PhaseConcealment.blocks(target,source))return false;
        if(!isPayment(source)&&source.getEntity()!=target)amount+=HeldCombatAttributes.flatBonus(source.getEntity());
        if(!Float.isFinite(amount)||amount<=0)return false;
        float before=target.getHealth(),applied=Math.min(before,amount);
        record(target,source,applied);
        target.setHealth(Math.max(0,before-amount));
        server.broadcastDamageEvent(target,source);
        if(target instanceof Player player)player.awardStat(net.minecraft.stats.Stats.DAMAGE_TAKEN,Math.round(applied*10));
        if(source.getEntity() instanceof Player player&&player!=target)player.awardStat(net.minecraft.stats.Stats.DAMAGE_DEALT,Math.round(applied*10));
        var container=new DamageContainer(source,amount);container.setNewDamage(applied);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new LivingDamageEvent.Post(target,container));
        if(target.getHealth()<=0&&!((com.mcmagic.omnira.mixin.DamageRecordAccessor)target).omnira$tryTotem(source))target.die(source);
        return true;
    }
    private static void record(LivingEntity target,DamageSource source,float amount){
        var access=(com.mcmagic.omnira.mixin.DamageRecordAccessor)target;
        access.omnira$lastDamageSource(source);access.omnira$lastDamageStamp(target.level().getGameTime());
        if(source.getEntity() instanceof LivingEntity attacker&&attacker!=target){
            target.setLastHurtByMob(attacker);
            if(attacker instanceof Player player){target.setLastHurtByPlayer(player);access.omnira$playerHitWindow(100);}
        }
        target.getCombatTracker().recordDamage(source,amount);
    }
    public static boolean payHealth(LivingEntity target,float amount,String reason){
        if(!Float.isFinite(amount)||amount<=0||!target.isAlive())return false;
        var source=new Source(target,null,reason,true);
        if(target.getHealth()<amount){overdraw(target);return false;}
        return trueDamage(target,amount,source)&&target.isAlive();
    }
    /** Fatal true damage still permits revival; only overdraw skips death protection. */
    public static void execute(LivingEntity target,DamageSource source){
        trueDamage(target,target.getHealth(),source);
    }
    public static void overdraw(LivingEntity target){
        if(target.level().isClientSide||!target.isAlive())return;
        var source=new Source(target,null,"overdraw",true);record(target,source,target.getHealth());
        target.setHealth(0);target.die(source);
    }
    private OmniraDamage(){}
}
