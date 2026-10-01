package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.block.DecorativeStairBlock;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.ArrayList;
import java.util.List;

/** Complete ordinary building-material families, without variants of functional blocks. */
public final class DecorationVariants {
    public record Family(String baseId,String stem,DeferredBlock<Block> base,DeferredBlock<Block> stairs,DeferredBlock<Block> slab) {}
    private static final List<Family> FAMILIES=new ArrayList<>();
    static {
        add("light_condensate","light_condensate",DreamContent.LIGHT_CONDENSATE,null,null);
        add("light_source_crystal","light_source_crystal",DreamContent.LIGHT_SOURCE_CRYSTAL,null,null);
        add("light_condensate_bricks","light_condensate_brick",DreamContent.LIGHT_CONDENSATE_BRICKS,null,null);
        add("light_source_bricks","light_source_brick",DreamContent.LIGHT_SOURCE_BRICKS,null,null);
        add("solidified_light_crystal_core","solidified_light_crystal_core",DreamContent.SOLIDIFIED_LIGHT_CRYSTAL_CORE,null,null);
        add("spiritual_crystal_block","spiritual_crystal",DreamContent.SPIRITUAL_CRYSTAL_BLOCK,DreamContent.SPIRITUAL_CRYSTAL_STAIRS,null);
        add("dream_crystal_block","dream_crystal",DreamContent.DREAM_CRYSTAL_BLOCK,null,null);
        add("shadow_rock","shadow_rock",DreamContent.SHADOW_ROCK,DreamContent.SHADOW_ROCK_STAIRS,DreamContent.SHADOW_ROCK_SLAB);
        add("shadow_rock_bricks","shadow_rock_brick",DreamContent.SHADOW_ROCK_BRICKS,null,null);
        add("chiseled_shadow_rock","chiseled_shadow_rock",DreamContent.CHISELED_SHADOW_ROCK,null,null);
        add("mossy_shadow_rock","mossy_shadow_rock",DreamContent.MOSSY_SHADOW_ROCK,null,null);
        add("mirror_rock","mirror_rock",DreamContent.MIRROR_ROCK,null,null);
        add("engraved_mirror_rock","engraved_mirror_rock",DreamContent.ENGRAVED_MIRROR_ROCK,DreamContent.ENGRAVED_MIRROR_ROCK_STAIRS,DreamContent.ENGRAVED_MIRROR_ROCK_SLAB);
        add("void_mirror_rock","void_mirror_rock",DreamContent.VOID_MIRROR_ROCK,null,null);
        add("void_engraved_mirror_rock","void_engraved_mirror_rock",DreamContent.VOID_ENGRAVED_MIRROR_ROCK,null,null);
        add("shadow_planks","shadow",DreamContent.SHADOW_PLANKS,ShadowWoodContent.STAIRS,ShadowWoodContent.SLAB);
    }
    private static void add(String baseId,String stem,DeferredBlock<Block> base,DeferredBlock<Block> stairs,DeferredBlock<Block> slab) {
        if(stairs==null) {
            stairs=ModBlocks.BLOCKS.register(stem+"_stairs",()->new DecorativeStairBlock(base.get().defaultBlockState(),BlockBehaviour.Properties.ofFullCopy(base.get())));
            DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(stairs));
        }
        if(slab==null) {
            slab=ModBlocks.BLOCKS.register(stem+"_slab",()->new SlabBlock(BlockBehaviour.Properties.ofFullCopy(base.get())));
            DreamContent.ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(slab));
        }
        FAMILIES.add(new Family(baseId,stem,base,stairs,slab));
    }
    public static List<Family> families(){return List.copyOf(FAMILIES);}
    public static void init() {}
    private DecorationVariants() {}
}
