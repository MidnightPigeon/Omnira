package com.mcmagic.omnira.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Physical harm by default; infusion opts instantaneous harm into potion semantics. */
public record SpellEffect(boolean healing,int amplifier,int duration,boolean infused,String operation,int enhancement,int delay,int infusion) {
    public static final Codec<SpellEffect> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.BOOL.fieldOf("healing").forGetter(SpellEffect::healing),
            Codec.intRange(0,255).fieldOf("amplifier").forGetter(SpellEffect::amplifier),
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("duration").forGetter(SpellEffect::duration),
            Codec.BOOL.optionalFieldOf("infused",false).forGetter(SpellEffect::infused),
            Codec.STRING.optionalFieldOf("operation","potion").forGetter(SpellEffect::operation),
            Codec.intRange(0,16).optionalFieldOf("enhancement",0).forGetter(SpellEffect::enhancement),
            Codec.intRange(0,8).optionalFieldOf("delay",0).forGetter(SpellEffect::delay),
            Codec.intRange(0,16).optionalFieldOf("infusion",0).forGetter(SpellEffect::infusion)
    ).apply(i,SpellEffect::new));
    public SpellEffect(boolean healing,int amplifier,int duration,boolean infused) {this(healing,amplifier,duration,infused,"potion",0,0,infused?1:0);}
    public static SpellEffect utility(String operation,int enhancement,int delay,int infusion) {
        return new SpellEffect(false,0,0,infusion>0,operation,enhancement,delay,infusion);
    }
    public boolean utility() {return operation.equals("dissociation") || operation.equals("construction");}
    public boolean darkness() {return operation.equals("dark_breath");}
    public SpellEffect(boolean healing,int amplifier,int duration) {this(healing,amplifier,duration,false);}
    public static SpellEffect compose(boolean healing,int clocks,int ironBlocks) {
        return new SpellEffect(healing,(clocks>0?0:1)+ironBlocks,clocks>0?800+(clocks-1)*1200:0);
    }
    public Holder<MobEffect> type() {
        return duration>0?(healing?MobEffects.REGENERATION:MobEffects.POISON):(healing?MobEffects.HEAL:MobEffects.HARM);
    }
    public float physicalDamage(double power) {
        if(darkness())return 0;
        if(utility()) return operation.equals("dissociation") && delay==0?(float)(2*(1+enhancement)*power):0;
        return duration==0 && !healing && !infused?(float)((6+3*Math.max(0,amplifier-1))*power):0;
    }
    private static int instantAmount(int base,int amplifier,double power) {
        return (int)Math.clamp(Math.floor(Math.scalb((double)base,amplifier)*power+.5),0,Integer.MAX_VALUE);
    }
    private static int equivalentAmplifier(double amount,int base) {
        int result=0;double threshold=base;
        // Compare against exact doubling thresholds instead of rounding a floating-point logarithm.
        while(amount>threshold && result<255) {threshold*=2;result++;}
        return result;
    }
    public int sustainedAmplifier(double power) {
        if(!healing && !infused)
            return equivalentAmplifier((6+3D*amplifier)*power,6);
        int base=healing?4:6;
        int amount=instantAmount(base,amplifier+1,power);
        // Delay retains the one-level gap: Healing II (8 HP) corresponds to Regeneration I.
        return Math.max(0,equivalentAmplifier(amount,base)-1);
    }
    public void apply(Entity source,Entity owner,Entity hit,double power) {
        apply(source,owner,hit,power,source instanceof com.mcmagic.omnira.spell.entity.SpellEntity spell && spell.holy());
    }
    public void apply(Entity source,Entity owner,Entity hit,double power,boolean holy) {
        LivingEntity target=SpellCasting.livingTarget(hit);
        if(target==null || !target.isAlive()) return;
        if(darkness())return;
        if(holy && (duration==0 || utility()))power*=1.5;
        if(utility()) {UtilitySpellEffects.applyLiving(this,source,owner,target,power);return;}
        if(duration==0 && !healing && !infused) {
            hit.hurt(SpellDamageSource.physical(target,source,owner,"spell"),
                    physicalDamage(power));
            return;
        }
        if(healing && infused) {
            int ticks=duration>0?duration:20;
            target.addEffect(new MobEffectInstance(duration>0?MobEffects.DAMAGE_RESISTANCE:MobEffects.SATURATION,
                    ticks,duration>0?sustainedAmplifier(power):0),owner);
        }
        if(duration==0&&healing==target.isInvertedHealAndHarm()) {
            // Vanilla instant-effect amount and undead inversion, with a spell-specific death message.
            int amount=instantAmount(6,amplifier,power);
            target.hurt(SpellDamageSource.of(target,source==null?net.minecraft.world.damagesource.DamageTypes.MAGIC:net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC,source,owner,"spell"),amount);
        }
        else if(duration==0) target.heal(holy?(float)Math.ceil(Math.scalb(4D,amplifier)*power):instantAmount(4,amplifier,power));
        else target.addEffect(new MobEffectInstance(type(),duration,sustainedAmplifier(power)),owner);
    }
}
