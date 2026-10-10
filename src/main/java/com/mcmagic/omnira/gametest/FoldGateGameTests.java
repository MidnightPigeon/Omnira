package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("omnira_fold_gates")
@PrefixGameTestTemplate(false)
public final class FoldGateGameTests {
    @GameTest(template="spell_arena",batch="anchor_portal_cross") public static void crossDimensionPairAndTransfer(GameTestHelper h){
        var destination=h.getLevel().getServer().getLevel(net.minecraft.world.level.Level.NETHER);
        h.assertTrue(destination!=null,"Nether unavailable for cross-dimension regression");
        var a=gate(h,new BlockPos(3,3,3),ModBlocks.STABLE_FOLD_GATE.get());
        var p=new BlockPos(a.getBlockPos().getX(),80,a.getBlockPos().getZ());destination.getChunkAt(p);
        var chunk=new net.minecraft.world.level.ChunkPos(p);
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)destination.setChunkForced(chunk.x+dx,chunk.z+dz,true);
        for(int dx=-3;dx<=3;dx++)for(int dz=-3;dz<=3;dz++)for(int dy=-1;dy<=3;dy++)destination.setBlock(p.offset(dx,dy,dz),(dy==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
        var s=ModBlocks.STABLE_FOLD_GATE.get().defaultBlockState();destination.setBlock(p,s,2);destination.setBlock(p.above(),s.setValue(FoldGateBlock.HALF,DoubleBlockHalf.UPPER),3);
        var b=(FoldGateBlockEntity)destination.getBlockEntity(p);FoldGateBlockEntity.pair(a,b);
        var anchorState=com.mcmagic.omnira.space.anchor.AnchorContent.ACTIVE.get().defaultBlockState();
        h.getLevel().setBlockAndUpdate(a.getBlockPos().above(4),anchorState);destination.setBlockAndUpdate(p.above(4),anchorState);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(3,3,4));pig.setNoAi(true);var id=pig.getUUID();
        h.runAfterDelay(20,()->h.assertTrue(FoldGateTransit.transfer(a,pig,true),"Cross-dimension transfer failed"));
        // A newly loaded destination publishes its entity section on a subsequent tick.
        h.runAfterDelay(40,()->{
            h.assertTrue(destination.getEntity(id)!=null,"Transferred entity missing from destination");
            h.assertTrue(a.peer!=null&&a.peer.dimension().equals(destination.dimension()),"Persistent destination lost");
            a.unlink();h.assertTrue(b.peer==null,"Cross-dimension unpair failed");
            destination.getEntity(id).discard();destination.removeBlock(p,false);destination.removeBlock(p.above(),false);
            h.getLevel().removeBlock(a.getBlockPos().above(4),false);destination.removeBlock(p.above(4),false);
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)destination.setChunkForced(chunk.x+dx,chunk.z+dz,false);
            h.succeed();
        });
    }
    @GameTest(template="spell_arena") public static void unsafeLandingAndUnstableDamage(GameTestHelper h){
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(5,3,5));pig.setNoAi(true);
        var p=h.absolutePos(new BlockPos(5,8,5));h.setBlock(new BlockPos(5,7,5),Blocks.STONE);h.setBlock(new BlockPos(5,8,5),Blocks.WATER);
        h.assertTrue(FoldGateTransit.landing(h.getLevel(),pig,p,0,0)==null,"Liquid arrival accepted");
        h.setBlock(new BlockPos(5,8,5),Blocks.STONE);
        h.assertTrue(FoldGateTransit.landing(h.getLevel(),pig,p,0,0)==null,"Solid arrival accepted");
        h.assertTrue(FoldGateTransit.unstableDamage(20,0)==4&&FoldGateTransit.unstableDamage(20,1)==10,"Unstable damage range incorrect");
        pig.discard();h.succeed();
    }
    private static FoldGateBlockEntity gate(GameTestHelper h,BlockPos p,Block block){
        var pos=h.absolutePos(p);var s=block.defaultBlockState();
        h.getLevel().setBlock(pos,s,2);h.getLevel().setBlock(pos.above(),s.setValue(FoldGateBlock.HALF,DoubleBlockHalf.UPPER),3);
        return (FoldGateBlockEntity)h.getLevel().getBlockEntity(pos);
    }
    @GameTest(template="spell_arena") public static void pairPersistUnlinkAndReplacement(GameTestHelper h){
        var a=gate(h,new BlockPos(3,3,3),ModBlocks.STABLE_FOLD_GATE.get());var b=gate(h,new BlockPos(10,3,10),ModBlocks.STABLE_FOLD_GATE.get());
        h.assertTrue(FoldGateBlockEntity.pair(a,b)&&a.white&&!b.white,"Pair markers missing");
        var saved=a.saveWithoutMetadata(h.getLevel().registryAccess());
        a.loadWithComponents(saved,h.getLevel().registryAccess());
        h.assertTrue(a.peer.pos().equals(b.getBlockPos())&&a.peerIdentity.equals(b.identity),"Pair not persisted");
        h.assertTrue(!FoldGateBlockEntity.pair(a,b),"Already paired gates accepted");
        h.getLevel().destroyBlock(b.getBlockPos().above(),false);
        h.assertTrue(a.peer==null&&h.getLevel().getBlockState(b.getBlockPos()).isAir(),"Breaking upper half did not unlink both ends");
        b=gate(h,new BlockPos(10,3,10),ModBlocks.STABLE_FOLD_GATE.get());
        h.assertTrue(b.peer==null&&a.peer==null,"Replacement inherited old link");h.succeed();
    }
    @GameTest(template="spell_arena") public static void stableTransferBothDirections(GameTestHelper h){
        for(int x=1;x<15;x++)for(int z=1;z<15;z++)h.setBlock(new BlockPos(x,2,z),Blocks.STONE);
        var a=gate(h,new BlockPos(3,3,3),ModBlocks.STABLE_FOLD_GATE.get());var b=gate(h,new BlockPos(10,3,10),ModBlocks.STABLE_FOLD_GATE.get());
        FoldGateBlockEntity.pair(a,b);
        var pig=h.spawn(net.minecraft.world.entity.EntityType.PIG,new BlockPos(3,3,4));pig.setNoAi(true);
        h.assertTrue(FoldGateTransit.transfer(a,pig,true)&&pig.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(b.getBlockPos()))<20,"Forward gate failed");
        h.assertTrue(FoldGateTransit.transfer(b,pig,true)&&pig.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(a.getBlockPos()))<20,"Return gate failed");
        pig.discard();a.unlink();h.succeed();
    }
    @GameTest(template="spell_arena") public static void tuningTopHalfAndCancel(GameTestHelper h){
        var a=gate(h,new BlockPos(3,3,3),ModBlocks.STABLE_FOLD_GATE.get());var b=gate(h,new BlockPos(10,3,10),ModBlocks.STABLE_FOLD_GATE.get());
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var rod=new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.storage.CrystalStorageContent.ROD.get());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,rod);
        for(var g:new FoldGateBlockEntity[]{a,b}){
            var p=g.getBlockPos().above();var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),Direction.SOUTH,p,false);
            rod.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit));
        }
        h.assertTrue(a.peer!=null&&b.peer!=null&&!FoldGateTuning.selected(rod),"Rod did not pair upper halves");
        var p=b.getBlockPos();rod.useOn(new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),Direction.SOUTH,p,false)));
        h.assertTrue(a.peer==null&&b.peer==null,"Rod did not unlink pair");h.succeed();
    }
    @GameTest(template="spell_arena") public static void frameActivationAndCollision(GameTestHelper h){
        var pos=new BlockPos(5,3,5);var frame=gate(h,pos,ModBlocks.FOLD_GATE_FRAME.get());var p=frame.getBlockPos();
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(h.getLevel());player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for(boolean stable:new boolean[]{true,false}){
            gate(h,pos,ModBlocks.FOLD_GATE_FRAME.get());var item=new net.minecraft.world.item.ItemStack((stable?ModItems.SPACE_WARP_POINT:ModItems.UNSTABLE_SPACETIME_AGGREGATE).get(),2);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,item);
            var state=h.getLevel().getBlockState(p);state.useItemOn(item,h.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),Direction.SOUTH,p,false));
            var expected=(stable?ModBlocks.STABLE_FOLD_GATE:ModBlocks.UNSTABLE_FOLD_GATE).get();
            h.assertTrue(h.getLevel().getBlockState(p).is(expected)&&h.getLevel().getBlockState(p.above()).is(expected)&&item.getCount()==1,"Activation not atomic or wrong cost");
            var shape=h.getLevel().getBlockState(p).getCollisionShape(h.getLevel(),p);
            h.assertTrue(!shape.isEmpty()&&!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape,net.minecraft.world.phys.shapes.Shapes.box(.2,0,0,.8,1,1),net.minecraft.world.phys.shapes.BooleanOp.AND),"Door opening blocked");
        }
        h.succeed();
    }
    @GameTest(template="spell_arena") public static void authoredPlansAndLoot(GameTestHelper h){
        for(boolean upper:new boolean[]{false,true}){
            int gates=0,loot=0;
            for(var cell:FoldObservatoryPlan.cells(upper)){
                var state=com.mcmagic.omnira.shop.KirisameShopPlan.state(cell);
                h.assertTrue(!state.isAir()||cell.name().equals("minecraft:air"),"Unknown authored block: "+cell.name());
                if(state.is(ModBlocks.STABLE_FOLD_GATE.get()))gates++;
                if(!cell.loot().isEmpty())loot++;
            }
            h.assertTrue(gates==2&&loot==1,"Each plaza needs one complete gate and one treasure orb");
        }
        h.succeed();
    }
}
