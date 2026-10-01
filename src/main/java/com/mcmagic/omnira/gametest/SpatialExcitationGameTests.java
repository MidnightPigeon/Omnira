package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_spatial_excitation")
@PrefixGameTestTemplate(false)
public final class SpatialExcitationGameTests {
    @GameTest(template="spell_arena")
    public static void interact(GameTestHelper h){
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,3,3));
        var state=DreamContent.SPATIAL_CRYSTAL.get().defaultBlockState();
        var hit=new BlockHitResult(pos.getCenter(),Direction.UP,pos,false);
        for(var mode:new GameType[]{GameType.SURVIVAL,GameType.CREATIVE}){
            var player=h.makeMockPlayer(mode);
            player.getAbilities().instabuild=mode==GameType.CREATIVE;
            for(boolean sneak:new boolean[]{true,false}){
                level.setBlockAndUpdate(pos,state);player.setShiftKeyDown(sneak);
                var stack=new ItemStack(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get(),2);
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                state.useItemOn(stack,level,player,InteractionHand.MAIN_HAND,hit);
                h.assertTrue(level.getBlockState(pos).is(sneak?DreamContent.SPATIAL_CRYSTAL.get():DreamContent.EXCITED_SPATIAL_CRYSTAL.get()),"Wrong excitation/sneak behavior");
                h.assertTrue(stack.getCount()==(!sneak&&mode==GameType.SURVIVAL?1:2),"Wrong crystal consumption: "+mode+", sneak="+sneak+", count="+stack.getCount());
            }
            level.setBlockAndUpdate(pos,state);player.setShiftKeyDown(false);
            state.useItemOn(new ItemStack(Items.STICK),level,player,InteractionHand.MAIN_HAND,hit);
            h.assertTrue(level.getBlockState(pos).is(state.getBlock()),"Unrelated item excited crystal");
        }
        h.succeed();
    }
}
