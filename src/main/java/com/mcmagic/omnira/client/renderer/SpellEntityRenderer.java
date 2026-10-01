package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class SpellEntityRenderer extends EntityRenderer<SpellEntity> {
    private final EntityRendererProvider.Context context;
    private int color = 0xFFFFFF;
    private static final int[][] OUTLINE = {{-2,-4},{2,-4},{2,-3},{3,-3},{3,-2},{4,-2},{4,2},{3,2},
            {3,3},{2,3},{2,4},{-2,4},{-2,3},{-3,3},{-3,2},{-4,2},{-4,-2},{-3,-2},{-3,-3},{-2,-3}};

    public SpellEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.context = context;
        shadowRadius = 0;
    }

    @Override
    public void render(SpellEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        color = entity.ghost()?0x83CFFF:entity.spellColor();
        float age = entity.age(partialTick);
        float fade = entity.opacity(partialTick);
        boolean composite=entity.kind()==SpellEntity.Kind.REVERSE_FLOW||entity.kind()==SpellEntity.Kind.SPATIAL_BLADE;
        if(composite)age=entity.compositeAge(partialTick);
        VertexConsumer mesh = buffers.getBuffer(composite?CrystalGlassLayer.COLOR:RenderType.debugQuads());
        switch (entity.kind()) {
            case REVERSE_FLOW -> {
                float progress=(age%com.mcmagic.omnira.spell.entity.CompositeSpellMotion.CONTRACTION_TICKS)/com.mcmagic.omnira.spell.entity.CompositeSpellMotion.CONTRACTION_TICKS;
                float radius=Math.max(.02F,(1-progress)*entity.burstRadius());
                ball(mesh,pose.last().pose(),radius,.06F);
                for(int axis=0;axis<3;axis++){
                    pose.pushPose();
                    if(axis==1)pose.mulPose(new Quaternionf().rotationX((float)Math.PI/2));
                    if(axis==2)pose.mulPose(new Quaternionf().rotationZ((float)Math.PI/2));
                    ring(mesh,pose.last().pose(),radius,.04F,.65F);
                    for(int i=0;i<8;i++){
                        double angle=i*Math.PI/4+age*.08;
                        pose.pushPose();pose.translate(Math.cos(angle)*radius,0,Math.sin(angle)*radius);
                        color=i%2==0?0xDAFFE8:entity.spellColor();ball(mesh,pose.last().pose(),.045F,.85F);pose.popPose();
                    }
                    pose.popPose();
                }
            }
            case SPATIAL_BLADE -> {
                if(age>=entity.compositeDelay()){
                    blade(entity,age,mesh,pose.last().pose());
                }
            }
            case BARRIER, FACE_BARRIER -> {
                boolean panel = entity.kind() == SpellEntity.Kind.FACE_BARRIER;
                if (panel) {
                    var normal = entity.face().getNormal();
                    pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, normal.getX(), normal.getY(), normal.getZ()));
                    pose.translate(0, -.06, 0);
                }
                float height = panel ? .12F : 2;
                float radius = panel ? .5F : .48F;
                prism(mesh, pose.last().pose(), radius, 0, height, .16F * fade);
                prism(mesh, pose.last().pose(), radius + .004F, 0, .0625F, .8F * fade);
                prism(mesh, pose.last().pose(), radius + .004F, height - .0625F, height, .8F * fade);
                if (entity.barrierHits() > 0) {
                    // Persistent fracture bands show the eight-hit barrier durability.
                    float progress = entity.barrierHits() / 8F;
                    prism(mesh, pose.last().pose(), radius + .01F, 0, height * progress, .28F * fade);
                    for (int i = 1; i <= (int) (progress * 8); i++) {
                        float y = height * i / 11;
                        prism(mesh, pose.last().pose(), radius + .015F, y, y + .0625F, .9F * fade);
                    }
                }
            }
            case WARD -> {
                for (int i = 0; i < entity.charges(); i++) {
                    double angle = i * Math.PI / 2 + age * .018;
                    pose.pushPose();
                    pose.translate(Math.cos(angle) * .7, .4, Math.sin(angle) * .7);
                    ball(mesh, pose.last().pose(), .12F, .9F * fade);
                    ring(mesh, pose.last().pose(), .2F, .018F, .6F * fade);
                    pose.popPose();
                }
            }
            case ORB, PROJECTILE -> {
                if(entity.ghost())fade*=.5F;
                if(entity.knowledgeBlood())fade*=.55F;
                ball(mesh, pose.last().pose(), .14F, .95F * fade);
                pose.mulPose(new Quaternionf().rotationY((float)Math.floor(age * entity.speedMultiplier() / 8) * (float)Math.PI / 4));
                ring(mesh, pose.last().pose(), .28F, .02F, .8F * fade);
            }
            case FLYING_BLOCK -> {
                pose.pushPose();
                pose.translate(-.5, 0, -.5);
                context.getBlockRenderDispatcher().renderSingleBlock(entity.blockAppearance(), pose, buffers, light, OverlayTexture.NO_OVERLAY);
                pose.popPose();
                mesh = buffers.getBuffer(RenderType.debugQuads());
                prism(mesh, pose.last().pose(), .54F, .01F, .04F, .9F * fade);
            }
            case SELF_FLIGHT -> {
                pose.translate(0, .8, 0);
                ring(mesh, pose.last().pose(), .5F, .025F, .7F * fade);
                pose.translate(0, -.55, 0);
                ring(mesh, pose.last().pose(), .4F, .018F, .5F * fade);
            }
            case BURST -> {
                float progress = Math.min(1, age / entity.duration());
                float radius = Math.max(.01F, progress * entity.burstRadius());
                ball(mesh, pose.last().pose(), radius, (1 - progress) * .15F);
                ring(mesh, pose.last().pose(), radius, .045F, (1 - progress) * .85F);
                pose.pushPose();
                pose.mulPose(new Quaternionf().rotationX((float) Math.PI / 2));
                ring(mesh, pose.last().pose(), radius, .035F, (1 - progress) * .65F);
                pose.popPose();
                pose.mulPose(new Quaternionf().rotationZ((float) Math.PI / 2));
                ring(mesh, pose.last().pose(), radius, .035F, (1 - progress) * .65F);
            }
        }
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public boolean shouldRender(SpellEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        double extent=entity.kind()==SpellEntity.Kind.BURST||entity.kind()==SpellEntity.Kind.REVERSE_FLOW?entity.burstRadius():3*entity.power();
        if(entity.kind()==SpellEntity.Kind.SPATIAL_BLADE)extent=Math.max(extent,entity.rangeMultiplier()*1.2+com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.TRAIL_LENGTH);
        return entity.shouldRender(x, y, z) && frustum.isVisible(entity.getBoundingBox().inflate(extent));
    }

    private void blade(SpellEntity entity,float age,VertexConsumer mesh,Matrix4f pose){
        var direction=entity.getDeltaMovement();
        if(direction.lengthSqr()<1e-8)direction=entity.getViewVector(1);
        var basis=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.basis(direction);
        double scale=entity.rangeMultiplier(),half=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.THICKNESS/2;
        double trail=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.trailLength(age,entity.compositeDelay());
        for(int i=0;i<8;i++){
            double x=(i-4)*.25*scale,xx=x+.25*scale;
            double front=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.front(i)*scale;
            double back=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.back(i)*scale;
            color=entity.spellColor();
            bladeQuad(mesh,pose,basis,new Vec3(x,half,back),new Vec3(xx,half,back),new Vec3(xx,half,front),new Vec3(x,half,front),.82F);
            bladeQuad(mesh,pose,basis,new Vec3(x,-half,front),new Vec3(xx,-half,front),new Vec3(xx,-half,back),new Vec3(x,-half,back),.65F);
            color=0xE4F7FF;
            bladeQuad(mesh,pose,basis,new Vec3(x,-half,front),new Vec3(x,half,front),new Vec3(xx,half,front),new Vec3(xx,-half,front),.95F);
            color=entity.spellColor();
            bladeQuad(mesh,pose,basis,new Vec3(x,-half,back),new Vec3(xx,-half,back),new Vec3(xx,half,back),new Vec3(x,half,back),.5F);
            // Only exposed interval differences get end caps; neighbours share no buried faces.
            for(int side:new int[]{-1,1}){
                int neighbour=i+side;double edge=side<0?x:xx;
                if(neighbour<0||neighbour>=8)bladeSide(mesh,pose,basis,edge,back,front,half);
                else{
                    double nf=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.front(neighbour)*scale;
                    double nb=com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.back(neighbour)*scale;
                    if(front>nf)bladeSide(mesh,pose,basis,edge,Math.max(back,nf),front,half);
                    if(back<nb)bladeSide(mesh,pose,basis,edge,back,Math.min(front,nb),half);
                }
            }
            if(trail>0)for(int j=0;j<6;j++){
                double a=back-trail*j/6,b=back-trail*(j+1)/6;
                float start=.24F*(1-j/6F)*(1-j/6F),end=.24F*(1-(j+1)/6F)*(1-(j+1)/6F);
                vertex(mesh,pose,basis.point(x,0,a),start);vertex(mesh,pose,basis.point(xx,0,a),start);
                vertex(mesh,pose,basis.point(xx,0,b),end);vertex(mesh,pose,basis.point(x,0,b),end);
            }
        }
    }
    private void bladeSide(VertexConsumer mesh,Matrix4f pose,com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.Basis basis,double x,double back,double front,double half){
        if(front<=back)return;
        bladeQuad(mesh,pose,basis,new Vec3(x,-half,back),new Vec3(x,half,back),new Vec3(x,half,front),new Vec3(x,-half,front),.65F);
    }
    private void bladeQuad(VertexConsumer mesh,Matrix4f pose,com.mcmagic.omnira.spell.visual.SpatialBladeGeometry.Basis basis,Vec3 a,Vec3 b,Vec3 c,Vec3 d,float alpha){
        quad(mesh,pose,basis.point(a.x,a.y,a.z),basis.point(b.x,b.y,b.z),basis.point(c.x,c.y,c.z),basis.point(d.x,d.y,d.z),alpha);
    }

    private void prism(VertexConsumer mesh, Matrix4f pose, float radius, float bottom, float top, float alpha) {
        for (int i = 0; i < OUTLINE.length; i++) {
            int[] a = OUTLINE[i], b = OUTLINE[(i+1) % OUTLINE.length];
            float x1 = a[0]*radius/4, z1 = a[1]*radius/4;
            float x2 = b[0]*radius/4, z2 = b[1]*radius/4;
            quad(mesh, pose, new Vec3(x1,bottom,z1), new Vec3(x2,bottom,z2), new Vec3(x2,top,z2), new Vec3(x1,top,z1), alpha);
        }
    }

    private void ring(VertexConsumer mesh, Matrix4f pose, float radius, float width, float alpha) {
        float unit = radius / 4;
        for (int x = -4; x < 4; x++) for (int z = -4; z < 4; z++) {
            double distance = (x+.5)*(x+.5) + (z+.5)*(z+.5);
            if (distance < 9 || distance > 16) continue;
            quad(mesh, pose, new Vec3(x*unit,0,z*unit), new Vec3((x+1)*unit,0,z*unit),
                    new Vec3((x+1)*unit,0,(z+1)*unit), new Vec3(x*unit,0,(z+1)*unit), alpha);
        }
    }

    private void ball(VertexConsumer mesh, Matrix4f pose, float radius, float alpha) {
        double unit = radius / 2;
        // An exposed-face voxel shell replaces latitude/longitude tessellation.
        for (int x=-2; x<2; x++) for (int y=-2; y<2; y++) for (int z=-2; z<2; z++) {
            if (!voxel(x,y,z)) continue;
            for (var direction : net.minecraft.core.Direction.values()) {
                if (voxel(x+direction.getStepX(),y+direction.getStepY(),z+direction.getStepZ())) continue;
                double a=x*unit,b=y*unit,c=z*unit, d=a+unit,e=b+unit,f=c+unit;
                switch (direction) {
                    case UP -> quad(mesh,pose,new Vec3(a,e,c),new Vec3(d,e,c),new Vec3(d,e,f),new Vec3(a,e,f),alpha);
                    case DOWN -> quad(mesh,pose,new Vec3(a,b,c),new Vec3(a,b,f),new Vec3(d,b,f),new Vec3(d,b,c),alpha);
                    case EAST -> quad(mesh,pose,new Vec3(d,b,c),new Vec3(d,b,f),new Vec3(d,e,f),new Vec3(d,e,c),alpha);
                    case WEST -> quad(mesh,pose,new Vec3(a,b,c),new Vec3(a,e,c),new Vec3(a,e,f),new Vec3(a,b,f),alpha);
                    case NORTH -> quad(mesh,pose,new Vec3(a,b,c),new Vec3(d,b,c),new Vec3(d,e,c),new Vec3(a,e,c),alpha);
                    case SOUTH -> quad(mesh,pose,new Vec3(a,b,f),new Vec3(a,e,f),new Vec3(d,e,f),new Vec3(d,b,f),alpha);
                }
            }
        }
    }

    private static boolean voxel(int x, int y, int z) {
        return (x+.5)*(x+.5)+(y+.5)*(y+.5)+(z+.5)*(z+.5) <= 3;
    }

    private void quad(VertexConsumer mesh, Matrix4f pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, float alpha) {
        vertex(mesh, pose, a, alpha); vertex(mesh, pose, b, alpha); vertex(mesh, pose, c, alpha); vertex(mesh, pose, d, alpha);
    }

    private void vertex(VertexConsumer mesh, Matrix4f pose, Vec3 point, float alpha) {
        mesh.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
                .setColor(((color >> 16)&255)/255F, ((color >> 8)&255)/255F, (color&255)/255F, alpha);
    }

    @Override public ResourceLocation getTextureLocation(SpellEntity entity) { return TextureAtlas.LOCATION_BLOCKS; }
}
