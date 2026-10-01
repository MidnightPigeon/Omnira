package com.mcmagic.omnira.time;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.mcmagic.omnira.item.MemoryCubeBlockItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Only explicitly curated, equal-unit item groups can cross timelines. */
public final class SundialTimeline {
    private static final String[] GROUPS={"elemental_microcores","ancient_remains","crystal_materials","crystal_blocks","temporal_silts",
            "time_eroded_blocks","mirror_blocks","void_mirror_blocks","shadow_rock_blocks",
            "time_eroded_stairs","time_eroded_slab","time_eroded_wall",
            "mirror_stairs","mirror_slab","void_mirror_stairs","void_mirror_slab",
            "shadow_rock_stairs","shadow_rock_slab",
            "wood_logs","wood_wood","wood_stripped_logs","wood_stripped_wood","wood_planks",
            "wood_stairs","wood_slabs","wood_fences","wood_fence_gates","wood_doors",
            "wood_trapdoors","wood_pressure_plates","wood_buttons","orb_blocks","heads","casings","memory_rewards"};
    private static final List<TagKey<Item>> TAGS=java.util.Arrays.stream(GROUPS).map(name->TagKey.create(
            Registries.ITEM,ResourceLocation.fromNamespaceAndPath("omnira","timeline/"+name))).toList();
    private static final TagKey<Item> INGOTS=TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath("c","ingots"));

    public static List<ItemStack> choices(ItemStack input){
        if(input.isEmpty())return List.of();
        if(input.is(ModItems.MEMORY_CUBE.get())){
            var plain=new ItemStack(ModItems.MEMORY_CUBE.get());
            var peaceful=MemoryCubeBlockItem.withState(plain.copy(),true);
            var corrupted=MemoryCubeBlockItem.withState(plain.copy(),false);
            if(ItemStack.isSameItemSameComponents(input,plain)||ItemStack.isSameItemSameComponents(input,corrupted))
                return List.of(peaceful);
            if(ItemStack.isSameItemSameComponents(input,peaceful))return List.of(corrupted);
            return List.of();
        }
        if(!ItemStack.isSameItemSameComponents(input,new ItemStack(input.getItem())))return List.of();
        var result=new ArrayList<ItemStack>();
        for(var tag:java.util.stream.Stream.concat(TAGS.stream(),java.util.stream.Stream.of(INGOTS)).toList()){
            if(!input.is(tag))continue;
            BuiltInRegistries.ITEM.stream().filter(item->item!=input.getItem()&&new ItemStack(item).is(tag)
                    &&result.stream().noneMatch(stack->stack.is(item))).map(ItemStack::new).forEach(result::add);
        }
        result.sort(Comparator.comparing(stack->BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        return result.stream().map(ItemStack::copy).toList();
    }
    private SundialTimeline(){}
}
