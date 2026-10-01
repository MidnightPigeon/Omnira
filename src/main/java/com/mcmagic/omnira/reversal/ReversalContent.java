package com.mcmagic.omnira.reversal;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.item.FateCurioItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.IEventBus;

public final class ReversalContent {
    public static final DeferredItem<Item> PART=ModItems.ITEMS.registerSimpleItem("repaired_mechanical_component");
    public static final DeferredItem<Item> LAND=talent("land_king",FateCurioItem.Kind.LAND_KING);
    public static final DeferredItem<Item> SEA=talent("sea_king",FateCurioItem.Kind.SEA_KING);
    public static final DeferredItem<Item> SKY=talent("sky_king",FateCurioItem.Kind.SKY_KING);
    public static final DeferredItem<Item> CURSE=talent("time_distortion",FateCurioItem.Kind.TIME_DISTORTION);
    public static final DeferredItem<Item> REX_REMAINS=ModItems.ITEMS.registerSimpleItem("tyrannosaurus_remains");
    public static final DeferredItem<Item> MOSA_REMAINS=ModItems.ITEMS.registerSimpleItem("mosasaurus_remains");
    public static final DeferredItem<Item> PTERO_REMAINS=ModItems.ITEMS.registerSimpleItem("pterosaur_remains");
    public static final DeferredItem<Item> REX=essence("tyrannosaurus_essence",()->LAND.get(),false);
    public static final DeferredItem<Item> MOSA=essence("mosasaurus_essence",()->SEA.get(),false);
    public static final DeferredItem<Item> PTERO=essence("pterosaur_essence",()->SKY.get(),false);
    public static final DeferredItem<Item> INACTIVE_EGG=ModItems.ITEMS.registerSimpleItem("inactive_creature_egg");
    public static final DeferredItem<Item> EGG=ModItems.ITEMS.register("creature_egg",()->new com.mcmagic.omnira.ancient.AncientSummonItem(new Item.Properties(),ModEntityTypes.VELOCIRAPTOR));
    public static final DeferredItem<Item> INACTIVE_SEED=ModItems.ITEMS.registerSimpleItem("inactive_spirit_seed");
    public static final DeferredItem<Item> SEED=ModItems.ITEMS.register("spirit_seed",()->new com.mcmagic.omnira.ancient.AncientSummonItem(new Item.Properties(),ModEntityTypes.ARCHAEOPTERYX_SPIRIT));
    public static final DeferredItem<Item> VIRUS=ModItems.ITEMS.registerSimpleItem("inactive_t_virus_solution");
    public static final DeferredItem<Item> TALENT_FLUID=essence("distorted_psychic_talent_solution",()->ModItems.PSYKER_TALENT.get(),true);
    public static final DeferredBlock<ReversalBlock> MACHINE=ModBlocks.BLOCKS.register("spacetime_reverser",()->new ReversalBlock(BlockBehaviour.Properties.of().strength(3,12).noOcclusion().sound(net.minecraft.world.level.block.SoundType.GLASS)));
    public static final DeferredItem<BlockItem> MACHINE_ITEM=ModItems.ITEMS.registerSimpleBlockItem(MACHINE);
    public static final java.util.function.Supplier<BlockEntityType<ReversalBlockEntity>> ENTITY=ModBlockEntityTypes.BLOCK_ENTITY_TYPES.register("spacetime_reverser",()->BlockEntityType.Builder.of(ReversalBlockEntity::new,MACHINE.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ReversalMenu>> MENU=ModMenuTypes.MENU_TYPES.register("spacetime_reverser",()->net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create(ReversalMenu::new));
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(Registries.RECIPE_TYPE,"omnira");
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(Registries.RECIPE_SERIALIZER,"omnira");
    public static final java.util.function.Supplier<RecipeType<ReversalRecipe>> TYPE=TYPES.register("spacetime_reversal",()->new RecipeType<>(){public String toString(){return "omnira:spacetime_reversal";}});
    public static final java.util.function.Supplier<RecipeSerializer<ReversalRecipe>> SERIALIZER=SERIALIZERS.register("spacetime_reversal",()->new RecipeSerializer<>(){
        public com.mojang.serialization.MapCodec<ReversalRecipe> codec(){return ReversalRecipe.CODEC;}
        public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,ReversalRecipe> streamCodec(){return ReversalRecipe.STREAM;}
    });
    private static DeferredItem<Item> talent(String name,FateCurioItem.Kind kind){return ModItems.ITEMS.register(name,()->new FateCurioItem(new Item.Properties().rarity(Rarity.RARE),kind));}
    private static DeferredItem<Item> essence(String name,java.util.function.Supplier<Item> talent,boolean drink){return ModItems.ITEMS.register(name,()->new TalentEssenceItem(new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON),talent,drink));}
    public static void register(IEventBus bus){TYPES.register(bus);SERIALIZERS.register(bus);bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent e)->e.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,ENTITY.get(),(be,side)->new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(be,side==null?net.minecraft.core.Direction.UP:side)));}
    private ReversalContent(){}
}
