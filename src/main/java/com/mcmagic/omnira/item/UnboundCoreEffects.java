package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.combat.OmniraDamage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Slot-scoped drawbacks; reflected damage has no spell attribution and cannot recurse. */
@EventBusSubscriber(modid="omnira")
public final class UnboundCoreEffects {
    public static boolean equipped(LivingEntity entity){
        return PrimarySpellAccessory.equipped(entity,"spell_core").is(ModItems.UNBOUND_MANA_CORE.get());
    }
    @SubscribeEvent public static void reflect(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0 || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || event.getEntity()==player || !player.isAlive() || !equipped(player))return;
        var source=OmniraDamage.original(event.getSource());
        if(!(source instanceof com.mcmagic.omnira.spell.SpellDamageSource)
                && !(source.getDirectEntity() instanceof com.mcmagic.omnira.spell.entity.SpellEntity))return;
        OmniraDamage.trueDamage(player,(float)Math.ceil(event.getNewDamage()*.25),OmniraDamage.source(player,"true_damage"));
    }
    private UnboundCoreEffects(){}
}
