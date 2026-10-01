package com.mcmagic.omnira.time;

import com.mcmagic.omnira.archaeology.ArchaeologyContent;
import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.TimeNatureContent;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/** Plants and crystals advance or decay at the shared five-minute node. */
public final class EventideRuins {
    public static final int PERIOD=TimeBiomePulse.PERIOD;
    public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:eventide_ruins"));
    private static final String ENTITY_CYCLE="OmniraEventideCycle";
    private static final net.minecraft.tags.TagKey<Block> COMMON_DIRTS=net.minecraft.tags.TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:dirts"));
    private static final net.minecraft.tags.TagKey<Block> COMMON_SANDS=net.minecraft.tags.TagKey.create(Registries.BLOCK,ResourceLocation.parse("c:sands"));

    public static void pulseChunk(ServerLevel level,int cx,int cz){
        pulseChunk(level,cx,cz,level.getGameTime()/PERIOD);
    }

    public static void pulseChunk(ServerLevel level,int cx,int cz,long cycle){
        var matrices=new ArrayList<BlockPos>();
        var pos=new BlockPos.MutableBlockPos();
        for(int x=cx<<4;x<(cx<<4)+16;x++)for(int z=cz<<4;z<(cz<<4)+16;z++){
            pos.set(x,224,z);
            if(!level.getBiome(pos).is(BIOME))continue;
            for(int y=com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE+1;y<level.getMaxBuildHeight();y++){
                pos.setY(y);var state=level.getBlockState(pos);
                if(state.getBlock() instanceof LiquidBlock&&!com.mcmagic.omnira.mire.MireContent.fluid(state.getFluidState())){
                    level.setBlockAndUpdate(pos,com.mcmagic.omnira.mire.MireContent.LIQUID.get().defaultBlockState());
                }else if(state.is(Blocks.STONE_BRICKS)){
                    level.setBlockAndUpdate(pos,EventideMasonry.BRICKS.get().defaultBlockState());
                }else if(state.is(Blocks.CRACKED_STONE_BRICKS)){
                    level.setBlockAndUpdate(pos,EventideMasonry.CRACKED.get().defaultBlockState());
                }else if(state.is(com.mcmagic.omnira.time.TemporalSoils.SILTS)&&!state.is(com.mcmagic.omnira.mire.MireContent.SILT.get())){
                    level.setBlockAndUpdate(pos,com.mcmagic.omnira.mire.MireContent.SILT.get().defaultBlockState());
                }else if(!state.is(TemporalSoils.SILTS)&&(state.is(BlockTags.SAND)||state.is(COMMON_SANDS)
                        ||state.is(net.neoforged.neoforge.common.Tags.Blocks.GRAVELS))){
                    if(!state.is(ArchaeologyContent.SAND.get()))level.setBlockAndUpdate(pos,ArchaeologyContent.SAND.get().defaultBlockState());
                }else if(!state.is(TemporalSoils.SILTS)&&(state.is(BlockTags.DIRT)||state.is(COMMON_DIRTS))){
                    // One transition per node: the new silt rots only on the next pass.
                    level.setBlockAndUpdate(pos,DreamContent.TEMPORAL_SILT.get().defaultBlockState());
                }else if(mature(state)){
                    level.setBlockAndUpdate(pos,ArchaeologyContent.SAND.get().defaultBlockState());
                }else if(matrix(state))matrices.add(pos.immutable());
                else{
                    var next=advance(state);
                    if(next!=null)level.setBlockAndUpdate(pos,next);
                    else if(withers(state)){
                        level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
                        level.sendParticles(ParticleTypes.LARGE_SMOKE,x+.5,y+.4,z+.5,3,.18,.12,.18,.01);
                    }
                }
            }
        }
        for(var root:matrices)for(var face:Direction.values())grow(level,root,face);
        for(var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(cx<<4,
                com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE+1,cz<<4,
                (cx+1)<<4,level.getMaxBuildHeight(),(cz+1)<<4)))
            if(!(living instanceof Player))pulseEntity(level,living,cycle,cx,cz);
    }

    public static void pulsePlayer(ServerLevel level,Player player,long cycle){
        if(player.getY()>com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE)
            pulseEntity(level,player,cycle,player.getBlockX()>>4,player.getBlockZ()>>4);
    }

    private static void pulseEntity(ServerLevel level,LivingEntity living,long cycle,int cx,int cz){
        if(!living.isAlive()||living instanceof ArmorStand
                ||living instanceof Player player&&(player.isCreative()||player.isSpectator())
                ||living.getBlockX()>>4!=cx||living.getBlockZ()>>4!=cz
                ||!level.getBiome(living.blockPosition()).is(BIOME)
                ||TemporalCreatureTags.timeImmune(living)
                ||com.mcmagic.omnira.item.bottle.BottleCaptureRules.forbidden(living))return;
        var data=living.getPersistentData();
        if(data.contains(ENTITY_CYCLE)&&data.getLong(ENTITY_CYCLE)==cycle)return;
        data.putLong(ENTITY_CYCLE,cycle);
        if(living instanceof Player player){
            // The time node removes health directly, independent of armor and damage modifiers.
            float remaining=player.getHealth()-player.getMaxHealth()*.5F;
            player.setHealth(Math.max(0,remaining));
            if(remaining<=0)player.die(new net.minecraft.world.damagesource.DamageSource(
                    level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(
                            ResourceKey.create(Registries.DAMAGE_TYPE,ResourceLocation.parse("omnira:time_ending")))));
            return;
        }
        if(living instanceof Mob mob&&mob.isBaby()){
            if(mob instanceof AgeableMob ageable)ageable.setAge(0);
            else mob.setBaby(false);
            return;
        }
        var origin=living.blockPosition();
        for(var at:new BlockPos[]{origin,origin.above(),origin.above(2),origin.north(),origin.south(),origin.east(),origin.west()}){
            if(!level.getBiome(at).is(BIOME)||!level.getBlockState(at).canBeReplaced())continue;
            if(level.setBlockAndUpdate(at,ModBlocks.TIME_WARP_POINT.get().defaultBlockState())){
                level.sendParticles(ParticleTypes.LARGE_SMOKE,living.getX(),living.getY()+living.getBbHeight()*.5,living.getZ(),8,.3,.3,.3,.02);
                living.discard();
                return;
            }
        }
        data.remove(ENTITY_CYCLE);
    }

    public static boolean withers(BlockState state){
        return state.is(com.mcmagic.omnira.mire.MireContent.REBORN.get())
                ||state.is(TimeNatureContent.GRASS.get())||state.is(TimeNatureContent.FLOWER.get())
                ||state.is(BlockTags.CROPS)||state.is(BlockTags.SAPLINGS)||state.is(BlockTags.FLOWERS)
                ||state.getBlock() instanceof BushBlock||state.getBlock() instanceof AttachedStemBlock;
    }

    private static BlockState advance(BlockState state){
        for(var stages:new Block[][]{
                {DreamContent.SMALL_DREAM_BUD.get(),DreamContent.MEDIUM_DREAM_BUD.get(),DreamContent.LARGE_DREAM_BUD.get(),DreamContent.DREAM_CRYSTAL.get()},
                {TimeNatureContent.SMALL_SPATIAL_BUD.get(),TimeNatureContent.MEDIUM_SPATIAL_BUD.get(),TimeNatureContent.LARGE_SPATIAL_BUD.get(),TimeNatureContent.SPATIAL_CLUSTER.get()},
                {Blocks.SMALL_AMETHYST_BUD,Blocks.MEDIUM_AMETHYST_BUD,Blocks.LARGE_AMETHYST_BUD,Blocks.AMETHYST_CLUSTER}}){
            for(int i=0;i<stages.length-1;i++)if(state.is(stages[i]))return stages[3].defaultBlockState()
                    .setValue(AmethystClusterBlock.FACING,state.getValue(AmethystClusterBlock.FACING))
                    .setValue(AmethystClusterBlock.WATERLOGGED,state.getValue(AmethystClusterBlock.WATERLOGGED));
        }
        if(state.getBlock() instanceof CropBlock crop&&!crop.isMaxAge(state))return crop.getStateForAge(crop.getMaxAge());
        if(!withers(state))return null;
        for(var property:state.getProperties())if(property instanceof IntegerProperty age&&age.getName().equals("age")){
            int max=age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
            if(state.getValue(age)<max)return state.setValue(age,max);
        }
        return null;
    }

    private static boolean matrix(BlockState state){
        return state.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())
                ||state.is(TimeNatureContent.SPATIAL_MATRIX.get())||state.is(Blocks.BUDDING_AMETHYST);
    }

    private static boolean mature(BlockState state){
        return state.is(DreamContent.DREAM_CRYSTAL.get())
                ||state.is(TimeNatureContent.SPATIAL_CLUSTER.get())||state.is(Blocks.AMETHYST_CLUSTER);
    }

    private static void grow(ServerLevel level,BlockPos root,Direction face){
        var at=root.relative(face);
        if(level.isOutsideBuildHeight(at)||!level.hasChunkAt(at)||!level.getBiome(at).is(BIOME))return;
        var matrix=level.getBlockState(root);var old=level.getBlockState(at);
        Block target=matrix.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())?DreamContent.DREAM_CRYSTAL.get()
                :matrix.is(TimeNatureContent.SPATIAL_MATRIX.get())?TimeNatureContent.SPATIAL_CLUSTER.get():Blocks.AMETHYST_CLUSTER;
        if(old.is(target))return;
        boolean bud=matrix.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())
                ?old.is(DreamContent.SMALL_DREAM_BUD.get())||old.is(DreamContent.MEDIUM_DREAM_BUD.get())||old.is(DreamContent.LARGE_DREAM_BUD.get())
                :matrix.is(TimeNatureContent.SPATIAL_MATRIX.get())
                ?old.is(TimeNatureContent.SMALL_SPATIAL_BUD.get())||old.is(TimeNatureContent.MEDIUM_SPATIAL_BUD.get())||old.is(TimeNatureContent.LARGE_SPATIAL_BUD.get())
                :old.is(Blocks.SMALL_AMETHYST_BUD)||old.is(Blocks.MEDIUM_AMETHYST_BUD)||old.is(Blocks.LARGE_AMETHYST_BUD);
        if(!BuddingAmethystBlock.canClusterGrowAtState(old)
                &&(!bud||old.getValue(AmethystClusterBlock.FACING)!=face))return;
        level.setBlockAndUpdate(at,target.defaultBlockState().setValue(AmethystClusterBlock.FACING,face)
                .setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().is(Fluids.WATER)));
    }

    private EventideRuins(){}
}
