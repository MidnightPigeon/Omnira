package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.time.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_wonderland_poker")
@PrefixGameTestTemplate(false)
public final class WonderlandPokerStandGameTests {
    @GameTest(template="spell_arena") public static void cyclesAndClubRewards(GameTestHelper h){
        var random=RandomSource.create(22);
        boolean peaceful=false,corrupted=false;
        for(int i=0;i<128;i++){
            var item=WonderlandPokerStandBlockEntity.clubReward(random);
            h.assertTrue(!item.is(ModItems.MEMORY_CUBE.get()),"Club gave the placeable cube");
            peaceful|=item.is(ModItems.PEACEFUL_MEMORY.get());corrupted|=item.is(ModItems.CORRUPTED_MEMORY.get());
        }
        h.assertTrue(peaceful&&corrupted,"Club reward is missing a memory state");
        var pos=h.absolutePos(new BlockPos(4,2,4));var level=h.getLevel();
        var lower=ModBlocks.WONDERLAND_POKER_STAND.get().defaultBlockState();
        level.setBlockAndUpdate(pos,lower);
        level.setBlockAndUpdate(pos.above(),lower.setValue(WonderlandPokerStandBlock.HALF,DoubleBlockHalf.UPPER));
        h.assertTrue(level.getBlockEntity(pos) instanceof WonderlandPokerStandBlockEntity,"Stand controller missing");
        var stand=(WonderlandPokerStandBlockEntity)level.getBlockEntity(pos);
        for(var expected:new WonderlandPokerStandBlockEntity.Suit[]{WonderlandPokerStandBlockEntity.Suit.SPADE,
                WonderlandPokerStandBlockEntity.Suit.DIAMOND,WonderlandPokerStandBlockEntity.Suit.CLUB,
                WonderlandPokerStandBlockEntity.Suit.HEART}){
            stand.advanceGarden();h.assertTrue(stand.suit()==expected,"Suit order changed");
        }
        level.setBlockAndUpdate(pos.above(),Blocks.AIR.defaultBlockState());
        h.succeed();
    }
}
