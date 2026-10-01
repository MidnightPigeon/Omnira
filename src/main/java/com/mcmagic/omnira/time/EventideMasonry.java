package com.mcmagic.omnira.time;

import com.mcmagic.omnira.block.DecorativeStairBlock;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.DreamContent;
import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Ruin masonry shares one brick grid; erosion changes only the surface treatment. */
public final class EventideMasonry {
    public static final DeferredBlock<Block> BRICKS=block("time_eroded_bricks",()->new Block(stone()));
    public static final DeferredBlock<Block> CRACKED=block("cracked_time_eroded_bricks",()->new Block(stone()));
    public static final DeferredBlock<Block> SANDBOUND=block("sandbound_time_eroded_bricks",()->new Block(stone()));
    public static final DeferredBlock<Block> BRICK_STAIRS=stairs("time_eroded_brick_stairs",BRICKS);
    public static final DeferredBlock<Block> BRICK_SLAB=block("time_eroded_brick_slab",()->new SlabBlock(stone()));
    public static final DeferredBlock<Block> BRICK_WALL=block("time_eroded_brick_wall",()->new WallBlock(stone().noOcclusion()));
    public static final DeferredBlock<Block> CRACKED_STAIRS=stairs("cracked_time_eroded_brick_stairs",CRACKED);
    public static final DeferredBlock<Block> CRACKED_SLAB=block("cracked_time_eroded_brick_slab",()->new SlabBlock(stone()));
    public static final DeferredBlock<Block> CRACKED_WALL=block("cracked_time_eroded_brick_wall",()->new WallBlock(stone().noOcclusion()));
    public static final DeferredBlock<Block> SANDBOUND_STAIRS=stairs("sandbound_time_eroded_brick_stairs",SANDBOUND);
    public static final DeferredBlock<Block> SANDBOUND_SLAB=block("sandbound_time_eroded_brick_slab",()->new SlabBlock(stone()));
    public static final DeferredBlock<Block> SANDBOUND_WALL=block("sandbound_time_eroded_brick_wall",()->new WallBlock(stone().noOcclusion()));
    public static final DeferredBlock<Block> BROKEN_PILLAR=block("broken_eventide_pillar",()->new EventideFragmentBlock(false,stone().noOcclusion()));
    public static final DeferredBlock<Block> DEBRIS=block("eventide_debris",()->new EventideFragmentBlock(true,stone().noOcclusion().noCollission()));

    private static BlockBehaviour.Properties stone(){
        return BlockBehaviour.Properties.of().strength(2.2F,6).requiresCorrectToolForDrops().sound(SoundType.STONE);
    }
    private static DeferredBlock<Block> stairs(String name,DeferredBlock<Block> base){
        return block(name,()->new DecorativeStairBlock(base.get().defaultBlockState(),stone()));
    }
    private static DeferredBlock<Block> block(String name,Supplier<Block> factory){
        var result=ModBlocks.BLOCKS.register(name,factory);
        DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(result));
        return result;
    }
    public static void init(){}
    private EventideMasonry(){}
}
