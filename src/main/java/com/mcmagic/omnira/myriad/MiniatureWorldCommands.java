package com.mcmagic.omnira.myriad;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only previews, independent of the survival development recipes. */
@EventBusSubscriber(modid="omnira")
public final class MiniatureWorldCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event){
        var position=Commands.argument("pos",BlockPosArgument.blockPos());
        for(var stage:MiniatureWorldBlockEntity.Stage.values())position.then(Commands.literal(stage.name().toLowerCase(java.util.Locale.ROOT)).executes(c->{
            var be=MiniatureWorldBlock.controller(c.getSource().getLevel(),BlockPosArgument.getLoadedBlockPos(c,"pos"));
            if(be==null){c.getSource().sendFailure(Component.translatable("commands.omnira.miniature_world.missing"));return 0;}
            be.preview(stage);c.getSource().sendSuccess(()->Component.translatable("commands.omnira.miniature_world.stage",stage.name().toLowerCase(java.util.Locale.ROOT)),false);return 1;
        }));
        event.getDispatcher().register(Commands.literal("omnira").requires(s->s.hasPermission(2)).then(Commands.literal("miniature_world").then(position)));
    }
    private MiniatureWorldCommands(){}
}
