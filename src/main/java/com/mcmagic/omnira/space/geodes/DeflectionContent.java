package com.mcmagic.omnira.space.geodes;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.*;

public final class DeflectionContent {
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:dislocated_geodes"));
    public static final DeferredBlock<Block> BLOCK=register("deflection_crystal_block",()->new DeflectionBlock(properties()));
    public static final DeferredBlock<Block> MATRIX=register("deflection_crystal_matrix",()->new DeflectionMatrixBlock(properties().randomTicks()));
    public static final DeferredItem<Item> CRYSTAL=ModItems.ITEMS.registerSimpleItem("deflection_crystal");
    public static final DeferredBlock<Block> SMALL=bud("small_deflection_bud",3,6);
    public static final DeferredBlock<Block> MEDIUM=bud("medium_deflection_bud",5,5);
    public static final DeferredBlock<Block> LARGE=bud("large_deflection_bud",7,3);
    public static final DeferredBlock<Block> CLUSTER=bud("deflection_crystal_cluster",12,2);
    private static BlockBehaviour.Properties properties(){return BlockBehaviour.Properties.ofFullCopy(DreamContent.SPATIAL_CRYSTAL.get());}
    private static DeferredBlock<Block> register(String id,java.util.function.Supplier<Block> factory){
        var block=ModBlocks.BLOCKS.register(id,factory);ModItems.ITEMS.registerSimpleBlockItem(block);return block;
    }
    private static DeferredBlock<Block> bud(String id,int height,int inset){return register(id,()->new AmethystClusterBlock(height,inset,
            BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).requiresCorrectToolForDrops().lightLevel(s->5)));}
    public static Block[] stages(){return new Block[]{SMALL.get(),MEDIUM.get(),LARGE.get(),CLUSTER.get()};}
    public static int radius(net.minecraft.world.level.block.state.BlockState state){
        if(state.is(SMALL.get()))return 2;if(state.is(MEDIUM.get()))return 3;
        if(state.is(LARGE.get()))return 4;if(state.is(CLUSTER.get()))return 5;return 0;
    }
    public static void init(){}
    private DeflectionContent(){}
}
