package com.mcmagic.omnira.reversal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Shared fuel catalogue for processing, JEI and the guide. */
public final class ReversalFuels {
    public record Fuel(String item,int energy){
        public ItemStack stack(){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)));}
    }
    public static List<Fuel> all(boolean spatial){return spatial?SPACE:TIME;}
    public static int value(ItemStack stack,boolean spatial){
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        return (spatial?SPACE_VALUES:TIME_VALUES).getOrDefault(id,0);
    }
    private static final List<Fuel> SPACE=List.of(new Fuel("omnira:rough_spatial_crystal_block",1),
            new Fuel("omnira:excited_rough_spatial_crystal_block",2),new Fuel("omnira:spatial_crystal_shard",4),new Fuel("omnira:deflection_crystal",6));
    private static final List<Fuel> TIME=List.of(new Fuel("omnira:spatial_crystal_shard",1),new Fuel("omnira:time_warp_point",4),
            new Fuel("omnira:reborn_rottenleaf_lily",4),new Fuel("omnira:peaceful_memory_cube",4),new Fuel("omnira:corrupted_memory_cube",4),
            new Fuel("omnira:timeflow_eel",2),new Fuel("omnira:chronal_blindfish",2),new Fuel("omnira:time_microcore",8),new Fuel("omnira:space_microcore",8));
    private static final java.util.Map<String,Integer> SPACE_VALUES=index(SPACE),TIME_VALUES=index(TIME);
    private static java.util.Map<String,Integer> index(List<Fuel> fuels){
        return fuels.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(Fuel::item,Fuel::energy));
    }
    private ReversalFuels(){}
}
