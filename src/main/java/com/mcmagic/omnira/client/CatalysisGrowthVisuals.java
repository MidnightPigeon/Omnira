package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.catalysis.*;
import com.mcmagic.omnira.client.renderer.*;
import com.mcmagic.omnira.paradox.ParadoxContent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;

public final class CatalysisGrowthVisuals {
    public static boolean sealed(ItemStack stack){return stack.is(ParadoxContent.STABLE_SOURCE.get())||stack.is(ParadoxContent.STABLE_AGGREGATE.get());}
    public static void sealedGrowth(CatalysisBlockEntity be,double time,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,1.38,.5);pose.scale(.65F,.65F,.65F);
        ParadoxSourceRenderer.core(be.items.getStackInSlot(0),time,pose,buffers,light,overlay);pose.popPose();
        boolean genesis=be.items.getStackInSlot(0).is(ParadoxContent.STABLE_AGGREGATE.get());
        if(!be.items.getStackInSlot(1).is(genesis?ParadoxContent.GENESIS.get():com.mcmagic.omnira.registry.ModItems.PARADOX_DUST.get()))return;
        float growth=(be.visualStage(time)-1)/3F;
        int grains=Math.max(1,(int)(1+growth*15));
        for(int i=0;i<grains;i++){
            double angle=i*2.39996;
            pose.pushPose();pose.translate(.5+Math.cos(angle)*.035*Math.sqrt(i),1.51+(i/5)*.035,.5+Math.sin(angle)*.035*Math.sqrt(i));
            float size=.3F+growth*.45F;pose.scale(size,size,size);pose.translate(-.5,-.5,-.5);
            var mc=Minecraft.getInstance();mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(ParadoxMaterialRenderer.model(genesis?"genesis_grain":"grain")),
                    ItemStack.EMPTY,LightTexture.FULL_BRIGHT,overlay,pose,CrystalGlassLayer.contents(buffers).getBuffer(Sheets.cutoutBlockSheet()));pose.popPose();
        }
    }
    public static void tree(CatalysisGrowth.Plan plan,float stage,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        var sapling=plan.plant();var id=BuiltInRegistries.BLOCK.getKey(sapling.getBlock());String family=id.getPath().replace("_sapling","").replace("_propagule","");
        String leaves=family,logs=family;
        if(family.equals("azalea")||family.equals("flowering_azalea"))logs="oak";
        var wood=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),logs+"_log"));
        var canopy=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),leaves+"_leaves"));
        if(stage<2||wood==Blocks.AIR||canopy==Blocks.AIR){block(sapling,.5,1.22,.5,.34F,.34F,.34F,pose,buffers,light,overlay);return;}
        float fraction=Math.clamp((stage-1)/Math.max(1,plan.stages()-1),0,1),size=.42F+.42F*fraction;
        float cell=size/5;
        boolean conifer=family.equals("spruce");
        // Every miniature block is uniformly scaled: bark and foliage retain the same pixel density.
        for(int y=0;y<6;y++){
            int radius=y<2?-1:y==5?0:conifer?(y==2?2:1):y==4?1:2;
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
                boolean trunk=x==0&&z==0&&y<4;
                boolean leaf=radius>=0&&Math.abs(x)<=radius&&Math.abs(z)<=radius
                        &&(radius<2||Math.abs(x)+Math.abs(z)<4);
                if(!trunk&&!leaf)continue;
                block((trunk?wood:canopy).defaultBlockState(),.5+x*cell,1.22+y*cell,.5+z*cell,
                        cell,cell,cell,pose,buffers,light,overlay);
            }
        }
    }
    private static void block(net.minecraft.world.level.block.state.BlockState state,double x,double y,double z,float sx,float sy,float sz,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(x-sx/2,y,z-sz/2);pose.scale(sx,sy,sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,CrystalGlassLayer.contents(buffers),light,overlay);pose.popPose();
    }
    private CatalysisGrowthVisuals(){}
}
