package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class ParadoxSparkVisuals {
    @SubscribeEvent public static void register(RegisterParticleProvidersEvent event){
        event.registerSpriteSet(ModParticles.PARADOX_SPARK.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new Spark(level,x,y,z,dx,dy,dz,sprites,false));
        event.registerSpriteSet(ModParticles.TIME_WARP_SPARK.get(),sprites->(type,level,x,y,z,dx,dy,dz)->new Spark(level,x,y,z,dx,dy,dz,sprites,true));
    }
    private static final class Spark extends TextureSheetParticle {
        private final float phase;
        private final boolean green;
        private final boolean pale;
        Spark(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean green){
            super(level,x,y,z,dx,dy,dz);pickSprite(sprites);xd=dx;yd=dy;zd=dz;
            this.green=green;this.pale=green && random.nextInt(4)==0;
            lifetime=green?28:20;quadSize=green?.075F:.035F;
            hasPhysics=false;gravity=0;friction=1;phase=random.nextFloat()*6.28F;
        }
        @Override public void tick(){
            super.tick();float shade=.5F+.5F*(float)Math.sin(age*.28+phase);
            if(green){
                if(pale)setColor(.78F+.18F*shade,.94F+.06F*shade,.82F+.16F*shade);
                else setColor(.26F+.3F*shade,.72F+.28F*shade,.42F+.26F*shade);
            }
            else{float gray=.09F+.91F*shade;setColor(gray,gray,gray);}
            alpha=(float)Math.sin(Math.PI*age/lifetime)*.85F;
        }
        @Override public ParticleRenderType getRenderType(){return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float partial){return 0xF000F0;}
    }
}
