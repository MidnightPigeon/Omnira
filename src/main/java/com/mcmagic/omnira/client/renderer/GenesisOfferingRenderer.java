package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.alchemy.AlchemyMaterials;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/** One continuous open-ended liquid ribbon; no internal caps or particle cloud. */
public final class GenesisOfferingRenderer {
    private GenesisOfferingRenderer() {}
    public static void render(PoseStack pose,MultiBufferSource buffers,float amount,float absorption,int overlay){
        if(amount<=0)return;
        var fluid=new FluidStack(AlchemyMaterials.REFINED.source.get(),1);
        var ext=IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite=Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int color=ext.getTintColor(fluid),alpha=color>>>24;if(alpha==0)alpha=255;
        float progress=Math.min(1,amount/10000),scale=1-Math.min(1,absorption/40);
        if(scale<=0)return;
        float radius=2.2F*scale,width=2/16F*scale,low=.55F+(1-scale)*.4F,high=low+1/16F*scale;
        double end=progress*Math.PI*2;
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(int i=0;i<64 && i*Math.PI/32<end;i++){
            double a=i*Math.PI/32,b=Math.min(end,(i+1)*Math.PI/32);
            float[][] v=new float[8][3];
            double[] angles={a,a,b,b};float[] radii={radius-width,radius+width,radius+width,radius-width};
            for(int k=0;k<8;k++)v[k]=new float[]{.5F+(float)Math.sin(angles[k%4])*radii[k%4],k<4?low:high,.5F-(float)Math.cos(angles[k%4])*radii[k%4]};
            var faces=new java.util.ArrayList<int[]>(java.util.List.of(new int[]{0,1,2,3},new int[]{7,6,5,4},new int[]{1,5,6,2},new int[]{3,7,4,0}));
            if(progress<1 && i==0)faces.add(new int[]{0,4,5,1});
            if(progress<1 && b==end)faces.add(new int[]{2,6,7,3});
            for(var face:faces){
                var u=new org.joml.Vector3f(v[face[1]]).sub(new org.joml.Vector3f(v[face[0]]));
                var n=u.cross(new org.joml.Vector3f(v[face[2]]).sub(new org.joml.Vector3f(v[face[0]]))).normalize();
                for(int k:face){var p=v[k];out.addVertex(pose.last(),p[0],p[1],p[2]).setColor(color>>16&255,color>>8&255,color&255,alpha)
                        .setUv(sprite.getU((p[0]%1+1)%1),sprite.getV((p[2]%1+1)%1)).setOverlay(overlay)
                        .setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(),n.x,n.y,n.z);}
            }
        }
    }
}
