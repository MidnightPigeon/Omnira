package com.mcmagic.omnira.workstation;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Effects are emitted by successful work steps, never by merely being an infused block. */
public final class WorkstationEffects {
    private static final DustParticleOptions CYAN=new DustParticleOptions(new Vector3f(.45F,.9F,1),.65F);
    private static final DustParticleOptions GOLD=new DustParticleOptions(new Vector3f(1,.85F,.45F),.8F);
    private static final DustParticleOptions ORANGE=new DustParticleOptions(new Vector3f(1,.38F,.08F),.5F);
    private static final DustParticleOptions WHITE=new DustParticleOptions(new Vector3f(1,1,1),.5F);
    public static void processing(CrystalWorkstationEntity work){
        if(!(work.getLevel() instanceof ServerLevel level))return;
        var pos=Vec3.atCenterOf(work.getBlockPos());
        if(work.kind()==CrystalWorkstations.Kind.FURNACE){
            pos=pos.add(0,-.15,0);
            level.sendParticles(work.infused()?WHITE:ORANGE,pos.x,pos.y,pos.z,5,.14,.008,.14,0);
            if(!work.infused())level.sendParticles(ParticleTypes.SMALL_FLAME,pos.x,pos.y,pos.z,1,.12,.005,.12,0);
        }else if(work.infused()&&work.kind()==CrystalWorkstations.Kind.ANVIL){
            pos=pos.add(0,.35,0);
            level.sendParticles(CYAN,pos.x,pos.y,pos.z,4,.22,.02,.1,.01);
            level.sendParticles(ParticleTypes.CRIT,pos.x,pos.y,pos.z,2,.12,.03,.06,.025);
        }
    }
    public static void repaired(CrystalWorkstationEntity work,int slot){
        if(!(work.getLevel() instanceof ServerLevel level))return;
        var pos=Vec3.atCenterOf(work.getBlockPos()).add(0,.25,slot==0?-.1:.1);
        level.sendParticles(ParticleTypes.END_ROD,pos.x,pos.y,pos.z,5,.12,.12,.12,.015);
        level.sendParticles(CYAN,pos.x,pos.y,pos.z,6,.14,.14,.14,.01);
    }
    public static void completed(CrystalWorkstationEntity work){
        if(!(work.getLevel() instanceof ServerLevel level))return;
        var pos=Vec3.atCenterOf(work.getBlockPos()).add(0,.35,0);
        level.sendParticles(ParticleTypes.CRIT,pos.x,pos.y,pos.z,12,.25,.04,.12,.08);
        level.sendParticles(GOLD,pos.x,pos.y,pos.z,8,.25,.08,.12,.015);
    }
    public static void absorbing(CrystalWorkstationEntity work,Player player){
        if(!(work.getLevel() instanceof ServerLevel level))return;
        var from=player.position().add(0,player.getBbHeight()*.6,0);
        var to=Vec3.atCenterOf(work.getBlockPos()).add(0,.15,0);
        for(int i=1;i<=8;i++){
            var at=from.lerp(to,i/8.0);
            level.sendParticles(CYAN,at.x,at.y,at.z,1,.02,.02,.02,0);
        }
        // ENCHANT particles use a directed offset, travelling back toward their origin.
        var offset=from.subtract(to);
        level.sendParticles(ParticleTypes.ENCHANT,to.x,to.y,to.z,0,offset.x,offset.y,offset.z,1);
    }
    private WorkstationEffects(){}
}
