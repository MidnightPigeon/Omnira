package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class RitualSwordUpgradeRenderer extends BlockEntityWithoutLevelRenderer {
    public RitualSwordUpgradeRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String id,String part){
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/sword_upgrades/"+id+"_"+part));
    }
    public static String id(RitualSwordItem sword){
        return (sword.kind==RitualSwordItem.Kind.NAIL?RitualSwordUpgrades.NAIL_IDS:RitualSwordUpgrades.NEEDLE_IDS).get(sword.upgradeLevel-1);
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var mc=Minecraft.getInstance();var sword=(RitualSwordItem)stack.getItem();
        boolean tail=sword.kind==RitualSwordItem.Kind.NEEDLE && mc.player!=null && context!=ItemDisplayContext.GUI
                && context!=ItemDisplayContext.GROUND && context!=ItemDisplayContext.FIXED
                && mc.player.getMainHandItem()==stack && FlyingNeedle.isOut(mc.player);
        draw(stack,tail?"ring":"body",pose,buffers,light,overlay);
        particles(stack,pose,buffers,overlay,tail);
    }
    public static void draw(ItemStack stack,String part,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var sword=(RitualSwordItem)stack.getItem();
        var layer=sword.kind==RitualSwordItem.Kind.NAIL?CrystalGlassLayer.ATLAS:RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS);
        Minecraft.getInstance().getItemRenderer().renderModelLists(Minecraft.getInstance().getModelManager().getModel(model(id(sword),part)),
                stack,light,overlay,pose,ItemRenderer.getFoilBufferDirect(buffers,layer,true,stack.hasFoil()));
    }
    public static void particles(ItemStack stack,PoseStack pose,MultiBufferSource buffers,int overlay,boolean tail){
        var sword=(RitualSwordItem)stack.getItem();var mc=Minecraft.getInstance();
        double t=mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        for(int i=0;i<sword.upgradeLevel*2;i++){
            double a=t*.045+i*2.39996,y=tail?.19:.25+(i+.5)/(sword.upgradeLevel*2)*1.65;
            pose.pushPose();pose.translate(.5+Math.cos(a)*.19,y+Math.sin(a*1.3)*.04,.5+Math.sin(a)*.19);
            pose.mulPose(Axis.YP.rotationDegrees((float)(a*57.2958)));pose.translate(-.5,-.5,-.5);
            Minecraft.getInstance().getItemRenderer().renderModelLists(mc.getModelManager().getModel(ParadoxMaterialRenderer.model("grain")),
                    stack,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));pose.popPose();
        }
    }
}
