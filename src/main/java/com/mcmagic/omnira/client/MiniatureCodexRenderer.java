package com.mcmagic.omnira.client;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.myriad.MiniatureWorldBlockEntity.Stage;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** Uses the already registered white crystal layer and its deferred world-glass flush. */
final class MiniatureCodexRenderer {
    private static final ResourceLocation WHITE = ResourceLocation.parse("omnira:textures/entity/solid_white.png");
    private static final int[][] FACES = {{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final float[][] NORMALS = {{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    static boolean contains(Stage stage) { return stage.ordinal() >= Stage.CODEX_WORLD.ordinal() && stage.ordinal() <= Stage.CODEX_COMPLETE.ordinal(); }
    static void draw(Stage previous, Stage next, float progress, double time, PoseStack pose,
                     MultiBufferSource buffers, int light, int overlay) {
        float target = next.ordinal() - Stage.CODEX_WORLD.ordinal();
        float source = contains(previous) ? previous.ordinal() - Stage.CODEX_WORLD.ordinal() : 0;
        float e = source + (target - source) * progress;
        float arrival = contains(previous) ? 1 : MiniatureWorldMotion.smooth(progress);
        float finish=next==Stage.CODEX_COMPLETE?MiniatureWorldMotion.smooth((progress-.90F)/.10F):0;
        if(finish>=1)return;
        pose.pushPose();
        pose.translate(2.5,1.6+finish*.9,2.5);
        pose.scale(1-finish,1-finish,1-finish);
        pose.translate(-2.5,-1.6,-2.5);
        var boxes = MiniatureCodexScene.boxes(e, time);
        for (var b : boxes) {
            float x = b.x(), y = .45F + (b.y() - .45F) * arrival, z = b.z();
            float w = b.width() / 2, h = b.height() * arrival / 2, d = b.depth() / 2;
            if (h < .0001F) continue;
            float[][] vertices = {{x-w,y-h,z-d},{x+w,y-h,z-d},{x+w,y+h,z-d},{x-w,y+h,z-d},
                    {x-w,y-h,z+d},{x+w,y-h,z+d},{x+w,y+h,z+d},{x-w,y+h,z+d}};
            var out = buffers.getBuffer(b.glass() ? CrystalGlassLayer.AMBER : RenderType.entityCutoutNoCull(WHITE));
            for (int face = 0; face < 6; face++) {
                float shade = face == 4 ? 1 : face == 5 ? .65F : face < 2 ? .85F : .75F;
                var n = NORMALS[face];
                var spans = face == 2 || face == 3 ? MiniatureCodexScene.sideSpans(b, boxes, face == 2 ? -1 : 1)
                        : java.util.List.of(new MiniatureCodexScene.Span(b.y()-b.height()/2, b.y()+b.height()/2));
                for (var span : spans) for (int i = 0; i < 4; i++) {
                    var v = vertices[FACES[face][i]];
                    float vertexY = face == 2 || face == 3 ? .45F + ((v[1] > y ? span.top() : span.bottom()) - .45F) * arrival : v[1];
                    out.addVertex(pose.last(), v[0], vertexY, v[2])
                            .setColor((int)((b.color() >>> 16 & 255) * shade), (int)((b.color() >>> 8 & 255) * shade),
                                    (int)((b.color() & 255) * shade), b.color() >>> 24)
                            .setUv(i >= 2 ? 1 : 0, i == 1 || i == 2 ? 0 : 1)
                            .setOverlay(overlay).setLight(light).setNormal(pose.last(), n[0], n[1], n[2]);
                }
            }
        }
        pose.popPose();
    }
    private MiniatureCodexRenderer() {}
}
