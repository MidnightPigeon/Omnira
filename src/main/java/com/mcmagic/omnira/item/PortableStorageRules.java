package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class PortableStorageRules {
    private PortableStorageRules(){}
    public static boolean stabilized(ItemStack stack){
        if(stack.isEmpty())return false;
        boolean vehicle=stack.is(ModItems.CRUISE_ORB.get());
        if(!vehicle && !stack.is(ModItems.CRYSTAL_BALL.get()) && !stack.is(ModItems.LIQUID_CRYSTAL_BALL.get()))return false;
        var data=stack.get(vehicle?DataComponents.CUSTOM_DATA:DataComponents.BLOCK_ENTITY_DATA);
        if(data==null)return false;
        // Read only the upgrade list; copying a filled container's entire NBT on every slot check is costly.
        var upgrades=data.getUnsafe().getCompound("Upgrades").getList("Items",10);
        for(int i=0;i<upgrades.size();i++){
            var item=upgrades.getCompound(i);
            if(item.getString("id").equals("omnira:spacetime_stabilization_upgrade")
                    && (!item.contains("count") || item.getInt("count")>0))return true;
        }
        return false;
    }
    public static boolean mayPlace(Container container,ItemStack stack){
        return container instanceof Inventory || container instanceof CrystalPedestalBlockEntity || !PocketBottleItem.restricted(stack);
    }
}
