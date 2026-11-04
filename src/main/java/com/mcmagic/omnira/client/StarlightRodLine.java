package com.mcmagic.omnira.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mcmagic.omnira.space.sea.StarlightContent;
import com.mcmagic.omnira.space.sea.StarlightFloat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.*;
import java.util.HashMap;
import java.util.Map;

/** Resolve both entities after their world poses have been rendered, regardless of draw order. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class StarlightRodLine {
    private static final Map<FishingHook,Vec3> tips=new HashMap<>(),fallbacks=new HashMap<>();
    private static boolean collecting;
    private static FishingHook handHook;
    private static final Vector4f handJoinClip=new Vector4f();
    private StarlightRodLine(){}

    public static void hook(FishingHook hook,Vec3 vanillaHand){
        if(collecting && ((StarlightFloat)hook).omnira$isStarlight())fallbacks.put(hook,vanillaHand);
    }

    public static void held(LivingEntity entity,ItemStack rod,ItemDisplayContext context,
                            PoseStack pose,MultiBufferSource buffers){
        if(!rod.is(StarlightContent.ROD.get()) || !(entity instanceof Player player) || player.fishing==null)return;
        boolean first=context.firstPerson();
        if(first ? player.fishing!=handHook : !collecting)return;
        var held=player.getMainHandItem().getItem() instanceof FishingRodItem?player.getMainHandItem():player.getOffhandItem();
        if(held!=rod || !((StarlightFloat)player.fishing).omnira$isStarlight())return;
        var mc=Minecraft.getInstance();

        // ItemRenderer has already applied the hand transform and centered the model.
        var eye=pose.last().pose().transformPosition(8.30f/16,26.15f/16,.5f,new Vector3f());
        var start=new Vec3(eye);
        if(!first){
            tips.put(player.fishing,start.add(mc.gameRenderer.getMainCamera().getPosition()));
            return;
        }
        // The hand uses a different FOV and view bob. Reproject just the short join near the hand;
        // the rest of the line stays in the depth-tested world pass.
        var join=new Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix())
                .invert().transform(new Vector4f(handJoinClip));
        if(java.lang.Math.abs(join.w)>1e-6f)
            strand(buffers,start,new Vec3(join.x/join.w,join.y/join.w,join.z/join.w),0);
    }

    @SubscribeEvent public static void stage(RenderLevelStageEvent event){
        if(event.getStage()==RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS){
            collecting=true;tips.clear();fallbacks.clear();handHook=null;
        }
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        collecting=false;
        var mc=Minecraft.getInstance();
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();
        // The local hook can be outside the frustum while the visible strand crosses the screen.
        if(mc.options.getCameraType().isFirstPerson() && mc.player!=null && mc.player.fishing!=null
                && ((StarlightFloat)mc.player.fishing).omnira$isStarlight())
            fallbacks.putIfAbsent(mc.player.fishing,mc.player.getEyePosition(partial));
        fallbacks.forEach(tips::putIfAbsent);
        var buffers=mc.renderBuffers().bufferSource();
        tips.forEach((hook,tip)->{
            var end=hook.getPosition(partial).add(0,.25,0).subtract(camera);
            var start=tip.subtract(camera);
            var player=hook.getPlayerOwner();
            if(player!=null && player==mc.player && mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui
                    && (player.getMainHandItem().is(StarlightContent.ROD.get()) || player.getOffhandItem().is(StarlightContent.ROD.get()))){
                double distance=start.distanceTo(end);
                start=start.lerp(end,java.lang.Math.min(1,1.25/java.lang.Math.max(distance,.001)));
                handHook=hook;
                handJoinClip.set((float)start.x,(float)start.y,(float)start.z,1);
                event.getModelViewMatrix().transform(handJoinClip);
                event.getProjectionMatrix().transform(handJoinClip);
            }
            strand(buffers,start,end,java.lang.Math.min(.32,start.distanceTo(end)*.028));
        });
        tips.clear();fallbacks.clear();
    }

    private static void strand(MultiBufferSource buffers,Vec3 start,Vec3 end,double sag){
        if(start.distanceToSqr(end)<1e-10)return;
        var out=buffers.getBuffer(RenderType.lines());
        int steps=sag==0?1:16;
        Vec3 a=start;
        for(int i=1;i<=steps;i++){
            double t=(double)i/steps;
            var b=start.lerp(end,t).add(0,-4*sag*t*(1-t),0);
            var n=b.subtract(a).normalize();
            out.addVertex((float)a.x,(float)a.y,(float)a.z).setColor(169,209,221,255).setNormal((float)n.x,(float)n.y,(float)n.z);
            out.addVertex((float)b.x,(float)b.y,(float)b.z).setColor(169,209,221,255).setNormal((float)n.x,(float)n.y,(float)n.z);
            a=b;
        }
    }
}
