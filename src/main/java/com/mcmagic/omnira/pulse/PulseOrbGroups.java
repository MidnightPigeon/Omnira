package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.item.OrbUpgradeItem;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.items.ItemStackHandler;
import java.util.*;

/** One persisted upgrade per frequency group; unloaded endpoints never carry duplicate plates. */
public final class PulseOrbGroups extends SavedData {
    private static final class Endpoint {
        UUID id,store;String dimension;BlockPos pos;ItemStack first,second;
        final List<ItemStack> returned=new ArrayList<>();
    }
    private final Map<UUID,Endpoint> endpoints=new LinkedHashMap<>();
    private final Map<UUID,ItemStackHandler> stores=new HashMap<>();
    private net.minecraft.server.MinecraftServer server;
    private boolean reconciling;
    public static PulseOrbGroups get(ServerLevel level){
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(new Factory<>(PulseOrbGroups::new,PulseOrbGroups::load,null),"omnira_pulse_orb_groups");
        data.server=level.getServer();return data;
    }
    private ItemStackHandler fresh(){return new ItemStackHandler(1){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override protected void onContentsChanged(int slot){setDirty();if(!reconciling){reconcile();notifyLoaded();}}
    };}
    public ItemStackHandler view(PulseOrbEntity orb){var e=endpoints.get(orb.storageIdentity);return e==null?orb.upgrade:stores.get(e.store);}
    public void register(PulseOrbEntity orb){
        if(!orb.resonant()||!orb.hasFrequency()){detach(orb);return;}
        var e=endpoints.get(orb.storageIdentity);String dimension=orb.getLevel().dimension().location().toString();
        if(e!=null&&(!e.pos.equals(orb.getBlockPos())||!e.dimension.equals(dimension))){
            var previous=level(e);
            // Relocate a vacated endpoint, not a copied identity with a live/unloaded original.
            if(previous!=null&&previous.hasChunkAt(e.pos)&&loaded(e)==null){
                e.pos=orb.getBlockPos();e.dimension=dimension;setDirty();reconcile();notifyLoaded();
            }else{orb.storageIdentity=UUID.randomUUID();e=null;}
        }
        if(e==null){
            e=new Endpoint();e.id=orb.storageIdentity;e.store=UUID.randomUUID();e.pos=orb.getBlockPos();e.dimension=dimension;
            e.first=orb.frequency[0].copy();e.second=orb.frequency[1].copy();
            var handler=fresh();stores.put(e.store,handler);reconciling=true;
            try{handler.setStackInSlot(0,orb.upgrade.local());orb.upgrade.local(ItemStack.EMPTY);}finally{reconciling=false;}
            endpoints.put(e.id,e);setDirty();reconcile();notifyLoaded();
        }
        if(!e.returned.isEmpty()){
            for(var stack:e.returned)net.minecraft.world.Containers.dropItemStack(orb.getLevel(),e.pos.getX()+.5,e.pos.getY()+.5,e.pos.getZ()+.5,stack);
            e.returned.clear();setDirty();
        }
    }
    public void detach(PulseOrbEntity orb){
        var e=endpoints.remove(orb.storageIdentity);if(e==null)return;
        for(var stack:e.returned)net.minecraft.world.Containers.dropItemStack(orb.getLevel(),e.pos.getX()+.5,e.pos.getY()+.5,e.pos.getZ()+.5,stack);
        if(endpoints.values().stream().noneMatch(other->other.store.equals(e.store))){
            orb.upgrade.local(stores.remove(e.store).getStackInSlot(0));
        }
        setDirty();reconcile();notifyLoaded();
    }
    private static boolean same(Endpoint a,Endpoint b){return !a.first.isEmpty()&&!a.second.isEmpty()&&!b.first.isEmpty()&&!b.second.isEmpty()&&ItemStack.isSameItemSameComponents(a.first,b.first)&&ItemStack.isSameItemSameComponents(a.second,b.second);}
    private static boolean stabilized(ItemStack stack){return stack.getItem() instanceof OrbUpgradeItem item&&item.kind==OrbUpgradeItem.Kind.STABILIZATION;}
    private static int rank(ItemStack stack){return stack.isEmpty()?0:stabilized(stack)?100:((OrbUpgradeItem)stack.getItem()).kind.capacityMultiplier;}
    private void reconcile(){
        if(reconciling)return;reconciling=true;
        try{
            var frequencies=new ArrayList<List<Endpoint>>();
            for(var e:endpoints.values()){
                var group=frequencies.stream().filter(g->same(e,g.getFirst())).findFirst().orElse(null);
                if(group==null){group=new ArrayList<>();frequencies.add(group);}group.add(e);
            }
            var groups=new ArrayList<List<Endpoint>>();
            for(var frequency:frequencies){
                if(frequency.stream().anyMatch(e->stabilized(stores.get(e.store).getStackInSlot(0))))groups.add(frequency);
                else{var dimensions=new LinkedHashMap<String,List<Endpoint>>();for(var e:frequency)dimensions.computeIfAbsent(e.dimension,k->new ArrayList<>()).add(e);groups.addAll(dimensions.values());}
            }
            // Removing stabilization splits ownership, never copies the shared item.
            var assigned=new HashSet<UUID>();
            for(var group:groups){
                var ids=new LinkedHashSet<UUID>();for(var e:group)ids.add(e.store);
                for(var id:ids)if(!assigned.add(id)){UUID empty=UUID.randomUUID();stores.put(empty,fresh());for(var e:group)if(e.store.equals(id))e.store=empty;}
            }
            for(var group:groups){
                UUID keeper=group.getFirst().store;var ids=new LinkedHashSet<UUID>();for(var e:group)ids.add(e.store);
                UUID winner=ids.stream().max(Comparator.comparingInt(id->rank(stores.get(id).getStackInSlot(0)))).orElseThrow();
                var plate=stores.get(winner).getStackInSlot(0);
                for(var id:ids)if(!id.equals(winner)){
                    var extra=stores.get(id).getStackInSlot(0);if(!extra.isEmpty()){
                        var members=group.stream().filter(e->e.store.equals(id)).toList();
                        var receiver=members.stream().filter(e->loaded(e)!=null).findFirst().orElse(members.getFirst());receiver.returned.add(extra);
                    }
                }
                for(var id:ids)if(!id.equals(keeper))stores.remove(id);
                stores.get(keeper).setStackInSlot(0,plate);
                for(var e:group)e.store=keeper;
            }
            setDirty();
        }finally{reconciling=false;}
    }
    private ServerLevel level(Endpoint e){
        return server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.ResourceLocation.parse(e.dimension)));
    }
    private PulseOrbEntity loaded(Endpoint e){
        var level=level(e);
        var orb=level!=null&&level.hasChunkAt(e.pos)?PulseOrbEntity.from(level.getBlockEntity(e.pos)):null;
        return orb!=null&&orb.storageIdentity.equals(e.id)?orb:null;
    }
    private void notifyLoaded(){
        for(var e:endpoints.values()){var orb=loaded(e);if(orb!=null){
            if(!e.returned.isEmpty()){
                for(var stack:e.returned)net.minecraft.world.Containers.dropItemStack(orb.getLevel(),e.pos.getX()+.5,e.pos.getY()+.5,e.pos.getZ()+.5,stack);
                e.returned.clear();setDirty();
            }
            PulseNetworks.orbChanged(orb);orb.sync();
        }}
    }
    public static PulseOrbGroups load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new PulseOrbGroups();data.reconciling=true;
        for(var value:tag.getList("Stores",10)){var t=(CompoundTag)value;var handler=data.fresh();handler.setStackInSlot(0,ItemStack.parseOptional(registries,t.getCompound("Upgrade")));data.stores.put(t.getUUID("Id"),handler);}
        for(var value:tag.getList("Endpoints",10)){
            var t=(CompoundTag)value;var e=new Endpoint();e.id=t.getUUID("Id");e.store=t.getUUID("Store");e.dimension=t.getString("Dimension");e.pos=BlockPos.of(t.getLong("Pos"));
            e.first=ItemStack.parseOptional(registries,t.getCompound("F0"));e.second=ItemStack.parseOptional(registries,t.getCompound("F1"));
            for(var returned:t.getList("Returned",10))e.returned.add(ItemStack.parseOptional(registries,(CompoundTag)returned));
            data.endpoints.put(e.id,e);
        }
        data.reconciling=false;return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var payloads=new ListTag();stores.forEach((id,handler)->{var t=new CompoundTag();t.putUUID("Id",id);t.put("Upgrade",handler.getStackInSlot(0).saveOptional(registries));payloads.add(t);});tag.put("Stores",payloads);
        var members=new ListTag();for(var e:endpoints.values()){
            var t=new CompoundTag();t.putUUID("Id",e.id);t.putUUID("Store",e.store);t.putString("Dimension",e.dimension);t.putLong("Pos",e.pos.asLong());
            t.put("F0",e.first.saveOptional(registries));t.put("F1",e.second.saveOptional(registries));var returned=new ListTag();for(var stack:e.returned)returned.add(stack.saveOptional(registries));t.put("Returned",returned);members.add(t);
        }tag.put("Endpoints",members);return tag;
    }
}
