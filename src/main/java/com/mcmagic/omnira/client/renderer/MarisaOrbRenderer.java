package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.shop.MarisaOrbBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.*;

public final class MarisaOrbRenderer implements BlockEntityRenderer<MarisaOrbBlockEntity> {
    public MarisaOrbRenderer(BlockEntityRendererProvider.Context context){}
    public static ModelResourceLocation model(String part){return CrystalGridRenderer.part("marisa_crystal_ball",part);}
    private static void draw(String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model(part)),ItemStack.EMPTY,
                light,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
    private static void globe(PoseStack pose,MultiBufferSource buffers,double time,int light,int overlay){
        var mc=Minecraft.getInstance();
        var goods=com.mcmagic.omnira.client.MarisaGoodsView.goods();
        MultiBufferSource contents=type->buffers.getBuffer(type==Sheets.translucentItemSheet()?CrystalGlassLayer.ATLAS:type);
        for(int i=0;i<goods.size();i++){
            var item=goods.get(i);var at=com.mcmagic.omnira.shop.MarisaDisplayMotion.position(i,time);
            pose.pushPose();pose.translate(at.x,at.y,at.z);
            pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((float)(time*.6+i*60)));
            var baked=mc.getItemRenderer().getModel(item,mc.level,null,i);
            boolean miniature=CrystalBallItemShape.apply(baked,pose);
            if(!miniature)pose.scale(.2F,.2F,.2F);
            mc.getItemRenderer().render(item,miniature?ItemDisplayContext.NONE:ItemDisplayContext.GROUND,false,
                    pose,contents,LightTexture.FULL_BRIGHT,overlay,baked);
            pose.popPose();
        }
        var vertices=buffers.getBuffer(CrystalGlassLayer.COLOR);
        for(int i=0;i<7;i++){
            double a=time*.022+i*2.4;
            float x=.5F+(float)Math.cos(a)*.18F,y=.57F+(float)Math.sin(a*1.3+i)*.17F,z=.5F+(float)Math.sin(a)*.18F;
            int color=i%3==0?0xFFF9D5:0xFFD864;
            TimePlantRenderer.mote(pose,vertices,x,y,z,.018F,color,.95F);
            if(i%2==0){TimePlantRenderer.mote(pose,vertices,x+.035F,y,z,.009F,color,.7F);TimePlantRenderer.mote(pose,vertices,x-.035F,y,z,.009F,color,.7F);}
        }
        draw("shell",pose,buffers,light,overlay);
    }
    @Override public void render(MarisaOrbBlockEntity orb,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        globe(pose,buffers,orb.getLevel()==null?0:orb.getLevel().getGameTime()+partial,light,overlay);
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            draw("base",pose,buffers,light,overlay);
            var mc=Minecraft.getInstance();globe(pose,buffers,mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false),light,overlay);
        }
    }
}
