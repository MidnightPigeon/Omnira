package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spell.world.WorldRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;

/** Run separately: the expanding 32-block effect intentionally reaches beyond a small test template. */
@GameTestHolder("omnira_world_wave")
@PrefixGameTestTemplate(false)
public final class WorldWaveGameTests {
    @GameTest(template="spell_arena",timeoutTicks=150) public static void waveExpandsAndStopsAtThirtyTwo(GameTestHelper h){
        var center=h.absolutePos(new BlockPos(5,3,5));var player=AdvancedForgeGameTests.player(h,center);
        h.getLevel().addFreshEntity(player);
        for(int i=0;i<36;i++)player.getInventory().setItem(i,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COBBLESTONE,64));
        player.getInventory().setItem(0,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,62));
        var near=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),center.getX()+10.5,center.getY()+.5,center.getZ()+.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND,7));
        near.setNoGravity(true);near.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);near.setNeverPickUp();h.getLevel().addFreshEntity(near);
        var inside=h.spawn(EntityType.ZOMBIE,new BlockPos(15,4,5));
        var outside=EntityType.ZOMBIE.create(h.getLevel());var outsidePos=center.east(34);h.getLevel().getChunkAt(outsidePos);
        outside.moveTo(outsidePos.getCenter());h.getLevel().addFreshEntity(outside);
        var far=new net.minecraft.world.entity.item.ItemEntity(h.getLevel(),outsidePos.getX()+.5,outsidePos.getY()+.5,outsidePos.getZ()+.5,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD));
        far.setNoGravity(true);far.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);far.setNeverPickUp();h.getLevel().addFreshEntity(far);
        var crop=center.east(3);h.getLevel().setBlockAndUpdate(crop.below(),Blocks.FARMLAND.defaultBlockState());h.getLevel().setBlockAndUpdate(crop,Blocks.WHEAT.defaultBlockState());
        h.assertTrue(WorldRotation.cast(player),"Wave did not start");h.assertTrue(!inside.isRemoved(),"Wave skipped expansion");
        h.assertTrue(!near.isRemoved()&&player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)==62,"Items collected before expansion");
        h.runAfterDelay(100,()->{
            h.assertTrue(inside.isRemoved()&&!outside.isRemoved(),"Wave missed a target or escaped radius");
            h.assertTrue(!near.isRemoved()&&!far.isRemoved()&&player.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND)==64&&near.getItem().getCount()==5&&near.position().distanceToSqr(player.position())<.000001,"Overflow collection missed feet or crossed boundary");
            h.assertTrue(((net.minecraft.world.level.block.CropBlock)Blocks.WHEAT).isMaxAge(h.getLevel().getBlockState(crop)),"Wave failed crop growth");
            outside.discard();near.discard();far.discard();WorldRotation.cancel(player.getUUID());player.discard();h.succeed();
        });
    }
}
