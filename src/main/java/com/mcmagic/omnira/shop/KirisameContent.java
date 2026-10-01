package com.mcmagic.omnira.shop;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

public final class KirisameContent {
    public static final DeferredBlock<Block> ORB=ModBlocks.BLOCKS.register("marisa_crystal_ball",
            ()->new MarisaOrbBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(.8F).noOcclusion().lightLevel(s->7)));
    static { DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(ORB)); }
    public static void init(){}
    private KirisameContent(){}
}
