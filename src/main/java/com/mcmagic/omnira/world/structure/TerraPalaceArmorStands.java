package com.mcmagic.omnira.world.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.resources.ResourceKey;

/** The ten complete, redstone-trimmed golden sets in the player-authored hall. */
public final class TerraPalaceArmorStands {
    private TerraPalaceArmorStands() {}

    static void place(WorldGenLevel level,BoundingBox clip,BoundingBox palace) {
        for(int x:new int[]{15,33})for(int z=18;z<=50;z+=8) {
            var floor=new BlockPos(palace.minX()+x,palace.minY()+2,palace.minZ()+z);
            if(!clip.isInside(floor))continue;
            level.addFreshEntity(create(level.getLevel(),floor,x==15?-90:90));
        }
    }

    public static ArmorStand create(ServerLevel level,BlockPos floor,float yaw) {
        var stand=new ArmorStand(level,floor.getX()+.5,floor.getY(),floor.getZ()+.5);
        stand.moveTo(stand.getX(),stand.getY(),stand.getZ(),yaw,0);
        equip(level,stand,EquipmentSlot.FEET,Items.GOLDEN_BOOTS,TrimPatterns.BOLT);
        equip(level,stand,EquipmentSlot.LEGS,Items.GOLDEN_LEGGINGS,TrimPatterns.DUNE);
        equip(level,stand,EquipmentSlot.CHEST,Items.GOLDEN_CHESTPLATE,TrimPatterns.FLOW);
        equip(level,stand,EquipmentSlot.HEAD,Items.GOLDEN_HELMET,TrimPatterns.SPIRE);
        return stand;
    }

    private static void equip(ServerLevel level,ArmorStand stand,EquipmentSlot slot,Item item,
                              ResourceKey<TrimPattern> pattern) {
        var stack=new ItemStack(item);
        var registries=level.registryAccess();
        stack.set(DataComponents.TRIM,new ArmorTrim(
                registries.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(TrimMaterials.REDSTONE),
                registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(pattern)));
        stand.setItemSlot(slot,stack);
    }
}
