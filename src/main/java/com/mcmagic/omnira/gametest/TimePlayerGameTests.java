package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.time.FleetingTime;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_time_player")
@PrefixGameTestTemplate(false)
public final class TimePlayerGameTests {
    @GameTest(template="spell_arena",timeoutTicks=100)
    public static void playerPotionsAndCooldowns(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(8,5,8));
        var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(FleetingTime.BIOME);
        var center=new net.minecraft.world.level.ChunkPos(pos);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)level.getChunk(center.x+x,center.z+z).fillBiomesFromNoise((qx,qy,qz,s)->biome,level.getChunkSource().randomState().sampler());
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"time-test"),false);
        var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,cookie.gameProfile(),cookie.clientInformation());
        player.connection=new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),player,cookie){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
        };
        try{
            player.setPos(pos.getCenter());player.setNoGravity(true);player.getAbilities().flying=true;
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE,100));
            var stack=new ItemStack(Items.STICK);long now=level.getGameTime();stack.set(ModDataComponents.SPELL_READY_AT,now+100);
            player.getInventory().setItem(0,stack);player.getCooldowns().addCooldown(Items.STICK,100);
            player.resetAttackStrengthTicker();float attack=player.getAttackStrengthScale(0);
            player.doTick();
            h.assertTrue(player.getEffect(MobEffects.DOLPHINS_GRACE).getDuration()==98,"Player potion time did not advance twice");
            h.assertTrue(Math.abs(player.getCooldowns().getCooldownPercent(Items.STICK,0)-.98F)<.0001,"Shared spell cooldown did not advance twice");
            h.assertTrue(stack.getOrDefault(ModDataComponents.SPELL_READY_AT,0L)==now+99,"Independent cooldown did not receive a bonus step");
            h.assertTrue(player.getAttackStrengthScale(0)>attack,"Attack recovery stopped");h.succeed();
        }finally{player.discard();}
    }
}
