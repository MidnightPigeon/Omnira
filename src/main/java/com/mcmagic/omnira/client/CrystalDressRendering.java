package com.mcmagic.omnira.client;

import com.mcmagic.omnira.registry.ArmorContent;
import net.minecraft.client.model.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.*;
import java.util.EnumMap;

public final class CrystalDressRendering {
    private CrystalDressRendering() {}
    public static void register(RegisterClientExtensionsEvent event){
        var models=new EnumMap<EquipmentSlot,CrystalDressModel>(EquipmentSlot.class);
        var extension=new IClientItemExtensions(){
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                return models.computeIfAbsent(slot,CrystalDressModel::new);
            }
        };
        ArmorContent.CLOTH.values().forEach(item->event.registerItem(extension,item.get()));
        var plates=new EnumMap<EquipmentSlot,CrystalPlateModel>(EquipmentSlot.class);
        var plateExtension=new IClientItemExtensions(){
            @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity,ItemStack stack,EquipmentSlot slot,HumanoidModel<?> original){
                return plates.computeIfAbsent(slot,CrystalPlateModel::new);
            }
        };
        ArmorContent.PLATE.values().forEach(item->event.registerItem(plateExtension,item.get()));
    }
}
