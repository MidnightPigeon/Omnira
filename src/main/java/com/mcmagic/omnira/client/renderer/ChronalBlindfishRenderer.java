package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.archaeology.ChronalBlindfish;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public final class ChronalBlindfishRenderer extends MobRenderer<ChronalBlindfish,ChronalBlindfishRenderer.Model> {
    public ChronalBlindfishRenderer(EntityRendererProvider.Context context){super(context,new Model(),.16F);}
    @Override public ResourceLocation getTextureLocation(ChronalBlindfish fish){return ResourceLocation.parse("omnira:textures/entity/chronal_blindfish.png");}
    @Override protected int getBlockLightLevel(ChronalBlindfish fish,BlockPos pos){return Math.max(10,super.getBlockLightLevel(fish,pos));}
    @Override protected void setupRotations(ChronalBlindfish fish,PoseStack pose,float age,float yaw,float partial,float scale){
        super.setupRotations(fish,pose,age,yaw,partial,scale);
        if(!fish.isInWater()){pose.translate(.1,.1,0);pose.mulPose(Axis.ZP.rotationDegrees(90));}
    }
    public static final class Model extends EntityModel<ChronalBlindfish> {
        public static final float[][] BOXES={{-2,-2,-5,4,4,8,0,0},{-1.5F,-1.5F,-7,3,3,2,24,0},
                {-1,-1,0,2,2,4,24,6},{-.5F,-3,3,1,6,2,36,0},{-.5F,-3.5F,-2,1,2,4,40,10}};
        private final ModelPart root,tail;
        private static CubeListBuilder cube(int i){var b=BOXES[i];return CubeListBuilder.create().texOffs((int)b[6],(int)b[7]).addBox(b[0],b[1],b[2],b[3],b[4],b[5]);}
        public Model(){
            var mesh=new MeshDefinition();var body=mesh.getRoot().addOrReplaceChild("body",cube(0),PartPose.offset(0,22,0));
            body.addOrReplaceChild("head",cube(1),PartPose.ZERO);
            body.addOrReplaceChild("tail",cube(2),PartPose.offset(0,0,3)).addOrReplaceChild("fin",cube(3),PartPose.ZERO);
            body.addOrReplaceChild("crest",cube(4),PartPose.ZERO);
            root=LayerDefinition.create(mesh,64,32).bakeRoot();tail=root.getChild("body").getChild("tail");
        }
        @Override public void setupAnim(ChronalBlindfish fish,float swing,float amount,float age,float yaw,float pitch){tail.yRot=(float)Math.sin(age*.4F)*.35F;}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color){root.render(pose,consumer,light,overlay,color);}
    }
}
