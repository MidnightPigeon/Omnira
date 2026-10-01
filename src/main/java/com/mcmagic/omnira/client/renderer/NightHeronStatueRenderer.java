package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.block.NightHeronStatueBlock;
import com.mcmagic.omnira.block.entity.NightHeronStatueBlockEntity;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.Vec3;

public final class NightHeronStatueRenderer implements BlockEntityRenderer<NightHeronStatueBlockEntity> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/night_heron_feathers.png");
    private static final ResourceLocation WHITE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/solid_white.png");
    public NightHeronStatueRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(NightHeronStatueBlockEntity statue,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);
        pose.mulPose(Axis.YP.rotationDegrees(NightHeronStatueBlock.modelYaw(statue.getBlockState().getValue(NightHeronStatueBlock.FACING))));
        draw(pose,buffers,light,overlay,statue.color(),statue.getBlockState().getValue(NightHeronStatueBlock.STANDING),statue.getLevel().getGameTime()+partial,statue.stretch(partial));
        pose.popPose();
    }
    @Override public boolean shouldRenderOffScreen(NightHeronStatueBlockEntity statue){return true;}
    public static void draw(PoseStack pose,MultiBufferSource buffers,int light,int overlay,int color,boolean standing,double time,float stretch) {
        var parts=NightHeronGeometry.parts(standing,time,stretch,color==0);
        // Finish each material pass before switching buffers. Eyes never inherit crystal alpha.
        for(boolean eyes:new boolean[]{false,true}) {
            boolean glass=color==2 && !eyes;
            var consumer=buffers.getBuffer(glass?CrystalGlassLayer.HERON:RenderType.entityCutoutNoCull(eyes?WHITE:TEXTURE));
            for(var part:parts) {
                int m=part.material();
                if(color==0 && m==NightHeronGeometry.FACE)continue;
                if((m==NightHeronGeometry.EYE || m==NightHeronGeometry.PUPIL)!=eyes)continue;
                int tile=materialTile(m,color),tint=materialTint(m,color);
                pose.pushPose();pose.translate(part.x()/16,part.y()/16,part.z()/16);
                pose.mulPose(Axis.ZP.rotationDegrees((float)part.angle()));
                part(pose,consumer,part,tile,tint,glass?110:255,eyes?LightTexture.FULL_BRIGHT:light,overlay);
                pose.popPose();
            }
        }
        if(color==0) {
            var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
            for(var part:parts)if(part.material()==NightHeronGeometry.FACE) {
                pose.pushPose();pose.translate(part.x()/16,part.y()/16,part.z()/16);
                pose.mulPose(Axis.ZP.rotationDegrees((float)part.angle()));
                part(pose,consumer,part,0,0xFFFFFF,255,light,overlay);pose.popPose();
            }
        }
    }
    private static int materialTile(int material,int color) {
        if(material>=NightHeronGeometry.LEGS && material!=NightHeronGeometry.CAP && material!=NightHeronGeometry.NECK && material!=NightHeronGeometry.FACE && material!=NightHeronGeometry.NECK_BACK)return 0;
        return color==2?3:color==1?2:material==NightHeronGeometry.WING?1:0;
    }
    private static int materialTint(int material,int color) {
        return switch(material) {
            case NightHeronGeometry.CAP, NightHeronGeometry.NECK_BACK -> color==0?0x344C6A:0xFFFFFF;
            case NightHeronGeometry.LEGS -> color==2?0xC7E7FA:0xCBBB72;
            case NightHeronGeometry.BILL -> color==2?0x96B9D4:0x303B4A;
            case NightHeronGeometry.EYE -> 0xED3840;
            case NightHeronGeometry.PUPIL -> 0x261720;
            default -> 0xFFFFFF;
        };
    }
    private static Vec3 vector(NightHeronGeometry.Point point){return new Vec3(point.x()/16,point.y()/16,point.z()/16);}
    private static void part(PoseStack pose,VertexConsumer c,NightHeronGeometry.Part part,int tile,int tint,int alpha,int light,int overlay) {
        for(var face:NightHeronGeometry.visibleFaces(part,alpha<255)) {
            Vec3[] p={vector(face.a()),vector(face.b()),vector(face.c()),vector(face.d())};
            Vec3 a=p[0],b=p[1],d=p[3],normal=b.subtract(a).cross(d.subtract(a)).normalize();
            for(int i=0;i<4;i++) {
                Vec3 point=p[i];
                boolean end=Math.abs(normal.x)>Math.abs(normal.y) && Math.abs(normal.x)>Math.abs(normal.z);
                boolean top=!end && Math.abs(normal.y)>Math.abs(normal.z);
                // One atlas tile spans 12 model pixels for every part, including the head.
                double horizontal=(end?point.z:point.x)*16/12;
                double vertical=(top?point.z:point.y)*16/12;
                float u=(float)(tile%2*.5+.02+(horizontal+.5)*.46),v=(float)(tile/2*.5+.02+(.5-vertical)*.46);
                c.addVertex(pose.last(),(float)point.x,(float)point.y,(float)point.z).setColor(tint>>16&255,tint>>8&255,tint&255,alpha)
                        .setUv(u,v).setOverlay(overlay).setLight(light).setNormal(pose.last(),(float)normal.x,(float)normal.y,(float)normal.z);
            }
        }
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(ModBlocks.NIGHT_HERON_STATUE.get().defaultBlockState(),pose,buffers,light,overlay);
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(180));draw(pose,buffers,light,overlay,0,true,0,0);pose.popPose();
        }
    }
}
