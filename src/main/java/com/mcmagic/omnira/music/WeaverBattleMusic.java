package com.mcmagic.omnira.music;

import com.mcmagic.omnira.entity.WeaverSpirit;
import com.mcmagic.omnira.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.HashSet;
import java.util.Set;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class WeaverBattleMusic {
    private static final Set<WeaverSpirit> tracked=new HashSet<>();
    private static Track track;
    private static boolean engaged;
    private static int gap;

    @SubscribeEvent public static void join(EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide() && event.getEntity() instanceof WeaverSpirit boss)tracked.add(boss);
    }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent event){
        if(event.getLevel().isClientSide() && event.getEntity() instanceof WeaverSpirit boss)tracked.remove(boss);
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload event){
        if(!event.getLevel().isClientSide())return;
        tracked.clear();
        if(track!=null)Minecraft.getInstance().getSoundManager().stop(track);
        track=null;
        engaged=false;
        gap=0;
    }
    static boolean occupiesMusicChannel(){return engaged || track!=null;}

    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();
        if(mc.isPaused() || mc.player==null)return;
        engaged=false;
        for(var boss:tracked){
            if(boss.level()==mc.level && boss.active() && boss.isAlive() && boss.departure()==0
                    && boss.distanceToSqr(mc.player)<=48*48){
                engaged=true;
                break;
            }
        }
        var sounds=mc.getSoundManager();
        if(track!=null && !sounds.isActive(track)){
            track=null;
            gap=100;
        }
        if(!engaged){gap=0;return;}
        if(track!=null)return;
        if(gap>0){gap--;return;}
        mc.getMusicManager().stopPlaying();
        track=new Track();
        sounds.play(track);
    }

    private static final class Track extends AbstractTickableSoundInstance {
        private static final int FADE_TICKS=200;
        private int fade=FADE_TICKS;
        Track(){
            super(ModSounds.WEAVER_BATTLE.get(),SoundSource.MUSIC,RandomSource.create());
            relative=true;
            attenuation=Attenuation.NONE;
        }
        @Override public void tick(){
            fade=engaged?Math.min(FADE_TICKS,fade+1):Math.max(0,fade-1);
            volume=(float)fade/FADE_TICKS;
            if(fade==0)stop();
        }
    }
    private WeaverBattleMusic(){}
}
