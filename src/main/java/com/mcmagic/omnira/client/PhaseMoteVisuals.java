package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.space.phase.*;
import com.mcmagic.omnira.spacetime.SpatialCrystalMotes;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.client.renderer.StellarVisuals;
import com.mcmagic.omnira.client.renderer.CrystalInteriorMotes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import java.util.LinkedHashMap;

/** Discover through native animate ticks, never scan terrain or create block entities. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class PhaseMoteVisuals {
    private static final LinkedHashMap<BlockPos,Long> SEEN=new LinkedHashMap<>();
    private static final LinkedHashMap<BlockPos,Long> EXCITED=new LinkedHashMap<>();
    private static Level world;
    private static final int[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},
            {-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    @EventBusSubscriber(modid="omnira",value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
    public static final class Setup {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){
        event.enqueueWork(()->{
            PhaseParticles.observe=PhaseMoteVisuals::observe;
            SpatialCrystalMotes.observe=PhaseMoteVisuals::observeExcited;
        });
    }
    }
    private static void observe(Level level,BlockPos pos){
        if(!level.isClientSide)return;
        if(world!=level){SEEN.clear();EXCITED.clear();world=level;}
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if(pos.distToCenterSqr(camera.x,camera.y,camera.z)>PhaseMoteLayout.RANGE*PhaseMoteLayout.RANGE)return;
        var key=pos.immutable();SEEN.remove(key);SEEN.put(key,level.getGameTime());
        if(SEEN.size()>PhaseMoteLayout.CACHE_LIMIT)SEEN.remove(SEEN.keySet().iterator().next());
    }
    private static void observeExcited(Level level,BlockPos pos){
        if(!level.isClientSide)return;
        if(world!=level){SEEN.clear();EXCITED.clear();world=level;}
        var camera=Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        if(pos.distToCenterSqr(camera.x,camera.y,camera.z)>SpatialCrystalMotes.RANGE*SpatialCrystalMotes.RANGE)return;
        boolean exposed=false;
        for(var face:net.minecraft.core.Direction.values()){
            var next=pos.relative(face);
            if(level.hasChunkAt(next)&&level.getBlockState(next).getCollisionShape(level,next).isEmpty()){exposed=true;break;}
        }
        if(!exposed)return;
        var key=pos.immutable();EXCITED.remove(key);EXCITED.put(key,level.getGameTime());
        if(EXCITED.size()>SpatialCrystalMotes.CACHE_LIMIT)EXCITED.remove(EXCITED.keySet().iterator().next());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;
        if(level!=world){SEEN.clear();EXCITED.clear();world=level;}
        if(level==null||(SEEN.isEmpty()&&EXCITED.isEmpty()))return;
        long now=level.getGameTime();double time=now+event.getPartialTick().getGameTimeDeltaPartialTick(false);
        var camera=event.getCamera().getPosition();var pose=event.getPoseStack();
        var out=mc.renderBuffers().bufferSource().getBuffer(CrystalGlassLayer.COLOR);
        int drawn=0;var it=SEEN.entrySet().iterator();
        while(it.hasNext()){
            var entry=it.next();var pos=entry.getKey();
            double distance=pos.distToCenterSqr(camera.x,camera.y,camera.z);
            if(now-entry.getValue()>100||distance>PhaseMoteLayout.RANGE*PhaseMoteLayout.RANGE||!level.hasChunkAt(pos)){it.remove();continue;}
            var state=level.getBlockState(pos);boolean block=state.is(PhaseContent.BLOCK.get());
            boolean cluster=state.is(PhaseContent.CLUSTER.get())||state.is(PhaseContent.CONTROL.get());
            if(!block&&!cluster){it.remove();continue;}
            if(drawn>=PhaseMoteLayout.VISIBLE_LIMIT||!event.getFrustum().isVisible(new AABB(pos).inflate(.08)))continue;
            drawn++;
            float phase=block?state.getValue(PhaseBlock.PHASE)/16F:0;
            int count=distance>256?1:cluster?3:2;
            var vertices=block?CrystalInteriorMotes.vertices():out;
            pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
            var matrix=pose.last().pose();
            for(int i=0;i<count;i++){
                var mote=PhaseMoteLayout.sample(cluster,phase,time+(pos.asLong()&127),i);
                int color=mote.color();
                for(var face:FACES)for(int corner:face){
                    var v=CORNERS[corner];float r=PhaseMoteLayout.RADIUS;
                    vertices.addVertex(matrix,mote.x()+v[0]*r,mote.y()+v[1]*r,mote.z()+v[2]*r)
                            .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,.85F);
                }
            }
            pose.popPose();
        }
        var excited=EXCITED.entrySet().iterator();
        while(excited.hasNext()){
            var entry=excited.next();var pos=entry.getKey();
            double distance=pos.distToCenterSqr(camera.x,camera.y,camera.z);
            if(now-entry.getValue()>60||distance>SpatialCrystalMotes.RANGE*SpatialCrystalMotes.RANGE
                    ||!level.hasChunkAt(pos)||!level.getBlockState(pos).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get())){
                excited.remove();continue;
            }
            if(drawn>=PhaseMoteLayout.VISIBLE_LIMIT||(distance>256&&(Integer.rotateLeft(pos.hashCode()*0x9E3779B9,13)&1)!=0)
                    ||!event.getFrustum().isVisible(new AABB(pos)))continue;
            drawn++;
            double t=time+(pos.asLong()&255);
            pose.pushPose();pose.translate(pos.getX()-camera.x,pos.getY()-camera.y,pos.getZ()-camera.z);
            StellarVisuals.cube(pose,CrystalInteriorMotes.vertices(),.5F+(float)Math.cos(t*.021)*.2F,
                    SpatialCrystalMotes.height(t),.5F+(float)Math.sin(t*.021)*.2F,
                    SpatialCrystalMotes.RADIUS,SpatialCrystalMotes.COLOR,.85F);
            pose.popPose();
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){SEEN.clear();EXCITED.clear();world=null;}
    private PhaseMoteVisuals(){}
}
