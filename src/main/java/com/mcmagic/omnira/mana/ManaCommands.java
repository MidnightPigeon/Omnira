package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Omnira.MOD_ID)
public final class ManaCommands {
    private ManaCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("omnira")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("mana")
                        .then(Commands.literal("get").executes(context -> {
                            ManaState state = context.getSource().getPlayerOrException().getData(ModAttachments.MANA);
                            context.getSource().sendSuccess(() -> Component.literal(state.current() + " / " + state.maximum()), false);
                            return 1;
                        }))
                        .then(Commands.literal("set").then(Commands.argument("amount", DoubleArgumentType.doubleArg(0, 1_000_000))
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    player.setData(ModAttachments.MANA, player.getData(ModAttachments.MANA)
                                            .withCurrent(DoubleArgumentType.getDouble(context, "amount")));
                                    return 1;
                                })))
                        .then(Commands.literal("maximum").then(Commands.argument("amount", DoubleArgumentType.doubleArg(1, 1_000_000))
                                .executes(context -> {
                                    var player = context.getSource().getPlayerOrException();
                                    player.getAttribute(com.mcmagic.omnira.registry.ModAttributes.MAX_MANA)
                                            .setBaseValue(DoubleArgumentType.getDouble(context,"amount"));
                                    player.setData(ModAttachments.MANA,player.getData(ModAttachments.MANA).withMaximum(
                                            player.getAttributeValue(com.mcmagic.omnira.registry.ModAttributes.MAX_MANA)));
                                    return 1;
                                })))));
    }
}
