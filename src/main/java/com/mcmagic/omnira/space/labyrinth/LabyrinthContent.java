package com.mcmagic.omnira.space.labyrinth;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class LabyrinthContent {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:topological_labyrinth"));
    public static final DeferredBlock<VoidPortalBlock> PORTAL=ModBlocks.BLOCKS.register("void_portal",()->new VoidPortalBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noLootTable().noOcclusion().lightLevel(s->8).forceSolidOn()));
    public static final DeferredItem<BlockItem> PORTAL_ITEM=ModItems.ITEMS.registerSimpleBlockItem(PORTAL);
    public static final DeferredBlock<VoidPortalBlock> LONG_RANGE_PORTAL=ModBlocks.BLOCKS.register("long_range_portal",()->new VoidPortalBlock(BlockBehaviour.Properties.of().strength(-1,3600000).noLootTable().noOcclusion().lightLevel(s->8).forceSolidOn()));
    public static final DeferredItem<BlockItem> LONG_RANGE_PORTAL_ITEM=ModItems.ITEMS.registerSimpleBlockItem(LONG_RANGE_PORTAL);
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<VoidPortalEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("void_portal",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(VoidPortalEntity::new,PORTAL.get(),LONG_RANGE_PORTAL.get()).build(null));
    public static final DeferredItem<Item> COMPASS=ModItems.ITEMS.registerSimpleItem("mapping_stability_compass",new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static void init(){}
    private LabyrinthContent(){}
}
