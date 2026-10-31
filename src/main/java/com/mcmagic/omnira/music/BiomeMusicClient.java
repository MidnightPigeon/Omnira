package com.mcmagic.omnira.music;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.Musics;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;

/** Authored dimensions never fall back to vanilla situational music. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class BiomeMusicClient {
    @SubscribeEvent public static void select(SelectMusicEvent event){
        var minecraft=Minecraft.getInstance();
        if(minecraft.player==null||minecraft.level==null)return;
        var authored=minecraft.level.getBiome(minecraft.player.blockPosition()).value().getBackgroundMusic()
                .filter(music->music.getEvent().value().getLocation().getNamespace().equals("omnira"));
        var dimension=minecraft.level.dimension();
        if(dimension.equals(ModDimensions.DREAM_REALM)||dimension.equals(ModDimensions.SPACETIME_CORRIDOR)){
            event.overrideMusic(authored.orElse(null));
        }else if(event.getMusic()==Musics.CREATIVE){
            authored.ifPresent(event::setMusic);
        }
    }
    private BiomeMusicClient(){}
}
