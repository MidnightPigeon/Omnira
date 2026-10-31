package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.sea.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_stardust_support") @PrefixGameTestTemplate(false)
public final class StardustSupportGameTests {
    private static int drops(GameTestHelper h,BlockPos pos){return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2),e->e.getItem().is(StarlightContent.DUST_ITEM.get())).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static void clearDrops(GameTestHelper h,BlockPos pos){h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2)).forEach(Entity::discard);}
    @GameTest(template="spell_arena") public static void dustNeedsTopSupportButSourceDoesNot(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,5,4));var dust=StarlightContent.DUST.get().defaultBlockState();
        level.setBlockAndUpdate(pos.below(),Blocks.AIR.defaultBlockState());h.assertTrue(!dust.canSurvive(level,pos),"Dust can float without support");
        h.assertTrue(StarlightContent.SOURCE.get().defaultBlockState().canSurvive(level,pos),"Suspended star source now needs a floor");
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());h.assertTrue(dust.canSurvive(level,pos),"Full support refused");
        level.setBlockAndUpdate(pos.below(),Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP));h.assertTrue(dust.canSurvive(level,pos),"Flush upper slab refused");
        level.setBlockAndUpdate(pos.below(),Blocks.STONE_SLAB.defaultBlockState());h.assertTrue(!dust.canSurvive(level,pos),"Dust floats above a lower slab");h.succeed();
    }
    @GameTest(template="spell_arena") public static void removingOrReplacingSupportDropsExactlyOnce(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,5,4));var dust=StarlightContent.DUST.get().defaultBlockState();
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,dust);
        level.setBlockAndUpdate(pos.below(),Blocks.GLASS.defaultBlockState());h.assertTrue(level.getBlockState(pos).is(dust.getBlock())&&drops(h,pos)==0,"Support replacement broke valid dust");
        level.destroyBlock(pos.below(),true);
        h.assertTrue(level.getBlockState(pos).isAir()&&drops(h,pos)==1&&level.getBlockEntity(pos)==null,"Floor destruction did not immediately drop one stardust");
        level.updateNeighborsAt(pos.below(),Blocks.STONE);level.updateNeighborsAt(pos.below(),Blocks.AIR);
        h.assertTrue(drops(h,pos)==1,"Repeated neighbour notifications duplicated stardust");clearDrops(h,pos);
        level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(pos,dust);
        level.setBlockAndUpdate(pos.below(),Blocks.STONE_SLAB.defaultBlockState());
        h.assertTrue(level.getBlockState(pos).isAir()&&drops(h,pos)==1,"Loss of supporting face did not drop dust");clearDrops(h,pos);
        level.setBlockAndUpdate(pos,StarlightContent.SOURCE.get().defaultBlockState());level.removeBlock(pos.below(),false);
        h.assertTrue(level.getBlockState(pos).is(StarlightContent.SOURCE.get()),"Floor loss removed independent star source");level.removeBlock(pos,false);h.succeed();
    }
    @GameTest(template="spell_arena") public static void submergedDustKeepsOriginalFluidOnSupportLoss(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(4,5,4));
        for(int kind=1;kind<=2;kind++)for(int height:new int[]{0,5,8}){
            level.setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
            var dust=StarlightContent.DUST.get().defaultBlockState().setValue(StarlightBlock.WATER,kind).setValue(StarlightBlock.FLUID_LEVEL,height);
            level.setBlockAndUpdate(pos,dust);var fluid=dust.getFluidState();level.destroyBlock(pos.below(),false);
            var restored=level.getBlockState(pos);
            h.assertTrue(!restored.is(dust.getBlock())&&restored.getFluidState().getType()==fluid.getType()
                    &&restored.getOptionalValue(LiquidBlock.LEVEL).orElse(-1)==height&&drops(h,pos)==1,"Support loss lost liquid type/level or duplicated dust: "+kind+"/"+height);
            clearDrops(h,pos);level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        }
        h.succeed();
    }
    private StardustSupportGameTests(){}
}
