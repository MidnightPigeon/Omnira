package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.item.FateCurioItem.Kind;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid="omnira")
public final class PredatorFate {
    public static boolean extraLoot(Entity credited){return credited instanceof ServerPlayer player&&ManuscriptReward.equipped(player,"talent")==Kind.APEX_PREDATOR;}
    @SubscribeEvent public static void hurt(LivingDamageEvent.Post event){
        if(event.getNewDamage()<=0||!(event.getEntity() instanceof ServerPlayer player)||!player.isAlive()
                ||ManuscriptReward.equipped(player,"curse")!=Kind.SPATIAL_OVERLOAD)return;
        var level=player.serverLevel();
        for(int attempt=0;attempt<16;attempt++){
            double x=player.getX()+(player.getRandom().nextDouble()*2-1)*8;
            double z=player.getZ()+(player.getRandom().nextDouble()*2-1)*8;
            double y=Math.min(level.getMaxBuildHeight()-2,player.getY()+4);
            var pos=BlockPos.containing(x,y,z);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||player.position().distanceToSqr(new net.minecraft.world.phys.Vec3(x,player.getY(),z))<1)continue;
            // Vanilla safe landing and the mod's existing spatial-stability restrictions apply.
            if(player.randomTeleport(x,y,z,true)){player.fallDistance=0;return;}
        }
    }
    private PredatorFate(){}
}
