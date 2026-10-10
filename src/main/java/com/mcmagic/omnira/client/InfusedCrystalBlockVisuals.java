package com.mcmagic.omnira.client;

import com.mcmagic.omnira.block.InfusedCrystalBlock;
import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.client.renderer.CrystalGridRenderer;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class InfusedCrystalBlockVisuals {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event){
        event.registerBlockEntityRenderer(ModBlockEntityTypes.INFUSED_CRYSTAL_MOTES.get(),context->new BlockEntityRenderer<InfusedCrystalBlock.Motes>(){
            @Override public void render(InfusedCrystalBlock.Motes entity,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                var level=entity.getLevel();if(level==null)return;
                var mc=Minecraft.getInstance();double time=level.getGameTime()+partial+(entity.getBlockPos().asLong()&127);
                var boxes=entity.getBlockState().getShape(level,entity.getBlockPos()).toAabbs();
                var model=mc.getModelManager().getModel(CrystalGridRenderer.part("infused_spiritual_crystal","mote"));
                for(int i=0;i<4;i++){
                    double angle=time*.025+i*Math.PI/2;
                    double x=.5+Math.cos(angle)*.25,y=.5+Math.sin(angle*1.3+i)*.28,z=.5+Math.sin(angle)*.25;
                    if(boxes.stream().noneMatch(box->box.contains(x-.035,y-.035,z-.035)&&box.contains(x+.035,y+.035,z+.035)))continue;
                    pose.pushPose();pose.translate(x,y,z);pose.mulPose(Axis.YP.rotationDegrees((float)(time*2+i*45)));pose.translate(-.03125,-.03125,-.03125);
                    mc.getItemRenderer().renderModelLists(model,ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(CrystalGlassLayer.ATLAS));
                    pose.popPose();
                }
            }
        });
    }
    private InfusedCrystalBlockVisuals(){}
}
