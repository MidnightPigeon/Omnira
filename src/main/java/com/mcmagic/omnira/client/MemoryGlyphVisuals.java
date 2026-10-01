package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

/** Native enchantment letters, tinted to belong to the complete memory state. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class MemoryGlyphVisuals {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ModParticles.PEACEFUL_MEMORY_GLYPH.get(),s->(t,l,x,y,z,dx,dy,dz)->new Glyph(l,x,y,z,dx,dy,dz,s,true));
        event.registerSpriteSet(ModParticles.CORRUPTED_MEMORY_GLYPH.get(),s->(t,l,x,y,z,dx,dy,dz)->new Glyph(l,x,y,z,dx,dy,dz,s,false));
    }
    private static final class Glyph extends TextureSheetParticle {
        Glyph(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean peaceful){
            super(level,x,y,z,dx,dy,dz);pickSprite(sprites);xd=dx;yd=dy;zd=dz;
            lifetime=36+random.nextInt(16);quadSize=.07F;hasPhysics=false;gravity=0;friction=1;
            if(peaceful)setColor(.82F,1F,.93F);else setColor(.62F,.34F,.72F);
            alpha=0;
        }
        @Override public void tick(){super.tick();alpha=.85F*(float)Math.sin(Math.PI*age/lifetime);}
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float partial){return 0xF000F0;}
    }
}
