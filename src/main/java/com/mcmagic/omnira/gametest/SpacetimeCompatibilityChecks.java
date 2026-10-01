package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.spacetime.SpacetimeStorm;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.*;
import java.util.*;

public final class SpacetimeCompatibilityChecks {
    private SpacetimeCompatibilityChecks(){}
    public static void create(GameTestHelper h){
        var level=h.getLevel();var anchor=h.absolutePos(new BlockPos(4,4,4));
        var structure=new com.simibubi.create.content.contraptions.bearing.BearingContraption(false,Direction.UP);
        structure.anchor=anchor;structure.bounds=new AABB(0,0,0,25,1,1);
        for(int x=0;x<25;x++){
            var local=new BlockPos(x,0,0);var world=anchor.offset(local);
            var state=(x==1 || x==24?Blocks.CHEST:Blocks.STONE).defaultBlockState();
            level.setBlock(world,state,2);
            if(level.getBlockEntity(world) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest){
                chest.setItem(0,new ItemStack(Items.DIAMOND,x==1?2:3));
                structure.getStorage().addBlock(level,state,world,local,chest);
            }
            structure.getBlocks().put(local,new StructureBlockInfo(local,state,null));
            level.removeBlockEntity(world);level.setBlock(world,Blocks.AIR.defaultBlockState(),18);
        }
        structure.getStorage().initialize();
        var entity=com.simibubi.create.content.contraptions.OrientedContraptionEntity.createAtYaw(level,structure,Direction.SOUTH,90);
        entity.setPos(Vec3.atLowerCornerOf(anchor));level.addFreshEntity(entity);
        var center=entity.toGlobalVector(new Vec3(.5,.5,.5),1);
        entity.setBoundingBox(new AABB(center,entity.toGlobalVector(new Vec3(25,1,1),1)).inflate(1));
        SpacetimeStorm.trigger(level,center);
        h.assertTrue(!entity.isRemoved(),"Storm erased whole Create structure");
        h.assertTrue(!structure.getBlocks().containsKey(BlockPos.ZERO) && structure.getBlocks().containsKey(new BlockPos(24,0,0)),"Rotated Create sphere removed wrong blocks");
        h.assertTrue(!structure.getStorage().getAllItemStorages().containsKey(new BlockPos(1,0,0)),"Destroyed Create storage survived");
        var retained=structure.getStorage().getAllItemStorages().get(new BlockPos(24,0,0));
        h.assertTrue(retained!=null && retained.getStackInSlot(0).getCount()==3,"Outside Create inventory lost");
        var saved=structure.writeNBT(level.registryAccess(),false);
        var restored=com.simibubi.create.content.contraptions.Contraption.fromNBT(level,saved,false);
        h.assertTrue(!restored.getBlocks().containsKey(BlockPos.ZERO) && restored.getBlocks().containsKey(new BlockPos(24,0,0)),"Create serialization restored erased blocks");
        entity.discard();h.succeed();
    }
    public static void sable(GameTestHelper h){
        SableChecks.run(h);
    }
    private static final class SableChecks {
    private static void run(GameTestHelper h){
        var level=h.getLevel();var anchor=h.absolutePos(new BlockPos(4,4,4));
        Set<BlockPos> blocks=new HashSet<>();
        for(int x=0;x<25;x++){var pos=anchor.offset(x,0,0);blocks.add(pos);level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);}
        var gathered=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.gatherConnectedBlocks(anchor,level,100,(a,b,c,d,e)->blocks.contains(c));
        var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level,anchor,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Sable fixture failed to assemble");
        h.runAfterDelay(4,()->{
            var first=ship.getPlot().getCenterBlock();
            var center=ship.logicalPose().transformPosition(Vec3.atCenterOf(first));
            SpacetimeStorm.trigger(level,center);
            h.assertTrue(!ship.isRemoved(),"Storm erased whole Sable ship");
            h.assertTrue(level.getBlockState(first).isAir() && level.getBlockState(first.offset(24,0,0)).is(Blocks.STONE),"Sable sphere did not preserve outside blocks");
            dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(level).removeSubLevel(ship,dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason.REMOVED);
            h.succeed();
        });
    }
    }
}
