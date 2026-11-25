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

/** Detached, cached pixel diagrams. Each example uses the actual maximum edit radius. */
public final class CodexExampleScene {
    private static final int RADIUS=7;
    private static final int STONE=0xFF919DA3;
    private static final int[][] DIRECTIONS={{0,0,-1},{0,0,1},{-1,0,0},{1,0,0},{0,1,0},{0,-1,0}};
    private static final int[][] CORNERS={{-1,-1,-1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,1},{1,-1,1},{1,1,1},{-1,1,1}};
    private static final int[][] FACES={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
    private static final ResourceLocation WHITE=ResourceLocation.parse("omnira:textures/entity/solid_white.png");
    private record Face(int x,int y,int z,int side,int color,float distance) {}
    private static final List<Face> MATTER=build(0,6);
    private static final List<List<Face>> SHELLS=java.util.stream.IntStream.rangeClosed(1,RADIUS)
            .mapToObj(radius->build(1,radius)).toList();
    private static final List<Face> ECOLOGY=build(2,RADIUS);
    public static boolean supports(String id){return id.equals("codex_matter")||id.equals("codex_form")||id.equals("codex_ecology");}
    private static int color(int mode,int radius,int x,int y,int z){
        if(x*x+y*y+z*z>radius*radius)return 0;
        if(mode==0)return STONE;
        if(mode==1)return EditGeometry.contains(Operation.SHAPE,radius,Direction.Axis.Y,true,x,y,z)?0x58BB9CE1:0;
        // Open the near upper quadrant to expose the soil, tree and central treasure.
        if(x>=0 && z>=0 && y>-3)return 0;
        if(EditGeometry.ecologyShell(radius,x,y,z))return 0x8C8DDDE7;
        if(y<=-3)return y==-3?0xFF78A35D:0xFF8C8170;
        if(x==-2 && z==-1 && y<=0)return 0xFF81624C;
        if(Math.abs(x+2)+Math.abs(z+1)+Math.abs(y-1)<=2)return 0xFF67944E;
        return 0;
    }
    private static List<Face> build(int model,int radius){
        var faces=new ArrayList<Face>();
        for(int x=-RADIUS;x<=RADIUS;x++)for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++){
            int c=color(model,radius,x,y,z);if(c==0)continue;
            for(int side=0;side<6;side++){
                var d=DIRECTIONS[side];int adjacent=color(model,radius,x+d[0],y+d[1],z+d[2]);
                if(adjacent!=0&&(adjacent>>>24==255||c>>>24<255))continue;
                // The sample stone globe sits one block to one side of the edit origin.
                int dx=model==0?x+1:x;
                faces.add(new Face(x,y,z,side,c,(float)Math.sqrt(dx*dx+y*y+z*z)));
            }
        }
        return List.copyOf(faces);
    }
    public static void render(GuiGraphics g,int x,int y,int ticks,String scene,float yaw,float pitch){
        var mc=Minecraft.getInstance();double time=net.minecraft.Util.getMillis()/50.0;
        int mode=scene.equals("codex_matter")?0:scene.equals("codex_form")?1:2;
        double phase=time%180;
        float radius=(float)Math.clamp((phase-20)/100,0,1)*RADIUS;
        var faces=mode==0?MATTER:mode==1?SHELLS.get(Math.max(1,(int)Math.ceil(radius))-1):ECOLOGY;
        var pose=g.pose();var buffers=mc.renderBuffers().bufferSource();
        g.flush();com.mcmagic.omnira.client.screen.UiScissor.enable(g,x,y,x+132,y+132);
        RenderSystem.enableDepthTest();pose.pushPose();
        pose.translate(x+66,y+54+Math.sin(time*.045)*1.5,180);
        float scale=mode==0?6.2F:5.6F;
        pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(24+pitch));pose.mulPose(Axis.YP.rotationDegrees(-35+yaw+(float)Math.sin(time*.008)*12));
        for(var face:faces){
            int c=face.color();
            if(mode==0 && face.distance()<=4+radius*3/RADIUS)c=0xFFE1C37E;
            if(mode==2){
                if(c>>>24<255 && phase<135)c=0x308DDDE7;
                else if(c>>>24==255 && (face.distance()>radius || face.y()>-3 && phase<125))continue;
            }
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
        if(mode==2&&phase>145){
            pose.pushPose();pose.translate(0,-2,0);
            mc.getItemRenderer().renderStatic(GuideContents.icon("omnira:crystal_ball"),net.minecraft.world.item.ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,mc.level,0);pose.popPose();
        }
        buffers.endBatch();pose.popPose();RenderSystem.disableDepthTest();
        String key=mode==0?"matter":mode==1?"sphere":"ecology";
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
