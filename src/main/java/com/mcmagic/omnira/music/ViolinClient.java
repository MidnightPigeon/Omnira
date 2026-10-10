package com.mcmagic.omnira.music;

import com.mcmagic.omnira.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ViolinClient {
    private static final Map<UUID,PerformanceSound> PLAYING=new HashMap<>();
    public static boolean active(Player p){return PLAYING.containsKey(p.getUUID())&&VoyagerPerformance.eligible(p);}
    public static void receive(ViolinPayload payload){
        var previous=PLAYING.remove(payload.player());if(previous!=null)previous.finish();
        if(payload.track()<0||payload.track()>1)return;
        var sound=new PerformanceSound(payload.player(),payload.track());PLAYING.put(payload.player(),sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e){
        PLAYING.values().forEach(PerformanceSound::finish);PLAYING.clear();
    }
    @SubscribeEvent public static void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload e){
        if(e.getLevel().isClientSide()){PLAYING.values().forEach(PerformanceSound::finish);PLAYING.clear();}
    }
    private static final class PerformanceSound extends AbstractTickableSoundInstance {
        private final UUID player;
        private final net.minecraft.client.multiplayer.ClientLevel world=Minecraft.getInstance().level;
        private int age;
        PerformanceSound(UUID id,int track){
            super(track==0?ModSounds.VIOLIN_ALLEMANDA.get():ModSounds.VIOLIN_ADAGIO.get(),SoundSource.RECORDS,RandomSource.create());
            player=id;volume=1;pitch=1;looping=false;
            var p=world==null?null:world.getPlayerByUUID(id);if(p!=null){x=p.getX();y=p.getEyeY();z=p.getZ();}
        }
        @Override public boolean canStartSilent(){return true;}
        void finish(){stop();}
        @Override public void tick(){
            var mc=Minecraft.getInstance();var p=world==null?null:world.getPlayerByUUID(player);age++;
            if(mc.level!=world||p==null&&age>40||p!=null&&!VoyagerPerformance.eligible(p)){
                stop();PLAYING.remove(player,this);return;
            }
            if(p!=null){x=p.getX();y=p.getEyeY();z=p.getZ();}
        }
    }
}
