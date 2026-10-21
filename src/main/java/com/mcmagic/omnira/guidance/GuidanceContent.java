package com.mcmagic.omnira.guidance;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class GuidanceContent {
    public static final DeferredBlock<GuidanceOrbBlock> BLOCK=ModBlocks.BLOCKS.register("guidance_orb",()->new GuidanceOrbBlock());
    public static final DeferredItem<BlockItem> ITEM=ModItems.ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredBlock<GuidanceOrbBlock> UNDERSTANDING=ModBlocks.BLOCKS.register("universal_understanding",()->new GuidanceOrbBlock(true));
    public static final DeferredItem<BlockItem> UNDERSTANDING_ITEM=ModItems.ITEMS.registerSimpleBlockItem(UNDERSTANDING,new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<GuidanceOrbEntity>> ENTITY=
            ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("guidance_orb",()->BlockEntityType.Builder.of(GuidanceOrbEntity::new,BLOCK.get(),UNDERSTANDING.get()).build(null));
    public static void init(){}
    private GuidanceContent(){}
}
