package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID)
public final class SpellEvents {
    private SpellEvents() {}

    @SubscribeEvent
    public static void damaged(LivingDamageEvent.Post event) {
        var entity = event.getEntity();
        if (entity.level().isClientSide || event.getNewDamage() <= 0) return;
        // Retaliatory wards must not recursively trigger other wards in the same damage chain.
        if(event.getSource().getDirectEntity() instanceof SpellEntity source && source.kind()==SpellEntity.Kind.WARD) return;
        for (SpellEntity spell : entity.level().getEntitiesOfClass(SpellEntity.class, entity.getBoundingBox().inflate(4),
                spell -> spell.kind() == SpellEntity.Kind.WARD && spell.follows(entity))) {
            if(spell.charges()>0) {
                spell.trigger(false);
                spell.applyWardEffects(entity,event.getSource().getEntity());
            }
        }
    }
}
