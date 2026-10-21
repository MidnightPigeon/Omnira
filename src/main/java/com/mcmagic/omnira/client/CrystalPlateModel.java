package com.mcmagic.omnira.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.*;
import java.util.*;

public final class CrystalPlateModel extends HumanoidModel<LivingEntity> {
    public CrystalPlateModel(EquipmentSlot slot){super(root(slot));}
    private static ModelPart root(EquipmentSlot slot){
        var parts=new LinkedHashMap<String,ModelPart>();
        for(var name:List.of("head","hat","body","right_arm","left_arm","right_leg","left_leg"))parts.put(name,new ModelPart(List.of(),Map.of()));
        if(slot==EquipmentSlot.HEAD){
            var m=new CrystalPlateMesh().skin(-4,-8,-4,8,8,8).clearance(1F/16);
            m.box(0,-5,-9,-5,10,1,10).box(0,-5,-8,-4,1,8,8).box(0,4,-8,-4,1,8,8)
                    .box(0,-4,-8,4,8,8,1).box(0,-4,-8,-5,8,2,1)
                    .box(0,-5,-6,-5,2,6,1).box(0,3,-6,-5,2,6,1)
                    .box(1,-4,-8,-5,8,1,1).box(2,-1,-8,-5,2,1,1)
                    .box(1,-1,-9,-3,2,1,6).box(2,-1,-5,4,2,2,1);
            parts.put("head",m.bake(0,0,0));
        }else if(slot==EquipmentSlot.CHEST){
            var body=new CrystalPlateMesh().skin(-4,0,-2,8,12,4).seamY(3,1F/16).seamY(7,-1F/16);
            body.box(0,-4,1,-4,8,6,1).box(0,-4,1,3,8,6,1)
                    .box(0,-5,3,-3,1,4,6).box(0,4,3,-3,1,4,6)
                    .box(1,-3,0,-3,6,1,1).box(1,-3,0,2,6,1,1)
                    .box(1,-4,6,-4,8,1,1).box(1,-4,6,3,8,1,1)
                    .box(1,-1,2,-4,2,4,1).box(2,-1,3,-4,2,2,1)
                    .box(1,-1,2,3,2,4,1)
                    .box(0,-4,8,-3,8,2,1).box(0,-4,8,2,8,2,1)
                    .box(0,-4,8,-2,1,2,4).box(0,3,8,-2,1,2,4);
            parts.put("body",body.bake(0,0,0));
            for(int sign:new int[]{-1,1}){
                var arm=new CrystalPlateMesh().skin(sign<0?-3:-1,-2,-2,4,12,4);int left=sign<0?-4:-1,fore=sign<0?-3:-1;
                arm.box(0,left,-3,-3,5,1,6)
                        .box(0,left,-2,-3,5,3,1).box(0,left,-2,2,5,3,1)
                        .box(0,sign<0?-4:3,-2,-2,1,4,4)
                        .box(1,left,0,-3,5,1,1).box(2,sign<0?-3:1,-2,-3,2,1,1)
                        .box(0,fore,5,-3,4,4,1).box(0,fore,5,2,4,4,1)
                        .box(0,sign<0?-4:3,5,-2,1,4,4)
                        .box(1,fore,8,-3,4,1,1).box(2,sign<0?-2:0,6,-3,2,1,1);
                parts.put(sign<0?"right_arm":"left_arm",arm.bake(sign*5,2,0));
            }
        }else if(slot==EquipmentSlot.LEGS){
            var belt=new CrystalPlateMesh().skin(-4,0,-2,8,12,4).seamY(10,1F/16).seamY(12,-1F/16);
            belt.box(1,-4,10,-3,8,2,1).box(1,-4,10,2,8,2,1)
                    .box(1,-5,10,-2,1,2,4).box(1,4,10,-2,1,2,4).box(2,-1,10,-3,2,1,1);
            parts.put("body",belt.bake(0,0,0));
            for(int sign:new int[]{-1,1}){
                var leg=new CrystalPlateMesh().skin(-2,0,-2,4,12,4).leg(sign);
                leg.box(0,-2,0,-3,4,2,1).box(0,-2,0,2,4,2,1)
                        .box(0,-2,3,-3,4,2,1).box(0,-2,3,2,4,2,1)
                        .box(0,sign<0?-3:2,0,-2,1,5,4)
                        .box(1,-2,6,-3,4,2,1).box(2,-1,6,-3,2,1,1)
                        .box(0,sign<0?-3:2,6,-2,1,2,4);
                parts.put(sign<0?"right_leg":"left_leg",leg.bake(sign*1.9F,12,0));
            }
        }else if(slot==EquipmentSlot.FEET){
            for(int sign:new int[]{-1,1}){
                var boot=new CrystalPlateMesh().skin(-2,0,-2,4,12,4).leg(sign).seamY(8,1F/16);
                boot.box(0,-2,9,-3,4,2,1).box(0,-2,9,2,4,2,1)
                        .box(0,sign<0?-3:-2,9,-2,1,2,4).box(0,sign<0?1:2,9,-2,1,2,4)
                        .box(0,-2,10,-4,4,2,1).box(1,-2,11,-3,4,1,6)
                        .box(1,sign<0?-3:-2,8,-2,1,1,4).box(1,sign<0?1:2,8,-2,1,1,4)
                        .box(2,-1,9,-3,2,1,1);
                parts.put(sign<0?"right_leg":"left_leg",boot.bake(sign*1.9F,12,0));
            }
        }
        return new ModelPart(List.of(),parts);
    }
}
