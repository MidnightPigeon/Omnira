package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class CrystalStorageContent {
    public static final DeferredBlock<Block> VOID_ORB=ModBlocks.BLOCKS.register("void_crystal_ball",
        ()->new com.mcmagic.omnira.block.CrystalBallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.CRYSTAL_BALL.get())));
    public static final DeferredItem<BlockItem> VOID_ITEM=ModItems.ITEMS.register("void_crystal_ball",
        ()->new BlockItem(VOID_ORB.get(),new Item.Properties()));
    public static final DeferredBlock<Block> ITEM_ORB=ModBlocks.BLOCKS.register("resonant_crystal_ball",
        ()->new com.mcmagic.omnira.block.CrystalBallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.CRYSTAL_BALL.get())));
    public static final DeferredBlock<Block> FLUID_ORB=ModBlocks.BLOCKS.register("resonant_liquid_crystal_ball",
        ()->new com.mcmagic.omnira.block.CrystalBallBlock(BlockBehaviour.Properties.ofFullCopy(ModBlocks.LIQUID_CRYSTAL_BALL.get())));
    public static final DeferredItem<BlockItem> ITEM=ModItems.ITEMS.register("resonant_crystal_ball",
        ()->new com.mcmagic.omnira.item.PortableOrbBlockItem(ITEM_ORB.get(),new Item.Properties()));
    public static final DeferredItem<BlockItem> FLUID=ModItems.ITEMS.register("resonant_liquid_crystal_ball",
        ()->new com.mcmagic.omnira.item.PortableOrbBlockItem(FLUID_ORB.get(),new Item.Properties()));
    public static final DeferredItem<TuningRodItem> ROD=ModItems.ITEMS.register("resonance_tuning_rod",
        ()->new TuningRodItem(new Item.Properties().stacksTo(1).attributes(
            net.minecraft.world.item.component.ItemAttributeModifiers.builder()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID,3,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,new net.minecraft.world.entity.ai.attributes.AttributeModifier(Item.BASE_ATTACK_SPEED_ID,-2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND).build())));
    public static void bootstrap(){}
    public static boolean resonant(net.minecraft.world.level.block.state.BlockState state){
        return state.is(ITEM_ORB.get())||state.is(FLUID_ORB.get());
    }
    private CrystalStorageContent(){}
}
