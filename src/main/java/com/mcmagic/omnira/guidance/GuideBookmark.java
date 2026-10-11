package com.mcmagic.omnira.guidance;

import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/** Stored on the actual book, independent of guidance-orb queries. */
public record GuideBookmark(String destination,int page) {
    public GuideBookmark {
        if(!destination.matches("[a-z0-9_\\-]{0,96}"))throw new IllegalArgumentException("Invalid guide destination");
        page=Math.clamp(page,0,8192)&~1;
    }
    public static UUID identity(ItemStack book){
        var tag=book.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!tag.hasUUID("GuideId")){
            tag.putUUID("GuideId",UUID.randomUUID());book.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        }
        return tag.getUUID("GuideId");
    }
    public static GuideBookmark read(ItemStack book){
        var tag=book.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getCompound("GuidePage");
        try{return new GuideBookmark(tag.getString("Target"),tag.getInt("Page"));}
        catch(IllegalArgumentException ignored){return new GuideBookmark("",0);}
    }
    public boolean save(ItemStack book,UUID id){
        var tag=book.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        if(!book.is(com.mcmagic.omnira.registry.ModItems.GUIDE_BOOK.get())||!tag.hasUUID("GuideId")||!tag.getUUID("GuideId").equals(id))return false;
        var position=new CompoundTag();position.putString("Target",destination);position.putInt("Page",page);
        tag.put("GuidePage",position);book.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return true;
    }
}
