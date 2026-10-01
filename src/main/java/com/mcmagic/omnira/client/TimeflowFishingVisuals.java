package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class TimeflowFishingVisuals {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ModParticles.TIMEFLOW_RIPPLE.get(),s->(t,l,x,y,z,dx,dy,dz)->new Ripple(l,x,y,z,dx,dz,s));
        event.registerSpriteSet(ModParticles.TIMEFLOW_BUBBLE.get(),s->(t,l,x,y,z,dx,dy,dz)->new Bubble(l,x,y,z,dx,dy,dz,s));
    }
    private static final class Ripple extends TextureSheetParticle {
        Ripple(ClientLevel l,double x,double y,double z,double dx,double dz,SpriteSet s){
            super(l,x,y,z);pickSprite(s);xd=dx;yd=0;zd=dz;hasPhysics=false;gravity=0;friction=.88F;lifetime=24;
            quadSize=.14F;setColor(.78F,.94F,.63F);alpha=.7F;
        }
        @Override public FacingCameraMode getFacingCameraMode(){return (q,c,p)->q.rotationX(-(float)Math.PI/2);}
        @Override public float getQuadSize(float partial){return .14F+.34F*(age+partial)/lifetime;}
        @Override public void tick(){super.tick();alpha=.7F*(1F-(float)age/lifetime);}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
    private static final class Bubble extends TextureSheetParticle {
        Bubble(ClientLevel l,double x,double y,double z,double dx,double dy,double dz,SpriteSet s){
            super(l,x,y,z);pickSprite(s);xd=dx;yd=.025+Math.abs(dy);zd=dz;hasPhysics=false;gravity=.045F;friction=.92F;lifetime=16;quadSize=.045F;
            setColor(.85F,.96F,.7F);alpha=.85F;
        }
        @Override public void tick(){super.tick();alpha=.85F*(1F-(float)age/lifetime);}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
}
