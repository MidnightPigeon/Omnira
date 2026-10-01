package com.mcmagic.omnira.time;

import com.mcmagic.omnira.mire.MireCycle;
import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

/** Owns local suit time, phase rewards and a reclaimable forced-chunk lease. */
public final class WonderlandPokerStandBlockEntity extends BlockEntity {
    public enum Suit { HEART, SPADE, DIAMOND, CLUB }
    private int phase,progress;
    private boolean ownsForcedChunk;
    public WonderlandPokerStandBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.WONDERLAND_POKER_STAND.get(),pos,state);}
    public Suit suit(){
        int index=phase;
        if(level!=null&&level.getBiome(worldPosition).is(MireCycle.BIOME)&&MireCycle.reversing(level.getGameTime()))index=(index+3)%4;
        return Suit.values()[index];
    }
    public int phase(){return phase;}
    public int progress(){return progress;}
    public boolean ownsForcedChunk(){return ownsForcedChunk;}
    private boolean valid(){return level!=null&&level.getBlockState(worldPosition).is(getBlockState().getBlock())
            &&level.getBlockState(worldPosition.above()).is(getBlockState().getBlock());}
    @Override public void onLoad(){super.onLoad();ensureForcedChunk();}
    private void ensureForcedChunk(){
        if(!(level instanceof ServerLevel server)||!valid())return;
        var chunk=new ChunkPos(worldPosition);
        if(!server.getForcedChunks().contains(chunk.toLong())){
            server.setChunkForced(chunk.x,chunk.z,true);ownsForcedChunk=true;setChanged();
        }
    }
    public void releaseForcedChunk(){
        if(!(level instanceof ServerLevel server)||!ownsForcedChunk)return;
        var chunk=new ChunkPos(worldPosition);
        var loaded=server.getChunkSource().getChunk(chunk.x,chunk.z,ChunkStatus.FULL,false);
        if(loaded instanceof LevelChunk full)for(var entry:full.getBlockEntities().entrySet()){
            if(!entry.getKey().equals(worldPosition)&&entry.getValue() instanceof WonderlandPokerStandBlockEntity other
                    &&server.getBlockState(entry.getKey()).is(getBlockState().getBlock())){
                other.ownsForcedChunk=true;other.setChanged();ownsForcedChunk=false;return;
            }
        }
        server.setChunkForced(chunk.x,chunk.z,false);ownsForcedChunk=false;
    }
    public void advanceGarden(){
        if(!(level instanceof ServerLevel)||!valid())return;
        advancePhase((ServerLevel)level);progress=0;
    }
    private void advancePhase(ServerLevel server){
        if(phase==Suit.CLUB.ordinal())reward(server);
        phase=(phase+1)%4;changed();
    }
    public void tick(){
        if(!(level instanceof ServerLevel server)||!valid())return;
        if(server.getGameTime()%100==0)ensureForcedChunk();
        if(!server.getBiome(worldPosition).is(RecurrenceGarden.BIOME)&&!StilledTime.stopped(server,worldPosition)){
            progress+=FleetingTime.accelerated(server,worldPosition)?2:1;
            if(progress>=RecurrenceGarden.PERIOD){progress-=RecurrenceGarden.PERIOD;advancePhase(server);}
        }
        var shown=suit();
        if(server.getGameTime()%600==0)for(var player:server.players()){
            if(!new ChunkPos(player.blockPosition()).equals(new ChunkPos(worldPosition))||!player.isAlive()||player.isSpectator())continue;
            switch(shown){
                case HEART->{player.addEffect(new MobEffectInstance(MobEffects.HEAL,1,1));player.addEffect(new MobEffectInstance(MobEffects.SATURATION,1,0));}
                case SPADE->{player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,1200,1));player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,1200,1));}
                case DIAMOND->player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1200,1));
                case CLUB->{}
            }
        }
        if(server.getGameTime()%200==0)setChanged();
    }
    private void reward(ServerLevel server){
        for(var player:server.players()){
            if(!player.isAlive()||player.isSpectator()||!new ChunkPos(player.blockPosition()).equals(new ChunkPos(worldPosition)))continue;
            var cube=clubReward(server.random);
            if(!player.getInventory().add(cube))player.drop(cube,false);
        }
    }
    public static ItemStack clubReward(RandomSource random){
        return new ItemStack(random.nextBoolean()?ModItems.PEACEFUL_MEMORY.get():ModItems.CORRUPTED_MEMORY.get());
    }
    public static int clubBonus(net.minecraft.world.entity.Entity entity){
        if(entity==null||!(entity.level() instanceof ServerLevel server))return 0;
        var pos=entity.blockPosition();var chunk=server.getChunkSource().getChunk(pos.getX()>>4,pos.getZ()>>4,ChunkStatus.FULL,false);
        if(!(chunk instanceof LevelChunk full))return 0;
        for(var blockEntity:full.getBlockEntities().values())if(blockEntity instanceof WonderlandPokerStandBlockEntity stand
                &&stand.valid()&&stand.suit()==Suit.CLUB)return 1;
        return 0;
    }
    private void changed(){setChanged();if(level instanceof ServerLevel server)server.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.putInt("Phase",phase);tag.putInt("Progress",progress);
        tag.putBoolean("OwnsForcedChunk",ownsForcedChunk);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);phase=Math.floorMod(tag.getInt("Phase"),4);progress=Math.floorMod(tag.getInt("Progress"),RecurrenceGarden.PERIOD);
        ownsForcedChunk=tag.getBoolean("OwnsForcedChunk");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
