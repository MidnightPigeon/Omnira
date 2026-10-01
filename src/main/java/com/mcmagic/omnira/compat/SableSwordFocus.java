package com.mcmagic.omnira.compat;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.function.Supplier;

/** Loaded only when Sable is present; the anchor follows the structure's current pose. */
public final class SableSwordFocus {
    private SableSwordFocus() {}
    public static Supplier<Vec3> capture(Player player) {
        var container=SubLevelContainer.getContainer(player.level());
        if(container==null)return null;
        var ship=((EntityMovementExtension)player).sable$getTrackingSubLevel();
        if(ship==null) {
            for(var candidate:container.getAllSubLevels()) {
                if(candidate.boundingBox().toMojang().intersects(player.getBoundingBox())) {ship=candidate;break;}
            }
        }
        if(ship==null)return null;
        var captured=ship;
        Vec3 local=captured.logicalPose().transformPositionInverse(player.position());
        return ()->captured.isRemoved()?null:captured.logicalPose().transformPosition(local);
    }
}
