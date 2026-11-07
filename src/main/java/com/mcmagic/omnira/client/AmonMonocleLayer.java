package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Forced visible curse ornament, independent of equipment and Curios cosmetic toggles. */
@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class AmonMonocleLayer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/entity/amon_monocle.png");
    private final ModelPart ring;
    public AmonMonocleLayer(PlayerRenderer renderer){
        super(renderer);var mesh=new MeshDefinition();
        // The worn centre is completely clear: only the frame and chain have geometry.
        var cubes=CubeListBuilder.create().texOffs(0,0)
                .addBox(.5F,-5.5F,-4.85F,3, .5F,.5F).addBox(.5F,-3F,-4.85F,3,.5F,.5F)
                .addBox(0,-5F,-4.85F,.5F,2,.5F).addBox(3.5F,-5F,-4.85F,.5F,2,.5F)
                .addBox(0,-5.5F,-4.85F,.5F,.5F,.5F).addBox(3.5F,-5.5F,-4.85F,.5F,.5F,.5F)
                .addBox(0,-3F,-4.85F,.5F,.5F,.5F).addBox(3.5F,-3F,-4.85F,.5F,.5F,.5F)
                .addBox(3.75F,-3,-4.75F,.25F,5,.25F);
        mesh.getRoot().addOrReplaceChild("monocle",cubes,PartPose.ZERO);ring=LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event){
        for(var skin:event.getSkins()){PlayerRenderer renderer=event.getSkin(skin);if(renderer!=null)renderer.addLayer(new AmonMonocleLayer(renderer));}
    }
    @Override public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,float limb,float amount,float partial,float age,float yaw,float pitch){
        if(player.isInvisible()||player.isSpectator())return;
        boolean cursed=top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).flatMap(i->i.getStacksHandler("curse"))
                .map(h->{for(int i=0;i<h.getSlots();i++)if(h.getStacks().getStackInSlot(i).is(ModItems.AMON_GAZE.get()))return true;return false;}).orElse(false);
        if(!cursed)return;
        pose.pushPose();getParentModel().head.translateAndRotate(pose);
        ring.render(pose,buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),light,OverlayTexture.NO_OVERLAY);pose.popPose();
    }
}
