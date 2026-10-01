package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class SundialContent {
    public static final DeferredBlock<SundialBlock> BLOCK=ModBlocks.BLOCKS.register("split_time_sundial",
            ()->new SundialBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion().lightLevel(state->8).sound(SoundType.AMETHYST)));
    public static final DeferredItem<BlockItem> ITEM=ModItems.ITEMS.registerSimpleBlockItem(BLOCK);
    public static final java.util.function.Supplier<BlockEntityType<SundialBlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES
            .register("split_time_sundial",()->BlockEntityType.Builder.of(SundialBlockEntity::new,BLOCK.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<SundialMenu>> MENU=ModMenuTypes.MENU_TYPES
            .register("split_time_sundial",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(SundialMenu::new));
    public static void init(){DreamContent.ITEMS.add(ITEM);}
    private SundialContent(){}
}
