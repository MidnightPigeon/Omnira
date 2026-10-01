package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.registry.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid="omnira")
public final class AffinityCommands {
    private AffinityCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root=Commands.literal("affinity");
        var set=Commands.literal("set");
        for(var affinity:Affinity.values())set.then(Commands.literal(affinity.name().toLowerCase(java.util.Locale.ROOT))
                .executes(c->set(c.getSource(),java.util.List.of(c.getSource().getPlayerOrException()),affinity))
                .then(Commands.argument("players",EntityArgument.players()).executes(c->set(c.getSource(),EntityArgument.getPlayers(c,"players"),affinity))));
        root.then(set);
        event.getDispatcher().register(Commands.literal("omnira").requires(source->source.hasPermission(2)).then(root));
    }
    private static int set(net.minecraft.commands.CommandSourceStack source,java.util.Collection<ServerPlayer> players,Affinity affinity) {
        for(var player:players)apply(player,affinity);
        source.sendSuccess(()->Component.translatable("commands.omnira.affinity.set",players.size(),Component.translatable(affinity.key())),true);
        return players.size();
    }
    public static void apply(ServerPlayer player,Affinity affinity) {
        if(com.mcmagic.omnira.world.dimension.AffinityRitual.active(player))com.mcmagic.omnira.world.dimension.AffinityRitual.cancel(player);
        player.setData(ModAttachments.AFFINITY,new AffinityState(affinity.ordinal(),0));
        player.setData(ModAttachments.DREAM_SOLIDIFY,0F);AffinityEffects.refresh(player);
        var mana=player.getData(ModAttachments.MANA);
        player.setData(ModAttachments.MANA,new ManaState(mana.current(),player.getAttributeValue(ModAttributes.MAX_MANA),affinity.ordinal()));
    }
}
