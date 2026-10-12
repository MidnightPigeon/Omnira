package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.*;

public final class CatalysisContent {
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_TYPE,"omnira");
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,"omnira");
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeType<?>,net.minecraft.world.item.crafting.RecipeType<CatalysisRecipe>> TYPE=TYPES.register("catalysis",()->RecipeRegistration.type(net.minecraft.resources.ResourceLocation.parse("omnira:catalysis")));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>,net.minecraft.world.item.crafting.RecipeSerializer<CatalysisRecipe>> SERIALIZER=SERIALIZERS.register("catalysis",()->RecipeRegistration.serializer(CatalysisRecipe.CODEC,CatalysisRecipe.STREAM));
    public static final DeferredBlock<CatalysisBlock> BLOCK=ModBlocks.BLOCKS.register("alchemical_catalysis_chamber",()->new CatalysisBlock(
            BlockBehaviour.Properties.of().strength(-1,3600000).noOcclusion().randomTicks().lightLevel(s->8)));
    public static final DeferredItem<CatalysisBlock.PlacementItem> ITEM=ModItems.ITEMS.register("alchemical_catalysis_chamber",()->new CatalysisBlock.PlacementItem(new Item.Properties()));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CatalysisBlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register(
            "alchemical_catalysis_chamber",()->BlockEntityType.Builder.of(CatalysisBlockEntity::new,BLOCK.get()).build(null));
    public static final DeferredHolder<MenuType<?>,MenuType<CatalysisMenu>> MENU=ModMenuTypes.MENU_TYPES.register(
            "alchemical_catalysis_chamber",()->IMenuTypeExtension.create(CatalysisMenu::new));
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);bus.addListener(CatalysisContent::capabilities);}
    private static void capabilities(RegisterCapabilitiesEvent event){
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->{
            var root=CatalysisBlock.controller(be.getLevel(),be.getBlockPos());
            if(root==null||!root.complete())return null;
            return be.getBlockState().getValue(CatalysisBlock.PART)==2?root.extractOnly:root.items;
        });
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK,ENTITY.get(),(be,side)->{
            var root=CatalysisBlock.controller(be.getLevel(),be.getBlockPos());
            return root!=null&&root.complete()&&be.getBlockState().getValue(CatalysisBlock.PART)==0
                    &&side==be.getBlockState().getValue(CatalysisBlock.FACING).getOpposite()?root.tank:null;
        });
    }
    private CatalysisContent(){}
}
