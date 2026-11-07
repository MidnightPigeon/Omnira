package com.mcmagic.omnira.gametest;

import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_brush_cooldown") @PrefixGameTestTemplate(false)
public final class BrushCooldownGameTests {
    @GameTest(template="spell_arena")
    public static void nativeAndSpacetimeCadence(GameTestHelper h){
        check(h,Items.BRUSH,10,2);
        check(h,com.mcmagic.omnira.archaeology.ArchaeologyContent.BRUSH.get(),5,5);
        h.succeed();
    }
    private static void check(GameTestHelper h,Item brush,int interval,int x){
        var pos=h.absolutePos(new BlockPos(x,3,3));
        h.getLevel().setBlockAndUpdate(pos.below(),Blocks.STONE.defaultBlockState());
        h.getLevel().setBlockAndUpdate(pos,Blocks.SUSPICIOUS_SAND.defaultBlockState());
        var player=h.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(brush));
        player.startUsingItem(InteractionHand.MAIN_HAND);
        var block=(BrushableBlockEntity)h.getLevel().getBlockEntity(pos);
        long start=h.getLevel().getGameTime();
        for(int n=0;n<9;n++){
            h.assertTrue(!block.brush(start+n*interval,player,Direction.UP),"Brush completed too early");
            h.assertTrue(!block.brush(start+n*interval+interval-1,player,Direction.UP),"Brush ignored cooldown");
        }
        h.assertTrue(block.brush(start+9*interval,player,Direction.UP),"Brush cadence changed: "+interval);
    }
}
