package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.item.*;

/** A thin voxel hexagon, with no buried faces or stacked transparent sheets. */
public final class DeflectionFieldRenderer extends BlockEntityWithoutLevelRenderer {
    public DeflectionFieldRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    private static boolean occupied(int x,int y){
        if(y<0||y>=24||x<0||x>=18)return false;
        int inset=Math.max(0,Math.abs(y*2-23)-11)/2;
        return x>=inset&&x<18-inset;
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        boolean held=context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND||context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                ||context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND||context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        // Held geometry uses vanilla shield's grip origin; display models stay centered.
        if(held)pose.translate(-.5625,-.75,.09375);else pose.translate(-.0625,-.25,.5);
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);var matrix=pose.last().pose();
        for(int y=0;y<24;y++)for(int x=0;x<18;x++){
            if(!occupied(x,y))continue;
            boolean edge=!occupied(x-1,y)||!occupied(x+1,y)||!occupied(x,y-1)||!occupied(x,y+1);
            float r=edge?.99F:.92F,g=edge?.75F:.47F,b=edge?.92F:.75F,a=edge?.75F:.32F;
            float x0=x/16F,x1=(x+1)/16F,y0=y/16F,y1=(y+1)/16F,z0=-.025F,z1=.025F;
            float[][] v={{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0},{x0,y0,z1},{x1,y0,z1},{x1,y1,z1},{x0,y1,z1}};
            int[][] faces={{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
            for(int f=0;f<6;f++){
                if(f==2&&occupied(x-1,y)||f==3&&occupied(x+1,y)||f==4&&occupied(x,y+1)||f==5&&occupied(x,y-1))continue;
                for(int n:faces[f])mesh.addVertex(matrix,v[n][0],v[n][1],v[n][2]).setColor(r,g,b,a);
            }
        }
        for(int i=0;i<9;i++){
            float x=(3+(i*7)%12+.5F)/16F,y=(3+(i*11)%18+.5F)/16F;
            float glow=.55F+.4F*(float)Math.sin(time*.075+i*2.3);
            StellarVisuals.cube(pose,buffers,x,y,.035F,.015F,0xFFF3FF,glow);
            if(i%3==0){StellarVisuals.cube(pose,buffers,x+.03125F,y,.035F,.008F,0xFFF3FF,glow*.65F);StellarVisuals.cube(pose,buffers,x,y+.03125F,.035F,.008F,0xFFF3FF,glow*.65F);}
        }
        pose.popPose();
    }
}
