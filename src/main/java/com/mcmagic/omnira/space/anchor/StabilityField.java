package com.mcmagic.omnira.space.anchor;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/** Persistent field sources remain effective across unloaded target chunks, without loading them. */
@EventBusSubscriber(modid="omnira")
public final class StabilityField extends SavedData {
    public static final int RADIUS=64;
    private final Set<BlockPos> anchors=new HashSet<>();
    private final Set<BlockPos> stellarOrbs=new HashSet<>();
    private static final ThreadLocal<Set<UUID>> TOOL_TRAVEL=ThreadLocal.withInitial(Set::of);
    public static <T> T toolTravel(Entity root,boolean enhanced,java.util.function.Supplier<T> action){
        if(!enhanced)return action.get();
        return permittedTravel(root,action);
    }
    public static Entity portalTravel(Entity root,net.minecraft.world.level.portal.DimensionTransition transition){
        return portalTravel(root,()->root.changeDimension(transition));
    }
    public static <T> T portalTravel(Entity root,java.util.function.Supplier<T> action){
        return permittedTravel(root,action);
    }
    private static <T> T permittedTravel(Entity root,java.util.function.Supplier<T> action){
        var previous=TOOL_TRAVEL.get();
        TOOL_TRAVEL.set(root.getSelfAndPassengers().map(Entity::getUUID).collect(java.util.stream.Collectors.toSet()));
        try{return action.get();}finally{TOOL_TRAVEL.set(previous);}
    }
    public static StabilityField get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(StabilityField::new,StabilityField::load),"omnira_stability_fields");}
    private static StabilityField load(CompoundTag tag,HolderLookup.Provider provider){var data=new StabilityField();for(long pos:tag.getLongArray("Anchors"))data.anchors.add(BlockPos.of(pos));for(long pos:tag.getLongArray("StellarOrbs"))data.stellarOrbs.add(BlockPos.of(pos));return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider provider){tag.putLongArray("Anchors",anchors.stream().mapToLong(BlockPos::asLong).toArray());tag.putLongArray("StellarOrbs",stellarOrbs.stream().mapToLong(BlockPos::asLong).toArray());return tag;}
    public static void update(ServerLevel level,BlockPos pos,boolean active){var data=get(level);if(active?data.anchors.add(pos.immutable()):data.anchors.remove(pos))data.setDirty();}
    public static void updateStellar(ServerLevel level,BlockPos pos,boolean active){var data=get(level);if(active?data.stellarOrbs.add(pos.immutable()):data.stellarOrbs.remove(pos))data.setDirty();}
    public static boolean contains(ServerLevel level,Vec3 point){
        return strength(level,point)>0;
    }
    public static int strength(ServerLevel level,Vec3 point){
        var data=get(level);var it=data.anchors.iterator();
        while(it.hasNext()){
            var pos=it.next();
            if(level.hasChunkAt(pos)&&!level.getBlockState(pos).is(AnchorContent.ACTIVE.get())){it.remove();data.setDirty();continue;}
            if(Vec3.atCenterOf(pos).distanceToSqr(point)<=RADIUS*RADIUS)return 2;
        }
        it=data.stellarOrbs.iterator();
        while(it.hasNext()){
            var pos=it.next();
            if(level.hasChunkAt(pos)&&(!(level.getBlockEntity(pos) instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity orb)||!orb.stabilized())){it.remove();data.setDirty();continue;}
            if(Vec3.atCenterOf(pos).distanceToSqr(point)<=32*32)return 1;
        }
        return 0;
    }
    public static boolean contains(Entity entity){return entity.level() instanceof ServerLevel level&&contains(level,entity.position());}
    public static boolean blocks(Entity entity,ServerLevel destination,Vec3 point){
        if(TOOL_TRAVEL.get().contains(entity.getUUID()))return false;
        if(entity.level()==destination&&entity.position().distanceToSqr(point)<1e-8)return false;
        return entity.getRootVehicle().getSelfAndPassengers().anyMatch(StabilityField::contains)||contains(destination,point);
    }
    @SubscribeEvent public static void pearl(net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl event){
        if(event.getPearlEntity().level() instanceof ServerLevel destination&&blocks(event.getEntity(),destination,event.getTarget()))event.setCanceled(true);
    }
    @SubscribeEvent public static void affectHostiles(EntityTickEvent.Post event){
        if(!(event.getEntity() instanceof LivingEntity living)||!(living.level() instanceof ServerLevel level)||!living.isAlive())return;
        if(!(living instanceof net.minecraft.world.entity.monster.Enemy)&&living.getType().getCategory()!=MobCategory.MONSTER)return;
        int damage=strength(level,living.position());if(damage==0)return;
        long now=level.getGameTime();
        if(now%5==0)StabilityAnchorEntity.orbit(level,living.position().add(0,living.getBbHeight()*.5,0),living.getBbWidth()*.7+.2,now);
        if(now%20==0&&living.getPersistentData().getLong("StabilityDamageTick")!=now){
            living.getPersistentData().putLong("StabilityDamageTick",now);
            com.mcmagic.omnira.space.sea.VoidSeaEffects.trueDamage(living,damage,living.damageSources().genericKill());
        }
    }
}
