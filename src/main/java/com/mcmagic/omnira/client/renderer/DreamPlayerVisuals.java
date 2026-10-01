package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

/** Dynamic player skins need their own retained buffers, not an early-flushing shared batch. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class DreamPlayerVisuals {
    private record Batch(ByteBufferBuilder memory,MultiBufferSource.BufferSource source) {}
    private static final java.util.Map<ResourceLocation,Batch> SKINS=new java.util.HashMap<>();
    private static boolean world;
    private DreamPlayerVisuals() {}
    public static boolean active(LivingEntity entity){return entity instanceof Player && entity.getData(ModAttachments.DREAM_AFFINITY);}
    public static int color(int original,LivingEntity player){
        float opacity=.55F+.45F*Math.clamp(player.getData(ModAttachments.DREAM_SOLIDIFY),0,1);
        return (original&0xFFFFFF)|((int)((original>>>24)*opacity)<<24);
    }
    public static VertexConsumer buffer(MultiBufferSource original,ResourceLocation skin) {
        var type=CrystalGlassLayer.entity(skin);
        if(!world || MirrorScene.rendering())return original.getBuffer(type);
        return SKINS.computeIfAbsent(skin,key->{var memory=new ByteBufferBuilder(8192);return new Batch(memory,MultiBufferSource.immediate(memory));}).source.getBuffer(type);
    }
    @SubscribeEvent public static void stage(RenderLevelStageEvent event) {
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_SKY)world=true;
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            SKINS.values().forEach(batch->batch.source.endBatch());world=false;
        }
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_LEVEL)world=false;
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SKINS.values().forEach(batch->batch.memory.close());SKINS.clear();world=false;
    }
}
