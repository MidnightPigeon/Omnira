package com.mcmagic.omnira.space.labyrinth;

import net.minecraft.core.*;
import net.minecraft.resources.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Seed unopened containers while generating, never regenerate already opened treasure. */
public final class LabyrinthDecoration {
    public static void configure(BlockEntity be,BlockPos pos){
        if(be instanceof com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity liquid)liquid.markFluidTreasure();
        else if(be instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball)
            ball.setLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse("omnira:chests/topological_labyrinth")),net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(pos.asLong()));
        else if(be instanceof net.minecraft.world.level.block.entity.BrushableBlockEntity sand)
            sand.setLootTable(ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.parse("omnira:archaeology/topological_labyrinth")),net.minecraft.world.level.levelgen.RandomSupport.mixStafford13(pos.asLong()));
        else if(be instanceof com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity pedestal)
            pedestal.setItem(0,new net.minecraft.world.item.ItemStack(LabyrinthTerrain.plan(LabyrinthTerrain.room(pos)).kind()==6
                    ?com.mcmagic.omnira.registry.ModBlocks.NIGHT_HERON_STATUE.get():com.mcmagic.omnira.registry.ModItems.CRUISE_ORB.get()));
    }
    private LabyrinthDecoration(){}
}
