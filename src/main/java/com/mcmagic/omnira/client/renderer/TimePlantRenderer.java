package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.time.TimePlantBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

public final class TimePlantRenderer implements BlockEntityRenderer<TimePlantBlockEntity> {
    public TimePlantRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(TimePlantBlockEntity plant,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(plant.getLevel()==null)return;
        if(plant.getBlockState().getBlock() instanceof com.mcmagic.omnira.mire.MireLilyBlock lily){
            lilyMotes(pose,buffers,(plant.getLevel().getGameTime()+partial)*.035+(plant.getBlockPos().asLong()&255),lily.reborn);return;
        }
        boolean flower=plant.getBlockState().is(TimeNatureContent.FLOWER.get());
        double time=(plant.getLevel().getGameTime()+partial)*.035+(plant.getBlockPos().asLong()&255);
        motes(pose,buffers,time,flower);
    }
    public static void motes(PoseStack pose,MultiBufferSource buffers,double time,boolean flower){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<6;i++){
            double angle=time+i*Math.PI/3,radius=.24+.035*Math.sin(time*.6+i);
            float x=.5F+(float)(Math.cos(angle)*radius),z=.5F+(float)(Math.sin(angle)*radius);
            float y=(flower?.58F:.38F)+(float)Math.sin(angle*1.5+i)*.14F;
            mote(pose,mesh,x,y,z,.012F,flower||i%3==0?0xE7F8D9:0x91D58C,.78F);
        }
    }
    public static void lilyMotes(PoseStack pose,MultiBufferSource buffers,double time,boolean reborn){
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<6;i++){
            double a=time+i*Math.PI/3;
            mote(pose,mesh,.5F+(float)Math.cos(a)*.32F,.12F+(float)(.5+.5*Math.sin(a*1.4))*.22F,.5F+(float)Math.sin(a)*.32F,.015F,reborn?0xF3FFE9:0x25272A,.85F);
        }
    }
    public static void mote(PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer mesh,float x,float y,float z,float r,int color,float alpha){
        float[][] v={{x-r,y-r,z-r},{x+r,y-r,z-r},{x+r,y+r,z-r},{x-r,y+r,z-r},
                {x-r,y-r,z+r},{x+r,y-r,z+r},{x+r,y+r,z+r},{x-r,y+r,z+r}};
        for(int[] face:new int[][]{{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}})
            for(int n:face)mesh.addVertex(pose.last().pose(),v[n][0],v[n][1],v[n][2])
                    .setColor((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);
    }
    @Override public int getViewDistance(){return 32;}
}
