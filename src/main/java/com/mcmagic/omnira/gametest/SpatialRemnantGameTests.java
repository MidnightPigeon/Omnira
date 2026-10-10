package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.space.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_remnant")
@PrefixGameTestTemplate(false)
public final class SpatialRemnantGameTests {
    private static SpatialRemnantBlockEntity place(GameTestHelper h){
        var pos=h.absolutePos(new BlockPos(7,4,7));
        h.getLevel().setBlockAndUpdate(pos,ModBlocks.SPATIAL_REMNANT.get().defaultBlockState());
        return (SpatialRemnantBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena",timeoutTicks=60)
    public static void poolsMobAndItemMovementAtExactlyOneHundred(GameTestHelper h){
        var remnant=place(h);var center=Vec3.atCenterOf(remnant.getBlockPos());
        var pig=h.spawn(EntityType.PIG,new BlockPos(6,4,7));pig.setNoAi(true);pig.setNoGravity(true);
        var item=new ItemEntity(h.getLevel(),center.x-1,center.y+2,center.z,new ItemStack(Items.STICK));
        item.setNoGravity(true);item.setDeltaMovement(Vec3.ZERO);h.getLevel().addFreshEntity(item);
        pig.setPos(center.x-1,center.y,center.z);remnant.tick();
        for(int i=1;i<=25;i++){
            int step=i;
            h.runAfterDelay(i,()->{
                h.assertTrue(Math.abs(remnant.distance()-(step-1)*4)<.001,"Movement was not pooled exactly: "+remnant.distance());
                double x=center.x+(step%2==1?1:-1);
                pig.setPos(x,center.y,center.z);pig.setDeltaMovement(Vec3.ZERO);
                item.setPos(x,center.y+2,center.z);item.setDeltaMovement(Vec3.ZERO);
            });
        }
        h.runAfterDelay(26,()->{
                    h.assertTrue(h.getLevel().getBlockState(remnant.getBlockPos()).is(ModBlocks.SPACE_WARP_POINT.get()),"100 units did not regenerate");
                    pig.discard();item.discard();
                    com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(h.getLevel(),remnant.getBlockPos(),h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL));
                    h.assertTrue(((SpatialRemnantBlockEntity)h.getLevel().getBlockEntity(remnant.getBlockPos())).distance()==0,"Harvest retained old progress");
                    h.succeed();
        });
    }
    @GameTest(template="spell_arena",timeoutTicks=30)
    public static void stationaryOutsideAndReentryDoNotEarnDistance(GameTestHelper h){
        var remnant=place(h);var center=Vec3.atCenterOf(remnant.getBlockPos());
        var pig=h.spawn(EntityType.PIG,new BlockPos(7,4,7));pig.setNoAi(true);pig.setNoGravity(true);pig.setPos(center);
        remnant.tick();
        h.runAfterDelay(3,()->{h.assertTrue(remnant.distance()==0,"Standing still earned movement");pig.setPos(center.add(8,0,0));remnant.tick();});
        h.runAfterDelay(4,()->{pig.setPos(center.add(8,2,0));remnant.tick();});
        h.runAfterDelay(5,()->{pig.setPos(center.add(-2,0,0));remnant.tick();h.assertTrue(remnant.distance()==0,"Outside/reentry travel counted");});
        h.runAfterDelay(6,()->pig.setPos(center.add(-2,1,0)));
        h.runAfterDelay(7,()->{h.assertTrue(Math.abs(remnant.distance()-1)<.001,"Vertical movement ignored: "+remnant.distance());pig.discard();h.succeed();});
    }
    @GameTest(template="spell_arena")
    public static void progressSurvivesReloadAndLegacyRemnantGetsTicker(GameTestHelper h){
        var remnant=place(h);remnant.restoreDistance(42.5);
        var saved=remnant.saveWithoutMetadata(h.getLevel().registryAccess());
        var copy=new SpatialRemnantBlockEntity(remnant.getBlockPos(),remnant.getBlockState());
        copy.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(copy.distance()==42.5,"Reload lost progress");
        var chunk=h.getLevel().getChunkAt(remnant.getBlockPos());chunk.removeBlockEntity(remnant.getBlockPos());
        SpatialRemnantMigration.restoreMissing(chunk);
        h.assertTrue(chunk.getBlockEntities().get(remnant.getBlockPos()) instanceof SpatialRemnantBlockEntity,"Legacy remnant remained inert");
        h.succeed();
    }
}
