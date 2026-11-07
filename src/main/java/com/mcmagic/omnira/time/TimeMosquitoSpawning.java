package com.mcmagic.omnira.time;

import com.mcmagic.omnira.entity.TimeThiefMosquito;
import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/** Invoked only by the existing five-minute temporal pulse. Never loads terrain. */
public final class TimeMosquitoSpawning {
    public static final TagKey<Biome> BIOMES=TagKey.create(Registries.BIOME,ResourceLocation.parse("omnira:time_creatures"));
    public static void pulsePlayer(ServerLevel level,ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||level.getDifficulty()==Difficulty.PEACEFUL
                ||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||!level.getBiome(player.blockPosition()).is(BIOMES)||!level.random.nextBoolean())return;
        if(level.getEntitiesOfClass(TimeThiefMosquito.class,player.getBoundingBox().inflate(96)).size()>=3)return;
        for(int i=0;i<8;i++){
            int x=player.getBlockX()+level.random.nextInt(129)-64,z=player.getBlockZ()+level.random.nextInt(129)-64;
            if(!level.hasChunk(x>>4,z>>4))continue;
            int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
            BlockPos pos=new BlockPos(x,ground+3+level.random.nextInt(3),z);
            if(!validPosition(level,pos)||level.getNearestPlayer(x+.5,pos.getY(),z+.5,24,false)!=null)continue;
            var mob=ModEntityTypes.TIME_THIEF_MOSQUITO.get().create(level);if(mob==null)return;
            mob.moveTo(x+.5,pos.getY(),z+.5,level.random.nextFloat()*360,0);
            if(!level.noCollision(mob))continue;
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(pos),MobSpawnType.NATURAL,null);level.addFreshEntity(mob);return;
        }
    }
    public static boolean validPosition(ServerLevel level,BlockPos pos){
        if(!level.hasChunkAt(pos)||!level.getBiome(pos).is(BIOMES)||!level.getBlockState(pos).isAir())return false;
        for(int depth=1;depth<=5;depth++){
            var ground=pos.below(depth);var state=level.getBlockState(ground);
            if(!state.isAir())return depth>=3&&state.isFaceSturdy(level,ground,Direction.UP)&&level.getFluidState(ground).isEmpty();
        }
        return false;
    }
    private TimeMosquitoSpawning(){}
}
