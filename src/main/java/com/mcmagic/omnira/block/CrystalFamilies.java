package com.mcmagic.omnira.block;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.space.geodes.DeflectionContent;
import net.minecraft.world.level.block.*;

/** Matrix followed by its four growth stages, shared by cultivation and direct fertilization. */
public final class CrystalFamilies {
    public static Block[][] all(){return new Block[][]{
            {Blocks.BUDDING_AMETHYST,Blocks.SMALL_AMETHYST_BUD,Blocks.MEDIUM_AMETHYST_BUD,Blocks.LARGE_AMETHYST_BUD,Blocks.AMETHYST_CLUSTER},
            {DreamContent.DREAM_CRYSTAL_BEDROCK.get(),DreamContent.SMALL_DREAM_BUD.get(),DreamContent.MEDIUM_DREAM_BUD.get(),DreamContent.LARGE_DREAM_BUD.get(),DreamContent.DREAM_CRYSTAL.get()},
            {TimeNatureContent.SPATIAL_MATRIX.get(),TimeNatureContent.SMALL_SPATIAL_BUD.get(),TimeNatureContent.MEDIUM_SPATIAL_BUD.get(),TimeNatureContent.LARGE_SPATIAL_BUD.get(),TimeNatureContent.SPATIAL_CLUSTER.get()},
            {DeflectionContent.MATRIX.get(),DeflectionContent.SMALL.get(),DeflectionContent.MEDIUM.get(),DeflectionContent.LARGE.get(),DeflectionContent.CLUSTER.get()}
    };}
    private CrystalFamilies(){}
}
