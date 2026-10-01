package com.mcmagic.omnira.client;

import com.mcmagic.omnira.item.RitualSwordItem;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class SwordFocusVisuals {
    @SubscribeEvent public static void inputs(MovementInputUpdateEvent event) {
        var player=event.getEntity();
        if(player.isUsingItem() && player.getUseItem().getItem() instanceof RitualSwordItem sword && sword.kind==RitualSwordItem.Kind.NEEDLE) {
            var input=event.getInput();
            input.forwardImpulse=input.leftImpulse=0;input.jumping=false;
            input.up=input.down=input.left=input.right=false;
        }
    }
    @SubscribeEvent public static void particles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.NAIL_FOCUS.get(),sprites->new Provider(sprites,false));
        event.registerSpriteSet(ModParticles.NEEDLE_FOCUS.get(),sprites->new Provider(sprites,true));
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        var player=event.getEntity();
        if(!player.level().isClientSide || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof RitualSwordItem sword))return;
        boolean needle=sword.kind==RitualSwordItem.Kind.NEEDLE;
        var mc=Minecraft.getInstance();
        // Keep particles below the first-person camera; observers see the same low wrap.
        for(int i=0;i<(needle?8:4);i++) {
            double angle=player.tickCount*.28+i*(needle?.18:Math.PI/2);
            double radius=needle?.52:1.05;
            double x=player.getX()+Math.cos(angle)*radius,z=player.getZ()+Math.sin(angle)*radius;
            double y=player.getY()+.25+(Math.sin(angle*.45)+1)*.40;
            mc.particleEngine.createParticle(needle?ModParticles.NEEDLE_FOCUS.get():ModParticles.NAIL_FOCUS.get(),x,y,z,
                    needle?0:(player.getX()-x)/20,needle?0:.007,needle?0:(player.getZ()-z)/20);
        }
    }
    private record Provider(SpriteSet sprites,boolean needle) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type,ClientLevel level,double x,double y,double z,double dx,double dy,double dz) {
            return new FocusParticle(level,x,y,z,dx,dy,dz,sprites,needle);
        }
    }
    private static final class FocusParticle extends TextureSheetParticle {
        FocusParticle(ClientLevel level,double x,double y,double z,double dx,double dy,double dz,SpriteSet sprites,boolean needle) {
            super(level,x,y,z,dx,dy,dz);pickSprite(sprites);
            xd=dx;yd=dy;zd=dz;gravity=0;friction=1;hasPhysics=false;
            lifetime=needle?16:24;quadSize=needle?.065F:.12F;
            setColor(needle?.85F:.72F,needle?.74F:.93F,1);alpha=.55F;
        }
        @Override public void tick() {super.tick();alpha=.55F*(1-age/(float)lifetime);}
        @Override public ParticleRenderType getRenderType() {return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;}
        @Override protected int getLightColor(float tick) {return 0xF000F0;}
    }
}
