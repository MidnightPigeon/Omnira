package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.time.WonderlandPokerStandBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class WonderlandPokerStandRenderer implements BlockEntityRenderer<WonderlandPokerStandBlockEntity> {
    private static final String[] SUITS={"heart","spade","diamond","club"};
    public WonderlandPokerStandRenderer(BlockEntityRendererProvider.Context context){}
    public static ModelResourceLocation cardModel(String suit){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/wonderland_poker_stand/"+suit));
    }
    public static ModelResourceLocation symbolModel(String suit){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/wonderland_poker_stand/"+suit+"_symbol"));
    }
    private static void draw(ModelResourceLocation model,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model),ItemStack.EMPTY,
                light,overlay,pose,buffers.getBuffer(Sheets.solidBlockSheet()));
    }
    private static void movingCards(int phase,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();
        pose.translate(.5,1.50,.5);
        pose.mulPose(Axis.YP.rotationDegrees((float)(time*.35)));
        pose.translate(-.5,-.5,-.5);
        draw(symbolModel(SUITS[phase]),pose,buffers,light,overlay);
        pose.popPose();
        for(int i=0;i<4;i++){
            double angle=time*.055+i*Math.PI/2;
            pose.pushPose();
            pose.translate(.5+Math.cos(angle)*.55,.98+Math.sin(time*.08+i)*.12,.5+Math.sin(angle)*.55);
            pose.mulPose(Axis.YP.rotationDegrees((float)(time*1.9+i*90)));
            pose.mulPose(Axis.ZP.rotationDegrees((float)(14*Math.sin(time*.11+i))));
            pose.scale(.38F,.38F,.38F);
            pose.translate(-.5,-.5,-.5);
            draw(cardModel(SUITS[i]),pose,buffers,light,overlay);
            pose.popPose();
        }
    }
    @Override public void render(WonderlandPokerStandBlockEntity stand,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        if(stand.getLevel()!=null)movingCards(stand.suit().ordinal(),stand.getLevel().getGameTime()+partial,pose,buffers,light,overlay);
    }
    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(WonderlandPokerStandBlockEntity stand){
        return new net.minecraft.world.phys.AABB(stand.getBlockPos()).inflate(1,1,1);
    }
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            var mc=Minecraft.getInstance();
            mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(ModelResourceLocation.standalone(
                    ResourceLocation.fromNamespaceAndPath("omnira","block/wonderland_poker_stand"))),stack,
                    light,overlay,pose,buffers.getBuffer(Sheets.solidBlockSheet()));
            double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
            movingCards(0,time,pose,buffers,light,overlay);
        }
    }
}
