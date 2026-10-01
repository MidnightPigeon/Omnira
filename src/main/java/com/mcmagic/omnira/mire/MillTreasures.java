package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.menu.CrystalBallMenu;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Four persistent treasure identities per mill; phase changes never reroll or copy loot. */
public final class MillTreasures extends SavedData {
    public static final String DATA="omnira_mill_treasures", TOKEN="OmniraMillTreasure";
    public record Site(BlockPos pos,boolean reversed,String loot){}
    private static final class Slot {
        final BlockPos pos;final boolean reversed;final UUID token;
        boolean visible,retired;BlockState state;CompoundTag snapshot;
        Slot(BlockPos pos,boolean reversed,UUID token){this.pos=pos.immutable();this.reversed=reversed;this.token=token;}
    }
    private final Map<BlockPos,List<Slot>> mills=new LinkedHashMap<>();
    public static MillTreasures get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new Factory<>(MillTreasures::new,MillTreasures::load,null),DATA);}
    public boolean register(ServerLevel level,BlockPos origin,List<Site> sites){
        if(mills.containsKey(origin))return false;
        if(sites.size()!=4||sites.stream().map(Site::pos).distinct().count()!=4||sites.stream().filter(Site::reversed).count()!=2)throw new IllegalArgumentException("Two ordinary and two reversed treasures required");
        for(var site:sites){
            if(!level.hasChunkAt(site.pos))return false;
            if(site.reversed?!level.isEmptyBlock(site.pos):!(level.getBlockEntity(site.pos) instanceof CrystalBallBlockEntity))return false;
        }
        var slots=new ArrayList<Slot>();
        for(var site:sites){
            var slot=new Slot(site.pos,site.reversed,UUID.randomUUID());slot.visible=!site.reversed;slot.state=ModBlocks.CRYSTAL_BALL.get().defaultBlockState();
            if(slot.visible){var ball=(CrystalBallBlockEntity)level.getBlockEntity(site.pos);ball.getPersistentData().putUUID(TOKEN,slot.token);ball.setChanged();slot.state=ball.getBlockState();slot.snapshot=ball.saveWithFullMetadata(level.registryAccess());}
            else{
                var n=new CompoundTag();n.putString("id","omnira:crystal_ball");n.putString("LootTable",site.loot);n.putLong("LootTableSeed",slot.token.getMostSignificantBits());
                var data=new CompoundTag();data.putUUID(TOKEN,slot.token);n.put("NeoForgeData",data);slot.snapshot=n;
            }
            slots.add(slot);
        }
        mills.put(origin.immutable(),slots);setDirty();return true;
    }
    private static boolean owns(ServerLevel level,Slot slot){
        return level.getBlockEntity(slot.pos) instanceof CrystalBallBlockEntity ball
                &&ball.getPersistentData().hasUUID(TOKEN)&&slot.token.equals(ball.getPersistentData().getUUID(TOKEN));
    }
    public void pulse(ServerLevel level,long now){
        for(var slots:mills.values()){
            if(slots.stream().anyMatch(s->!level.hasChunkAt(s.pos)))continue;
            boolean reversed=MireCycle.reversing(now);
            // A removed or replaced visible ball is gone permanently, not a new spawn opportunity.
            for(var s:slots)if(!s.retired&&s.visible&&!owns(level,s)){s.retired=true;s.visible=false;s.snapshot=new CompoundTag();setDirty();}
            for(var s:slots)if(!s.retired&&s.visible&&s.reversed!=reversed)hide(level,s);
            for(var s:slots)if(!s.retired&&!s.visible&&s.reversed==reversed&&level.isEmptyBlock(s.pos)
                    &&level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(s.pos),e->e.isAlive()&&!e.isSpectator()).isEmpty())show(level,s);
        }
    }
    private void hide(ServerLevel level,Slot s){
        var ball=(CrystalBallBlockEntity)level.getBlockEntity(s.pos);
        for(var player:level.getServer().getPlayerList().getPlayers())if(player.containerMenu instanceof CrystalBallMenu menu&&menu.ball==ball)player.closeContainer();
        s.state=ball.getBlockState();s.snapshot=ball.saveWithFullMetadata(level.registryAccess());
        // Removing the BE first bypasses the ordinary block-break inventory/upgrade drops.
        level.removeBlockEntity(s.pos);level.setBlock(s.pos,Blocks.AIR.defaultBlockState(),3);
        s.visible=false;setDirty();particles(level,s.pos);
    }
    private void show(ServerLevel level,Slot s){
        if(!level.setBlock(s.pos,s.state,3))return;
        var ball=(CrystalBallBlockEntity)level.getBlockEntity(s.pos);var n=s.snapshot.copy();
        n.putInt("x",s.pos.getX());n.putInt("y",s.pos.getY());n.putInt("z",s.pos.getZ());
        ball.loadWithComponents(n,level.registryAccess());ball.getPersistentData().putUUID(TOKEN,s.token);ball.setChanged();
        s.visible=true;setDirty();particles(level,s.pos);
    }
    private static void particles(ServerLevel level,BlockPos p){level.sendParticles(ModParticles.TIME_WARP_SPARK.get(),p.getX()+.5,p.getY()+.65,p.getZ()+.5,16,.3,.4,.3,.015);}
    public static MillTreasures load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new MillTreasures();
        for(var raw:tag.getList("Mills",Tag.TAG_COMPOUND)){
            var m=(CompoundTag)raw;var slots=new ArrayList<Slot>();
            for(var value:m.getList("Slots",Tag.TAG_COMPOUND)){
                var n=(CompoundTag)value;var s=new Slot(BlockPos.of(n.getLong("Pos")),n.getBoolean("Reversed"),n.getUUID("Token"));
                s.visible=n.getBoolean("Visible");s.retired=n.getBoolean("Retired");s.state=NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK),n.getCompound("State"));s.snapshot=n.getCompound("Snapshot").copy();slots.add(s);
            }
            data.mills.put(BlockPos.of(m.getLong("Origin")),slots);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();mills.forEach((origin,slots)->{
            var m=new CompoundTag();m.putLong("Origin",origin.asLong());var entries=new ListTag();
            for(var s:slots){var n=new CompoundTag();n.putLong("Pos",s.pos.asLong());n.putBoolean("Reversed",s.reversed);n.putUUID("Token",s.token);n.putBoolean("Visible",s.visible);n.putBoolean("Retired",s.retired);n.put("State",NbtUtils.writeBlockState(s.state));n.put("Snapshot",s.snapshot.copy());entries.add(n);}
            m.put("Slots",entries);list.add(m);
        });tag.put("Mills",list);return tag;
    }
}
