package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class StarlightContent {
    public static final DeferredBlock<StarlightBlock> SOURCE = ModBlocks.BLOCKS.register("star_source", () -> new StarlightBlock(true));
    public static final DeferredBlock<StarlightBlock> DUST = ModBlocks.BLOCKS.register("stardust", () -> new StarlightBlock(false));
    public static final DeferredItem<BlockItem> SOURCE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SOURCE);
    // Harvestable material, deliberately not a BlockItem.
    public static final DeferredItem<Item> DUST_ITEM = ModItems.ITEMS.registerSimpleItem("stardust", new Item.Properties());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StarlightEntity>> ENTITY = ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(
            "starlight", () -> BlockEntityType.Builder.of(StarlightEntity::new, SOURCE.get(), DUST.get()).build(null));
    public static void init() {}
    private StarlightContent() {}
}
