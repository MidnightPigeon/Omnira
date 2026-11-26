package com.mcmagic.omnira.entity.spacetime;

import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.time.TimeMosquitoSpawning;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

/** Shares the existing five-minute pulse; bounded searches stay in loaded chunks. */
public final class SpacetimeFaunaSpawning {
    public static final TagKey<Biome> SPACE=TagKey.create(Registries.BIOME,ResourceLocation.parse("omnira:space_creatures"));
    public static boolean habitat(net.minecraft.core.Holder<Biome> biome,EntityType<?> type){
        if(type==ModEntityTypes.AGECOIL_SNAIL.get())return biome.is(TimeMosquitoSpawning.BIOMES);
        if(type==ModEntityTypes.RIFT_MOTH.get())return biome.is(TimeMosquitoSpawning.BIOMES)||biome.is(SPACE);
        return biome.is(SPACE);
    }
    public static void registerPlacements(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event){
        for(var type:java.util.List.of(ModEntityTypes.AGECOIL_SNAIL.get(),ModEntityTypes.RIFTSTEP_DEER.get(),ModEntityTypes.RIFT_MOTH.get())){
            event.register(type, type==ModEntityTypes.RIFT_MOTH.get()?SpawnPlacementTypes.NO_RESTRICTIONS:SpawnPlacementTypes.ON_GROUND,
                    net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    (entity,level,reason,pos,random)->habitat(level.getBiome(pos),entity)&&level.getFluidState(pos).isEmpty(),
                    net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }
    public static void pulsePlayer(ServerLevel level,ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        var biome=level.getBiome(player.blockPosition());
        if(!biome.is(SPACE)||level.getDifficulty()==Difficulty.PEACEFUL||level.random.nextFloat()>=.2F)return;
        var type=ModEntityTypes.RIFT_MANTIS.get();
        if(level.getEntitiesOfClass(RiftMantis.class,player.getBoundingBox().inflate(96)).size()>=2)return;
        for(int attempt=0;attempt<12;attempt++){
            int x=player.getBlockX()+level.random.nextInt(97)-48,z=player.getBlockZ()+level.random.nextInt(97)-48;
            if(!level.hasChunk(x>>4,z>>4))continue;
            var mob=type.create(level);if(mob==null)return;
            for(int offset=0;offset<=64;offset++){
                int y=player.getBlockY()+(offset%2==0?offset/2:-(offset+1)/2);
                var feet=new BlockPos(x,y,z);Vec3 point=Vec3.atBottomCenterOf(feet);
                if(!habitat(level.getBiome(feet),type)||!SpatialSteps.safe(mob,point)||level.getNearestPlayer(point.x,point.y,point.z,24,false)!=null)continue;
                mob.moveTo(point.x,point.y,point.z,level.random.nextFloat()*360,0);
                mob.finalizeSpawn(level,level.getCurrentDifficultyAt(feet),MobSpawnType.NATURAL,null);
                level.addFreshEntity(mob);
                return;
            }
        }
    }
    private SpacetimeFaunaSpawning(){}
}
