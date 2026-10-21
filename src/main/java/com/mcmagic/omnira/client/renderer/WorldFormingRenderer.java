package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.client.*;
import com.mcmagic.omnira.myriad.*;
import com.mcmagic.omnira.world.dimension.DreamRitual;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** A bounded, render-only world projection; no entities or world blocks are created. */
public final class WorldFormingRenderer {
    public static void render(WorldFormingRitual ritual,double time,float partial,PoseStack pose,MultiBufferSource buffers,int overlay) {
        if (!ritual.active()) return;
        draw(ritual.elapsed()+partial, time, ritual::anchor, pose, buffers, overlay);
    }

    public static void draw(double elapsed,double time,java.util.function.IntUnaryOperator anchor,PoseStack pose,MultiBufferSource buffers,int overlay) {
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        double assembly=Math.clamp((elapsed-8*WorldFormingRitual.ASPECT_TICKS)/WorldFormingRitual.ASSEMBLY_TICKS,0,1);
        for (var aspect:WorldComponent.values()) {
            double progress=Math.clamp((elapsed-aspect.ordinal()*WorldFormingRitual.ASPECT_TICKS)/WorldFormingRitual.ASPECT_TICKS,0,1);
            if (progress<=0) continue;
            int index=anchor.applyAsInt(aspect.ordinal()); if(index<0||index>=8)continue;
            var offset=DreamRitual.ANCHORS[index];
            for(int i=0;i<4;i++) {
                double a=time*.04+i*Math.PI/2;
                StellarVisuals.cube(pose,mesh,.5F+offset[0]+(float)Math.cos(a)*.3F,.95F,.5F+offset[1]+(float)Math.sin(a)*.3F,
                        .022F,aspect.color,(float)(progress*(1-assembly)*.8));
            }
            if (progress<1) {
                for(int i=0;i<12;i++) {
                    float p=(float)((time*.025+i/12.0)%1);
                    float x=.5F+offset[0]*(1-p),z=.5F+offset[1]*(1-p),y=1.02F+p*.85F+(float)Math.sin(p*Math.PI)*.55F;
                    StellarVisuals.cube(pose,mesh,x,y,z,.024F,aspect.color,(float)Math.sin(p*Math.PI));
                }
                pose.pushPose();
                float p=smooth((float)progress);
                pose.translate(.5+offset[0]*(1-p),1.1+p*.8+Math.sin(p*Math.PI)*.5,.5+offset[1]*(1-p));
                pose.mulPose(Axis.YP.rotationDegrees((float)(time*1.5)));
                pose.scale(.65F,.65F,.65F);pose.translate(-.5,-.5,-.5);
                WorldComponentClient.draw(aspect,time,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
                pose.popPose();
            }
        }
        pose.pushPose();pose.translate(.5,1.45,.5);
        float shrink=1-smooth((float)assembly)*.97F;
        pose.scale(shrink,shrink,shrink);
        landscape(elapsed,time,pose,buffers);
        pose.popPose();

        var mc=Minecraft.getInstance();
        pose.pushPose();pose.translate(.5,1.88,.5);
        float size=.7F*(1-smooth((float)assembly));
        if (size>.001F) {
            pose.scale(size,size,size);
            mc.getItemRenderer().renderStatic(new ItemStack(WorldSeedContent.ACTIVE.get()),ItemDisplayContext.FIXED,
                    LightTexture.FULL_BRIGHT,overlay,pose,CrystalGlassLayer.contents(buffers),mc.level,0);
        }
        pose.popPose();
        if (assembly>0) {
            pose.pushPose();float s=.03F+.57F*smooth((float)assembly);
            pose.translate(.5,1.1,.5);pose.scale(s,s,s);pose.translate(-2.5,0,-2.5);
            MiniatureWorldClient.draw(null,MiniatureWorldBlockEntity.Stage.EMPTY,time,pose,buffers,LightTexture.FULL_BRIGHT,overlay);
            pose.popPose();
        }
    }

    private static float visibility(double elapsed,int index) {
        return smooth((float)Math.clamp((elapsed-index*WorldFormingRitual.ASPECT_TICKS)/WorldFormingRitual.ASPECT_TICKS,0,1));
    }
    private static void landscape(double elapsed,double time,PoseStack pose,MultiBufferSource buffers) {
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        float earth=visibility(elapsed,0),water=visibility(elapsed,1),sky=visibility(elapsed,2),heat=visibility(elapsed,3),life=visibility(elapsed,4);
        float ages=visibility(elapsed,5),boundary=visibility(elapsed,6),dream=visibility(elapsed,7);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
            float y=((x*x+z*z)%3)*.07F;
            StellarVisuals.cube(pose,mesh,x*.23F,y,z*.23F,.11F,0x93969E,earth*.75F);
            if(z==0||x==2&&z>0)StellarVisuals.cube(pose,mesh,x*.23F,y+.1F,z*.23F,.105F,0x63BEE9,water*.85F);
            else StellarVisuals.cube(pose,mesh,x*.23F,y+.055F,z*.23F,.108F,0x8DBB80,life*.8F);
        }
        for(int i=0;i<12;i++) {
            double a=time*.014+i*Math.PI/6;
            StellarVisuals.cube(pose,mesh,(float)Math.cos(a)*.95F,.85F+(float)Math.sin(a*2)*.05F,(float)Math.sin(a)*.95F,.045F,0xDBF7FF,sky*.6F);
            StellarVisuals.cube(pose,mesh,(float)Math.cos(a)*.38F,-.18F,(float)Math.sin(a)*.38F,.07F,0xFF9D4E,heat*.8F);
            StellarVisuals.cube(pose,mesh,(float)Math.cos(a)*1.06F,.25F,(float)Math.sin(a)*1.06F,.025F,0xDBF3B9,ages*.65F);
        }
        for(int i=0;i<4;i++) {
            float x=i<2?-.46F:.46F,z=i%2==0?-.46F:.46F;
            StellarVisuals.cube(pose,mesh,x,.26F,z,.075F,0x8C9A71,life*.8F);
            StellarVisuals.cube(pose,mesh,x,.43F,z,.16F,0x9ED78A,life*.8F);
        }
        for(int x=-1;x<=1;x+=2)for(int z=-1;z<=1;z+=2)for(int y=0;y<=8;y++)
            StellarVisuals.cube(pose,mesh,x*1.05F,-.18F+y*.16F,z*1.05F,.018F,0xB5E9F3,boundary*.6F);
        for(int i=0;i<8;i++) {
            double a=time*.02+i*Math.PI/4;
            StellarVisuals.cube(pose,mesh,(float)Math.cos(a)*.55F,1.02F+(float)Math.sin(a*2)*.12F,(float)Math.sin(a)*.55F,.045F,0xC5A0EB,dream*.7F);
        }
    }
    private static float smooth(float value) { return value*value*(3-2*value); }
    private WorldFormingRenderer() {}
}
