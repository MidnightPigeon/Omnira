package com.mcmagic.omnira.event;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

@EventBusSubscriber(modid="omnira")
public final class CrystalPickaxeEvents {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void oreDrops(BlockDropsEvent event) {
        if(!(event.getBreaker() instanceof ServerPlayer player) || player.isCreative() || player.isSpectator()
                || !event.getTool().is(ModItems.INFUSED_CRYSTAL_PICKAXE.get())
                || !event.getState().is(Tags.Blocks.ORES) || !event.getTool().isCorrectToolForDrops(event.getState())) return;
        if(event.getLevel().random.nextInt(5)!=0) return;
        var pos=event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(),pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,new ItemStack(ModItems.SPIRITUAL_CRYSTAL.get())));
    }
}
