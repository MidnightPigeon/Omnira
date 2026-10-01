package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.ArquebusPlugin;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class ArquebusRenderer extends BlockEntityWithoutLevelRenderer {
    public static final String[] PARTS={"body","ancestor","ghost","black","red","blue","lavender"};
    public ArquebusRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part) {return CrystalGridRenderer.part("arcane_arquebus",part);}
    private void draw(String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),stack,light,overlay,
                pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private void pixel(String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay,
                       double x,double y,double z,float width,float height,float length) {
        pose.pushPose();pose.translate(x/16,y/16,z/16);pose.scale(width,height,length);
        draw(part,stack,pose,buffers,light,overlay);pose.popPose();
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();var kit=ArquebusPlugin.of(stack);
        double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:
                mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        String body=switch(kit) {
            case ANCESTOR_LAUNCHER -> "ancestor";
            case KINGS_NEW_CLOTHES -> "ghost";
            default -> "body";
        };
        draw(body,stack,pose,buffers,light,overlay);
        if(kit==ArquebusPlugin.ANCESTOR_LAUNCHER) {
            // Local model particles follow the muzzle in every display context without world spawning.
            for(int i=0;i<5;i++) {
                double phase=(time*.012+i*.2)%1,angle=i*2.4+time*.018;
                double radius=.65+phase*.8;
                float size=(float)(.5*Math.sin(phase*Math.PI));
                pixel(i%3==0?"red":"lavender",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay,
                        8+Math.cos(angle)*radius,12+Math.sin(angle)*radius+phase*.8,
                        -13.5-phase*3,size,size,size);
            }
            // Opposite voxel helices advance together around the barrel, never through the bore.
            for(int i=0;i<34;i++)for(int strand=0;strand<2;strand++) {
                double z=-12.75+i*.5,angle=z*.55+time*.045+strand*Math.PI;
                double x=8+Math.rint(Math.cos(angle)*2.15*4)/4;
                double y=12+Math.rint(Math.sin(angle)*2.15*4)/4;
                pixel(strand==0?"black":"red",stack,pose,buffers,strand==0?light:LightTexture.FULL_BRIGHT,overlay,
                        x,y,z,.55F,.55F,.65F);
            }
        } else if(kit==ArquebusPlugin.KINGS_NEW_CLOTHES) {
            // Bright moving streaks remain visible independently of world light and animated water.
            for(double center:new double[]{6.5,9.5})for(int lane=0;lane<4;lane++)for(int pulse=0;pulse<3;pulse++) {
                double angle=lane*Math.PI/2;
                double z=3.5-((time*.22+pulse*5.3+lane*1.3)%16);
                pixel("blue",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay,
                        center+Math.cos(angle)*1.13,12+Math.sin(angle)*1.13,z,.22F,.22F,1.2F);
            }
            for(int i=0;i<5;i++) {
                double z=19-((time*.13+i*2.2)%11);
                double y=z>=16?10.5:z>=14?10:z>=12?9:z>=10?10:12.5;
                pixel("blue",stack,pose,buffers,LightTexture.FULL_BRIGHT,overlay,8,y+.12,z,.4F,.24F,.9F);
            }
        }
    }
}
