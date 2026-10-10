package com.mcmagic.omnira.space.anchor;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.*;

public final class AnchorContent {
    public static final DeferredBlock<StabilityAnchorBlock> ACTIVE=ModBlocks.BLOCKS.register("spatial_stability_anchor",()->new StabilityAnchorBlock(true));
    public static final DeferredBlock<StabilityAnchorBlock> DORMANT=ModBlocks.BLOCKS.register("dormant_spatial_stability_anchor",()->new StabilityAnchorBlock(false));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<StabilityAnchorEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("spatial_stability_anchor",()->BlockEntityType.Builder.of(StabilityAnchorEntity::new,ACTIVE.get(),DORMANT.get()).build(null));
    public static void init(){ModItems.ITEMS.registerSimpleBlockItem(ACTIVE);ModItems.ITEMS.registerSimpleBlockItem(DORMANT);}
    private AnchorContent(){}
}
