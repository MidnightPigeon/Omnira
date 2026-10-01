package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder("omnira_crystal_revision")
@PrefixGameTestTemplate(false)
public final class CrystalRevisionGameTests {
    private static FakePlayer player(GameTestHelper h) {
        var player=new FakePlayer(h.getLevel(),new GameProfile(UUID.randomUUID(),"crystal-test"));
        player.setPos(h.absoluteVec(new Vec3(1,2,1)));
        return player;
    }
    @GameTest(template="spell_arena")
    public static void burstHarvestsWholeSphere(GameTestHelper h) {
        var center=h.absolutePos(new BlockPos(6,3,6));
        var offsets=List.of(new BlockPos(0,0,0),new BlockPos(1,0,0),new BlockPos(-1,0,0),new BlockPos(0,1,1));
        for(var offset:offsets) h.getLevel().setBlockAndUpdate(center.offset(offset),Blocks.STONE.defaultBlockState());
        var outside=center.offset(4,0,0);
        h.getLevel().setBlockAndUpdate(outside,Blocks.STONE.defaultBlockState());
        SpellCasting.burst(h.getLevel(),player(h),1,new SpellPayload(30,0,List.of(SpellEffect.utility("dissociation",0,0,0))),Vec3.atCenterOf(center),true);
        for(var offset:offsets) h.assertTrue(h.getLevel().getBlockState(center.offset(offset)).isAir(),"Burst skipped an in-range block");
        h.assertTrue(h.getLevel().getBlockState(outside).is(Blocks.STONE),"Burst exceeded its radius");
        h.succeed();
    }
    @GameTest(template="spell_arena",timeoutTicks=30)
    public static void permanentConstructionHasNoBlockEntities(GameTestHelper h) {
        var pos=h.absolutePos(new BlockPos(6,3,6));
        for(int i=0;i<2;i++) {
            var p=pos.offset(i,0,0);
            UtilitySpellEffects.applyBlock(SpellEffect.utility("construction",i,0,1),player(h),p.below(),Direction.UP,p);
            h.assertTrue(h.getLevel().getBlockState(p).is((i==0?ModBlocks.PERMANENT_VOID_CRYSTAL:ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL).get()),"Wrong permanent variant");
            h.assertTrue(h.getLevel().getBlockEntity(p)==null,"Permanent construction has per-block NBT");
        }
        var legacy=pos.offset(0,0,2);
        h.getLevel().setBlockAndUpdate(legacy,ModBlocks.REINFORCED_VOID_CRYSTAL.get().defaultBlockState());
        ((VoidCrystalBlockEntity)h.getLevel().getBlockEntity(legacy)).configure(400,true);
        h.runAtTickTime(5,()->{
            h.assertTrue(h.getLevel().getBlockState(legacy).is(ModBlocks.PERMANENT_REINFORCED_VOID_CRYSTAL.get()),"Legacy permanent block not upgraded");
            h.assertTrue(h.getLevel().getBlockEntity(legacy)==null,"Legacy block entity retained");
            h.succeed();
        });
    }
}
