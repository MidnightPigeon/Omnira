package com.mcmagic.omnira.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Infusion motes stay within the occupied crystal shape, including stairs and slabs. */
public final class InfusedCrystalBlock extends TransparentBlock implements EntityBlock {
    public InfusedCrystalBlock(Properties properties){super(properties);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Motes(pos,state);}
    public static final class Motes extends BlockEntity {
        public Motes(BlockPos pos,BlockState state){super(com.mcmagic.omnira.registry.ModBlockEntityTypes.INFUSED_CRYSTAL_MOTES.get(),pos,state);}
    }
    public static final class Slab extends SlabBlock implements EntityBlock {
        public Slab(Properties properties){super(properties);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Motes(pos,state);}
    }
    public static final class Stairs extends DecorativeStairBlock implements EntityBlock {
        public Stairs(BlockState base,Properties properties){super(base,properties);}
        @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new Motes(pos,state);}
    }
    public static net.minecraft.world.level.block.Block[] blocks(){
        var family=com.mcmagic.omnira.registry.DecorationVariants.families().stream().filter(f->f.baseId().equals("infused_crystal_block")).findFirst().orElseThrow();
        return new net.minecraft.world.level.block.Block[]{family.base().get(),family.stairs().get(),family.slab().get()};
    }
}
