package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.entity.DreamMirror;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import java.util.*;

/** Bounded local-scene planar reflection. Never invokes the recursive world renderer. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class MirrorScene {
    private static final Map<UUID,View> VIEWS=new LinkedHashMap<>();
    private static Object world;
    private static long lastPass;
    private static boolean rendering;
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        if(RenderSystem.isOnRenderThread()) clear();else RenderSystem.recordRenderCall(MirrorScene::clear);
    }
    static boolean rendering() {return rendering;}
    static ResourceLocation frozen(UUID id) {
        if(world!=Minecraft.getInstance().level)return null;
        var view=VIEWS.get(id);return view==null || view.updated==0?null:view.location;
    }
    private static final class View extends AbstractTexture {
        final TextureTarget target=new TextureTarget(128,224,true,Minecraft.ON_OSX);
        final ResourceLocation location;
        long updated;
        View(UUID id) {location=ResourceLocation.fromNamespaceAndPath("omnira","dynamic/mirror_"+id);}
        @Override public int getId() {return target.getColorTextureId();}
        @Override public void load(ResourceManager resources) {}
        @Override public void releaseId() {}
        @Override public void close() {
            int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int id=target.frameBufferId;
            target.destroyBuffers();
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw==id?0:draw);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read==id?0:read);
        }
    }
    static ResourceLocation texture(DreamMirror mirror,float partial,MultiBufferSource source) {
        var mc=Minecraft.getInstance();
        Vec3 eye=mc.gameRenderer.getMainCamera().getPosition(),center=mirror.getPosition(partial).add(0,.9,0),
                n=Vec3.directionFromRotation(0,net.minecraft.util.Mth.rotLerp(partial,mirror.yRotO,mirror.getYRot()));
        double distance=eye.subtract(center).dot(n);
        int range=DreamMirror.OFFERING_REFLECTION_RANGE;
        if(rendering || distance<.1 || eye.distanceToSqr(center)>range*range) return null;
        if(world!=mc.level) {clear();world=mc.level;}
        View view=VIEWS.get(mirror.getUUID());
        long now=net.minecraft.Util.getMillis();
        if(view==null) {
            if(VIEWS.size()>=4) {
                var oldest=VIEWS.entrySet().stream().min(java.util.Comparator.comparingLong(e->e.getValue().updated)).orElseThrow();
                if(now-oldest.getValue().updated<1000) return null;
                mc.getTextureManager().release(VIEWS.remove(oldest.getKey()).location);
            }
            int draw=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),read=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
            int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
            try {
                view=new View(mirror.getUUID());VIEWS.put(mirror.getUUID(),view);mc.getTextureManager().register(view.location,view);
            } finally {
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,draw);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,read);
                RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            }
        }
        if(now-view.updated>=100 && now-lastPass>=45) {
            if(source instanceof MultiBufferSource.BufferSource buffers) buffers.endBatch();
            render(view,mirror,eye,center,n,distance,partial);
            view.updated=now;lastPass=now;
        }
        return view.updated==0?null:view.location;
    }
    static void clear() {
        var textures=Minecraft.getInstance().getTextureManager();
        for(var view:VIEWS.values()) textures.release(view.location);
        VIEWS.clear();world=null;
    }
    private static void render(View view,DreamMirror mirror,Vec3 eye,Vec3 center,Vec3 n,double distance,float partial) {
        var mc=Minecraft.getInstance();
        Vec3 reflected=eye.subtract(n.scale(2*distance)),right=n.cross(new Vec3(0,1,0));
        double ex=eye.subtract(center).dot(right),ey=eye.y-center.y;
        float near=(float)(distance+.02);
        var projection=new Matrix4f().setFrustum((float)((-.43-ex)*near/distance),(float)((.43-ex)*near/distance),
                (float)((-.8-ey)*near/distance),(float)((.8-ey)*near/distance),near,near+40);
        var matrix=new Matrix4f().setLookAt((float)reflected.x,(float)reflected.y,(float)reflected.z,
                (float)(reflected.x+n.x),(float)reflected.y,(float)(reflected.z+n.z),0,1,0);
        int previous=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int previousRead=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int[] viewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);
        var oldProjection=new Matrix4f(RenderSystem.getProjectionMatrix());
        var oldSorting=RenderSystem.getVertexSorting();
        var modelView=RenderSystem.getModelViewStack();modelView.pushMatrix();modelView.identity();RenderSystem.applyModelViewMatrix();
        rendering=true;
        try (var buffers=new MirrorBufferSource()) {
            Vec3 sky = mc.level.getSkyColor(reflected, partial);
            view.target.setClearColor((float)sky.x,(float)sky.y,(float)sky.z,1);
            view.target.clear(Minecraft.ON_OSX);view.target.bindWrite(true);
            // Sky is infinitely distant: use reflected orientation without camera translation,
            // and a separate far plane so the local geometry budget cannot clip sun/stars.
            float skyNear=.05F;
            var skyProjection=new Matrix4f().setFrustum((float)((-.43-ex)*skyNear/distance),(float)((.43-ex)*skyNear/distance),
                    (float)((-.8-ey)*skyNear/distance),(float)((.8-ey)*skyNear/distance),skyNear,4096);
            RenderSystem.setProjectionMatrix(skyProjection,VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.setShader(GameRenderer::getPositionShader);
            RenderSystem.setShaderColor(1,1,1,1);
            mc.levelRenderer.renderSky(new Matrix4f(matrix).setTranslation(0,0,0),skyProjection,partial,
                    mc.gameRenderer.getMainCamera(),false,FogRenderer::setupNoFog);
            RenderSystem.setProjectionMatrix(projection,VertexSorting.DISTANCE_TO_ORIGIN);
            RenderSystem.enableDepthTest();RenderSystem.depthMask(true);RenderSystem.setShaderColor(1,1,1,1);
            FogRenderer.setupNoFog();
            var pose=new PoseStack();pose.mulPose(matrix);
            var origin=mirror.blockPosition();
            int rendered=0;
            var random = net.minecraft.util.RandomSource.create();
            for(var mutable:BlockPos.betweenClosed(origin.offset(-16,-16,-16),origin.offset(16,16,16))) {
                if(Vec3.atCenterOf(mutable).subtract(center).dot(n)<-.8 || !mc.level.hasChunkAt(mutable)) continue;
                var state=mc.level.getBlockState(mutable);
                if(state.getRenderShape()!=RenderShape.MODEL) continue;
                boolean exposed=false;
                for(var direction:Direction.values()) if(!mc.level.getBlockState(mutable.relative(direction)).isSolidRender(mc.level,mutable.relative(direction))) {exposed=true;break;}
                if(!exposed || rendered++>=4500) continue;
                pose.pushPose();pose.translate(mutable.getX(),mutable.getY(),mutable.getZ());
                // World tessellation samples exposed-face light and biome tint. Item rendering
                // lights the entire block from its opaque interior, turning terrain black.
                var model = mc.getBlockRenderer().getBlockModel(state);
                var modelData = model.getModelData(mc.level, mutable, state,
                        mc.level.getModelDataManager().getAt(mutable));
                random.setSeed(state.getSeed(mutable));
                for (var layer : model.getRenderTypes(state, random, modelData)) {
                    mc.getBlockRenderer().renderBatched(state, mutable, mc.level, pose,
                            buffers.getBuffer(layer), true, random, modelData, layer);
                }
                pose.popPose();
            }
            buffers.endBatch();
            int count=0;
            // Other mirrors render only their bodies/plain glass while rendering is true.
            int range=DreamMirror.OFFERING_REFLECTION_RANGE;
            for(var entity:mc.level.getEntities(mirror,mirror.getBoundingBox().inflate(range),
                    e->!e.isInvisible() && e.distanceToSqr(mirror)<=range*range)) {
                if(count++>=24) break;
                Vec3 pos=entity.getPosition(partial);
                if(pos.subtract(center).dot(n)<0) continue;
                mc.getEntityRenderDispatcher().render(entity,pos.x,pos.y,pos.z,entity.getYRot(),partial,pose,buffers,
                        mc.getEntityRenderDispatcher().getPackedLightCoords(entity,partial));
            }
            buffers.endBatch();
        } finally {
            rendering=false;
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,previous);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,previousRead);
            RenderSystem.viewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            RenderSystem.setProjectionMatrix(oldProjection,oldSorting);
            modelView.popMatrix();RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderColor(1,1,1,1);
            FogRenderer.setupFog(mc.gameRenderer.getMainCamera(),FogRenderer.FogMode.FOG_TERRAIN,
                    mc.gameRenderer.getRenderDistance(),false,partial);
        }
    }
}
