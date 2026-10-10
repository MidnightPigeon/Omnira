package com.mcmagic.omnira.engine;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class SpacetimeEngineContent {
    public static final DeferredBlock<SpacetimeEngineBlock> BLOCK=ModBlocks.BLOCKS.register("spacetime_engine",()->{
        var p=BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().lightLevel(s->12);
        return net.neoforged.fml.ModList.get().isLoaded("create")
                ?com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics.block(p):new SpacetimeEngineBlock(p);
    });
    public static final DeferredItem<SpacetimeEngineBlock.PlacementItem> ITEM=ModItems.ITEMS.register("spacetime_engine",()->new SpacetimeEngineBlock.PlacementItem(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<SpacetimeEngineController>> CONTROLLER=
            ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("spacetime_engine",()->BlockEntityType.Builder.of(SpacetimeEngineController::new,BLOCK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<BlockEntity>> OUTPUT=
            ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("spacetime_engine_output",()->BlockEntityType.Builder.of(
                    (net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state)->
                            net.neoforged.fml.ModList.get().isLoaded("create")
                                    ?com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics.entity(pos,state)
                                    :new SpacetimeEngineController(pos,state),BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<SpacetimeEngineMenu>> MENU=ModMenuTypes.MENU_TYPES.register("spacetime_engine",
            ()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(SpacetimeEngineMenu::new));
    public static void init(){}
    private SpacetimeEngineContent(){}
}
