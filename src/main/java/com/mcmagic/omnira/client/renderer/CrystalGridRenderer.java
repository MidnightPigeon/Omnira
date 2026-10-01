package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.item.CrystalGridItem;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;

/** The frame stays still; only the inset spell circuit turns. All parts remain voxel models. */
public final class CrystalGridRenderer extends BlockEntityWithoutLevelRenderer {
    public CrystalGridRenderer() {super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation part(String name,String part) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/"+name+"_"+part));
    }
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        if(!(stack.getItem() instanceof CrystalGridItem grid)) return;
        var mc=Minecraft.getInstance();
        String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        draw(name,"frame",stack,pose,buffers,light,overlay);
        double time=mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        pose.pushPose();
        pose.translate(.5,.5+Math.sin(time*.045)*.012,.5);
        pose.mulPose(Axis.ZP.rotationDegrees((float)(time*.4%360)));
        pose.translate(-.5,-.5,-.5);
        draw(name,"orbit",stack,pose,buffers,light,overlay);
        var contents=NonNullList.withSize(grid.capacity,ItemStack.EMPTY);
        stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(contents);
        for(int i=0;i<grid.capacity;i++) if(CrystalGridMenu.isCrystal(contents.get(i))) {
            double angle=i*Math.PI*2/grid.capacity;
            pose.pushPose();
            pose.translate((Math.round(7.5+4.5*Math.sin(angle))+.5)/16,
                    (Math.round(7.5+4.5*Math.cos(angle))+.5)/16,.5);
            pose.scale(.13F,.13F,.13F);
            mc.getItemRenderer().renderStatic(contents.get(i),ItemDisplayContext.FIXED,LightTexture.FULL_BRIGHT,overlay,pose,buffers,mc.level,0);
            pose.popPose();
        }
        pose.popPose();
    }
    private static void draw(String name,String part,ItemStack stack,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(part(name,part)),stack,light,overlay,pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS));
    }
}
