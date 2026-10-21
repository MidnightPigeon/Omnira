package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.*;
import net.minecraft.world.phys.*;

/** Pulse overflow is an environmental magic blast, not a player-scaled spell or terrain explosion. */
public final class PulseAbsorption {
    public static final double RADIUS=5;
    public static final float DAMAGE=20;
    public static void receive(ServerPlayer player,double amount){
        if(!player.isAlive()||player.isSpectator()||!Double.isFinite(amount)||amount<=0)return;
        var mana=player.getData(ModAttachments.MANA);
        if(!mana.pulseOverloaded()){
            player.setData(ModAttachments.MANA,mana.absorbPulse(amount));return;
        }
        player.setData(ModAttachments.MANA,mana.withCurrent(mana.maximum()));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,3600,1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,3600,1));
        var level=player.serverLevel();var center=player.getBoundingBox().getCenter();
        var visual=SpellEntity.spawn(level,null,SpellEntity.Kind.BURST,center);
        visual.configure(1,0);visual.burstRadius(RADIUS);visual.setSpellColor(0x81D4FA);visual.suppressCloud();
        var damage=new DamageSource(level.damageSources().magic().typeHolder(),visual,null){
            @Override public boolean is(TagKey<DamageType> tag){return tag==DamageTypeTags.IS_EXPLOSION||super.is(tag);}
        };
        level.playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.PLAYERS,1,1.15F);
        var targets=new java.util.ArrayList<>(level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(center,center).inflate(RADIUS),
                e->e.isAlive()&&!e.isSpectator()&&!(e instanceof SpellEntity)));
        if(!targets.contains(player))targets.add(player);
        for(var entity:targets){
            var delta=entity.getBoundingBox().getCenter().subtract(center);double distance=delta.length();if(distance>RADIUS)continue;
            if(entity instanceof LivingEntity living&&!(entity instanceof ArmorStand))living.hurt(damage,DAMAGE);
            var direction=distance<.001?new Vec3(0,1,0):delta.scale(1/distance);
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(1.5-.5*distance/RADIUS)).add(0,.25,0));entity.hurtMarked=true;
        }
    }
    private PulseAbsorption(){}
}
