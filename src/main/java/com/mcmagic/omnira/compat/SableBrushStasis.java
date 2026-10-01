package com.mcmagic.omnira.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.physics.constraint.*;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;
import java.util.*;

/** A native world constraint holds a ship without teleporting its physics body each tick. */
public final class SableBrushStasis {
    private record Hold(Player owner,ServerSubLevel ship,PhysicsConstraintHandle constraint,int ticks){}
    private static final Map<UUID,Hold> HOLDS=new HashMap<>();
    private static ServerSubLevel target(Player player){
        var hit=player.pick(player.blockInteractionRange(),0,false);
        if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK)return null;
        var container=SubLevelContainer.getContainer(player.level());
        if(container==null||!container.inBounds(block.getBlockPos()))return null;
        var plot=container.getPlot(new ChunkPos(block.getBlockPos()));
        return plot!=null&&plot.getSubLevel() instanceof ServerSubLevel ship?ship:null;
    }
    public static boolean targetsShip(Player player,BlockPos pos){
        var c=SubLevelContainer.getContainer(player.level());return c!=null&&c.inBounds(pos)&&c.getPlot(new ChunkPos(pos))!=null;
    }
    public static boolean active(Player player){return HOLDS.containsKey(player.getUUID());}
    public static boolean tick(Player player){
        var ship=target(player);var old=HOLDS.get(player.getUUID());
        if(ship==null||ship.isRemoved()||(old!=null&&old.ship!=ship)){release(player);return false;}
        // Never erase a physical structure occupied by a player.
        if(player.level().players().stream().anyMatch(p->ship.boundingBox().toMojang().inflate(.25,2,.25).intersects(p.getBoundingBox()))){release(player);return false;}
        if(old==null){
            var pipeline=dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem.require(player.level()).getPipeline();
            var pose=ship.logicalPose();var world=new org.joml.Vector3d(pose.position());var local=pose.transformPositionInverse(world,new org.joml.Vector3d());
            var constraint=pipeline.addConstraint(null,ship,new FixedConstraintConfiguration(world,local,new org.joml.Quaterniond(pose.orientation())));
            old=new Hold(player,ship,constraint,0);
        }
        int ticks=old.ticks+1;HOLDS.put(player.getUUID(),new Hold(player,ship,old.constraint,ticks));
        if(ticks%5==0&&player.level() instanceof net.minecraft.server.level.ServerLevel level)com.mcmagic.omnira.archaeology.BrushStasis.particles(level,ship.boundingBox().toMojang());
        if(ticks%20==0)player.getUseItem().hurtAndBreak(1,player,player.getUsedItemHand()==net.minecraft.world.InteractionHand.MAIN_HAND?net.minecraft.world.entity.EquipmentSlot.MAINHAND:net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        if(ticks>=1200){release(player);SubLevelContainer.getContainer(player.level()).removeSubLevel(ship,SubLevelRemovalReason.REMOVED);player.releaseUsingItem();}
        return true;
    }
    public static void release(Player player){var h=HOLDS.remove(player.getUUID());if(h!=null&&h.constraint.isValid())h.constraint.remove();}
    public static void sweep(){
        for(var h:List.copyOf(HOLDS.values()))if(!h.owner.isAlive()||h.owner.isRemoved()||!h.owner.isUsingItem()
                ||!(h.owner.getUseItem().getItem() instanceof com.mcmagic.omnira.archaeology.SpacetimeBrushItem)||target(h.owner)!=h.ship)release(h.owner);
    }
    private SableBrushStasis(){}
}
