package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.NailSlash;
import com.mcmagic.omnira.network.NailSlashPayload;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.ArrayList;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class NailSlashVisuals {
    private record Wave(ClientLevel level,NailSlashPayload data,long start) {}
    private static final ArrayList<Wave> WAVES=new ArrayList<>();
    public static void receive(NailSlashPayload data) {
        var level=Minecraft.getInstance().level;
        if(level==null)return;
        if(WAVES.size()>=64)WAVES.removeFirst();
        WAVES.add(new Wave(level,data,level.getGameTime()));
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        WAVES.removeIf(w->w.level()!=mc.level || w.level().getGameTime()-w.start()>=6);
        for(var wave:WAVES) {
            var data=wave.data();double radius=Math.clamp(data.reach(),0,64)*(wave.level().getGameTime()-wave.start()+1)/6;
            var up=NailSlash.right(data.forward()).cross(data.forward());
            int segments=8+(int)(radius*12);
            for(int i=0;i<=segments;i++)for(int row=-1;row<=1;row++) {
                var point=NailSlash.point(data.origin(),data.forward(),radius,-NailSlash.HALF_ANGLE+2*NailSlash.HALF_ANGLE*i/segments).add(up.scale(row*.14));
                if(wave.level().clip(new ClipContext(data.origin(),point,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()!=HitResult.Type.MISS)continue;
                mc.particleEngine.createParticle(ModParticles.NAIL_FOCUS.get(),point.x,point.y,point.z,0,0,0);
            }
        }
    }
    private NailSlashVisuals() {}
}
