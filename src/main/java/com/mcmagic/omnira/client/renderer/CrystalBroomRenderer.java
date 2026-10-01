package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.vehicle.CrystalBroomEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;

public final class CrystalBroomRenderer extends EntityRenderer<CrystalBroomEntity> {
    public static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath("omnira","textures/entity/crystal_broom.png");
    private static final ResourceLocation SPRITE=ResourceLocation.fromNamespaceAndPath("omnira","entity/crystal_broom");
    private static final ModelPart BODY=model();
    public CrystalBroomRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.35F;}
    public static ModelPart model(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("handle",CubeListBuilder.create().texOffs(0,0).addBox(-1,3,-20,2,2,24),PartPose.ZERO);
        var tail=CubeListBuilder.create();
        // Four faceted tufts surround the rotating crystal, leaving a visible hollow center.
        for(int side=0;side<4;side++){
            for(int step=0;step<4;step++){
                float r=1.5F+step*.5F,z=4+step*2;
                if(side<2)tail.texOffs(0,32).addBox(side==0?-r-1:r,4-r,z,1,r*2,2);
                else tail.texOffs(0,32).addBox(-r,side==2?3-r:4+r,z,r*2,1,2);
            }
        }
        root.addOrReplaceChild("tail",tail,PartPose.ZERO);
        root.addOrReplaceChild("collar",CubeListBuilder.create().texOffs(54,0).addBox(-1.75F,2.25F,3,3.5F,3.5F,1),PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }
    public static void draw(PoseStack pose,MultiBufferSource buffers,int light,float time){
        pose.pushPose();pose.translate(0,.25,.5);pose.mulPose(Axis.ZP.rotationDegrees(time*3));pose.mulPose(Axis.XP.rotationDegrees(90));pose.scale(.6F,.6F,.6F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.ANALYSIS_CRYSTAL.get()),ItemDisplayContext.FIXED,
                LightTexture.FULL_BRIGHT,OverlayTexture.NO_OVERLAY,pose,buffers,Minecraft.getInstance().level,0);pose.popPose();
        // Share sorting with the inner crystal and surrounding atlas-based crystal geometry.
        var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(SPRITE);
        BODY.render(pose,sprite.wrap(buffers.getBuffer(CrystalGlassLayer.ATLAS)),light,OverlayTexture.NO_OVERLAY);
    }
    @Override public void render(CrystalBroomEntity broom,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(-net.minecraft.util.Mth.rotLerp(partial,broom.yRotO,broom.getYRot())));
        draw(pose,VehicleGlassBuffers.wrap(buffers),light,broom.tickCount+partial);pose.popPose();
        super.render(broom,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(CrystalBroomEntity broom){return TEXTURE;}
    public static final class ItemRenderer extends BlockEntityWithoutLevelRenderer {
        public ItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
        @Override public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            pose.pushPose();pose.translate(.5,.3,.5);
            var mc=Minecraft.getInstance();draw(pose,buffers,light,mc.level==null?0:mc.level.getGameTime()+mc.getTimer().getGameTimeDeltaPartialTick(false));pose.popPose();
        }
    }
}
