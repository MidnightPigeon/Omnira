package com.mcmagic.omnira.item.bottle;

import net.minecraft.nbt.CompoundTag;

/** Sable's section keys are array indices, not absolute section Y coordinates. */
public final class StructureSections {
    private StructureSections() {}

    public static void remap(CompoundTag ship,int sourceMinSection,int targetMinSection,int targetSections) {
        var chunks=ship.getCompound("plot").getCompound("chunks");
        for(String key:chunks.getAllKeys()) {
            var chunk=chunks.getCompound(key);
            var old=chunk.getCompound("sections");
            var sections=new CompoundTag();
            for(String index:old.getAllKeys()) {
                int target=Integer.parseInt(index)+sourceMinSection-targetMinSection;
                if(target<0 || target>=targetSections) throw new IllegalArgumentException("Structure exceeds destination storage height");
                sections.put(Integer.toString(target),old.get(index).copy());
            }
            chunk.put("sections",sections);
        }
    }

    public static boolean containsPivot(CompoundTag ship,int minSection) {
        double pivot=ship.getCompound("pose").getCompound("rotation_point").getDouble("y");
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        var chunks=ship.getCompound("plot").getCompound("chunks");
        for(String key:chunks.getAllKeys()) for(String index:chunks.getCompound(key).getCompound("sections").getAllKeys()) {
            int section=Integer.parseInt(index)+minSection;
            low=Math.min(low,section*16);high=Math.max(high,(section+1)*16);
        }
        return pivot>=low && pivot<=high;
    }
}
