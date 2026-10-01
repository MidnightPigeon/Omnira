package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.travel.WaymarkDirectory;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Active graves and deferred spills survive unloaded chunks and server restarts. */
@EventBusSubscriber(modid="omnira")
public final class AmberDirectory extends SavedData {
    public static final String MARK_NAME="gui.omnira.waymark.death";
    public record Grave(UUID owner,UUID entity,ResourceLocation dimension,BlockPos pos) {}
    private final Map<UUID,Grave> active=new HashMap<>();
    private final Map<UUID,Grave> pending=new HashMap<>();
    private static final TicketType<UUID> SPILL_TICKET=TicketType.create("omnira_amber_spill",Comparator.comparing(UUID::toString),40);
    public static AmberDirectory get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AmberDirectory::new,AmberDirectory::load,null),"omnira_amber");
    }
    public Grave grave(UUID owner){return active.get(owner);}
    public boolean owns(UUID owner,UUID entity){var grave=active.get(owner);return grave!=null && grave.entity().equals(entity);}
    public void register(TemporalAmber amber){
        if(amber.owner()==null)return;
        var level=(ServerLevel)amber.level();
        var grave=new Grave(amber.owner(),amber.getUUID(),level.dimension().location(),amber.blockPosition());
        var previous=active.put(grave.owner(),grave);
        if(previous!=null && !previous.entity().equals(grave.entity())){
            WaymarkDirectory.get(level).remove(previous.entity());pending.put(previous.entity(),previous);
        }
        var marks=WaymarkDirectory.get(level);
        marks.put(new WaymarkDirectory.Entry(grave.entity(),grave.dimension(),grave.pos(),MARK_NAME));
        marks.discover(grave.owner(),grave.entity());setDirty();spillPending(level);
    }
    public void removed(TemporalAmber amber){
        if(owns(amber.owner(),amber.getUUID()))active.remove(amber.owner());
        pending.remove(amber.getUUID());WaymarkDirectory.get((ServerLevel)amber.level()).remove(amber.getUUID());setDirty();
    }
    public boolean retired(UUID entity){return pending.containsKey(entity);}
    private void spillPending(ServerLevel source){
        for(var grave:new ArrayList<>(pending.values())){
            var level=source.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,grave.dimension()));
            if(level==null)continue;
            var chunk=new ChunkPos(grave.pos());
            level.getChunkSource().addRegionTicket(SPILL_TICKET,chunk,2,grave.entity());level.getChunkAt(grave.pos());
            if(level.getEntity(grave.entity()) instanceof TemporalAmber amber)amber.breakAndSpill();
            else if(level.areEntitiesLoaded(chunk.toLong())){pending.remove(grave.entity());setDirty();}
        }
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        var level=event.getServer().overworld();if(level.getGameTime()%20==0)get(level).spillPending(level);
    }
    public static AmberDirectory load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new AmberDirectory();read(tag,"Active",data.active,true);read(tag,"Pending",data.pending,false);return data;
    }
    private static void read(CompoundTag tag,String key,Map<UUID,Grave> target,boolean byOwner){
        for(var value:tag.getList(key,Tag.TAG_COMPOUND)){
            var n=(CompoundTag)value;var dim=ResourceLocation.tryParse(n.getString("Dimension"));
            if(dim==null||!n.hasUUID("Owner")||!n.hasUUID("Entity"))continue;
            var grave=new Grave(n.getUUID("Owner"),n.getUUID("Entity"),dim,BlockPos.of(n.getLong("Position")));
            target.put(byOwner?grave.owner():grave.entity(),grave);
        }
    }
    private static ListTag write(Collection<Grave> graves){
        var list=new ListTag();for(var grave:graves){
            var n=new CompoundTag();n.putUUID("Owner",grave.owner());n.putUUID("Entity",grave.entity());
            n.putString("Dimension",grave.dimension().toString());n.putLong("Position",grave.pos().asLong());list.add(n);
        }return list;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.put("Active",write(active.values()));tag.put("Pending",write(pending.values()));return tag;
    }
}
