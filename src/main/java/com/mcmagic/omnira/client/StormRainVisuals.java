package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class StormRainVisuals {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ModParticles.STORM_RAIN.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new Rain(level,x,y,z,sprites));
        event.registerSpriteSet(ModParticles.STORM_WARNING.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new Rain(level,x,y,z,sprites));
    }
    private static final class Rain extends TextureSheetParticle {
        Rain(ClientLevel level,double x,double y,double z,SpriteSet sprites){
            super(level,x,y,z);pickSprite(sprites);quadSize=.16F;lifetime=24+random.nextInt(9);
            xd=0;yd=.07+random.nextDouble()*.025;zd=0;gravity=0;friction=1;hasPhysics=false;alpha=.65F;
        }
        @Override public void tick(){super.tick();alpha=.65F*(1-age/(float)lifetime);}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
    private StormRainVisuals(){}
}
