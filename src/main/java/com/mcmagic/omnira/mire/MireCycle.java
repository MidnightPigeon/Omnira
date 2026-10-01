package com.mcmagic.omnira.mire;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;

@EventBusSubscriber(modid="omnira")
public final class MireCycle {
    public static final int PERIOD=com.mcmagic.omnira.time.TimeBiomePulse.PERIOD,DURATION=1200,RECORD_AT=4800;
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:reversion_mire"));
    private static final String RECORD="OmniraMirePosition",AGE="OmniraMireAge";
    public static boolean reversing(long time){return time>=PERIOD&&Math.floorMod(time,PERIOD)<DURATION;}
    public static long end(long time){return time-Math.floorMod(time,PERIOD)+DURATION;}
    @SubscribeEvent public static void tick(EntityTickEvent.Post event){
        if(event.getEntity().level() instanceof ServerLevel level)update(event.getEntity(),level.getGameTime());
    }
    public static void update(Entity entity,long now){
        if(!(entity.level() instanceof ServerLevel level)||!entity.isAlive())return;
        var data=entity.getPersistentData();
        if(com.mcmagic.omnira.time.TemporalCreatureTags.timeImmune(entity)){
            data.remove(RECORD);
            if(entity instanceof Mob mob&&data.contains(AGE))restoreAge(mob);
            return;
        }
        boolean inside=level.getBiome(entity.blockPosition()).is(BIOME);
        if(Math.floorMod(now,PERIOD)==RECORD_AT&&inside){
            var n=new CompoundTag();n.putLong("At",now);n.putString("Dimension",level.dimension().location().toString());
            n.putDouble("X",entity.getX());n.putDouble("Y",entity.getY());n.putDouble("Z",entity.getZ());data.put(RECORD,n);
        }
        if(data.contains(RECORD)){
            var n=data.getCompound(RECORD);long elapsed=now-n.getLong("At");
            if(elapsed==DURATION&&!n.getBoolean("Applied")&&n.getString("Dimension").equals(level.dimension().location().toString())){
                n.putBoolean("Applied",true);
                var root=entity.getRootVehicle();var groupRecord=root.getPersistentData().getCompound(RECORD);
                boolean follows=root!=entity&&groupRecord.getLong("At")==n.getLong("At")&&groupRecord.getString("Dimension").equals(n.getString("Dimension"));
                if(!follows){
                    if(entity.isPassenger())entity.stopRiding();
                    entity.teleportTo(n.getDouble("X"),n.getDouble("Y"),n.getDouble("Z"));
                    entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;
                    for(var passenger:entity.getIndirectPassengers()){
                        passenger.teleportTo(entity.getX(),entity.getY()+entity.getBbHeight(),entity.getZ());passenger.fallDistance=0;
                    }
                    if(entity instanceof net.minecraft.server.level.ServerPlayer player)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.mire_reversal"),true);
                }
                // Retain the record until the next tick so riders can recognize a rewound vehicle.
                inside=level.getBiome(entity.blockPosition()).is(BIOME);
            }
            if(elapsed>DURATION||elapsed<0)data.remove(RECORD);
        }
        if(entity instanceof Mob mob){
            if(data.contains(AGE)&&now>=data.getCompound(AGE).getLong("Until"))restoreAge(mob);
            if(reversing(now)&&inside&&!data.contains(AGE)){
                var n=new CompoundTag();n.putLong("Until",end(now));n.putBoolean("Baby",mob.isBaby());
                if(mob instanceof AgeableMob ageable)n.putInt("Age",ageable.getAge());
                mob.setBaby(true);
                if(mob.isBaby())data.put(AGE,n);
            }
            if(data.contains(AGE)){
                if(mob instanceof AgeableMob ageable)ageable.setAge(-24000);else mob.setBaby(true);
            }
        }
    }
    private static void restoreAge(Mob mob){
        var data=mob.getPersistentData();var n=data.getCompound(AGE);
        if(mob instanceof AgeableMob ageable)ageable.setAge(n.getInt("Age"));else mob.setBaby(n.getBoolean("Baby"));
        data.remove(AGE);
    }
    @SubscribeEvent public static void level(LevelTickEvent.Post event){
        if(event.getLevel() instanceof ServerLevel level&&level.getGameTime()%20==0){
            MireTrees.get(level).pulse(level,level.getGameTime());
            MillTreasures.get(level).pulse(level,level.getGameTime());
        }
    }
}
