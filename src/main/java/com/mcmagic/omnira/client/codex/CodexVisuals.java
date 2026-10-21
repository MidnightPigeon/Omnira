package com.mcmagic.omnira.client.codex;

import com.mcmagic.omnira.client.PulseClient;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.codex.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.io.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;

public final class CodexVisuals {
    private static final Map<String,List<CodexGeometry.Face>> MODELS=new HashMap<>();
    private static final ResourceLocation WHITE=ResourceLocation.parse("omnira:block/myriad_codex_solid");
    private static final RenderType SOLID=RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS);
    private static final CodexDrawContext.Pool DRAWS=new CodexDrawContext.Pool();
    private static CodexDrawContext draw(MultiBufferSource buffers,double time,boolean busy){
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(WHITE);
        return DRAWS.borrow(buffers,sprite.getU(0),sprite.getU(1),sprite.getV(0),sprite.getV(1),time,busy);
    }
    public static void reload(){
        MODELS.clear();
        // Populate the bounded mesh cache during resource loading, before any world draw.
        for(int i=0;i<33;i++)CodexGeometry.world(i/8F);
    }
    private static List<CodexGeometry.Face> geometry(String name){
        return MODELS.computeIfAbsent(name,n->{
            try(var stream=Minecraft.getInstance().getResourceManager().open(ResourceLocation.parse("omnira:models/"+n+".json"));
                var reader=new InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)){
                return CodexGeometry.model(reader);
            }catch(IOException e){throw new IllegalStateException("Missing codex model "+n,e);}
        });
    }
    public static void codex(Direction facing,double time,boolean busy,boolean home,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        codex(facing,time,busy,home,pose,buffers,light,overlay,0);
    }
    public static void codex(Direction facing,double time,boolean busy,boolean home,PoseStack pose,MultiBufferSource buffers,int light,int overlay,float shaftAngle){
        try(var draw=draw(buffers,time,busy)){
            pose.pushPose();pose.translate(.5,0,.5);pose.mulPose(Axis.YP.rotationDegrees(180-facing.toYRot()));pose.translate(-.5,0,-.5);
            model("block/myriad_codex",1,pose,draw,light,overlay);
            // Reuse the native crystal socket and its real facets/UVs, not an opaque imitation.
            pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees(180));pose.mulPose(Axis.ZP.rotationDegrees(shaftAngle));pose.translate(-.5,-.5,-.5);
            var mc=Minecraft.getInstance();
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(com.mcmagic.omnira.client.renderer.ManaEngineRenderer.model("shaft")),
                    net.minecraft.world.item.ItemStack.EMPTY,light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
            pose.popPose();
            pose.pushPose();pose.translate(.5,CodexGeometry.bookY(time,busy),.5);pose.mulPose(Axis.XP.rotationDegrees((float)CodexGeometry.bookPitch()));pose.mulPose(Axis.ZP.rotationDegrees((float)CodexGeometry.bookTilt(time,busy)));pose.translate(-.5,0,-.5);
            model("item/myriad_codex_book",1,pose,draw,light,overlay);
            pose.pushPose();pose.translate(.5,.16,.5);pose.scale(.11F,.11F,.11F);pose.translate(-2.5,0,-2.5);
            var world=CodexGeometry.world(CodexGeometry.evolution(time,busy));
            for(int pass=0;pass<2;pass++){
                VertexConsumer out=null;
                for(var face:world.faces())if((face.color()>>>24<255)==(pass==1)){
                    if(out==null)out=buffers.getBuffer(pass==1?CrystalGlassLayer.ATLAS:SOLID);
                    face(face.geometry(),face.color(),pose,draw,out,light,overlay);
                }
            }
            pose.popPose();pose.popPose();
            if(home){pose.pushPose();pose.translate(0,CodexGeometry.homeHeartY()-.5,0);pose.translate(.5,.5,.5);pose.scale(.42F,.42F,.42F);pose.translate(-.5,-.5,-.5);heart(time,0,pose,draw,overlay);pose.popPose();}
            pose.popPose();
        }
    }
    public static void input(Direction rear,double time,PoseStack pose,MultiBufferSource buffers){
        input(rear,time,pose,buffers,com.mcmagic.omnira.pulse.PulseOutput.PULSE);
    }
    public static void input(Direction rear,double time,PoseStack pose,MultiBufferSource buffers,int mode){
        PulseClient.shaft(pose,buffers,rear,time,false,mode);
    }
    public static void heart(double time,int fade,PoseStack pose,MultiBufferSource buffers,int overlay){
        if(fade>=10)return;
        try(var draw=draw(buffers,time,false)){heart(time,fade,pose,draw,overlay);}
    }
    private static void heart(double time,int fade,PoseStack pose,CodexDrawContext draw,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.YP.rotationDegrees((float)(time*.7%360)));
        float pulse=1+(float)Math.sin(time*.06)*.025F;pose.scale(pulse,pulse,pulse);pose.translate(-.5,-.5,-.5);
        float alpha=CodexGeometry.opacity(fade);
        var out=draw.buffers().getBuffer(alpha<1?CrystalGlassLayer.ATLAS:SOLID);
        for(var face:CodexGeometry.heart()){
            int color=draw.palette()[face.tint()];color=(color&0xFFFFFF)|Math.round((color>>>24)*alpha)<<24;
            face(face,color,pose,draw,out,LightTexture.FULL_BRIGHT,overlay);
        }pose.popPose();
    }
    private static void model(String name,float alpha,PoseStack pose,CodexDrawContext draw,int light,int overlay){
        var faces=geometry(name);
        // Immediate GUI buffers can invalidate a consumer on layer changes; finish each pass first.
        for(int pass=0;pass<2;pass++){
            VertexConsumer out=null;
            for(var f:faces){
                int color=draw.palette()[f.tint()];
                color=(color&0xFFFFFF)|Math.round((color>>>24)*alpha)<<24;
                if((color>>>24<255)!=(pass==1))continue;
                if(out==null)out=draw.buffers().getBuffer(pass==1?CrystalGlassLayer.ATLAS:SOLID);
                face(f,color,pose,draw,out,light,overlay);
            }
        }
    }
    private static void face(CodexGeometry.Face f,int color,PoseStack pose,CodexDrawContext draw,VertexConsumer out,int light,int overlay){
        var p=f.vertices();float shade=f.shade();
        for(int i=0;i<4;i++)out.addVertex(pose.last(),p[i][0],p[i][1],p[i][2])
                .setColor((int)((color>>>16&255)*shade),(int)((color>>>8&255)*shade),(int)((color&255)*shade),color>>>24)
                .setUv(i>=2?draw.u1():draw.u0(),i==1||i==2?draw.v0():draw.v1()).setOverlay(overlay).setLight(light).setNormal(pose.last(),f.nx(),f.ny(),f.nz());
    }
    public static MyriadCodexEntity origin(HeartLink link){
        var level=Minecraft.getInstance().level;
        var codex=link!=null&&level!=null&&level.dimension().equals(link.dimension())&&level.hasChunkAt(link.origin())?MyriadCodexEntity.from(level.getBlockEntity(link.origin())):null;
        return codex!=null&&codex.identity.equals(link.identity())&&codex.lease.equals(link.lease())?codex:null;
    }
    public static Vec3 target(MyriadCodexEntity codex,float partial){
        var level=codex.getLevel();if(level==null)return null;
        if(codex.heartPos!=null)return Vec3.atCenterOf(codex.heartPos);
        if(codex.holder==null)return codex.potential>=8192&&!codex.busy()?Vec3.atLowerCornerOf(codex.getBlockPos()).add(.5,CodexGeometry.homeHeartY(),.5):null;
        var player=level.getPlayerByUUID(codex.holder);
        var link=player==null?null:HeartLink.of(player.getMainHandItem());
        return player!=null&&link!=null&&link.dimension().equals(level.dimension())&&link.origin().equals(codex.getBlockPos())&&link.identity().equals(codex.identity)&&link.lease().equals(codex.lease)
                ?player.getPosition(partial).add(0,player.getBbHeight()*.65,0):null;
    }
    /** Bounded, six-sided physical dots share the sorted color buffer; no particle entities. */
    public static void dottedLink(Vec3 a,Vec3 b,double time,int color,PoseStack pose,MultiBufferSource buffers){
        double length=a.distanceTo(b);if(length<.001)return;
        int count=CodexGeometry.linkSamples(length);var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
        double size=Math.min(.045,length/count*.35);
        for(int i=0;i<count;i++){
            double f=CodexGeometry.linkFraction(i,count),x=a.x+(b.x-a.x)*f-size/2,y=a.y+(b.y-a.y)*f-size/2,z=a.z+(b.z-a.z)*f-size/2;
            int tint=CodexGeometry.linkAlpha(color>>>24,time,i)<<24|(color&0xFFFFFF);
            for(var face:CodexGeometry.particle())for(var p:face.vertices())vertex(x+p[0]*size,y+p[1]*size,z+p[2]*size,tint,pose,out);
        }
    }
    private static void vertex(double x,double y,double z,int color,PoseStack pose,VertexConsumer out){
        out.addVertex(pose.last().pose(),(float)x,(float)y,(float)z).setColor(color);
    }
    public static void sphere(int diameter,int fade,PoseStack pose,MultiBufferSource buffers){
        if(fade>=10)return;
        var out=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(var face:CodexGeometry.range(diameter)){
            int alpha=Math.round((face.ring()?CodexGeometry.RANGE_RING_ALPHA:CodexGeometry.RANGE_SHELL_ALPHA)*CodexGeometry.opacity(fade));
            int color=alpha<<24|0xA7E4DE;
            for(var p:face.geometry().vertices())vertex(p[0],p[1],p[2],color,pose,out);
        }
    }
    private CodexVisuals(){}
}
