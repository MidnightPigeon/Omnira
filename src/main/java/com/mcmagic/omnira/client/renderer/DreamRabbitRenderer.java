package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** A complete voxel head beneath a separate hat; the sigil adds circulating dream motes. */
public final class DreamRabbitRenderer extends BlockEntityWithoutLevelRenderer {
    public DreamRabbitRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    public static ModelResourceLocation model(String part){return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("omnira","item/dream_rabbit/"+part));}
    @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        boolean sigil=stack.is(ModItems.ALICE_DREAM_RABBIT_SIGIL.get());
        var mc=Minecraft.getInstance();double time=mc.level==null?net.minecraft.Util.getMillis()/50.0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false);
        ParticleModelQuads.tinted(ParticleModelQuads.collect(mc.getModelManager().getModel(model("head"))),pose,
                buffers.getBuffer(CrystalGlassLayer.ATLAS),LightTexture.FULL_BRIGHT,overlay,sigil?0xB9A8F0:0xD5D1E4,sigil?.78F:.42F);
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model("hat")),stack,light,overlay,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
        mc.getItemRenderer().renderModelLists(mc.getModelManager().getModel(model("eyes")),stack,LightTexture.FULL_BRIGHT,overlay,pose,buffers.getBuffer(Sheets.cutoutBlockSheet()));
        if(!sigil)return;
        var grains=ParticleModelQuads.collect(mc.getModelManager().getModel(model("grain")));
        var out=buffers.getBuffer(CrystalGlassLayer.ATLAS);
        for(int i=0;i<7;i++){
            double angle=time*.08+i*Math.PI*2/7;
            double y=.3+.28*Math.sin(angle*.8+i);
            pose.pushPose();pose.translate(.5+Math.cos(angle)*.43,y,.5+Math.sin(angle)*.43);
            ParticleModelQuads.tinted(grains,pose,out,LightTexture.FULL_BRIGHT,overlay,i%2==0?0xA8C5FF:0xCC9DEC,.75F);
            pose.popPose();
        }
    }
}
