package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.time.FleetingTime;
import com.mcmagic.omnira.spell.IndependentSpellCooldown;
import com.mcmagic.omnira.spell.WeaponCooldowns;
import com.mcmagic.omnira.util.GameTimeCooldown;
import com.mcmagic.omnira.weaver.FounderApprovalItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_time_player")
@PrefixGameTestTemplate(false)
public final class TimePlayerGameTests {
    @GameTest(template="spell_arena")
    public static void weaponHandoffContinuesRemainingTicks(GameTestHelper h) {
        var level = h.getLevel();
        var first = new net.neoforged.neoforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "first-wielder"));
        var second = new net.neoforged.neoforge.common.util.FakePlayer(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "next-wielder"));
        var firstTick = new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(first);
        var secondTick = new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(second);
        var weapon = new ItemStack(Items.IRON_SWORD);
        IndependentSpellCooldown.start(first, weapon, 2);
        WeaponCooldowns.tick(secondTick);
        WeaponCooldowns.tick(firstTick);
        h.assertTrue(!IndependentSpellCooldown.ready(second, weapon), "Handoff lost the remaining tick");
        WeaponCooldowns.tick(firstTick);
        h.assertTrue(!IndependentSpellCooldown.ready(second, weapon), "Previous wielder kept advancing the weapon");
        WeaponCooldowns.tick(secondTick);
        h.assertTrue(IndependentSpellCooldown.ready(second, weapon), "New wielder did not finish the cooldown");
        h.succeed();
    }

    @GameTest(template="spell_arena")
    public static void rollbackClearsImpossibleCooldowns(GameTestHelper h) {
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var item = new ItemStack(Items.STICK);
        long now = player.level().getGameTime();
        IndependentSpellCooldown.start(player, item, 40);
        h.assertTrue(!IndependentSpellCooldown.ready(player, item), "Fresh cooldown was skipped");
        item = new ItemStack(Items.STICK);
        item.set(ModDataComponents.SPELL_COOLDOWN_DURATION, 40);
        item.set(ModDataComponents.SPELL_READY_AT, now + 41);
        h.assertTrue(IndependentSpellCooldown.ready(player, item), "Rolled-back item remained locked");
        var data = new net.minecraft.nbt.CompoundTag();
        data.putLong("gate", now + 20);
        h.assertTrue(GameTimeCooldown.active(data, "gate", now, 20), "Valid portal cooldown lost");
        h.assertTrue(!GameTimeCooldown.active(data, "gate", now - 1, 20) && !data.contains("gate"), "Stale portal cooldown survived");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void playerPotionsAndCooldowns(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(8,300,8));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FleetingTime.BIOME);
        var center=new net.minecraft.world.level.ChunkPos(pos);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var chunk=level.getChunk(center.x+x,center.z+z);
            chunk.fillBiomesFromNoise((qx,qy,qz,s)->qy>=48 ? biome : chunk.getNoiseBiome(qx,qy,qz),
                    level.getChunkSource().randomState().sampler());
        }
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"time-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        try{
            player.setPos(pos.getCenter());player.setNoGravity(true);player.getAbilities().flying=true;
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,100));
            var stack=new ItemStack(Items.STICK);IndependentSpellCooldown.start(player,stack,2);
            // The independently cooling weapon is in storage, outside the inventory.
            var founder = new net.minecraft.nbt.CompoundTag();
            founder.putInt("BOLT", 3);
            player.getPersistentData().put(FounderApprovalItem.TIMERS, founder);
            GameTimeCooldown.start(player, "testPortal", level.getGameTime(), 2);
            player.getCooldowns().addCooldown(Items.STICK,100);
            player.resetAttackStrengthTicker();float attack=player.getAttackStrengthScale(0);
            player.doTick();
            h.assertTrue(player.getEffect(MobEffects.DOLPHINS_GRACE).getDuration()==98,"Player potion time did not advance twice");
            h.assertTrue(Math.abs(player.getCooldowns().getCooldownPercent(Items.STICK,0)-.98F)<.0001,"Shared spell cooldown did not advance twice");
            h.assertTrue(IndependentSpellCooldown.ready(player,stack),"Independent cooldown did not receive a bonus step");
            h.assertTrue(founder.getInt("BOLT")==1, "Founder timer did not follow the two player ticks");
            h.assertTrue(!GameTimeCooldown.active(player, "testPortal", level.getGameTime(), 2), "Portal cooldown did not expire on the second player tick");
            h.assertTrue(player.getAttackStrengthScale(0)>attack,"Attack recovery stopped");h.succeed();
        }finally{player.discard();}
    }
}
