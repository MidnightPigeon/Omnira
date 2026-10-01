package com.mcmagic.omnira.client.guide;
import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.client.renderer.AdvancedForgeRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;

/** The construction diagram reads the same layout used by formation validation. */
public final class AdvancedForgeScene {
    public static void render(GuiGraphics g,int x,int y,int ticks){
        var mc=Minecraft.getInstance();g.flush();RenderSystem.enableDepthTest();var p=g.pose();var buffers=mc.renderBuffers().bufferSource();
        p.pushPose();p.translate(x+62,y+83,180);p.scale(25,-25,25);p.mulPose(Axis.XP.rotationDegrees(25));p.mulPose(Axis.YP.rotationDegrees(-35+(float)Math.sin(ticks*.007)*20));
        if(ticks%320<200){
            for(int layer=0;layer<2;layer++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
                var b=ForgeLayout.required(dx,layer,dz);if(b==Blocks.AIR)continue;var state=b.defaultBlockState();
                if(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING))state=state.setValue(BlockStateProperties.HORIZONTAL_FACING,layer==0?Direction.SOUTH:dx<0?Direction.WEST:dx>0?Direction.EAST:dz>0?Direction.SOUTH:Direction.NORTH);
                p.pushPose();p.translate(dx-.5,layer,dz-.5);mc.getBlockRenderer().renderSingleBlock(state,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);p.popPose();
            }
        }else AdvancedForgeRenderer.renderLocal(null,0,ticks,p,buffers,LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY);
        buffers.endBatch();p.popPose();RenderSystem.disableDepthTest();
    }
}
