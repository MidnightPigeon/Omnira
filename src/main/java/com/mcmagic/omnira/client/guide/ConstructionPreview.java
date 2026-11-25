package com.mcmagic.omnira.client.guide;

import com.mcmagic.omnira.client.renderer.CrystalGlassLayer;
import com.mcmagic.omnira.guidance.ConstructionPattern;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.joml.*;

public final class ConstructionPreview {
    private ConstructionPreview() {}
    public static String draw(GuiGraphics g,int x,int y,int ticks,String scene,float yaw,float pitch,int mouseX,int mouseY) {
        var pose=g.pose();
        var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
        var pattern=ConstructionPattern.of(scene);
        boolean forge=scene.equals("advanced_assembly_table");
        int scale=forge?25:24;
        var cursor=pose.last().pose().transformPosition(mouseX,mouseY,0,new Vector3f());
        g.flush();
        RenderSystem.enableDepthTest();
        pose.pushPose();
        pose.translate(x+62,y+(forge?83:80),180);
        pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(25+pitch));
        pose.mulPose(Axis.YP.rotationDegrees(-35+yaw+(float)java.lang.Math.sin(ticks*.007)*20));
        double base=scene.equals("advanced_crystal_aggregation_ring")?-1:scene.equals("alchemical_catalysis_chamber")?-.4:0;
        pose.translate(-.5,base,-.5);
        ConstructionPattern.Cell selected=null;
        if(mouseX>=x && mouseX<x+124 && mouseY>=y && mouseY<y+134){
            var inverse=new Matrix4f(pose.last().pose()).invert();
            var from=new Vec3(inverse.transformPosition(cursor.x,cursor.y,cursor.z+1000,new Vector3f()));
            var to=new Vec3(inverse.transformPosition(cursor.x,cursor.y,cursor.z-1000,new Vector3f()));
            double nearest=Double.POSITIVE_INFINITY;
            for(var cell:pattern.cells()){
                var hit=cell.display(ticks).getShape(EmptyBlockGetter.INSTANCE,cell.offset()).clip(from,to,cell.offset());
                if(hit!=null && hit.getLocation().distanceToSqr(from)<nearest){
                    nearest=hit.getLocation().distanceToSqr(from);
                    selected=cell;
                }
            }
        }
        for(var cell:pattern.cells()){
            pose.pushPose();
            var pos=cell.offset();
            pose.translate(pos.getX(),pos.getY(),pos.getZ());
            BlockState state=cell.display(ticks);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,CrystalGlassLayer.contents(buffers),LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
            if(cell==selected){
                for(var box:state.getShape(EmptyBlockGetter.INSTANCE,BlockPos.ZERO).toAabbs())
                    LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),box.inflate(.004),.65F,.95F,1F,.85F);
            }
            pose.popPose();
        }
        buffers.endBatch();
        pose.popPose();
        RenderSystem.disableDepthTest();
        return selected==null?"":selected.display(ticks).getBlock().getName().getString();
    }
}
