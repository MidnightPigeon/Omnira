package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.ArcaneArquebusItem;
import com.mcmagic.omnira.item.ArquebusPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import java.util.*;

public final class GhostSense {
    private static Set<UUID> entities=Set.of();
    private static ClientLevel level;
    private static long expires;
    private GhostSense() {}
    public static void receive(List<UUID> ids) {
        level=Minecraft.getInstance().level;
        entities=Set.copyOf(ids);
        expires=level==null?0:level.getGameTime()+40;
    }
    public static boolean highlighted(Entity entity) {
        var client=Minecraft.getInstance();
        if(client.level!=level || level==null || level.getGameTime()>=expires) {
            entities=Set.of();level=null;return false;
        }
        return client.player!=null && client.player.getMainHandItem().getItem() instanceof ArcaneArquebusItem
                && ArquebusPlugin.of(client.player.getMainHandItem())==ArquebusPlugin.KINGS_NEW_CLOTHES
                && entities.contains(entity.getUUID());
    }
}
