package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.entity.OrbUpgrades;
import com.mcmagic.omnira.vehicle.CruiseOrbEntity;
import com.mcmagic.omnira.vehicle.CruiseOrbPanel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Local origin is the bottom of the sphere; forward is -Z, passenger-facing is +Z. */
public final class CruiseOrbRenderer extends EntityRenderer<CruiseOrbEntity> {
    private static final ResourceLocation SHELL = texture("shell");
    private static final ResourceLocation MOTE = texture("mote");
    private static final ResourceLocation GOLD = ResourceLocation.fromNamespaceAndPath("omnira", "block/cruise_orb_golden_seat");
    private static final ResourceLocation WATER = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final int BLUE = 0x9DDAF4;
    // Compensate the existing pale-blue mote texel to neutral grey, without changing shared textures.
    private static final int CLEAR = 0xFFF5F2;
    private static final int SEGMENTS = 16;
    private static final int[][] NEIGHBORS={{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private static final ShellMeshes SHELL_GEOMETRY = shellMeshes(25);
    private static final Mesh SPHERE = SHELL_GEOMETRY.intact();
    private static final Mesh[] CRACKED_SHELLS = SHELL_GEOMETRY.shells();
    private static final Mesh[] CRACK_VOXELS = SHELL_GEOMETRY.cracks();
    private static final Mesh SMALL_SPHERE = sphere(8);
    private static final Mesh FLOOR = floor();
    private static final Mesh CHAIR = chair();
    private static final Mesh GOLDEN_SEAT = goldenSeat();
    private static final Mesh GOLDEN_WATER = box(-.16F, 1.003F, -.11F, .16F, 1.012F, .27F);
    private static final Mesh PANEL = panel();
    private static final Mesh BUTTON = box(-CruiseOrbPanel.BUTTON_WIDTH, -CruiseOrbPanel.BUTTON_HEIGHT, -.025F,
            CruiseOrbPanel.BUTTON_WIDTH, CruiseOrbPanel.BUTTON_HEIGHT, .025F);
    private static final float SHELL_ALPHA=.16F, SMALL_ALPHA=.24F;
    private static final float CRACK_ALPHA=.8F;
    private static final Mesh RECESS = box(-.12F, -.12F, -.01F, .12F, .12F, 0);
    private static final Mesh RIM = rim();
    private static final Mesh RING = ring();
    private static final Mesh SPARK = box(-.018F, -.018F, -.018F, .018F, .018F, .018F);

    public CruiseOrbRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0;
    }

    @Override public ResourceLocation getTextureLocation(CruiseOrbEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }

    @Override public boolean shouldRender(CruiseOrbEntity entity, Frustum frustum, double x, double y, double z) {
        return entity.shouldRender(x, y, z)
                && frustum.isVisible(entity.getBoundingBox().inflate(entity.vortexTicks() > 0 ? 4 : .3));
    }

    @Override public void render(CruiseOrbEntity entity, float yaw, float partial, PoseStack pose,
                                 MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(partial,entity.yRotO,entity.getYRot())));
        renderOrb(entity, pose, VehicleGlassBuffers.wrap(buffers), entity.level().getGameTime() + partial, partial, light,
                OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, yaw, partial, pose, buffers, light);
    }

    private static void renderOrb(CruiseOrbEntity entity, PoseStack pose, MultiBufferSource buffers,
                                  double time, float partial, int light, int overlay) {
        // Match CrystalBallRenderer's fixed sorted batch, including custom spell-core items.
        MultiBufferSource contents = type -> buffers.getBuffer(type == Sheets.translucentItemSheet()
                ? CrystalGlassLayer.ATLAS : type);
        FLOOR.drawFloor(pose,buffers,time,entity!=null && entity.started(),entity!=null && entity.overloaded(),light,overlay);
        if (entity != null && entity.storage.hasGoldenToilet()) {
            GOLDEN_SEAT.drawOpaque(pose, buffers, GOLD, light, overlay);
            GOLDEN_WATER.draw(pose, buffers, WATER, 0xA6DFF5, .68F, light, overlay);
        } else CHAIR.draw(pose, buffers, MOTE, BLUE, .16F, light, overlay);
        pose.pushPose();
        pose.mulPose(CruiseOrbPanel.transform());
        PANEL.draw(pose, buffers, MOTE, BLUE, .16F, light, overlay);
        boolean active = entity != null && entity.started();
        boolean overloaded = entity != null && entity.overloaded();
        button(pose, buffers, -CruiseOrbPanel.BUTTON_X, 0x163873, 0x408FFF,
                active && !overloaded, entity!=null && entity.buttonPressed(0), light, overlay);
        button(pose, buffers, CruiseOrbPanel.BUTTON_X, 0x701B2C, 0xEE586D,
                active && overloaded, entity!=null && entity.buttonPressed(1), light, overlay);
        for (int slot = 0; slot < 3; slot++) {
            ItemStack upgrade = entity == null ? ItemStack.EMPTY : entity.storage.upgrades.getStackInSlot(slot);
            pose.pushPose();
            pose.translate((slot - 1) * CruiseOrbPanel.SLOT_X, 1.53, -.95);
            pose.scale(CruiseOrbPanel.SLOT_SCALE,CruiseOrbPanel.SLOT_SCALE*.7F/1.12F,CruiseOrbPanel.SLOT_SCALE);
            RECESS.draw(pose, buffers, SHELL, 0x538DA9, 1, light, overlay);
            pose.translate(0, 0, .06);
            var kind = OrbUpgrades.kind(upgrade);
            RIM.draw(pose, buffers, kind == null ? SHELL : MOTE,
                    kind == null ? BLUE : kind.displayColor, kind == null ? 1 : .8F,
                    kind == null ? light : LightTexture.FULL_BRIGHT, overlay);
            if (!upgrade.isEmpty()) {
                pose.translate(0, 0, -.025);
                pose.scale(.18F, .18F, .045F);
                Minecraft.getInstance().getItemRenderer().renderStatic(upgrade, ItemDisplayContext.NONE,
                        LightTexture.FULL_BRIGHT, overlay, pose, contents, Minecraft.getInstance().level, slot);
            }
            pose.popPose();
        }

        pose.popPose();
        if (entity != null && !entity.core().isEmpty()) {
            pose.pushPose();
            pose.translate(0, 2.75, 0);
            pose.mulPose(Axis.YP.rotationDegrees((float) (time * .8)));
            pose.scale(.32F, .32F, .32F);
            pose.translate(-.5, -.5, -.5);
            SpellCoreRenderer.renderCore(entity.core(), pose, contents, LightTexture.FULL_BRIGHT, overlay, time);
            pose.popPose();
        }
        for (int orb = 0; orb < 2; orb++) {
            double phase = time * .025 + orb * Math.PI;
            pose.pushPose();
            pose.translate(Math.cos(phase) * .55, .48, Math.sin(phase) * .55);
            if (entity != null) {
                if (orb == 0) storedItems(entity, pose, contents, time, overlay);
                else if(entity.storage.tank.isEmpty()) {
                    airMotes(pose,buffers,time,overlay);
                } else {
                    pose.pushPose();
                    pose.scale(.72F, .72F, .72F);
                    pose.translate(-.5, -.5, -.5);
                    LiquidOrbVisuals.render(entity.storage.tank.getFluid(), entity.storage.tank.getCapacity(),
                            pose, contents, time, light, overlay);
                    pose.popPose();
                }
            }
            pose.scale(.27F, .27F, .27F);
            SMALL_SPHERE.draw(pose, buffers, MOTE, CLEAR, SMALL_ALPHA, light, overlay);
            pose.popPose();
        }

        effects(entity, pose, buffers, time, partial, overlay);
        pose.pushPose();
        pose.translate(0, 1.5, 0);
        pose.scale(1.5F, 1.5F, 1.5F);
        if (entity != null && entity.hasCracks()) {
            int stage=entity.crackStage()-1;
            CRACKED_SHELLS[stage].draw(pose, buffers, MOTE, CLEAR, SHELL_ALPHA, light, overlay);
            CRACK_VOXELS[stage].draw(pose, buffers, MOTE, 0xFFFFFF, CRACK_ALPHA, light, overlay);
        } else SPHERE.draw(pose, buffers, MOTE, CLEAR, SHELL_ALPHA, light, overlay);
        pose.popPose();
    }

    private static void button(PoseStack pose, MultiBufferSource buffers, double x, int color, int activeColor,
                               boolean lit, boolean pressed, int light, int overlay) {
        pose.pushPose();
        pose.translate(x, 1.8, pressed?-.94:-.9);
        BUTTON.draw(pose, buffers, MOTE, lit?activeColor:color, lit?.8F:.4F, lit ? LightTexture.FULL_BRIGHT : light, overlay);
        pose.popPose();
    }

    private static void airMotes(PoseStack pose,MultiBufferSource buffers,double time,int overlay){
        for(int i=0;i<6;i++){
            double a=time*.045+i*Math.PI/3;
            pose.pushPose();
            pose.translate(Math.cos(a)*.145,Math.sin(a*1.3+i)*.12,Math.sin(a)*.145);
            SPARK.draw(pose,buffers,MOTE,0xDDF7FF,.85F,LightTexture.FULL_BRIGHT,overlay);
            pose.popPose();
        }
    }

    private static void storedItems(CruiseOrbEntity entity, PoseStack pose, MultiBufferSource buffers,
                                    double time, int overlay) {
        var renderer = Minecraft.getInstance().getItemRenderer();
        // The storage contract has twelve slots; keep rendering bounded if it grows later.
        int slots = Math.min(12, entity.storage.items.getSlots());
        int displayed = 0;
        for (int slot = 0; slot < slots; slot++) {
            var item = entity.storage.items.getStackInSlot(slot);
            if (item.isEmpty()) continue;
            double phase = time * .025 + displayed * 2.399963;
            pose.pushPose();
            pose.translate(Math.cos(phase) * .115, Math.sin(phase * 1.31) * .1, Math.sin(phase) * .115);
            pose.mulPose(Axis.YP.rotationDegrees((float) (time * .65 + displayed * 137.5)));
            var model = renderer.getModel(item, entity.level(), null, slot);
            boolean thin = !model.isGui3d() || !model.usesBlockLight();
            if (thin) pose.mulPose(Axis.XP.rotationDegrees(20 + (float) Math.sin(phase) * 15));
            boolean miniature = thin && CrystalBallItemShape.apply(model, pose);
            if (!miniature) pose.scale(.2F, .2F, .2F);
            renderer.render(item, miniature ? ItemDisplayContext.NONE : ItemDisplayContext.GROUND,
                    false, pose, buffers, LightTexture.FULL_BRIGHT, overlay, model);
            pose.popPose();
            displayed++;
        }
    }

    private static void effects(CruiseOrbEntity entity, PoseStack pose, MultiBufferSource buffers,
                                double time, float partial, int overlay) {
        int color = propulsionColor(entity,time);
        for (int i = 0; i < 2; i++) {
            pose.pushPose();
            pose.translate(0, .08 + i * .10, 0);
            float radius = .42F + i * .19F;
            pose.scale(radius, radius, radius);
            RING.draw(pose, buffers, MOTE, color, .55F, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
        for (int i = 0; i < 8; i++) {
            double phase = time * .035 + i * Math.PI / 4;
            pose.pushPose();
            pose.translate(Math.cos(phase) * .66, .15 + .065 * Math.sin(phase * 2), Math.sin(phase) * .66);
            SPARK.draw(pose, buffers, MOTE, color, .6F, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
        Vec3 axes = entity == null ? Vec3.ZERO : entity.motionAxes();
        boolean accelerated=entity!=null && entity.storage.speedMultiplier()>1 && entity.getDeltaMovement().lengthSqr()>1.0E-6;
        for (int axis = 0; axis < 3; axis++) {
            double value = axis == 0 ? axes.x : axis == 1 ? axes.y : axes.z;
            if (Math.abs(value) < .001) continue;
            pose.pushPose();
            pose.translate(0, 1.5, 0);
            if (axis == 0) pose.mulPose(Axis.ZP.rotationDegrees(-90));
            if (axis == 2) pose.mulPose(Axis.XP.rotationDegrees(-90));
            // The ring's normal points along +X, +Y or forward (-Z), respectively.
            for (int spark = 0; spark < 4; spark++) {
                double phase = sparkPhase(time, spark);
                double angle = spark * Math.PI / 2;
                pose.pushPose();
                pose.translate(Math.cos(angle) * 1.27, sparkTravel(phase, -value), Math.sin(angle) * 1.27);
                SPARK.draw(pose, buffers, MOTE, color, (float) (.25 + .55 * Math.sin(phase * Math.PI)),
                        LightTexture.FULL_BRIGHT, overlay);
                pose.popPose();
            }
            if(accelerated)for(int spark=0;spark<2;spark++){
                double phase=sparkPhase(time*1.4,spark*2),angle=time*.04+spark*Math.PI;
                pose.pushPose();
                pose.translate(Math.cos(angle)*1.27,sparkTravel(phase,-value),Math.sin(angle)*1.27);
                SPARK.draw(pose,buffers,MOTE,0xC184F5,(float)(.65*Math.sin(phase*Math.PI)),LightTexture.FULL_BRIGHT,overlay);
                pose.popPose();
            }
            pose.translate(0, Math.copySign(.86, -value), 0);
            pose.scale(1.27F, 1.27F, 1.27F);
            RING.draw(pose, buffers, MOTE, color, .65F, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
        if (entity != null && entity.vortexTicks() > 0) {
            float progress = 1 - Math.clamp(entity.vortexTicks() - partial, 0, 160) / 160F;
            float radius = (float)com.mcmagic.omnira.vehicle.CruiseVortex.radius(entity.vortexTicks()-partial);
            if (radius > .01F) {
                for(int i=0;i<96;i++){
                    double y=1-2*(i+.5)/96,ring=Math.sqrt(1-y*y),angle=i*2.399963229728653+progress*Math.PI*8;
                    pose.pushPose();pose.translate(Math.cos(angle)*ring*radius,1.5+y*radius,Math.sin(angle)*ring*radius);
                    pose.scale(1.7F,1.7F,1.7F);
                    SPARK.draw(pose,buffers,MOTE,color,.72F,LightTexture.FULL_BRIGHT,overlay);pose.popPose();
                }
            }
        }
    }

    private static double sparkPhase(double time, int spark) {
        double cycle = time / 24 + spark * .25;
        return cycle - Math.floor(cycle);
    }
    private static int propulsionColor(CruiseOrbEntity entity,double time){
        if(entity!=null && !entity.core().isEmpty())return SpellCoreRenderer.effectColor(entity.core(),time);
        return entity!=null && entity.overloaded()?0xEF7885:0xA6E9FA;
    }

    private static float floorRotation(double time, boolean overloaded, int sector) {
        return (float) (time * (overloaded ? -2.4 : 1.2) + sector * 120);
    }

    private static int floorColor(boolean overloaded, int sector) {
        return overloaded ? (sector == 1 ? 0xEE486B : 0xAC67EE)
                : (sector == 1 ? 0x478FFF : 0x65DCF4);
    }
    private static int floorPixelColor(double x,double z,double time,boolean active,boolean overload){
        if(!active)return BLUE;
        double phase=Math.atan2(z,x)-Math.toRadians(floorRotation(time,overload,0))+Math.hypot(x,z)*2;
        return Math.cos(phase*3)>.7?floorColor(overload,(int)Math.floor((phase+20)*3)&1):BLUE;
    }

    private static double sparkTravel(double phase, double axisValue) {
        return Math.copySign(.55 + .9 * phase, axisValue);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath("omnira", "block/crystal_ball_" + name);
    }

    // Cached vertices: position, UV, flat normal. No per-face allocations during rendering.
    private record Mesh(float[] vertices) {
        void drawOpaque(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int light, int overlay) {
            draw(pose, buffers, texture, 0xFFFFFF, 1, light, overlay,
                    RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        }
        void drawFloor(PoseStack pose,MultiBufferSource buffers,double time,boolean active,boolean overload,int light,int overlay){
            var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MOTE);
            var consumer=buffers.getBuffer(CrystalGlassLayer.ATLAS);
            for(int i=0;i<vertices.length;i+=32){
                double x=(vertices[i]+vertices[i+16])*.5,z=(vertices[i+2]+vertices[i+18])*.5;
                int color=floorPixelColor(x,z,time,active,overload);
                float alpha=color==BLUE?.10F:.23F;
                for(int j=i;j<i+32;j+=8)consumer.addVertex(pose.last(),vertices[j],vertices[j+1],vertices[j+2])
                        .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha)
                        .setUv(sprite.getU(vertices[j+3]),sprite.getV(vertices[j+4])).setOverlay(overlay)
                        .setLight(color==BLUE?light:LightTexture.FULL_BRIGHT).setNormal(pose.last(),vertices[j+5],vertices[j+6],vertices[j+7]);
            }
        }
        void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int color,
                  float alpha, int light, int overlay) {
            draw(pose,buffers,texture,color,alpha,light,overlay,CrystalGlassLayer.ATLAS);
        }
        private void draw(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int color,
                          float alpha, int light, int overlay, RenderType layer) {
            var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
            var consumer = buffers.getBuffer(layer);
            for (int i = 0; i < vertices.length; i += 8) {
                consumer.addVertex(pose.last(), vertices[i], vertices[i + 1], vertices[i + 2])
                        .setColor((color >> 16 & 255) / 255F, (color >> 8 & 255) / 255F,
                                (color & 255) / 255F, alpha)
                        .setUv(sprite.getU(vertices[i + 3]), sprite.getV(vertices[i + 4]))
                        .setOverlay(overlay).setLight(light)
                        .setNormal(pose.last(), vertices[i + 5], vertices[i + 6], vertices[i + 7]);
            }
        }
    }

    private static final class Builder {
        private final List<Float> data = new ArrayList<>();

        void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d) {
            int columns=Math.max(1,(int)Math.ceil(a.distanceTo(b)/.125-1e-5));
            int rows=Math.max(1,(int)Math.ceil(a.distanceTo(d)/.125-1e-5));
            for(int x=0;x<columns;x++)for(int y=0;y<rows;y++){
                Vec3 low=a.lerp(d,(double)y/rows),high=a.lerp(d,(double)(y+1)/rows);
                Vec3 lowEnd=b.lerp(c,(double)y/rows),highEnd=b.lerp(c,(double)(y+1)/rows);
                cell(low.lerp(lowEnd,(double)x/columns),low.lerp(lowEnd,(double)(x+1)/columns),
                        high.lerp(highEnd,(double)(x+1)/columns),high.lerp(highEnd,(double)x/columns));
            }
        }
        private void cell(Vec3 a,Vec3 b,Vec3 c,Vec3 d){
            Vec3 normal = b.subtract(a).cross(c.subtract(a)).normalize();
            // One tile per block, independent of part size; all individual faces fit a tile.
            float u = (float) Math.min(1, a.distanceTo(b));
            float v = (float) Math.min(1, a.distanceTo(d));
            vertex(a, 0, 0, normal); vertex(b, u, 0, normal);
            vertex(c, u, v, normal); vertex(d, 0, v, normal);
        }

        private void vertex(Vec3 p, float u, float v, Vec3 normal) {
            for (float value : new float[]{(float) p.x, (float) p.y, (float) p.z, u, v,
                    (float) normal.x, (float) normal.y, (float) normal.z}) data.add(value);
        }

        void box(float x0, float y0, float z0, float x1, float y1, float z1, int omit) {
            Vec3 a = new Vec3(x0,y0,z0), b = new Vec3(x1,y0,z0), c = new Vec3(x1,y1,z0), d = new Vec3(x0,y1,z0);
            Vec3 e = new Vec3(x0,y0,z1), f = new Vec3(x1,y0,z1), g = new Vec3(x1,y1,z1), h = new Vec3(x0,y1,z1);
            if ((omit & 1) == 0) quad(d,c,b,a);
            if ((omit & 2) == 0) quad(e,f,g,h);
            if ((omit & 4) == 0) quad(a,e,h,d);
            if ((omit & 8) == 0) quad(f,b,c,g);
            if ((omit & 16) == 0) quad(a,b,f,e);
            if ((omit & 32) == 0) quad(h,g,c,d);
        }

        Mesh finish() {
            float[] vertices = new float[data.size()];
            for (int i = 0; i < vertices.length; i++) vertices[i] = data.get(i);
            return new Mesh(vertices);
        }
    }

    private static Mesh box(float x0, float y0, float z0, float x1, float y1, float z1) {
        Builder b = new Builder(); b.box(x0,y0,z0,x1,y1,z1,0); return b.finish();
    }

    private static Vec3 polar(double latitude, double longitude) {
        return new Vec3(Math.cos(latitude) * Math.cos(longitude), Math.sin(latitude),
                Math.cos(latitude) * Math.sin(longitude));
    }

    private static boolean[][][] sphereCells(int size) {
        float step=2F/size;boolean[][][] cells=new boolean[size][size][size];
        double inner=Math.max(0,1-2.4/size);
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++){
            double a=(x+.5)*step-1,c=(y+.5)*step-1,d=(z+.5)*step-1,r=a*a+c*c+d*d;
            cells[x][y][z]=r<=1 && r>=inner*inner;
        }
        return cells;
    }

    private static Mesh sphere(int size) {
        Builder b = new Builder();
        float step=2F/size;boolean[][][] cells=sphereCells(size);
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)if(cells[x][y][z]){
            int omit=sphereOmit(cells,x,y,z);
            b.box(x*step-1,y*step-1,z*step-1,(x+1)*step-1,(y+1)*step-1,(z+1)*step-1,omit);
        }
        return b.finish();
    }
    private record ShellMeshes(Mesh intact,Mesh[] shells,Mesh[] cracks,boolean[][][][] masks) {}

    private static ShellMeshes shellMeshes(int size) {
        var intact=new Builder();Builder[] shells={new Builder(),new Builder(),new Builder()};
        Builder[] cracks={new Builder(),new Builder(),new Builder()};
        float step=2F/size;boolean[][][] cells=sphereCells(size);
        boolean[][][][] masks=new boolean[3][][][];
        for(int stage=0;stage<3;stage++)masks[stage]=crackMask(cells,stage,stage==0?null:masks[stage-1]);
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)if(cells[x][y][z]){
            int omit=sphereOmit(cells,x,y,z);
            float x0=x*step-1,y0=y*step-1,z0=z*step-1;
            float x1=x0+step,y1=y0+step,z1=z0+step;
            intact.box(x0,y0,z0,x1,y1,z1,omit);
            for(int stage=0;stage<3;stage++){
                boolean cracked=masks[stage][x][y][z];
                (cracked?cracks[stage]:shells[stage]).box(x0,y0,z0,x1,y1,z1,cracked?0:omit);
            }
        }
        return new ShellMeshes(intact.finish(),java.util.Arrays.stream(shells).map(Builder::finish).toArray(Mesh[]::new),
                java.util.Arrays.stream(cracks).map(Builder::finish).toArray(Mesh[]::new),masks);
    }

    private static boolean[][][] crackMask(boolean[][][] cells,int stage,boolean[][][] prior) {
        int size=cells.length,center=size/2;
        boolean[][][] mask=new boolean[size][size][size];
        if(prior!=null)for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)
            mask[x][y][z]=prior[x][y][z];
        int root=voxelIndex(center,size-1,center,size);
        if(!cells[center][size-1][center])throw new IllegalStateException("Crystal shell has no crown voxel");
        int[] crownDrop={-7,-6,-8,-7},middleDrop={0,-1,1,0},deepDrop={8,7,9,8};
        int[] branchOffset={3,-3,-4,4};
        for(int direction=0;direction<4;direction++){
            int crownTip=crackTip(cells,direction,center-crownDrop[direction],0);
            if(stage==0)traceCrack(cells,mask,root,crownTip,direction,0);
            else {
                int middleTip=crackTip(cells,direction,center-middleDrop[direction],0);
                int branchTip=crackTip(cells,direction,center-middleDrop[direction]+1,branchOffset[direction]);
                if(stage==1){
                    traceCrack(cells,mask,crownTip,middleTip,direction,0);
                    traceCrack(cells,mask,crownTip,branchTip,direction,branchOffset[direction]);
                } else {
                    traceCrack(cells,mask,middleTip,crackTip(cells,direction,center-deepDrop[direction],0),direction,0);
                    traceCrack(cells,mask,branchTip,crackTip(cells,direction,center-deepDrop[direction]+2,branchOffset[direction]),
                            direction,branchOffset[direction]);
                    if(direction%2==0)
                        traceCrack(cells,mask,middleTip,crackTip(cells,direction,center-deepDrop[direction]+3,-branchOffset[direction]),
                                direction,-branchOffset[direction]);
                }
            }
        }
        return mask;
    }

    private record CrackStep(int index,double cost) {}

    private static int crackTip(boolean[][][] cells,int direction,int targetY,int crossOffset) {
        int size=cells.length,center=size/2,best=-1;double bestScore=Double.POSITIVE_INFINITY;
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)if(cells[x][y][z]){
            int along=direction<2?x:z,cross=direction<2?z:x;
            int outward=direction%2==0?along:size-1-along;
            double score=Math.abs(y-targetY)*40+Math.abs(cross-center-crossOffset)*8-outward;
            if(score<bestScore){bestScore=score;best=voxelIndex(x,y,z,size);}
        }
        if(best<0)throw new IllegalStateException("Crystal crack has no side target");
        return best;
    }

    private static void traceCrack(boolean[][][] cells,boolean[][][] mask,int start,int target,
                                   int direction,int crossOffset) {
        int size=cells.length,center=size/2,count=size*size*size;
        double[] distance=new double[count];java.util.Arrays.fill(distance,Double.POSITIVE_INFINITY);
        int[] previous=new int[count];java.util.Arrays.fill(previous,-1);
        var queue=new java.util.PriorityQueue<CrackStep>(java.util.Comparator.comparingDouble(CrackStep::cost));
        distance[start]=0;queue.add(new CrackStep(start,0));
        while(!queue.isEmpty()){
            var step=queue.remove();int id=step.index();
            if(step.cost()>distance[id])continue;
            if(id==target)break;
            int x=id/(size*size),y=id/size%size,z=id%size;
            for(var d:NEIGHBORS){
                int xx=x+d[0],yy=y+d[1],zz=z+d[2];
                if(yy>y || !occupied(cells,xx,yy,zz))continue;
                int next=voxelIndex(xx,yy,zz,size);
                int cross=direction<2?zz:xx;
                double cost=distance[id]+1+Math.abs(cross-center-crossOffset)*.08;
                if(cost<distance[next]){
                    distance[next]=cost;previous[next]=id;queue.add(new CrackStep(next,cost));
                }
            }
        }
        if(!Double.isFinite(distance[target]))throw new IllegalStateException("Disconnected crystal shell");
        for(int id=target;id!=start;id=previous[id])mask[id/(size*size)][id/size%size][id%size]=true;
        mask[start/(size*size)][start/size%size][start%size]=true;
    }

    private static int voxelIndex(int x,int y,int z,int size) {
        return (x*size+y)*size+z;
    }

    private static int sphereOmit(boolean[][][] cells,int x,int y,int z) {
        return (occupied(cells,x,y,z-1)?1:0)|(occupied(cells,x,y,z+1)?2:0)
                |(occupied(cells,x-1,y,z)?4:0)|(occupied(cells,x+1,y,z)?8:0)
                |(occupied(cells,x,y-1,z)?16:0)|(occupied(cells,x,y+1,z)?32:0);
    }
    private static boolean occupied(boolean[][][] cells,int x,int y,int z){
        int n=cells.length;return x>=0 && y>=0 && z>=0 && x<n && y<n && z<n && cells[x][y][z];
    }

    private static Vec3 circle(double angle, double radius, double y) {
        return new Vec3(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
    }

    private static Mesh floor() {
        Builder b = new Builder();
        for(int x=-11;x<11;x++)for(int z=-11;z<11;z++)if(floorCell(x,z)){
            int omit=(floorCell(x,z-1)?1:0)|(floorCell(x,z+1)?2:0)|(floorCell(x-1,z)?4:0)|(floorCell(x+1,z)?8:0);
            b.box(x/8F,.9375F,z/8F,(x+1)/8F,1,(z+1)/8F,omit);
        }
        return b.finish();
    }
    private static boolean floorCell(int x,int z){return Math.hypot((x+.5)/8,(z+.5)/8)<=1.32;}

    private static Mesh chair() {
        Builder b = new Builder();
        b.box(-.4F,1F,-.3F,.4F,1.0625F,.5F,2);
        b.box(-.4F,1F,.5F,.4F,1.8125F,.625F,1);
        b.quad(new Vec3(-.4,1.8125,.5),new Vec3(.4,1.8125,.5),new Vec3(.4,1.0625,.5),new Vec3(-.4,1.0625,.5));
        var mesh=b.finish();
        for(int i=0;i<mesh.vertices.length;i+=32){
            for(int j=i;j<i+32;j+=8)mesh.vertices[j+2]+=Math.max(0,mesh.vertices[j+1]-1.0625F)*.2F;
            var v=mesh.vertices;
            var a=new Vec3(v[i],v[i+1],v[i+2]);
            var normal=new Vec3(v[i+8],v[i+9],v[i+10]).subtract(a)
                    .cross(new Vec3(v[i+16],v[i+17],v[i+18]).subtract(a)).normalize();
            for(int j=i;j<i+32;j+=8){v[j+5]=(float)normal.x;v[j+6]=(float)normal.y;v[j+7]=(float)normal.z;}
        }
        return mesh;
    }

    private static Mesh goldenSeat() {
        Builder b = new Builder();
        for (int x = -3; x < 3; x++) for (int z = -3; z < 3; z++) {
            boolean rim = x == -3 || x == 2 || z == -3 || z == 2;
            if (rim) b.box(x / 8F, 1.02F, z / 8F + .08F,
                    (x + 1) / 8F, 1.0825F, (z + 1) / 8F + .08F, 0);
        }
        b.box(-.33F, 1.13F, .51F, .33F, 1.72F, .72F, 0);
        b.box(-.37F, 1.7F, .48F, .37F, 1.78F, .76F, 0);
        b.box(.33F, 1.48F, .57F, .4F, 1.53F, .69F, 0);
        return b.finish();
    }


    private static Mesh panel() {
        Builder b = new Builder();
        b.box(-.68F,1.67F,-.96F,.68F,1.96F,-.9F,0);
        b.box(-.54F,1.42F,-.99F,.54F,1.64F,-.975F,0);
        return b.finish();
    }

    private static Mesh rim() {
        Builder b = new Builder();
        b.box(-.14F,-.14F,-.015F,.14F,-.12F,.015F,0);
        b.box(-.14F,.12F,-.015F,.14F,.14F,.015F,0);
        b.box(-.14F,-.12F,-.015F,-.12F,.12F,.015F,16|32);
        b.box(.12F,-.12F,-.015F,.14F,.12F,.015F,16|32);
        return b.finish();
    }

    private static Vec3 torus(double a, double b) {
        return circle(a, 1 + .012 * Math.cos(b), .012 * Math.sin(b));
    }

    private static Mesh ring() {
        Builder b = new Builder();
        for(int x=-17;x<17;x++)for(int z=-17;z<17;z++)if(ringCell(x,z)){
            int omit=(ringCell(x,z-1)?1:0)|(ringCell(x,z+1)?2:0)|(ringCell(x-1,z)?4:0)|(ringCell(x+1,z)?8:0);
            b.box(x/16F,-.015625F,z/16F,(x+1)/16F,.015625F,(z+1)/16F,omit);
        }
        return b.finish();
    }
    private static boolean ringCell(int x,int z){return Math.abs(Math.hypot((x+.5)/16,(z+.5)/16)-1)<.045;}

    /**
     * CPU-only verification of the actual cached meshes, with a texture-mapped PNG contact sheet.
     * Run against already compiled main classes and their runtime dependencies (no game bootstrap):
     * java -Djava.awt.headless=true -cp "MAIN_CLASSES;RUNTIME_CLASSPATH"
     * 'com.mcmagic.omnira.client.renderer.CruiseOrbRenderer$GeometryVerification' ASSET_ROOT OUTPUT_PNG
     * ASSET_ROOT is src/main/resources/assets/omnira. No build or client is invoked here.
     * The preview covers empty-orb geometry, not external baked item models or GPU transparency.
     */
    public static final class GeometryVerification {
        private record Face(float[] vertices, ResourceLocation texture, int tint) {}

        public static void main(String[] args) throws Exception {
            if (args.length != 2) throw new IllegalArgumentException("Expected ASSET_ROOT OUTPUT_PNG");
            var meshes = new java.util.TreeMap<String, Mesh>();
            for (var field : CruiseOrbRenderer.class.getDeclaredFields()) {
                if (field.getType() != Mesh.class) continue;
                field.setAccessible(true);
                meshes.put(field.getName(), (Mesh) field.get(null));
            }
            int quads = 0;
            for (var entry : meshes.entrySet()) {
                float[] v = entry.getValue().vertices;
                require(v.length > 0 && v.length % 32 == 0, entry.getKey() + " quad stride");
                for (float value : v) require(Float.isFinite(value), entry.getKey() + " finite vertices");
                for (int i = 0; i < v.length; i += 8) {
                    require(v[i+3] >= 0 && v[i+3] <= 1 && v[i+4] >= 0 && v[i+4] <= 1, "UV bounds");
                    near(v[i+5]*v[i+5]+v[i+6]*v[i+6]+v[i+7]*v[i+7], 1, "unit normal");
                }
                for (int i = 0; i < v.length; i += 32) {
                    Vec3 a = point(v,i), b = point(v,i+8), c = point(v,i+16), d = point(v,i+24);
                    Vec3 n = new Vec3(v[i+5],v[i+6],v[i+7]);
                    require(b.subtract(a).cross(c.subtract(a)).dot(n) > 1e-12, entry.getKey() + " triangle 1");
                    require(c.subtract(a).cross(d.subtract(a)).dot(n) > 1e-12, entry.getKey() + " triangle 2");
                    if (entry.getKey().equals("SPHERE")) require(Math.max(Math.abs(n.x),Math.max(Math.abs(n.y),Math.abs(n.z)))>.999, "voxel shell faces");
                }
                quads += v.length / 32;
            }
            for (String name : new String[]{"SPHERE", "FLOOR", "RING", "BUTTON", "RECESS", "SPARK"})
                closed(meshes.get(name), name);
            for (int axis = 0; axis < 3; axis++) {
                near(bound(SPHERE,axis,false), -1, "sphere minimum");
                near(bound(SPHERE,axis,true), 1, "sphere maximum");
            }
            near(bound(FLOOR,1,true)-bound(FLOOR,1,false), 1/16.0, "separator thickness");
            near(bound(BUTTON,0,true), CruiseOrbPanel.BUTTON_WIDTH, "button half width");
            near(bound(BUTTON,1,true), CruiseOrbPanel.BUTTON_HEIGHT, "button half height");
            for(int stage=0;stage<3;stage++){
                wholeCrackVoxels(SHELL_GEOMETRY.masks()[stage],CRACKED_SHELLS[stage],CRACK_VOXELS[stage]);
                if(stage==2)crackCoverage(CRACK_VOXELS[stage]);
                crackConnectivity(stage);
                if(stage>0)require(CRACK_VOXELS[stage].vertices.length>CRACK_VOXELS[stage-1].vertices.length,
                        "later damage stage must grow");
            }
            near(CruiseOrbPanel.RIDER_Y+(1.501-.75)*.9375-Math.sin(1.4137)*.125*.9375,1.0625,"seated thigh touches chair");
            var slotMatrix=CruiseOrbPanel.transform().scale(CruiseOrbPanel.SLOT_SCALE,CruiseOrbPanel.SLOT_SCALE*.7F/1.12F,CruiseOrbPanel.SLOT_SCALE);
            near(slotMatrix.transformDirection(new org.joml.Vector3f(1,0,0)).length(),
                    slotMatrix.transformDirection(new org.joml.Vector3f(0,1,0)).length(),"upgrade slots are square");
            require(floorPixelColor(.6,.6,0,false,false)==BLUE,"stopped floor not plain");
            require(SPHERE.vertices.length<200000,"shell mesh exceeds budget");
            require(Math.hypot(.55,.48-1.5)+.27 < 1.5, "lower spheres inside shell");
            require(.48+.27 < bound(FLOOR,1,false), "lower spheres below floor");
            for (int axis = 0; axis < 3; axis++) for (int sign : new int[]{-1,1}) {
                double a = sparkTravel(.2,-sign), b = sparkTravel(.3,-sign);
                var movement = axisMatrix(axis).transformDirection(new org.joml.Vector3f(0,(float)(b-a),0));
                double directed = axis == 0 ? movement.x : axis == 1 ? movement.y : -movement.z;
                require(directed*sign < 0, "exhaust travels opposite movement");
                require(Math.abs(sparkTravel(.999,sign))+.018 < 1.5, "spark bounds");
            }
            // JOML and Minecraft must transform the actual button coordinates identically.
            for (int yaw = 0; yaw < 360; yaw += 45) {
                Vec3 p = new Vec3(-.45,1.8,-.9).yRot((float)Math.toRadians(-yaw));
                var q = new org.joml.Matrix4f().rotateY((float)Math.toRadians(-yaw))
                        .transformPosition(new org.joml.Vector3f(-.45F,1.8F,-.9F));
                // Minecraft uses a quantized trig table, JOML uses floating-point trig.
                require(Math.abs(p.x-q.x)<.0002 && Math.abs(p.z-q.z)<.0002,"yaw transform agreement");
            }
            require(floorColor(false,0) != floorColor(true,0), "distinct floor modes");
            require(floorRotation(10,false,0) != floorRotation(11,false,0), "moving normal floor");
            require(floorRotation(10,true,0) != floorRotation(11,true,0), "moving overload floor");
            preview(java.nio.file.Path.of(args[0]), java.nio.file.Path.of(args[1]));
            System.out.println("PASS: " + meshes.size() + " reflected meshes / " + quads
                    + " quads; topology, UVs, bounds, yaw, spark directions and floor modes. PNG: " + args[1]);
        }

        private static void require(boolean condition, String label) {
            if (!condition) throw new AssertionError(label);
        }

        private static void near(double actual, double expected, String label) {
            require(Math.abs(actual-expected) < 1e-5, label + ": " + actual + " != " + expected);
        }

        private static void crackCoverage(Mesh mesh) {
            boolean[] sides = new boolean[6];
            boolean inner = false;
            for (int i = 0; i < mesh.vertices.length; i += 32) {
                float[] v = mesh.vertices;
                float x=(v[i]+v[i+16])/2,y=(v[i+1]+v[i+17])/2,z=(v[i+2]+v[i+18])/2;
                float nx=v[i+5],ny=v[i+6],nz=v[i+7];
                if(x*nx+y*ny+z*nz<0)inner=true;
                else sides[nx>.5F?0:nx<-.5F?1:ny>.5F?2:ny<-.5F?3:nz>.5F?4:5]=true;
            }
            require(inner, "crack voxels need inner faces too");
            for (boolean visible : sides) require(visible, "crack missing on a side of the sphere");
        }

        private static void wholeCrackVoxels(boolean[][][] mask,Mesh normal,Mesh cracked) {
            int size=mask.length,voxels=0,exteriorFaces=0;
            boolean[][][] cells=sphereCells(size);
            for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)if(mask[x][y][z]){
                require(cells[x][y][z],"crack voxel outside shell");
                voxels++;
                exteriorFaces+=6-Integer.bitCount(sphereOmit(cells,x,y,z));
            }
            require(cracked.vertices.length==voxels*6*32,"each cracked voxel needs all six faces");
            require(normal.vertices.length+exteriorFaces*32==SPHERE.vertices.length,
                    "normal shell overlaps or omits cracked voxels");
        }

        private static void crackConnectivity(int stage) {
            int size=25,root=-1,highest=-1,total=0;
            boolean[][][] cells=sphereCells(size);
            boolean[][][] mask=SHELL_GEOMETRY.masks()[stage];
            for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)
                if(cells[x][y][z] && mask[x][y][z]){
                    total++;
                    if(y>highest && Math.abs(x-size/2)<2 && Math.abs(z-size/2)<2){
                        highest=y;root=(x*size+y)*size+z;
                    }
                }
            require(root>=0,"cracks do not start at the crown");
            boolean[][][] seen=new boolean[size][size][size];
            var queue=new java.util.ArrayDeque<Integer>();queue.add(root);
            boolean[] arms=new boolean[4];int visited=0;
            while(!queue.isEmpty()){
                int cell=queue.removeFirst(),x=cell/(size*size),y=cell/size%size,z=cell%size;
                if(seen[x][y][z])continue;
                seen[x][y][z]=true;visited++;
                if(Math.abs(x-size/2)>3 || Math.abs(z-size/2)>3){
                    if(x>size/2+3)arms[0]=true;
                    if(x<size/2-3)arms[1]=true;
                    if(z>size/2+3)arms[2]=true;
                    if(z<size/2-3)arms[3]=true;
                }
                for(var d:NEIGHBORS){
                    int xx=x+d[0],yy=y+d[1],zz=z+d[2];
                    if(occupied(cells,xx,yy,zz) && !seen[xx][yy][zz]
                            && mask[xx][yy][zz])queue.add((xx*size+yy)*size+zz);
                }
            }
            for(boolean arm:arms)require(arm,"crown crack does not reach all four sides");
            require(visited==total,"detached crack voxel");
            if(stage>0)for(int x=0;x<size;x++)for(int y=0;y<size;y++)for(int z=0;z<size;z++)
                require(!SHELL_GEOMETRY.masks()[stage-1][x][y][z] || mask[x][y][z],"damage stage loses an old crack");
        }

        private static Vec3 point(float[] v, int i) { return new Vec3(v[i],v[i+1],v[i+2]); }

        private static double bound(Mesh mesh, int axis, boolean maximum) {
            double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
            for (int i = axis; i < mesh.vertices.length; i += 8)
                result = maximum ? Math.max(result,mesh.vertices[i]) : Math.min(result,mesh.vertices[i]);
            return result;
        }

        private static String key(float[] v, int i) {
            return Math.round(v[i]*100000) + "," + Math.round(v[i+1]*100000) + "," + Math.round(v[i+2]*100000);
        }

        private static void closed(Mesh mesh, String name) {
            var counts = new java.util.HashMap<String,Integer>();
            var directions = new java.util.HashMap<String,Integer>();
            for (int i = 0; i < mesh.vertices.length; i += 32) for (int j = 0; j < 4; j++) {
                String a = key(mesh.vertices,i+j*8), b = key(mesh.vertices,i+((j+1)%4)*8);
                int direction = a.compareTo(b) < 0 ? 1 : -1;
                String edge = direction == 1 ? a + "/" + b : b + "/" + a;
                counts.merge(edge,1,Integer::sum); directions.merge(edge,direction,Integer::sum);
            }
            for (String edge : counts.keySet())
                require(counts.get(edge)%2 == 0 && directions.get(edge) == 0, name + " closed oriented edge " + edge);
        }

        private static org.joml.Matrix4f axisMatrix(int axis) {
            var matrix = new org.joml.Matrix4f().translation(0,1.5F,0);
            if (axis == 0) matrix.rotateZ(-(float)Math.PI/2);
            if (axis == 2) matrix.rotateX(-(float)Math.PI/2);
            return matrix;
        }

        private static void add(List<Face> faces, Mesh mesh, org.joml.Matrix4f transform,
                                ResourceLocation texture, int color, float alpha) {
            for (int i = 0; i < mesh.vertices.length; i += 32) {
                float[] v = new float[20];
                for (int corner = 0; corner < 4; corner++) {
                    int source = i+corner*8, target = corner*5;
                    var p = transform.transformPosition(new org.joml.Vector3f(mesh.vertices[source],
                            mesh.vertices[source+1],mesh.vertices[source+2]));
                    v[target] = p.x; v[target+1] = p.y; v[target+2] = p.z;
                    v[target+3] = mesh.vertices[source+3]; v[target+4] = mesh.vertices[source+4];
                }
                faces.add(new Face(v,texture,((int)(alpha*255)<<24) | color));
            }
        }

        private static List<Face> scene(int mode, double time) {
            List<Face> faces = new ArrayList<>();
            var identity = new org.joml.Matrix4f();
            for(int i=0;i<FLOOR.vertices.length;i+=32){
                var v=FLOOR.vertices;int color=floorPixelColor((v[i]+v[i+16])*.5,(v[i+2]+v[i+18])*.5,time,mode==1||mode==2,mode==2);
                add(faces,new Mesh(java.util.Arrays.copyOfRange(v,i,i+32)),identity,MOTE,color,color==BLUE?.10F:.23F);
            }
            add(faces,CHAIR,identity,MOTE,BLUE,.16F);
            add(faces,PANEL,CruiseOrbPanel.transform(),MOTE,BLUE,.16F);
            for (int i = 0; i < 2; i++) {
                add(faces,BUTTON,CruiseOrbPanel.transform().translate(i == 0 ? -CruiseOrbPanel.BUTTON_X : CruiseOrbPanel.BUTTON_X,1.8F,-.9F),
                        MOTE,i == 0 ? 0x163873 : 0x701B2C,.55F);
                double phase = time*.025+i*Math.PI;
                add(faces,SMALL_SPHERE,new org.joml.Matrix4f().translation((float)Math.cos(phase)*.55F,.48F,
                        (float)Math.sin(phase)*.55F).scale(.27F),MOTE,CLEAR,SMALL_ALPHA);
                if(i==1)for(int mote=0;mote<6;mote++){
                    double a=time*.045+mote*Math.PI/3;
                    add(faces,SPARK,new org.joml.Matrix4f().translation((float)(Math.cos(phase)*.55+Math.cos(a)*.145),
                            (float)(.48+Math.sin(a*1.3+mote)*.12),(float)(Math.sin(phase)*.55+Math.sin(a)*.145)),MOTE,0xDDF7FF,.85F);
                }
                add(faces,RING,new org.joml.Matrix4f().translation(0,.08F+i*.10F,0).scale(.42F+i*.19F),
                        MOTE,mode == 2 ? 0xEF7885 : 0xA6E9FA,.55F);
            }
            for (int slot = 0; slot < 3; slot++) {
                var slotPose=CruiseOrbPanel.transform().translate((slot-1)*CruiseOrbPanel.SLOT_X,1.53F,-.95F)
                        .scale(CruiseOrbPanel.SLOT_SCALE,CruiseOrbPanel.SLOT_SCALE*.7F/1.12F,CruiseOrbPanel.SLOT_SCALE);
                add(faces,RECESS,slotPose,SHELL,0x538DA9,1);
                add(faces,RIM,new org.joml.Matrix4f(slotPose).translate(0,0,.06F),SHELL,BLUE,1);
            }
            if (mode == 1 || mode == 2) for (int axis = 0; axis < 3; axis++) {
                int sign = mode == 2 ? -1 : 1, color = mode == 2 ? 0xEF7885 : 0xA6E9FA;
                add(faces,RING,axisMatrix(axis).translate(0,-sign*.86F,0).scale(1.27F),MOTE,color,.65F);
                for (int spark = 0; spark < 4; spark++) {
                    double phase = sparkPhase(time,spark), a = spark*Math.PI/2;
                    add(faces,SPARK,axisMatrix(axis).translate((float)Math.cos(a)*1.27F,
                            (float)sparkTravel(phase,-sign),(float)Math.sin(a)*1.27F),MOTE,color,
                            (float)(.25+.55*Math.sin(phase*Math.PI)));
                }
            }
            var shellPose=new org.joml.Matrix4f().translation(0,1.5F,0).scale(1.5F);
            add(faces,mode>=3?CRACKED_SHELLS[mode-3]:SPHERE,shellPose,MOTE,CLEAR,SHELL_ALPHA);
            if (mode>=3) add(faces,CRACK_VOXELS[mode-3],shellPose,MOTE,0xFFFFFF,CRACK_ALPHA);
            return faces;
        }

        private static void preview(java.nio.file.Path assets, java.nio.file.Path output) throws Exception {
            int width = 360, height = 400;
            var image = new java.awt.image.BufferedImage(width*4,height*6,java.awt.image.BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            graphics.setColor(new java.awt.Color(0x26343A)); graphics.fillRect(0,0,image.getWidth(),image.getHeight());
            var textures = new java.util.HashMap<ResourceLocation,java.awt.image.BufferedImage>();
            for (ResourceLocation texture : new ResourceLocation[]{SHELL,MOTE})
                textures.put(texture,javax.imageio.ImageIO.read(assets.resolve("textures/"+texture.getPath()+".png").toFile()));
            float[] yaw = {30,90,180,0}, pitch = {20,0,15,90};
            String[] names = {"Oblique","Side","Rear","Top"}, modes = {"Stopped","Normal +XYZ","Overload -XYZ","Stage 1","Stage 2","Stage 3"};
            for (int mode = 0; mode < modes.length; mode++) for (int view = 0; view < 4; view++) {
                var matrix = new org.joml.Matrix4f().rotateX((float)Math.toRadians(pitch[view]))
                        .rotateY((float)Math.toRadians(yaw[view])).translate(0,-1.5F,0);
                List<Face> projected = new ArrayList<>();
                for (Face face : scene(mode,13)) {
                    float[] v = face.vertices.clone();
                    for (int i = 0; i < 20; i += 5) {
                        var p = matrix.transformPosition(new org.joml.Vector3f(v[i],v[i+1],v[i+2]));
                        v[i] = p.x; v[i+1] = p.y; v[i+2] = p.z;
                    }
                    double facing = (v[5]-v[0])*(v[11]-v[1])-(v[6]-v[1])*(v[10]-v[0]);
                    if (facing > 1e-8) projected.add(new Face(v,face.texture,face.tint));
                }
                projected.sort(java.util.Comparator.comparingDouble(f -> f.vertices[2]+f.vertices[7]+f.vertices[12]+f.vertices[17]));
                for (Face face : projected) raster(image,textures.get(face.texture),face,view*width,mode*height,width,height);
                graphics.setColor(java.awt.Color.WHITE);
                graphics.drawString(modes[mode]+" / "+names[view],view*width+12,mode*height+22);
            }
            graphics.dispose();
            var parent = output.toAbsolutePath().getParent();
            if (parent != null) java.nio.file.Files.createDirectories(parent);
            javax.imageio.ImageIO.write(image,"PNG",output.toFile());
        }

        private static void raster(java.awt.image.BufferedImage image, java.awt.image.BufferedImage texture,
                                   Face face, int left, int top, int width, int height) {
            float[] v = face.vertices.clone();
            int minX = left+width, minY = top+height, maxX = left, maxY = top;
            for (int i = 0; i < 20; i += 5) {
                v[i] = left+width*.5F+v[i]*96; v[i+1] = top+height*.53F-v[i+1]*96;
                minX = Math.min(minX,(int)Math.floor(v[i])); maxX = Math.max(maxX,(int)Math.ceil(v[i]));
                minY = Math.min(minY,(int)Math.floor(v[i+1])); maxY = Math.max(maxY,(int)Math.ceil(v[i+1]));
            }
            double[] uv = new double[2];
            for (int y = Math.max(top,minY); y < Math.min(top+height,maxY); y++)
                for (int x = Math.max(left,minX); x < Math.min(left+width,maxX); x++) {
                    if (!sample(v,0,5,10,x+.5,y+.5,uv) && !sample(v,0,10,15,x+.5,y+.5,uv)) continue;
                    int texel = texture.getRGB(Math.clamp((int)(uv[0]*texture.getWidth()),0,texture.getWidth()-1),
                            Math.clamp((int)(uv[1]*texture.getHeight()),0,texture.getHeight()-1));
                    double alpha = (texel>>>24)/255.0*(face.tint>>>24)/255.0;
                    int old = image.getRGB(x,y), result = 0xFF000000;
                    for (int shift = 0; shift <= 16; shift += 8) {
                        double channel = (texel>>shift&255)*(face.tint>>shift&255)/255.0;
                        result |= (int)Math.round(channel*alpha+(old>>shift&255)*(1-alpha)) << shift;
                    }
                    image.setRGB(x,y,result);
                }
        }

        private static boolean sample(float[] v, int a, int b, int c, double x, double y, double[] uv) {
            double area = (v[b+1]-v[c+1])*(v[a]-v[c])+(v[c]-v[b])*(v[a+1]-v[c+1]);
            if (Math.abs(area) < 1e-10) return false;
            double u = ((v[b+1]-v[c+1])*(x-v[c])+(v[c]-v[b])*(y-v[c+1]))/area;
            double w = ((v[c+1]-v[a+1])*(x-v[c])+(v[a]-v[c])*(y-v[c+1]))/area;
            double t = 1-u-w;
            if (u < -1e-8 || w < -1e-8 || t < -1e-8) return false;
            uv[0] = u*v[a+3]+w*v[b+3]+t*v[c+3]; uv[1] = u*v[a+4]+w*v[b+4]+t*v[c+4];
            return true;
        }
    }

    /** Uses the same native geometry; the main item JSON supplies ordinary display transforms. */
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        private net.minecraft.client.multiplayer.ClientLevel previewLevel;
        private net.minecraft.core.RegistryAccess previewRegistries;
        private final java.util.LinkedHashMap<net.minecraft.world.item.component.CustomData, CruiseOrbEntity> previews
                = new java.util.LinkedHashMap<>(4, .75F, true);
        private net.minecraft.world.item.component.CustomData lastData;
        private CruiseOrbEntity lastPreview;
        private int previewDepth;

        public ItemRenderer() {
            super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        }

        private CruiseOrbEntity preview(ItemStack stack) {
            var level = Minecraft.getInstance().level;
            var registries = level == null ? null : level.registryAccess();
            if (previewLevel != level || previewRegistries != registries) {
                previews.clear(); lastData = null; lastPreview = null;
                previewLevel = level; previewRegistries = registries;
            }
            if (level == null) return null;
            var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null) return null;
            // Identity fast path avoids hashing/copying NBT for the usual repeated draw.
            if (data == lastData) return lastPreview;
            var result = previews.get(data);
            if (result == null) {
                result = new CruiseOrbEntity(com.mcmagic.omnira.registry.ModEntityTypes.CRUISE_ORB.get(), level);
                result.restoreItem(stack);
                previews.put(data, result);
                if (previews.size() > 4) previews.remove(previews.keySet().iterator().next());
            }
            lastData = data; lastPreview = result;
            return result;
        }

        @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose,
                                           MultiBufferSource buffers, int light, int overlay) {
            var mc = Minecraft.getInstance();
            double time = mc.level == null ? 0 : mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(false);
            pose.pushPose();
            pose.translate(.5, 0, .5);
            pose.scale(1/3F, 1/3F, 1/3F);
            try {
                // A stored cruise-orb item may itself contain another cruise orb.
                CruiseOrbEntity preview = previewDepth == 0 ? preview(stack) : null;
                previewDepth++;
                try { renderOrb(preview, pose, buffers, time, 0, light, overlay); }
                finally { previewDepth--; }
            } finally { pose.popPose(); }
        }
    }
}
