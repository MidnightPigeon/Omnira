package com.mcmagic.omnira.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class GuideBookRenderer extends BlockEntityWithoutLevelRenderer {
    public GuideBookRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());
    }
    public static ModelResourceLocation model(String part) {
        return CrystalGridRenderer.part("guide_book",part);
    }
    public static ModelResourceLocation grimoire(String kind) {
        return CrystalGridRenderer.part(kind+"_grimoire","closed");
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,
                                      MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        boolean hand=context.firstPerson() || context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        boolean open=hand && mc.screen instanceof com.mcmagic.omnira.client.screen.GuideBookScreen;
        var variant=stack.getItem() instanceof com.mcmagic.omnira.item.InfusedGrimoireItem book
                ?grimoire(book.kind().name().toLowerCase(java.util.Locale.ROOT)):model(open?"open":"closed");
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(variant),
                stack,light,overlay,pose,buffers.getBuffer(Sheets.translucentItemSheet()));
        if(stack.getItem() instanceof com.mcmagic.omnira.item.InfusedGrimoireItem book) {
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            grimoireMotes(book.kind(),pose,buffers,time);
        }
    }
    private static void grimoireMotes(com.mcmagic.omnira.item.InfusedGrimoireItem.Kind kind,
                                      PoseStack pose,MultiBufferSource buffers,double time) {
        int color=kind==com.mcmagic.omnira.item.InfusedGrimoireItem.Kind.SANCTIFIED?0xF2DD83:0xBB9BE8;
        var mesh=buffers.getBuffer(CrystalGlassLayer.COLOR);
        var matrix=pose.last().pose();
        for(int i=0;i<7;i++) {
            double cycle=(time*.017+i*.143)%1;
            float x=(float)(.5+Math.sin(i*2.399+time*.017)*(.25+cycle*.11));
            float y=(float)(.23+cycle*.67);
            float z=(float)(.5+Math.cos(i*2.399+time*.012)*(.24+cycle*.1));
            float size=(i%3==0?.021F:.014F);
            int alpha=(int)(200*Math.min(1,cycle*6)*Math.min(1,(1-cycle)*5));
            if(alpha==0)continue;
            square(mesh,matrix,x-size,y-size,z,x+size,y+size,z,color,alpha,false);
            square(mesh,matrix,x,y-size,z-size,x,y+size,z+size,color,alpha,true);
        }
    }
    private static void square(com.mojang.blaze3d.vertex.VertexConsumer mesh,org.joml.Matrix4f matrix,
                               float ax,float ay,float az,float bx,float by,float bz,int color,int alpha,boolean side) {
        int r=color>>16&255,g=color>>8&255,b=color&255;
        mesh.addVertex(matrix,ax,ay,az).setColor(r,g,b,alpha);
        mesh.addVertex(matrix,side?ax:bx,ay,side?bz:az).setColor(r,g,b,alpha);
        mesh.addVertex(matrix,bx,by,bz).setColor(r,g,b,alpha);
        mesh.addVertex(matrix,side?bx:ax,by,side?az:bz).setColor(r,g,b,alpha);
    }
}
