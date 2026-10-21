package com.mcmagic.omnira.codex;

import com.mcmagic.omnira.item.MultiblockPlacementItem;
import com.mcmagic.omnira.item.bottle.PocketBottleItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.capabilities.Capabilities;

/** One read-only policy for formula slots, execution and persisted output plans. */
public final class CodexMaterials {
    public static final TagKey<Item> ITEM_OUTPUTS=TagKey.create(Registries.ITEM,ResourceLocation.parse("omnira:codex_item_outputs"));
    public static boolean itemOutput(ItemStack stack){return stack.getItem() instanceof MultiblockPlacementItem||stack.is(ITEM_OUTPUTS);}
    public static boolean allowed(ItemStack stack){
        return stack.isEmpty()||!stack.is(MyriadCodexContent.ITEM.get())&&!stack.is(MyriadCodexContent.HEART_ITEM.get())&&!hasContents(stack);
    }
    public static boolean hasContents(ItemStack stack){
        if(stack.isEmpty())return false;
        var staff=stack.get(com.mcmagic.omnira.registry.ModDataComponents.STAFF_ASSEMBLY);
        if(staff!=null&&!staff.upgrades().isEmpty()||com.mcmagic.omnira.item.ArquebusPlugin.of(stack)!=com.mcmagic.omnira.item.ArquebusPlugin.NONE)return true;
        if(PocketBottleItem.restricted(stack)||stack.has(DataComponents.CONTAINER_LOOT)||stack.has(DataComponents.ENTITY_DATA)
                ||stack.has(DataComponents.BUCKET_ENTITY_DATA))return true;
        var container=stack.get(DataComponents.CONTAINER);
        if(container!=null&&container.stream().anyMatch(s->!s.isEmpty()))return true;
        var bundle=stack.get(DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null&&!bundle.isEmpty())return true;
        var projectiles=stack.get(DataComponents.CHARGED_PROJECTILES);
        if(projectiles!=null&&!projectiles.isEmpty())return true;
        var bees=stack.get(DataComponents.BEES);
        if(bees!=null&&!bees.isEmpty())return true;
        var items=stack.getCapability(Capabilities.ItemHandler.ITEM);
        if(items!=null)for(int i=0;i<items.getSlots();i++)if(!items.getStackInSlot(i).isEmpty())return true;
        var fluids=stack.getCapability(Capabilities.FluidHandler.ITEM);
        if(fluids!=null)for(int i=0;i<fluids.getTanks();i++)if(!fluids.getFluidInTank(i).isEmpty())return true;
        for(var type:java.util.List.of(DataComponents.BLOCK_ENTITY_DATA,DataComponents.CUSTOM_DATA)){
            var data=stack.get(type);if(data!=null&&storedContents(data.getUnsafe()))return true;
        }
        return false;
    }
    private static boolean storedContents(Tag tag){
        if(tag instanceof CompoundTag data){
            if(data.contains("LootTable",Tag.TAG_STRING))return true;
            boolean item=data.contains("id",Tag.TAG_STRING)&&(data.getInt("count")>0||data.getInt("Count")>0);
            boolean fluid=(data.contains("id",Tag.TAG_STRING)||data.contains("FluidName",Tag.TAG_STRING)||data.contains("fluid",Tag.TAG_STRING))
                    &&(data.getInt("amount")>0||data.getInt("Amount")>0);
            if(item||fluid)return true;
            for(String key:data.getAllKeys())if(storedContents(data.get(key)))return true;
        }else if(tag instanceof ListTag list)for(Tag child:list)if(storedContents(child))return true;
        return false;
    }
    private CodexMaterials(){}
}
