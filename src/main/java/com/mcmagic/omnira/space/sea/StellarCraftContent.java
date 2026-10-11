package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class StellarCraftContent {
    public static final DeferredBlock<Block> ORB=ModBlocks.BLOCKS.register("wandering_star_cluster_orb",()->
        new com.mcmagic.omnira.block.CrystalBallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.CRYSTAL_BALL.get()).lightLevel(s->15)));
    public static final DeferredItem<BlockItem> ORB_ITEM=ModItems.ITEMS.register("wandering_star_cluster_orb",()->
        new com.mcmagic.omnira.item.PortableOrbBlockItem(ORB.get(),new Item.Properties()));
    public static final DeferredItem<DeflectionFieldItem> SHIELD=ModItems.ITEMS.register("deflection_field",()->new DeflectionFieldItem(new Item.Properties().durability(256).rarity(Rarity.UNCOMMON)));
    public static void init(){}
    private StellarCraftContent(){}
}
