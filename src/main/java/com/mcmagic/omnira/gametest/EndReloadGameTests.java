package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.world.ReloadEffect;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_end_reload")
@PrefixGameTestTemplate(false)
public class EndReloadGameTests {
    private static <T extends Mob> T reload(GameTestHelper h, Mob mob, Class<T> type) {
        var result = ReloadEffect.replaceLiving(SpellEffect.utility("reload",1,0,0), mob);
        h.assertTrue(type.isInstance(result) && mob.isRemoved(), "Reload failed: " + type.getSimpleName());
        return type.cast(result);
    }

    private static void defeat(GameTestHelper h, EnderDragon dragon, FakePlayer player) {
        dragon.getPhaseManager().setPhase(EnderDragonPhase.SITTING_SCANNING);
        dragon.hurt(dragon.head, dragon.damageSources().playerAttack(player), 10000);
        for (int i=0; i<210 && !dragon.isRemoved(); i++) dragon.tick();
        h.assertTrue(dragon.isRemoved(), "Dragon death did not finish");
    }

    @GameTest(template="spell_arena")
    public static void reloadAcrossVanillaSearchBoundary(GameTestHelper h) {
        ServerLevel end = h.getLevel().getServer().getLevel(Level.END);
        var fight = end.getDragonFight();
        var player = new FakePlayer(end, new GameProfile(UUID.randomUUID(),"ReloadEndTest"));
        player.setPos(0,110,0);
        end.addNewPlayer(player);
        try {
            fight.skipArenaLoadedCheck();
            for (int i=0; i<21; i++) fight.tick();
            var original = (EnderDragon)end.getEntity(fight.getDragonUUID());
            h.assertTrue(original != null && !fight.hasPreviouslyKilledDragon(), "Run this first-kill test with a fresh testRunRoot");
            fight.updateDragon(original);
            var oldForm = reload(h, original, WitherBoss.class);
            oldForm.setPos(32,160,0);
            var other = EntityType.WITHER.create(end);
            other.setPos(-32,160,0);
            end.addFreshEntity(other);
            var unrelated = reload(h, other, EnderDragon.class);
            for (int i=0; i<1199; i++) fight.tick();
            h.assertTrue(fight.getDragonUUID().equals(original.getUUID()), "Encounter changed before vanilla timeout");
            fight.tick();
            var successor = (EnderDragon)end.getEntity(fight.getDragonUUID());
            h.assertTrue(successor != null && successor != unrelated, "Vanilla did not spawn a new encounter dragon");
            var late = reload(h, oldForm, EnderDragon.class);
            h.assertTrue(successor.getUUID().equals(fight.getDragonUUID()), "Late return stole the encounter");
            defeat(h, unrelated, player);
            defeat(h, late, player);
            h.assertFalse(fight.hasPreviouslyKilledDragon(), "An unrelated or late-returning dragon completed first kill");

            fight.updateDragon(successor);
            var form = reload(h, successor, WitherBoss.class);
            for (int i=0; i<1199; i++) fight.tick();
            var restored = reload(h, form, EnderDragon.class);
            h.assertTrue(restored.getDragonFight()==fight && restored.getUUID().equals(fight.getDragonUUID()),
                    "Dragon did not immediately rejoin encounter");
            h.assertTrue(restored.getPhaseManager().getCurrentPhase().getPhase()==EnderDragonPhase.HOLDING_PATTERN,
                    "Returning dragon was left orbiting its spawn point");
            restored.getPhaseManager().setPhase(EnderDragonPhase.HOVERING);
            var saved=restored.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            restored.discard();
            restored=EntityType.ENDER_DRAGON.create(end);
            restored.load(saved);
            end.addLegacyChunkEntities(java.util.stream.Stream.of(restored));
            h.assertTrue(restored.getPhaseManager().getCurrentPhase().getPhase()==EnderDragonPhase.HOLDING_PATTERN,
                    "Old hovering encounter dragon did not resume patrol on load");
            restored.tick();
            defeat(h, restored, player);
            h.assertTrue(fight.hasPreviouslyKilledDragon(), "Immediate kill did not complete the encounter");
            var podium = fight.saveData().exitPortalLocation().orElseThrow();
            var min = podium.offset(-4,-1,-4);
            var max = podium.offset(4,12,4);
            for (var block : new net.minecraft.world.level.block.Block[]{Blocks.END_PORTAL,Blocks.DRAGON_EGG}) {
                h.assertTrue(BlockPos.betweenClosedStream(min,max).anyMatch(p -> end.getBlockState(p).is(block)),
                        "Missing first kill reward: "+block);
            }
            boolean gateway = false;
            for (int i=0; i<20; i++) {
                double angle = 2*(-Math.PI+Math.PI*i/20);
                var pos = BlockPos.containing(96*Math.cos(angle),75,96*Math.sin(angle));
                gateway |= end.getBlockState(pos).is(Blocks.END_GATEWAY);
            }
            h.assertTrue(gateway && fight.saveData().gateways().orElseThrow().size()==19, "Expected one end gateway");
        } finally {
            player.discard();
        }
        h.succeed();
    }
}
