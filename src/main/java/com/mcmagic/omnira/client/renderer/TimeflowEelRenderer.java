package com.mcmagic.omnira.client.renderer;

import com.mcmagic.omnira.mire.TimeflowEel;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;

public final class TimeflowEelRenderer extends MobRenderer<TimeflowEel,TimeflowEelRenderer.Model> {
    public static final ResourceLocation TEXTURE=ResourceLocation.parse("omnira:textures/entity/timeflow_eel.png");
    public TimeflowEelRenderer(EntityRendererProvider.Context context){super(context,new Model(),.2F);}
    @Override public ResourceLocation getTextureLocation(TimeflowEel eel){return TEXTURE;}
    @Override protected void setupRotations(TimeflowEel eel,PoseStack pose,float age,float yaw,float partial,float scale){
        super.setupRotations(eel,pose,age,yaw,partial,scale);
        if(!eel.isInWater()){pose.translate(.1,.1,0);pose.mulPose(Axis.ZP.rotationDegrees(90));}
    }
    public static final class Model extends EntityModel<TimeflowEel> {
        // x/y/z, width/height/depth, atlas u/v; shared with the offline model preview.
        public static final float[][] BOXES={{-2,-1.5F,-10,4,3,4,0,0},{-1.5F,-1.5F,-6,3,3,8,0,8},{-1,-1,0,2,2,8,24,8},{-.5F,-2,6,1,4,4,24,20}};
        private final ModelPart root,tail;
        public Model(){
            var mesh=new MeshDefinition();var body=mesh.getRoot().addOrReplaceChild("body",cube(0),PartPose.offset(0,22,0));
            // Each cuboid owns its UV rectangle, with a single texel per model unit.
            body.addOrReplaceChild("middle",cube(1),PartPose.ZERO);
            body.addOrReplaceChild("tail",cube(2),PartPose.offset(0,0,2)).addOrReplaceChild("fin",cube(3),PartPose.ZERO);
            root=LayerDefinition.create(mesh,64,32).bakeRoot();tail=root.getChild("body").getChild("tail");
        }
        private static CubeListBuilder cube(int index){var b=BOXES[index];return CubeListBuilder.create().texOffs((int)b[6],(int)b[7]).addBox(b[0],b[1],b[2],b[3],b[4],b[5]);}
        @Override public void setupAnim(TimeflowEel eel,float swing,float amount,float age,float yaw,float pitch){tail.yRot=(float)Math.sin(age*.35F)*(eel.isInWater()?.35F:.6F);}
        @Override public void renderToBuffer(PoseStack pose,VertexConsumer consumer,int light,int overlay,int color){root.render(pose,consumer,light,overlay,color);}
    }
}
