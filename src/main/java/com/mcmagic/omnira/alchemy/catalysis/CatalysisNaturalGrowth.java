package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** One virtual planting site, advanced only when the controller receives a native random tick. */
public final class CatalysisNaturalGrowth {
    public enum Kind { CROP, BERRY, COCOA, TREE, STEM, BAMBOO, CRYSTAL, VINE }
    public record Rule(CatalysisGrowth.Plan plan,Kind kind) {
        public boolean advance(ServerLevel level,BlockPos pos,ItemStack soil,RandomSource random){
            if(kind!=Kind.COCOA&&kind!=Kind.CRYSTAL&&kind!=Kind.VINE&&!plan.plant().is(Blocks.MANGROVE_PROPAGULE)&&level.getMaxLocalRawBrightness(pos)<9)return false;
            return switch(kind){
                case CROP,STEM -> (!(plan.plant().getBlock() instanceof BeetrootBlock||plan.plant().getBlock() instanceof TorchflowerCropBlock)
                        ||random.nextInt(3)!=0)&&random.nextInt((int)(25F/growthSpeed(level,pos,soil,plan.plant()))+1)==0;
                case TREE -> (!plan.plant().is(TimeNatureContent.SAPLING.get())||Block.byItem(soil.getItem()).defaultBlockState().is(com.mcmagic.omnira.time.TemporalSoils.SILTS)||random.nextBoolean())&&random.nextInt(7)==0;
                case BERRY,COCOA -> random.nextInt(5)==0;
                case BAMBOO -> random.nextInt(3)==0;
                // Only the upper face is planted; the other five matrix faces remain unoccupied.
                case CRYSTAL -> random.nextInt(5)==0&&random.nextInt(6)==Direction.UP.ordinal();
                case VINE -> random.nextDouble()<.1;
            };
        }
        public List<ItemStack> harvest(ServerLevel level,BlockPos pos,ItemStack soil,ItemStack seed){
            if(kind==Kind.TREE)return CatalysisTreeGrowth.harvest(level,plan.plant(),Block.byItem(soil.getItem()).defaultBlockState());
            if(kind==Kind.BAMBOO)return List.of(new ItemStack(Items.BAMBOO));
            var mature=kind==Kind.STEM?(plan.plant().is(Blocks.MELON_STEM)?Blocks.MELON:Blocks.PUMPKIN).defaultBlockState():plan.plant();
            var tool=kind==Kind.CRYSTAL?new ItemStack(Items.DIAMOND_PICKAXE):ItemStack.EMPTY;
            return CatalysisGrowth.products(Block.getDrops(mature,level,pos,null,null,tool),seed,1);
        }
    }
    public static Rule resolve(Level level,ItemStack soil,ItemStack seed){
        if(!(soil.getItem() instanceof BlockItem)||seed.isEmpty())return null;
        var base=Block.byItem(soil.getItem()).defaultBlockState();var plant=Block.byItem(seed.getItem()).defaultBlockState();
        if(seed.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))for(var family:CatalysisGrowth.crystalFamilies())if(base.is(family[0]))
            return rule(5,family[4].defaultBlockState(),Kind.CRYSTAL,CatalysisGrowth.Yield.FIXED);
        var water=new FluidStack(net.minecraft.world.level.material.Fluids.WATER,500);
        var plan=CatalysisGrowth.resolve(level,soil,seed,water);
        if(plant.is(TimeNatureContent.SAPLING.get())&&com.mcmagic.omnira.time.TemporalSoils.timeTree(base))
            return rule(3,plant,Kind.TREE,CatalysisGrowth.Yield.TREE);
        if(plan==null)return null;
        if(seed.is(Items.GLOW_BERRIES)&&base.is(Blocks.VINE))return rule(26,Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES,true),Kind.VINE,CatalysisGrowth.Yield.FIXED);
        if(plant.getBlock() instanceof SaplingBlock)
            return rule(3,plant,Kind.TREE,CatalysisGrowth.Yield.TREE);
        if(plant.getBlock() instanceof StemBlock)return rule(2,plant,Kind.STEM,CatalysisGrowth.Yield.FRUIT);
        if(plant.is(Blocks.BAMBOO)&&base.is(BlockTags.BAMBOO_PLANTABLE_ON))return rule(2,plant,Kind.BAMBOO,CatalysisGrowth.Yield.FIXED);
        if(plant.getBlock() instanceof CocoaBlock){
            // The catalyst accepts extra wood variants, but natural cocoa needs its actual growing wood.
            boolean supported=plant.getBlock() instanceof com.mcmagic.omnira.block.ShadowBerryBlock?base.is(DreamContent.SHADOW_LOG.get()):base.is(BlockTags.JUNGLE_LOGS);
            return supported?rule(3,plant.setValue(CocoaBlock.AGE,2),Kind.COCOA,CatalysisGrowth.Yield.CROP):null;
        }
        if(plant.is(Blocks.SWEET_BERRY_BUSH))return rule(4,plant.setValue(SweetBerryBushBlock.AGE,3),Kind.BERRY,CatalysisGrowth.Yield.CROP);
        if(plant.getBlock() instanceof CropBlock||plant.is(Blocks.PITCHER_CROP)){
            var age=CatalysisGrowth.age(plant);int stages=java.util.Collections.max(age.getPossibleValues())+1;
            return rule(stages,plan.plant(),Kind.CROP,CatalysisGrowth.Yield.CROP);
        }
        // Water-dependent plants, bonemeal-only growth and material synthesis are not invented here.
        return null;
    }
    private static float growthSpeed(Level level,BlockPos pos,ItemStack soil,BlockState plant){
        var base=Block.byItem(soil.getItem()).defaultBlockState();
        var cultivated=base.is(BlockTags.DIRT)?Blocks.FARMLAND.defaultBlockState():base;
        var view=new net.minecraft.world.level.BlockGetter(){
            public BlockState getBlockState(BlockPos p){return p.equals(pos.below())?cultivated:p.equals(pos)?plant:Blocks.AIR.defaultBlockState();}
            public net.minecraft.world.level.block.entity.BlockEntity getBlockEntity(BlockPos p){return null;}
            public net.minecraft.world.level.material.FluidState getFluidState(BlockPos p){return getBlockState(p).getFluidState();}
            public int getHeight(){return level.getHeight();}public int getMinBuildHeight(){return level.getMinBuildHeight();}
        };
        return com.mcmagic.omnira.mixin.CatalysisCropAccessor.omnira$growthSpeed(plant,view,pos);
    }
    private static Rule rule(int stages,BlockState plant,Kind kind,CatalysisGrowth.Yield yield){return new Rule(new CatalysisGrowth.Plan(stages,plant,ItemStack.EMPTY,yield),kind);}
    private CatalysisNaturalGrowth(){}
}
