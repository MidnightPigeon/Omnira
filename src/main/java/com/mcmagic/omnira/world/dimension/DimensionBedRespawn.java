package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.mixin.CompassRespawnAccessor;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;

@EventBusSubscriber(modid="omnira")
public final class DimensionBedRespawn {
    private DimensionBedRespawn() {}
    @SubscribeEvent public static void mark(CanPlayerSleepEvent event){
        var player=event.getEntity();var world=player.serverLevel();var pos=event.getPos();
        if(world.dimension()!=ModDimensions.DREAM_REALM && world.dimension()!=ModDimensions.SPACETIME_CORRIDOR)return;
        if(event.getProblem()!=Player.BedSleepingProblem.NOT_POSSIBLE_HERE || !player.isAlive() || player.isSleeping()
                || !event.getState().isBed(world,pos,player))return;
        if(Math.abs(player.getX()-(pos.getX()+.5))>3 || Math.abs(player.getY()-(pos.getY()+.5))>2
                || Math.abs(player.getZ()-(pos.getZ()+.5))>3){event.setProblem(Player.BedSleepingProblem.TOO_FAR_AWAY);return;}
        if(CompassRespawnAccessor.omnira$respawn(world,pos,player.getYRot(),false,true).isEmpty()){
            event.setProblem(Player.BedSleepingProblem.OBSTRUCTED);return;
        }
        player.setRespawnPosition(world.dimension(),pos,player.getYRot(),false,true);
        // These dimensions have their own clocks; beds mark a spawn without advancing time.
        event.setProblem(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
    }
}
