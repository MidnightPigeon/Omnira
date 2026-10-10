package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.block.CrystalFamilies;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_crystal_growth")
@PrefixGameTestTemplate(false)
public final class ArcaneDustGameTests {
    private static Player player(GameTestHelper h,GameType mode){
        var player=h.makeMockPlayer(mode);mode.updatePlayerAbilities(player.getAbilities());return player;
    }
    private static InteractionResult use(Player player,BlockPos pos){
        return player.getMainHandItem().getItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,
                new BlockHitResult(pos.getCenter(),Direction.UP,pos,false)));
    }
    @GameTest(template="spell_arena") public static void oneGrowableFaceIsEnough(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,6,6));var player=player(h,GameType.SURVIVAL);
        for(var family:CrystalFamilies.all())for(var eligible:Direction.values())for(int stage:new int[]{0,1,3}){
            level.setBlockAndUpdate(pos,family[0].defaultBlockState());
            for(var face:Direction.values())level.setBlockAndUpdate(pos.relative(face),face.ordinal()%2==0
                    ?Blocks.STONE.defaultBlockState():family[4].defaultBlockState().setValue(AmethystClusterBlock.FACING,face));
            level.setBlockAndUpdate(pos.relative(eligible),stage==0?Blocks.AIR.defaultBlockState():
                    family[stage].defaultBlockState().setValue(AmethystClusterBlock.FACING,eligible));
            var dust=new ItemStack(ModItems.ARCANE_DUST.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
            h.assertTrue(use(player,pos).consumesAction()&&dust.getCount()==1
                    &&level.getBlockState(pos.relative(eligible)).is(family[stage+1]),"One eligible face failed to grow or consume one dust");
            for(var face:Direction.values())if(face!=eligible)h.assertTrue(level.getBlockState(pos.relative(face)).is(face.ordinal()%2==0?Blocks.STONE:family[4]),"Blocked or mature face changed");
            level.setBlockAndUpdate(pos.relative(eligible),family[4].defaultBlockState().setValue(AmethystClusterBlock.FACING,eligible));
            h.assertTrue(!use(player,pos).consumesAction()&&dust.getCount()==1,"All blocked or mature faces consumed dust");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void allMatricesAndFacesAdvanceExactlyOneStage(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,6,6));var player=player(h,GameType.SURVIVAL);
        for(var family:CrystalFamilies.all()){
            level.setBlockAndUpdate(pos,family[0].defaultBlockState());
            for(var face:Direction.values())level.setBlockAndUpdate(pos.relative(face),Blocks.AIR.defaultBlockState());
            var dust=new ItemStack(ModItems.ARCANE_DUST.get(),6);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
            for(int stage=1;stage<=4;stage++){
                h.assertTrue(use(player,pos).consumesAction()&&dust.getCount()==6-stage,"Growth did not consume exactly one dust per matrix");
                for(var face:Direction.values()){
                    var state=level.getBlockState(pos.relative(face));
                    h.assertTrue(state.is(family[stage])&&state.getValue(AmethystClusterBlock.FACING)==face,"Growth skipped a stage or used incorrect family/facing");
                }
            }
            h.assertTrue(!use(player,pos).consumesAction()&&dust.getCount()==2,"Mature matrix consumed dust");
        }h.succeed();
    }
    @GameTest(template="spell_arena") public static void waterAndBlockedFacesArePreserved(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,6,6));var family=CrystalFamilies.all()[2];var foreign=CrystalFamilies.all()[1];
        level.setBlockAndUpdate(pos,family[0].defaultBlockState());
        level.setBlockAndUpdate(pos.north(),Blocks.WATER.defaultBlockState());
        level.setBlockAndUpdate(pos.south(),Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.east().below(),Blocks.STONE.defaultBlockState());
        var wrong=family[1].defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.UP);level.setBlockAndUpdate(pos.east(),wrong);
        var other=foreign[1].defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.WEST);level.setBlockAndUpdate(pos.west(),other);
        var mature=family[4].defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.UP);level.setBlockAndUpdate(pos.above(),mature);
        level.setBlockAndUpdate(pos.below(),family[2].defaultBlockState().setValue(AmethystClusterBlock.FACING,Direction.DOWN));
        var player=player(h,GameType.SURVIVAL);var dust=new ItemStack(ModItems.ARCANE_DUST.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
        h.assertTrue(use(player,pos).consumesAction()&&dust.getCount()==1,"Mixed matrix growth failed");
        h.assertTrue(level.getBlockState(pos.north()).is(family[1])&&level.getBlockState(pos.north()).getValue(AmethystClusterBlock.WATERLOGGED),"Water was lost when sprouting");
        h.assertTrue(level.getBlockState(pos.below()).is(family[3]),"Existing cluster did not advance one stage");
        h.assertTrue(level.getBlockState(pos.south()).is(Blocks.STONE)&&level.getBlockState(pos.east()).equals(wrong)
                &&level.getBlockState(pos.west()).equals(other)&&level.getBlockState(pos.above()).equals(mature),"Growth replaced blocked, foreign, reversed or mature faces");
        use(player,pos);h.assertTrue(level.getBlockState(pos.north()).is(family[2])&&level.getBlockState(pos.north()).getValue(AmethystClusterBlock.WATERLOGGED),"Growth lost waterlogging");h.succeed();
    }
    @GameTest(template="spell_arena") public static void permissionsAndUnsuccessfulUsesDoNotConsume(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(6,6,6));var player=player(h,GameType.CREATIVE);
        var dust=new ItemStack(ModItems.ARCANE_DUST.get(),2);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
        level.setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        h.assertTrue(!use(player,pos).consumesAction()&&dust.getCount()==2,"Dust grew a non-matrix block");
        level.setBlockAndUpdate(pos,Blocks.BUDDING_AMETHYST.defaultBlockState());
        for(var face:Direction.values())level.setBlockAndUpdate(pos.relative(face),Blocks.AIR.defaultBlockState());
        h.assertTrue(use(player,pos).consumesAction()&&dust.getCount()==2,"Creative growth consumed dust");
        player=player(h,GameType.ADVENTURE);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
        h.assertTrue(!use(player,pos).consumesAction()&&dust.getCount()==2,"Adventure growth bypassed build permission");
        for(var face:Direction.values())h.assertTrue(level.getBlockState(pos.relative(face)).is(Blocks.SMALL_AMETHYST_BUD),"Adventure mode changed a cluster");
        player=player(h,GameType.SURVIVAL);player.setItemInHand(InteractionHand.MAIN_HAND,dust);
        for(var face:Direction.values())level.setBlockAndUpdate(pos.relative(face),Blocks.STONE.defaultBlockState());
        h.assertTrue(!use(player,pos).consumesAction()&&dust.getCount()==2,"Blocked matrix consumed dust");h.succeed();
    }
}
