package com.mcmagic.omnira.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;

/** Pixel-scale clothing with a fixed rounded skirt independent of the legs. */
public final class CrystalDressModel extends HumanoidModel<LivingEntity> {
    public static final float SKIRT_LENGTH=9.5F;
    public CrystalDressModel(EquipmentSlot slot){super(layer(slot).bakeRoot());}
    @Override public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack pose,com.mojang.blaze3d.vertex.VertexConsumer vertices,int light,int overlay,int color){
        if(body.hasChild("skirt")){
            body.getChild("skirt").visible=!riding;
            body.getChild("seated_skirt").visible=riding;
        }
        super.renderToBuffer(pose,vertices,light,overlay,color);
    }
    private static CubeListBuilder cube(int u,int v,float x,float y,float z,float w,float h,float d){
        return CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,w,h,d);
    }
    private static boolean seatedFootprint(double x,double z,double width,double front,double back){
        double corner=1.2;
        double flare=Math.clamp((2-z)/(2-front),0,1);
        double halfWidth=width+4.1*flare;
        double dx=Math.max(0,Math.abs(x)-(halfWidth-corner));
        double dz=Math.max(0,Math.max(front+corner-z,z-(back-corner)));
        return dx*dx+dz*dz<=corner*corner;
    }
    public static LayerDefinition layer(EquipmentSlot slot){
        var mesh=HumanoidModel.createMesh(new CubeDeformation(0),0);
        var root=mesh.getRoot();
        for(String name:new String[]{"head","hat","body","right_arm","left_arm","right_leg","left_leg"})
            root.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.ZERO);
        if(slot==EquipmentSlot.HEAD){
            var head=root.addOrReplaceChild("head",cube(0,0,-5,-11,-5,10,3,10),PartPose.ZERO);
            head.addOrReplaceChild("brim",cube(0,16,-7,-8.25F,-5,14,1,10)
                    .texOffs(0,28).addBox(-5,-8.25F,-7,10,1,2).addBox(-5,-8.25F,5,10,1,2),PartPose.ZERO);
            head.addOrReplaceChild("ribbon",cube(0,48,-5.1F,-9,-5.1F,10.2F,1,10.2F),PartPose.ZERO);
            head.addOrReplaceChild("crystal",cube(48,48,-1,-9.5F,-5.4F,2,2,1),PartPose.ZERO);
        } else if(slot==EquipmentSlot.CHEST){
            root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16)
                    .addBox(-4,0,-2,8,12,4,new CubeDeformation(.55F))
                    .texOffs(0,48).addBox(-4.65F,10,-2.65F,9.3F,2.6F,5.3F),PartPose.ZERO);
            for(int sign:new int[]{-1,1}){
                var sleeve=CubeListBuilder.create().texOffs(40,16).addBox(sign<0?-3.5F:-1.5F,-2.5F,-2.5F,5,5,5,new CubeDeformation(.2F))
                        .texOffs(40,28).addBox(sign<0?-3:-1,2.5F,-2,4,1,4);
                root.addOrReplaceChild(sign<0?"right_arm":"left_arm",sleeve,PartPose.offset(sign*5,2,0));
            }
        } else if(slot==EquipmentSlot.LEGS){
            var body=root.addOrReplaceChild("body",cube(0,48,-4.7F,7,-2.7F,9.4F,1.6F,5.4F),PartPose.ZERO);
            var skirt=CubeListBuilder.create();
            // Fixed, hollow pixel bands. Never follow the individual leg animations.
            int rows=Math.round(SKIRT_LENGTH*2);
            for(int row=0;row<rows;row++){
                double t=(row+.5)/rows,rx=4.7+4.3*t,rz=2.7+6.3*t;
                for(int x=-9;x<9;x++)for(int z=-9;z<9;z++){
                    double px=x+.5,pz=z+.5;
                    if(px*px/(rx*rx)+pz*pz/(rz*rz)>1)continue;
                    if(px*px/((rx-1.2)*(rx-1.2))+pz*pz/((rz-1.2)*(rz-1.2))<1)continue;
                    skirt.texOffs(row==rows-1?0:Math.floorMod(x+z,8)*2,row==rows-1?48:32)
                            .addBox(x,row*.5F,z,1,.5F,1);
                }
            }
            body.addOrReplaceChild("skirt",skirt,PartPose.offset(0,8,0));
            var seated=CubeListBuilder.create();
            // Vanilla riding legs project forward from y=12 to about z=-12; cover both thighs as one drape.
            for(int row=0;row<9;row++){
                double width=4.8+Math.min(row,5)*.14;
                double front=row==0?-3.2:row==1?-11:-12;
                double back=3.2+Math.min(row,4)*.2;
                for(int x=-10;x<10;x++)for(int z=-13;z<5;z++){
                    double px=x+.5,pz=z+.5;
                    if(!seatedFootprint(px,pz,width,front,back))continue;
                    boolean edge=!seatedFootprint(px-1,pz,width,front,back)
                            ||!seatedFootprint(px+1,pz,width,front,back)
                            ||!seatedFootprint(px,pz-1,width,front,back)
                            ||!seatedFootprint(px,pz+1,width,front,back);
                    if(row!=1 && !edge)continue;
                    seated.texOffs(row==8?0:Math.floorMod(x+z,8)*2,row==8?48:32)
                            .addBox(x,row,z,1,1,1);
                }
            }
            body.addOrReplaceChild("seated_skirt",seated,PartPose.offset(0,8,0));
        } else if(slot==EquipmentSlot.FEET){
            for(int sign:new int[]{-1,1})root.addOrReplaceChild(sign<0?"right_leg":"left_leg",
                    CubeListBuilder.create().texOffs(0,0).addBox(-2,4,-2,4,6,4,new CubeDeformation(.12F))
                            .texOffs(40,48).addBox(-2.2F,10,-2.6F,4.4F,2.1F,4.8F),PartPose.offset(sign*1.9F,12,0));
        }
        return LayerDefinition.create(mesh,64,64);
    }
}
