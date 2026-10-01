package com.mcmagic.omnira.client;

import com.mcmagic.omnira.vehicle.CruiseOrbProtection;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class CruiseOrbVision {
    @SubscribeEvent public static void overlay(RenderBlockScreenEffectEvent event){
        if(CruiseOrbProtection.cabin(event.getPlayer())!=null)event.setCanceled(true);
    }
    @SubscribeEvent public static void fog(ViewportEvent.RenderFog event){
        if(CruiseOrbProtection.cabin(event.getCamera().getEntity())==null || event.getType()!=FogType.WATER && event.getType()!=FogType.LAVA)return;
        event.setNearPlaneDistance(0);event.setFarPlaneDistance(48);event.setCanceled(true);
    }
    @SubscribeEvent public static void color(ViewportEvent.ComputeFogColor event){
        if(CruiseOrbProtection.cabin(event.getCamera().getEntity())==null)return;
        var type=event.getCamera().getFluidInCamera();
        if(type==FogType.WATER){event.setRed(.22F);event.setGreen(.46F);event.setBlue(.62F);}
        if(type==FogType.LAVA){event.setRed(.55F);event.setGreen(.25F);event.setBlue(.1F);}
    }
}
