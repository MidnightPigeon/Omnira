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
        if(type==ModEntityTypes.RIFT_MOTH.get())return biome.is(TimeMosquitoSpawning.BIOMES)||biome.is(SPACE)||biome.is(ResourceLocation.parse("omnira:spacetime_corridor"));
        return biome.is(SPACE);
    }
    public static void pulsePlayer(ServerLevel level,ServerPlayer player){
        if(!player.isAlive()||player.isSpectator()||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING))return;
        var biome=level.getBiome(player.blockPosition());
        if(biome.is(TimeMosquitoSpawning.BIOMES)&&level.random.nextFloat()<.75F)spawn(level,player,ModEntityTypes.AGECOIL_SNAIL.get(),8,1);
        if(biome.is(SPACE)){
            if(level.random.nextFloat()<.75F)spawn(level,player,ModEntityTypes.RIFTSTEP_DEER.get(),8,1);
            if(level.getDifficulty()!=Difficulty.PEACEFUL&&level.random.nextFloat()<.2F)spawn(level,player,ModEntityTypes.RIFT_MANTIS.get(),2,1);
        }
        if(habitat(biome,ModEntityTypes.RIFT_MOTH.get())&&level.random.nextBoolean())spawn(level,player,ModEntityTypes.RIFT_MOTH.get(),15,3+level.random.nextInt(3));
    }
    private static <T extends Mob> void spawn(ServerLevel level,ServerPlayer player,EntityType<T> type,int cap,int count){
        int existing=level.getEntitiesOfClass(Mob.class,player.getBoundingBox().inflate(96),e->e.getType()==type).size();
        if(existing+count>cap)return;
        for(int attempt=0;attempt<12;attempt++){
            int x=player.getBlockX()+level.random.nextInt(97)-48,z=player.getBlockZ()+level.random.nextInt(97)-48;
            if(!level.hasChunk(x>>4,z>>4))continue;
            var mob=type.create(level);if(mob==null)return;
            for(int offset=0;offset<=64;offset++){
                int y=player.getBlockY()+(offset%2==0?offset/2:-(offset+1)/2);
                var feet=new BlockPos(x,y,z);Vec3 point=Vec3.atBottomCenterOf(feet);
                if(!habitat(level.getBiome(feet),type)||!(type==ModEntityTypes.RIFT_MOTH.get()?SpatialSteps.clear(mob,point.add(0,2,0)):SpatialSteps.safe(mob,point))||level.getNearestPlayer(point.x,point.y,point.z,24,false)!=null)continue;
                var flock=java.util.UUID.randomUUID();var group=new java.util.ArrayList<T>();
                for(int i=0;i<count;i++){
                    var member=i==0?mob:type.create(level);if(member==null)break;
                    Vec3 p=point.add(i==0?0:Math.cos(i*2.4)*1.4,type==ModEntityTypes.RIFT_MOTH.get()?2+i*.2:0,i==0?0:Math.sin(i*2.4)*1.4);
                    member.moveTo(p.x,p.y,p.z,level.random.nextFloat()*360,0);
                    if(!level.hasChunkAt(member.blockPosition())||!habitat(level.getBiome(member.blockPosition()),type)||!level.noCollision(member)||level.containsAnyLiquid(member.getBoundingBox()))break;
                    if(member instanceof RiftMoth moth)moth.setFlock(flock);
                    group.add(member);
                }
                // A moth flock is added whole or not at all.
                if(group.size()!=count)continue;
                for(var member:group){member.finalizeSpawn(level,level.getCurrentDifficultyAt(member.blockPosition()),MobSpawnType.NATURAL,null);level.addFreshEntity(member);}return;
            }
        }
    }
    private SpacetimeFaunaSpawning(){}
}
