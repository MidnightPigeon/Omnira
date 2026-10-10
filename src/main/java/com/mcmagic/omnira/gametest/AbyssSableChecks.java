package com.mcmagic.omnira.gametest;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3d;
import java.util.Set;

final class AbyssSableChecks {
    static void run(GameTestHelper h){
        var level=h.getLevel();var p=h.absolutePos(new BlockPos(10,5,10));int middle=InvertedAbyssGameTests.paint(h,p);
        var anchor=new BlockPos(p.getX(),middle+12,p.getZ());level.setBlockAndUpdate(anchor,Blocks.STONE.defaultBlockState());
        var gathered=SubLevelAssemblyHelper.gatherConnectedBlocks(anchor,level,10,(a,b,c,d,e)->c.equals(anchor));
        var ship=SubLevelAssemblyHelper.assembleBlocks(level,anchor,gathered.blocks(),gathered.boundingBox());
        h.assertTrue(ship!=null,"Sable fixture failed");
        h.runAfterDelay(6,()->{
            try{
                h.assertTrue(AbyssGravity.direction(level,ship.boundingBox().toMojang().getCenter())==-1,"Sable body left inverted region");
                var velocity=RigidBodyHandle.of(ship).getLinearVelocity(new Vector3d());
                h.assertTrue(velocity.y>0,"Sable native body did not accelerate toward cave ceiling: "+velocity);
            }finally{SubLevelContainer.getContainer(level).removeSubLevel(ship,SubLevelRemovalReason.REMOVED);}
            h.succeed();
        });
    }
}
