package com.mcmagic.omnira.space.phase;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.*;

/** Entity-only phasing: never changes block collision, flight or position. */
@EventBusSubscriber(modid="omnira")
public final class PhaseConcealment extends MobEffect {
    public PhaseConcealment(){super(MobEffectCategory.BENEFICIAL,0x96BDEA);}
    public static boolean active(Entity entity){return entity instanceof Player p&&p.hasEffect(ModEffects.PHASE_CONCEALMENT);}
    public static boolean singleTarget(Entity source){
        if(source instanceof SpellEntity spell)return switch(spell.kind()){
            case BURST -> spell.directSingleTarget();
            case REVERSE_FLOW,SPATIAL_BLADE,BARRIER,FACE_BARRIER,WARD -> false;
            default -> !spell.explosiveImpact();
        };
        return source!=null&&!(source instanceof ThrownPotion)&&!(source instanceof AreaEffectCloud)&&!(source instanceof FireworkRocketEntity)&&!(source instanceof LightningBolt);
    }
    public static boolean blocks(Entity target,DamageSource source){
        if(!active(target))return false;
        if(source.getDirectEntity() instanceof SpellEntity spell)return singleTarget(spell);
        if(source.is(DamageTypeTags.IS_EXPLOSION))return false;
        return singleTarget(source.getDirectEntity());
    }
    @SubscribeEvent public static void incoming(LivingIncomingDamageEvent event){
        if(blocks(event.getEntity(),event.getSource()))event.setCanceled(true);
    }
    @SubscribeEvent public static void melee(net.neoforged.neoforge.event.entity.player.AttackEntityEvent event){
        if(active(event.getTarget()))event.setCanceled(true);
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event){
        if(event.getNewDamage()>0||event.getReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ABSORPTION)>0)afterDamage(event.getEntity());
    }
    public static void afterDamage(LivingEntity target){
        if(target instanceof Player player&&!player.level().isClientSide&&player.isAlive()&&PhaseConcealmentItem.equipped(player))
            player.addEffect(new MobEffectInstance(ModEffects.PHASE_CONCEALMENT,100,0,false,false,false));
    }
}
