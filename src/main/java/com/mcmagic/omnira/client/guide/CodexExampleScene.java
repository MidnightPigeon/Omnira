package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.codex.edit.EditGeometry;
import com.mcmagic.omnira.codex.edit.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;

/** Detached, cached pixel diagrams. Radius 15 is illustrative, not a new machine power tier. */
public final class CodexExampleScene {
    public static final int RADIUS=15;
    private static final int[][] DIRECTIONS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static final int[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final ResourceLocation WHITE=ResourceLocation.parse("omnira:textures/entity/solid_white.png");
    private record Face(int x,int y,int z,int side,int color,float distance) {}
    private static final List<List<Face>> MODELS=List.of(build(0),build(1),build(2),build(3),build(4));
    public static boolean supports(String id){return id.equals("codex_matter")||id.equals("codex_form")||id.equals("codex_ecology");}
    private static int color(int model,int x,int y,int z){
        if(x*x+y*y+z*z>RADIUS*RADIUS)return 0;
        if(model>=1&&model<=3){
            var op=model==1?Operation.SHAPE:model==2?Operation.INSCRIBED:Operation.PLANE;
            return EditGeometry.contains(op,RADIUS,Direction.Axis.Y,true,x,y,z)?0xFFBB9CE1:0;
        }
        if(model==4){
            // A fixed cutaway exposes ground, trees and treasure without hiding the far shell.
            if(x>0&&z<0&&y>-6)return 0;
            if(EditGeometry.ecologyShell(RADIUS,x,y,z))return 0x688DDDE7;
        }
        if(y<=-6)return y==-6?0xFF78A35D:0xFF8C8170;
        if(model==0)return y<=-1&&x>=-5&&x<=5&&Math.abs(z)<=3?0xFF919DA3:0;
        if(x==-3&&z==3&&y<=0)return 0xFF81624C;
        if(Math.abs(x+3)<=3&&Math.abs(z-3)<=3&&y>=0&&y<=3&&Math.abs(x+3)+Math.abs(z-3)+Math.abs(y-1)<6)return 0xFF67944E;
        return 0;
    }
    private static List<Face> build(int model){
        var faces=new ArrayList<Face>();
        for(int x=-RADIUS;x<=RADIUS;x++)for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++){
            int c=color(model,x,y,z);if(c==0)continue;
            for(int side=0;side<6;side++){
                var d=DIRECTIONS[side];int adjacent=color(model,x+d[0],y+d[1],z+d[2]);
                if(adjacent!=0&&(adjacent>>>24==255||c>>>24<255))continue;
                faces.add(new Face(x,y,z,side,c,(float)Math.sqrt(x*x+y*y+z*z)));
            }
        }
        return List.copyOf(faces);
    }
    public static void render(GuiGraphics g,int x,int y,int ticks,String scene,float yaw,float pitch){
        var mc=Minecraft.getInstance();double time=net.minecraft.Util.getMillis()/50.0;
        int mode=scene.equals("codex_matter")?0:scene.equals("codex_ecology")?4:1+(int)(time/180)%3;
        double phase=time%180;float radius=(float)Math.clamp((phase-20)/100,0,1)*RADIUS;
        var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();
        g.flush();com.mcmagic.omnira.client.screen.UiScissor.enable(g,x,y,x+132,y+132);RenderSystem.enableDepthTest();pose.pushPose();
        pose.translate(x+66,y+60,180);pose.scale(2.1F,-2.1F,2.1F);
        pose.mulPose(Axis.XP.rotationDegrees(24+pitch));pose.mulPose(Axis.YP.rotationDegrees(-35+yaw+(float)Math.sin(time*.008)*12));
        for(var face:MODELS.get(mode)){
            boolean changed=face.distance()<=radius;
            if(mode!=0&&!changed)continue;
            if(mode==4&&(face.color()>>>24<255?phase<135:face.y()>-6&&phase<125))continue;
            int c=face.color();if(mode==0&&changed&&c==0xFF919DA3)c=0xFFE1C37E;
            var mesh=buffers.getBuffer(c>>>24<255?CrystalGlassLayer.AMBER:RenderType.entityCutoutNoCull(WHITE));
            var normal=DIRECTIONS[face.side()];float shade=face.side()==4?1:face.side()==5?.6F:face.side()<2?.86F:.73F;
            for(int i=0;i<4;i++){
                var v=CORNERS[FACES[face.side()][i]];
                mesh.addVertex(pose.last(),face.x()+v[0]*.5F,face.y()+v[1]*.5F,face.z()+v[2]*.5F)
                        .setColor((int)((c>>>16&255)*shade),(int)((c>>>8&255)*shade),(int)((c&255)*shade),c>>>24)
                        .setUv(i>=2?1:0,i==1||i==2?0:1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(pose.last(),normal[0],normal[1],normal[2]);
            }
        }
        if(mode==4&&phase>145){
            pose.pushPose();pose.translate(-.5,-5.5,-.5);
            mc.getItemRenderer().renderStatic(GuideContents.icon("omnira:crystal_ball"),net.minecraft.world.item.ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);pose.popPose();
        }
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
        String key=mode==0?"matter":mode==4?"ecology":mode==1?"sphere":mode==2?"cube":"plane";
        var label=Component.translatable("guide.omnira.codex_example."+key);
        label(g,label,x+66,y+108);
        label(g,Component.translatable("guide.omnira.codex_example.scale"),x+66,y+120);
        g.disableScissor();
    }
    private static void label(GuiGraphics g,Component text,int center,int y){
        var font=Minecraft.getInstance().font;int width=font.width(text);float scale=Math.min(1,124F/Math.max(1,width));
        g.pose().pushPose();g.pose().translate(center,y,0);g.pose().scale(scale,scale,1);
        g.drawString(font,text,-width/2,0,0xFF405462,false);g.pose().popPose();
    }
    private CodexExampleScene(){}
}
