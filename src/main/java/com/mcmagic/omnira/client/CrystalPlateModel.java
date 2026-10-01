package com.mcmagic.omnira.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.*;

public final class CrystalPlateModel extends HumanoidModel<LivingEntity> {
    public CrystalPlateModel(EquipmentSlot slot){super(LayerDefinition.create(
            HumanoidModel.createMesh(new CubeDeformation(slot==EquipmentSlot.LEGS?.85F:1.1F),0),64,32).bakeRoot());}
}
