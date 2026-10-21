package com.mcmagic.omnira.codex;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class MyriadCodexContent {
    public static final DeferredBlock<MyriadCodexBlock> BLOCK=ModBlocks.BLOCKS.register("myriad_codex",()->{
        var properties=BlockBehaviour.Properties.of().strength(4,16).noOcclusion().lightLevel(s->12);
        return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.CodexKinetics.block(properties):new MyriadCodexBlock(properties);
    });
    public static final DeferredItem<BlockItem> ITEM=ModItems.ITEMS.register("myriad_codex",()->new BlockItem(BLOCK.get(),new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredBlock<WorldHeartBlock> HEART=ModBlocks.BLOCKS.register("myriad_heart",()->new WorldHeartBlock(
            BlockBehaviour.Properties.of().strength(0).noCollission().noOcclusion().noLootTable().lightLevel(s->15)));
    public static final DeferredItem<WorldHeartItem> HEART_ITEM=ModItems.ITEMS.register("myriad_heart",()->new WorldHeartItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<net.minecraft.world.level.block.entity.BlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("myriad_codex",()->BlockEntityType.Builder.of(MyriadCodexContent::entity,BLOCK.get()).build(null));
    public static net.minecraft.world.level.block.entity.BlockEntity entity(net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState state){
        return net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.CodexKinetics.entity(pos,state):new MyriadCodexEntity(pos,state);
    }
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<WorldHeartEntity>> HEART_ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("myriad_heart",()->BlockEntityType.Builder.of(WorldHeartEntity::new,HEART.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<MyriadCodexMenu>> MENU=ModMenuTypes.MENU_TYPES.register("myriad_codex",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(MyriadCodexMenu::new));
    public static void init(){}
    private MyriadCodexContent(){}
}
