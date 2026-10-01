package com.mcmagic.omnira.archaeology;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Sessions expire immediately when their owner stops using or changes targets. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class BrushStasis {
    private record Hold(Player owner, Entity target) {}
    private static final Map<UUID, Hold> HOLDS = new HashMap<>();
    @net.neoforged.bus.api.SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))com.mcmagic.omnira.compat.SableBrushStasis.sweep();
        for(var hold:java.util.List.copyOf(HOLDS.values()))if(held(hold.target)&&hold.target.level() instanceof net.minecraft.server.level.ServerLevel level&&level.getGameTime()%5==0)particles(level,hold.target.getBoundingBox());
    }
    private static void sync(Entity target,boolean held){net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(target,new com.mcmagic.omnira.network.BrushHoldPayload(target.getId(),held));}
    public static void hold(Player owner, Entity target) {
        var old=HOLDS.put(owner.getUUID(),new Hold(owner,target));
        if(old==null||old.target!=target)sync(target,true);
    }
    public static void particles(net.minecraft.server.level.ServerLevel level,net.minecraft.world.phys.AABB bounds){
        double t=level.getGameTime()*.15;
        for(int i=0;i<12;i++){
            double angle=t+i*Math.PI/6,y=bounds.minY+(bounds.getYsize()+.15)*(i%4)/3;
            var color=i%3==0?new org.joml.Vector3f(.95F,1,1):new org.joml.Vector3f(.58F,.95F,.77F);
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(color,.8F),bounds.getCenter().x+Math.cos(angle)*(bounds.getXsize()/2+.15),y,bounds.getCenter().z+Math.sin(angle)*(bounds.getZsize()/2+.15),1,0,0,0,0);
        }
    }
    public static void release(Player owner) {var h=HOLDS.remove(owner.getUUID());if(h!=null&&HOLDS.values().stream().noneMatch(other->other.target==h.target))sync(h.target,false);}
    public static boolean held(Entity target) {
        if(target.level().isClientSide)return target.getPersistentData().getBoolean("OmniraBrushHeld");
        for(var h:List.copyOf(HOLDS.values()))if(!h.owner.isAlive() || h.owner.isRemoved() || !h.owner.isUsingItem()
                || !(h.owner.getUseItem().getItem() instanceof SpacetimeBrushItem)
                || h.target.isRemoved() || SpacetimeBrushItem.target(h.owner)!=h.target)release(h.owner);
        return HOLDS.values().stream().anyMatch(h->h.target==target);
    }
    @net.neoforged.bus.api.SubscribeEvent public static void tracking(net.neoforged.neoforge.event.entity.player.PlayerEvent.StartTracking event){
        if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player&&held(event.getTarget()))net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new com.mcmagic.omnira.network.BrushHoldPayload(event.getTarget().getId(),true));
    }
    private BrushStasis() {}
}
