package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID)
public final class ManaEvents {
    private ManaEvents() {}

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.isAlive() && !player.isSpectator()) {
            AffinityEffects.tick(player);
            ManaState before=player.getData(ModAttachments.MANA);
            double maximum=player.getAttributeValue(com.mcmagic.omnira.registry.ModAttributes.MAX_MANA);
            ManaState after=new ManaState(before.current(),maximum,player.getData(ModAttachments.AFFINITY).active().ordinal());
            if(player.tickCount % ManaState.REGEN_INTERVAL==0)
                after=after.withCurrent(after.current()+maximum*.01*player.getAttributeValue(com.mcmagic.omnira.registry.ModAttributes.MANA_REGEN));
            if(!after.equals(before)) player.setData(ModAttachments.MANA,after);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        synchronize(event);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        synchronize(event);
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        synchronize(event);
    }

    private static void synchronize(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getData(ModAttachments.MANA);
            player.syncData(ModAttachments.MANA.get());
        }
    }

    public static boolean trySpend(ServerPlayer player, double amount) {
        amount = ManaCosts.cost(player,amount);
        ManaState mana = player.getData(ModAttachments.MANA);
        if (!mana.canSpend(amount)) {
            return false;
        }
        player.setData(ModAttachments.MANA, mana.spend(amount));
        return true;
    }
}
