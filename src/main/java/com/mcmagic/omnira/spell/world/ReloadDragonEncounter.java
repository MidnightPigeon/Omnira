package com.mcmagic.omnira.spell.world;

import com.mcmagic.omnira.mixin.EndDragonFightAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;

/** An alternate form can return only while vanilla is still looking for its original dragon. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class ReloadDragonEncounter {
    private static final String ORIGINAL = "omnira:reload_original_dragon";
    private static final String UNRELATED = "omnira:unrelated_reload_dragon";

    public static boolean unrelated(EnderDragon dragon) {
        return dragon.getPersistentData().getBoolean(UNRELATED);
    }

    static void transfer(ServerLevel level, LivingEntity previous, Mob next) {
        var fight = level.getDragonFight();
        var tracked = fight == null ? null : fight.getDragonUUID();
        var data = previous.getPersistentData();
        boolean ownsFight = tracked != null && (previous instanceof EnderDragon
                ? tracked.equals(previous.getUUID())
                : data.hasUUID(ORIGINAL) && tracked.equals(data.getUUID(ORIGINAL)));
        if (next instanceof EnderDragon dragon) {
            if (ownsFight) {
                ((EndDragonFightAccessor)fight).omnira$setDragonUUID(dragon.getUUID());
                dragon.setDragonFight(fight);
                dragon.setFightOrigin(((EndDragonFightAccessor)fight).omnira$getOrigin());
                startPatrol(dragon);
                fight.updateDragon(dragon);
            } else {
                dragon.getPersistentData().putBoolean(UNRELATED, true);
            }
        } else if (ownsFight) {
            next.getPersistentData().putUUID(ORIGINAL, tracked);
        }
    }

    private static void startPatrol(EnderDragon dragon) {
        dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
    }

    // Earlier versions saved returning encounter dragons in the constructor's hover phase.
    @net.neoforged.bus.api.SubscribeEvent
    public static void restorePatrol(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof EnderDragon dragon)) return;
        var fight = level.getDragonFight();
        if (fight != null && dragon.getUUID().equals(fight.getDragonUUID())
                && dragon.getPhaseManager().getCurrentPhase().getPhase() == net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING) {
            dragon.setFightOrigin(((EndDragonFightAccessor)fight).omnira$getOrigin());
            startPatrol(dragon);
        }
    }

    private ReloadDragonEncounter() {}
}
