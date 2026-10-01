package com.mcmagic.omnira.client;

import com.mcmagic.omnira.spacetime.SpacetimeSky;
import com.mcmagic.omnira.time.FleetingTime;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class SpacetimeSkyEffects extends DimensionSpecialEffects {
    public SpacetimeSkyEffects() { super(Float.NaN, false, SkyType.NONE, false, false); }

    @Override public Vec3 getBrightnessDependentFogColor(Vec3 color, float brightness) {
        var minecraft = Minecraft.getInstance();
        float forest = minecraft.level == null ? 0 : forestTint(minecraft.level, minecraft.gameRenderer.getMainCamera().getPosition());
        float ruins = minecraft.level == null ? 0 : biomeTint(minecraft.level,minecraft.gameRenderer.getMainCamera().getPosition(),com.mcmagic.omnira.time.EventideRuins.BIOME);
        int rgb = SpacetimeSkyMesh.ruinsTint(SpacetimeSkyMesh.tint(0xFF3D5B76, forest),ruins);
        return Vec3.fromRGB24(rgb & 0xFFFFFF);
    }

    @Override public boolean isFoggyAt(int x, int z) { return false; }
    @Override public float[] getSunriseColor(float time, float partial) { return null; }
    @Override public boolean renderClouds(ClientLevel level, int ticks, float partial, PoseStack pose,
                                          double x, double y, double z, Matrix4f view, Matrix4f projection) { return true; }

    @Override public void adjustLightmapColors(ClientLevel level, float partial, float skyDarken,
                                              float blockFlicker, float skyLight, int block, int sky, Vector3f colors) {
        float emitted = LightTexture.getBrightness(level.dimensionType(), block) * blockFlicker;
        float green = emitted * ((emitted * .6F + .4F) * .6F + .4F);
        float blue = emitted * (emitted * emitted * .6F + .4F);
        float daylight = LightTexture.getBrightness(level.dimensionType(), SpacetimeSky.effectiveLight(sky));
        colors.set(emitted + daylight * .90F, green + daylight * .96F, blue + daylight);
        colors.lerp(new Vector3f(.75F, .75F, .75F), .04F);
    }

    @Override public boolean renderSky(ClientLevel level, int ticks, float partial, Matrix4f view,
                                        Camera camera, Matrix4f projection, boolean foggy, Runnable setupFog) {
        setupFog.run();
        if (foggy || camera.getFluidInCamera() == FogType.LAVA || camera.getFluidInCamera() == FogType.POWDER_SNOW) return true;
        if (camera.getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) return true;
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1,1,1,1);
        FogRenderer.setupNoFog();
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            var buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            double seconds = (level.getGameTime() + (double)partial) / 20.0;
            SpacetimeSkyMesh.render(seconds, forestTint(level, camera.getPosition()),biomeTint(level,camera.getPosition(),com.mcmagic.omnira.time.EventideRuins.BIOME),
                    (x,y,z,color) -> buffer.addVertex(view,x,y,z).setColor(color));
            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } finally {
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.setShaderColor(1,1,1,1);
            setupFog.run();
        }
        return true;
    }

    // Spatial sampling blends biome edges and stays correct for the mirror's second camera pass.
    private static float forestTint(ClientLevel level, Vec3 position) {
        return biomeTint(level,position,FleetingTime.BIOME);
    }
    private static float biomeTint(ClientLevel level, Vec3 position, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome) {
        BlockPos center = BlockPos.containing(position);
        float weight = 0, total = 0;
        for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            float w = (3 - Math.abs(x)) * (3 - Math.abs(z));
            total += w;
            if (level.getBiome(center.offset(x * 4, 0, z * 4)).is(biome)) weight += w;
        }
        return weight / total;
    }
}
