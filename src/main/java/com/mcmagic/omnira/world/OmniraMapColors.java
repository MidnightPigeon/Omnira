package com.mcmagic.omnira.world;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Fallback palette for custom materials which did not declare a vanilla map colour. */
public final class OmniraMapColors {
    public static MapColor fallback(BlockState state){
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if(!id.getNamespace().equals("omnira")||state.isAir())return MapColor.NONE;
        String name=id.getPath();
        if(name.contains("silt"))return name.contains("living")?MapColor.TERRACOTTA_GREEN:name.contains("rotted")?MapColor.TERRACOTTA_BROWN:MapColor.TERRACOTTA_GRAY;
        if(name.contains("sand"))return MapColor.SAND;
        if(name.contains("shadow")||name.contains("void")||name.contains("dense"))return MapColor.COLOR_PURPLE;
        if(name.contains("dream"))return MapColor.COLOR_PURPLE;
        if(name.contains("deflection"))return MapColor.COLOR_PINK;
        if(name.contains("mirror")||name.contains("brick"))return MapColor.STONE;
        if(name.contains("time")||name.contains("temporal"))return MapColor.COLOR_GREEN;
        if(name.contains("infused"))return MapColor.SNOW;
        return MapColor.COLOR_LIGHT_BLUE;
    }
    private OmniraMapColors(){}
}
