package com.mcmagic.omnira.client;

import java.util.*;
import com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity.Stage;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.*;

/** Display-only residents: no world insertion, AI, collisions or entity ticks. */
final class MiniatureWorldInhabitants {
    private static final Map<EntityType<?>,Entity> CACHE=new IdentityHashMap<>();
    static void clear(){CACHE.clear();}
    static void draw(Stage stage,double time,PoseStack pose,MultiBufferSource buffers,int light){
        draw(stage,time,pose,buffers,light,1);
    }
    static void draw(Stage stage,double time,PoseStack pose,MultiBufferSource buffers,int light,float appearance){
        if(appearance<=.001F)return;
        switch(stage){
            case NETHER->{for(int i=0;i<2;i++){double a=time*.012+i*Math.PI;actor(EntityType.PIGLIN,time,2.25+Math.cos(a)*.45,1.125,2.4+Math.sin(a)*.6,.19F*appearance,pose,buffers,light);}}
            case END->actor(EntityType.END_CRYSTAL,time,2.5,2.62,2.5,.16F*appearance,pose,buffers,net.minecraft.client.renderer.LightTexture.FULL_BRIGHT);
            case OVERWORLD->actor(EntityType.VILLAGER,time,2.5,1.125,3.5,.16F*appearance,pose,buffers,light);
            case FORTRESS->actor(EntityType.BLAZE,time,2.95,1.7,2.75,.16F*appearance,pose,buffers,light);
            case BASTION->actor(EntityType.PIGLIN_BRUTE,time,2.5,1.25,2.95,.17F*appearance,pose,buffers,light);
            case CHORUS->actor(EntityType.ENDERMAN,time,2.6,1.125,2.2,.14F*appearance,pose,buffers,light);
            case CITY->{actor(EntityType.ENDERMAN,time,2.15,1.125,3.1,.14F*appearance,pose,buffers,light);actor(EntityType.SHULKER,time,3.05,2.375,2.15,.17F*appearance,pose,buffers,light);}
            case DREAM_LAYERS->{actor(ModEntityTypes.LIGHT_SPIRIT.get(),time,2.1,3.6,3.1,.24F*appearance,pose,buffers,light);actor(ModEntityTypes.SHADOW_GHOST.get(),time,2.4,1.15,3.05,.16F*appearance,pose,buffers,light);}
            default->{ }
        }
    }
    private static void actor(EntityType<?> type,double time,double x,double y,double z,float scale,PoseStack pose,MultiBufferSource buffers,int light){
        var mc=Minecraft.getInstance();if(mc.level==null)return;
        var entity=CACHE.get(type);if(entity==null||entity.level()!=mc.level){entity=type.create(mc.level);if(entity==null)return;CACHE.put(type,entity);}
        float angle=(float)(Math.sin(time*.009)*35);
        if(entity instanceof LivingEntity living){living.yBodyRot=living.yBodyRotO=living.yHeadRot=living.yHeadRotO=angle;if(entity.tickCount!=(int)time)living.walkAnimation.update(.06F,1);}
        entity.tickCount=(int)time;
        entity.moveTo(0,0,0,angle,0);
        if(entity instanceof net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal){crystal.setShowBottom(false);crystal.time=(int)time;}
        pose.pushPose();pose.translate(x,y,z);pose.scale(scale,scale,scale);
        mc.getEntityRenderDispatcher().getRenderer(entity).render(entity,angle,(float)(time-Math.floor(time)),pose,buffers,light);pose.popPose();
    }
    private MiniatureWorldInhabitants(){}
}
