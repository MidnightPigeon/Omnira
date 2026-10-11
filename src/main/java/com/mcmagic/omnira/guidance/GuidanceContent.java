package com.mcmagic.omnira.guidance;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class GuidanceContent {
    public static final DeferredBlock<GuidanceOrbBlock> BLOCK=ModBlocks.BLOCKS.register("guidance_orb",GuidanceOrbBlock::new);
    public static final DeferredItem<BlockItem> ITEM=ModItems.ITEMS.registerSimpleBlockItem(BLOCK);
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<GuidanceOrbEntity>> ENTITY=
            ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("guidance_orb",()->BlockEntityType.Builder.of(GuidanceOrbEntity::new,BLOCK.get()).build(null));
    public static void init(){}
    private GuidanceContent(){}
}
