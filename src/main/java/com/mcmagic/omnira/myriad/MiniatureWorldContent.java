package com.mcmagic.omnira.myriad;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class MiniatureWorldContent {
    public static final DeferredItem<Item> PROBABILITY_CLOUD=ModItems.ITEMS.registerSimpleItem("world_generation_probability_cloud",new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC));
    public static final DeferredBlock<MiniatureWorldBlock> BLOCK=ModBlocks.BLOCKS.register("miniature_world_framework",()->new MiniatureWorldBlock(
            BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE).strength(3.5F,12).lightLevel(MiniatureWorldBlock::lightLevel).noOcclusion().noLootTable()));
    public static final DeferredItem<MiniatureWorldBlock.PlacementItem> ITEM=ModItems.ITEMS.register("miniature_world_framework",
            ()->new MiniatureWorldBlock.PlacementItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<MiniatureWorldBlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(
            "miniature_world_framework",()->BlockEntityType.Builder.of(MiniatureWorldBlockEntity::new,BLOCK.get()).build(null));
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_TYPE,"omnira");
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,"omnira");
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,net.minecraft.world.item.crafting.RecipeType<WorldDevelopmentRecipe>> TYPE=TYPES.register("world_development",()->RecipeRegistration.type(net.minecraft.resources.ResourceLocation.parse("omnira:world_development")));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.RecipeSerializer<WorldDevelopmentRecipe>> SERIALIZER=SERIALIZERS.register("world_development",()->RecipeRegistration.serializer(WorldDevelopmentRecipe.CODEC,WorldDevelopmentRecipe.STREAM));
    public static void register(net.neoforged.bus.api.IEventBus bus){WorldSeedContent.init();WorldComponent.init();TYPES.register(bus);SERIALIZERS.register(bus);bus.addListener(MiniatureWorldContent::capabilities);}
    private static void capabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event){
        event.registerBlock(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,(level,pos,state,be,side)->{
            var root=MiniatureWorldBlock.controller(level,pos);return root==null?null:com.mcmagic.omnira.progression.BranchAutomation.items(root,root.inventory);
        },BLOCK.get());
    }
    private MiniatureWorldContent(){}
}
