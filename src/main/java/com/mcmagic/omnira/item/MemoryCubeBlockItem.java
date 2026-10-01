package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.MemoryCubeBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class MemoryCubeBlockItem extends BlockItem {
    private static final String STATE="OmniraMemoryPeaceful";
    public MemoryCubeBlockItem(Block block,Properties properties){super(block,properties);}
    public static boolean peaceful(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag().getBoolean(STATE);}
    public static ItemStack withState(ItemStack stack,boolean peaceful){
        var tag=stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();
        tag.putBoolean(STATE,peaceful);stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
    @Override protected BlockState getPlacementState(BlockPlaceContext context){
        var state=super.getPlacementState(context);
        return state==null?null:state.setValue(MemoryCubeBlock.PEACEFUL,peaceful(context.getItemInHand()));
    }
    @Override public Component getName(ItemStack stack){
        return Component.translatable(peaceful(stack)?"block.omnira.memory_cube.peaceful":"block.omnira.memory_cube.corrupted");
    }
}
