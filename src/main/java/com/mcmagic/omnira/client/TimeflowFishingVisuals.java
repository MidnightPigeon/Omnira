package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModParticles;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class TimeflowFishingVisuals {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ModParticles.TIMEFLOW_BITE_RING.get(),s->(t,l,x,y,z,dx,dy,dz)->new Ripple(l,x,y,z,dx,dz,s,true,true));
        event.registerSpriteSet(ModParticles.STARFLOW_BITE_RING.get(),s->(t,l,x,y,z,dx,dy,dz)->new Ripple(l,x,y,z,dx,dz,s,false,true));
        event.registerSpriteSet(ModParticles.STARFLOW_BUBBLE.get(),s->(t,l,x,y,z,dx,dy,dz)->{var p=new Bubble(l,x,y,z,dx,dy,dz,s);p.setColor(.8F,.7F,1F);return p;});
        event.registerSpriteSet(ModParticles.TIMEFLOW_BUBBLE.get(),s->(t,l,x,y,z,dx,dy,dz)->new Bubble(l,x,y,z,dx,dy,dz,s));
        event.registerSpriteSet(ModParticles.STARFLOW_RIPPLE.get(),s->(t,l,x,y,z,dx,dy,dz)->new Ripple(l,x,y,z,dx,dz,s,false,false));
        event.registerSpriteSet(ModParticles.STARFLOW_FOAM.get(),s->(t,l,x,y,z,dx,dy,dz)->new WaitingFoam(l,x,y,z,dx,dy,dz,s,false));
        event.registerSpriteSet(ModParticles.TIMEFLOW_RIPPLE.get(),s->(t,l,x,y,z,dx,dy,dz)->new Ripple(l,x,y,z,dx,dz,s,true,false));
        event.registerSpriteSet(ModParticles.TIMEFLOW_FOAM.get(),s->(t,l,x,y,z,dx,dy,dz)->new WaitingFoam(l,x,y,z,dx,dy,dz,s,true));
    }
    private static final class Ripple extends TextureSheetParticle {
        private final boolean rewind,bite;
        Ripple(ClientLevel l,double x,double y,double z,double dx,double dz,SpriteSet s,boolean rewind,boolean bite){
            super(l,x,y,z);pickSprite(s);this.rewind=rewind;this.bite=bite;xd=dx;yd=0;zd=dz;hasPhysics=false;gravity=0;friction=.88F;lifetime=24;
            quadSize=bite?1.4F:.14F;
            if(rewind)setColor(.78F,.94F,.63F);else setColor(.72F,.86F,1F);
            alpha=bite?1F:.7F;
        }
        // Sodium's generic billboard path skips getFacingCameraMode. Submit an oriented quad directly.
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){
            renderRotatedQuad(buffer,camera,new Quaternionf().rotationX(-(float)Math.PI/2),partial);
        }
        @Override public float getQuadSize(float partial){float phase=Math.min(1,(age+partial)/lifetime);return (bite?.22F:.14F)+(bite?1.18F:.34F)*(rewind?1-phase:phase);}
        @Override public void tick(){super.tick();alpha=bite?Math.clamp((lifetime-age)/6F,0F,1F):.7F*(1F-(float)age/lifetime);}
        @Override protected int getLightColor(float partial){return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
    private static final class Bubble extends TextureSheetParticle {
        Bubble(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites){
            super(level,x,y,z);pickSprite(sprites);xd=dx;yd=.025+Math.abs(dy);zd=dz;hasPhysics=false;gravity=.045F;friction=.92F;lifetime=16;quadSize=.045F;
            setColor(.85F,.96F,.7F);alpha=.85F;
        }
        @Override public void tick(){super.tick();alpha=.85F*(1F-(float)age/lifetime);}
        @Override protected int getLightColor(float partial){return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
    private static final class WaitingFoam extends TextureSheetParticle {
        private final boolean flips;
        WaitingFoam(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean flips){
            super(level,x,y,z);
            pickSprite(sprites);
            this.flips=flips;
            xd=dx; yd=.035+Math.abs(dy); zd=dz;
            hasPhysics=false; gravity=0; friction=.95F;
            lifetime=22; quadSize=.09F;
        }
        @Override public float getQuadSize(float partial){return .045F+.085F*Math.min(1,(age+partial)/lifetime);}
        @Override public void render(VertexConsumer buffer,Camera camera,float partial){
            var rotation=new Quaternionf(camera.rotation());
            if(flips){
                float turn=Math.clamp((age+partial-3)/13F,0F,1F);
                float angle=(float)Math.PI*turn*turn*(3-2*turn);
                rotation.rotateZ(angle).rotateX(.55F*(float)Math.sin(angle));
            }
            super.renderRotatedQuad(buffer,camera,rotation,partial);
        }
        @Override public void tick(){
            super.tick();
            if(age%4==0 && age<lifetime-4)level.addParticle(net.minecraft.core.particles.ParticleTypes.FIREWORK,xo,yo,zo,0,.002,0);
            alpha=Math.clamp((lifetime-age)/7F,0F,1F);
        }
        @Override protected int getLightColor(float partial){return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
    }
}
