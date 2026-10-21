package com.mcmagic.omnira.music;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Musics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/** Keep authored biome music available during creative flight without replacing other music selectors. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class BiomeMusicClient {
    @SubscribeEvent public static void select(SelectMusicEvent event){
        var minecraft=Minecraft.getInstance();
        if(event.getMusic()!=Musics.CREATIVE||minecraft.player==null||minecraft.level==null)return;
        minecraft.level.getBiome(minecraft.player.blockPosition()).value().getBackgroundMusic()
                .filter(music->music.getEvent().value().getLocation().getNamespace().equals("omnira")).ifPresent(event::setMusic);
    }
    private BiomeMusicClient(){}
}
