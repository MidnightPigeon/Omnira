package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.fluids.FluidStack;
import java.util.*;

/** One persisted payload per channel; endpoints never save copies of shared contents. */
public final class CrystalStorageNetworks extends SavedData {
    private static final class Endpoint {
        UUID id,store;String dimension;BlockPos pos;boolean liquid,rejected;
        ItemStack[] frequency;int status;
    }
    private final Map<UUID,Endpoint> endpoints=new LinkedHashMap<>();
    private final Map<UUID,CrystalBallBlockEntity> stores=new HashMap<>();
    private final Map<UUID,CompoundTag> pending=new HashMap<>();
    private net.minecraft.server.MinecraftServer server;
    private boolean reconciling,notifying;
    private boolean topologyDirty=true;
    private record Location(String dimension,BlockPos pos){}
    private final Map<Location,Endpoint> locations=new HashMap<>();
    private final Map<CrystalBallBlockEntity,Boolean> stabilization=new IdentityHashMap<>();
    private final Map<CrystalBallBlockEntity,List<Endpoint>> views=new IdentityHashMap<>();
    public static CrystalStorageNetworks get(ServerLevel level){
        var data=level.getServer().overworld().getDataStorage().computeIfAbsent(
            new Factory<>(CrystalStorageNetworks::new,CrystalStorageNetworks::load,null),"omnira_crystal_storage");
        data.server=level.getServer();return data;
    }
    private ServerLevel level(Endpoint e){return server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(e.dimension)));}
    private CrystalBallBlockEntity loaded(Endpoint e){
        var level=level(e);
        return level!=null&&level.hasChunkAt(e.pos)&&level.getBlockEntity(e.pos) instanceof CrystalBallBlockEntity ball
            &&ball.storageIdentity.equals(e.id)?ball:null;
    }
    private CrystalBallBlockEntity fresh(boolean liquid,ServerLevel level){
        var state=(liquid?ModBlocks.LIQUID_CRYSTAL_BALL:ModBlocks.CRYSTAL_BALL).get().defaultBlockState();
        CrystalBallBlockEntity ball=liquid?new LiquidCrystalBallBlockEntity(BlockPos.ZERO,state):new CrystalBallBlockEntity(BlockPos.ZERO,state);
        ball.virtualStorage=true;ball.setLevel(level);ball.storageChanged=()->{
            boolean current=ball.stabilized();
            var previous=stabilization.put(ball,current);
            if(previous!=null&&previous!=current)topologyDirty=true;
            setDirty();if(!reconciling)notifyViews(ball);
        };
        return ball;
    }
    private CrystalBallBlockEntity store(Endpoint e){
        var value=stores.computeIfAbsent(e.store,id->{
            var payload=fresh(e.liquid,level(e)==null?server.overworld():level(e));
            var tag=pending.remove(id);if(tag!=null)payload.importLocal(tag);return payload;
        });
        if(!reconciling){
            if(!stabilization.containsKey(value))stabilization.put(value,value.stabilized());
            // A loaded save may be queried before its first endpoint tick.
            if(!views.containsKey(value)){
                var members=new ArrayList<Endpoint>();
                for(var endpoint:endpoints.values())if(endpoint.store.equals(e.store))members.add(endpoint);
                views.put(value,members);
            }
        }
        return value;
    }
    public CrystalBallBlockEntity view(CrystalBallBlockEntity ball){
        var e=endpoints.get(ball.storageIdentity);return e==null?ball:store(e);
    }
    public String terminalStorageKey(ServerLevel level,BlockPos pos){
        String dimension=level.dimension().location().toString();
        var endpoint=locations.get(new Location(dimension,pos));
        return endpoint==null?null:"omnira:shared_"+(endpoint.liquid?"fluid":"items")+"@"+endpoint.store;
    }
    public void register(CrystalBallBlockEntity ball){
        if(ball.virtualStorage||ball.hasPendingLoot())return;
        var e=endpoints.get(ball.storageIdentity);
        if(e!=null&&(!e.pos.equals(ball.getBlockPos())||!e.dimension.equals(ball.getLevel().dimension().location().toString()))){
            ball.storageIdentity=UUID.randomUUID();e=null;
        }
        if(e!=null&&e.rejected){ball.frequency[0]=ItemStack.EMPTY;ball.frequency[1]=ItemStack.EMPTY;ball.networkStatus=3;return;}
        if(ball.frequency[0].isEmpty()&&ball.frequency[1].isEmpty())return;
        boolean added=e==null;
        if(added){
            e=new Endpoint();e.id=ball.storageIdentity;e.store=UUID.randomUUID();e.pos=ball.getBlockPos();
            e.dimension=ball.getLevel().dimension().location().toString();e.liquid=ball instanceof LiquidCrystalBallBlockEntity;
            e.frequency=new ItemStack[]{ball.frequency[0].copy(),ball.frequency[1].copy()};
            var payload=fresh(e.liquid,(ServerLevel)ball.getLevel());payload.importLocal(ball.exportLocal());
            stores.put(e.store,payload);ball.clearLocal();endpoints.put(e.id,e);
            locations.put(new Location(e.dimension,e.pos),e);setDirty();
        }
        if(added||topologyDirty)reconcile();ball.networkStatus=e.status;
    }
    public void changed(CrystalBallBlockEntity ball){
        var endpoint=endpoints.get(ball.storageIdentity);
        if(endpoint!=null&&!notifying)store(endpoint).storageChanged.run();
    }
    public void detach(CrystalBallBlockEntity ball){
        var e=endpoints.remove(ball.storageIdentity);if(e==null)return;
        locations.remove(new Location(e.dimension,e.pos),e);
        if(endpoints.values().stream().noneMatch(other->other.store.equals(e.store))){
            ball.importLocal(store(e).exportLocal());stores.remove(e.store);pending.remove(e.store);
        }
        ball.networkStatus=0;setDirty();reconcile();
    }
    private boolean sameFrequency(Endpoint a,Endpoint b){
        return a.liquid==b.liquid&&ItemStack.isSameItemSameComponents(a.frequency[0],b.frequency[0])
            &&ItemStack.isSameItemSameComponents(a.frequency[1],b.frequency[1]);
    }
    public void reconcile(){
        if(reconciling)return;reconciling=true;
        try{
            var frequencyGroups=new ArrayList<List<Endpoint>>();
            for(var e:endpoints.values()){
                if(e.rejected){e.status=3;continue;}
                var group=frequencyGroups.stream().filter(g->sameFrequency(g.getFirst(),e)).findFirst().orElse(null);
                if(group==null){group=new ArrayList<>();frequencyGroups.add(group);}group.add(e);
            }
            var groups=new ArrayList<List<Endpoint>>();
            for(var frequency:frequencyGroups){
                boolean bridge=frequency.stream().anyMatch(e->store(e).stabilized());
                if(bridge)groups.add(frequency);
                else{
                    var dimensions=new LinkedHashMap<String,List<Endpoint>>();
                    for(var e:frequency)dimensions.computeIfAbsent(e.dimension,k->new ArrayList<>()).add(e);
                    groups.addAll(dimensions.values());
                }
            }
            // Splitting hands the original payload to exactly one surviving dimension.
            var assigned=new HashSet<UUID>();
            for(var group:groups){
                var ids=new HashSet<UUID>();
                for(var e:group)ids.add(e.store);
                for(var id:ids){
                    if(!assigned.add(id)){
                        UUID empty=UUID.randomUUID();var sample=group.stream().filter(e->e.store.equals(id)).findFirst().orElseThrow();
                        stores.put(empty,fresh(sample.liquid,level(sample)==null?server.overworld():level(sample)));
                        for(var e:group)if(e.store.equals(id))e.store=empty;
                    }
                }
            }
            for(var group:groups){
                var unique=new LinkedHashMap<UUID,CrystalBallBlockEntity>();
                for(var e:group)unique.put(e.store,store(e));
                UUID keeper=group.getFirst().store;
                var accepted=unique.get(keeper);
                // Admission order is persisted by the endpoint map. An entrant cannot poison an older network.
                for(var entry:unique.entrySet()){
                    UUID candidate=entry.getKey();if(candidate.equals(keeper))continue;
                    var merged=merge(group.getFirst(),List.of(accepted,entry.getValue()));
                    if(merged==null){
                        for(var e:group)if(e.store.equals(candidate)){
                            e.rejected=true;e.status=3;e.frequency=new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY};
                            var ball=loaded(e);
                            if(ball!=null){ball.frequency[0]=ItemStack.EMPTY;ball.frequency[1]=ItemStack.EMPTY;ball.setChanged();}
                        }
                        continue;
                    }
                    accepted=merged;stores.put(keeper,accepted);
                    for(var e:group)if(e.store.equals(candidate))e.store=keeper;
                    stores.remove(candidate);pending.remove(candidate);
                }
                long members=group.stream().filter(e->!e.rejected).count();
                for(var e:group)if(!e.rejected)e.status=members>1?(e.liquid?2:1):0;
            }
            for(var level:server.getAllLevels())TuningOutputs.get(level).disconnectShared(level);
            stabilization.clear();
            for(var storage:stores.values())stabilization.put(storage,storage.stabilized());
            views.clear();
            for(var endpoint:endpoints.values())views.computeIfAbsent(store(endpoint),key->new ArrayList<>()).add(endpoint);
            topologyDirty=false;
            setDirty();notifyViews();
        }finally{reconciling=false;}
    }
    private CrystalBallBlockEntity merge(Endpoint sample,Collection<CrystalBallBlockEntity> sources){
        if(sources.size()==1)return sources.iterator().next();
        var result=fresh(sample.liquid,level(sample)==null?server.overworld():level(sample));
        int upgradeSlot=0;
        for(var source:sources)for(int i=0;i<3;i++){
            var plate=source.upgrades.getStackInSlot(i);
            if(plate.isEmpty())continue;
            if(upgradeSlot>=3||!result.upgrades.isItemValid(upgradeSlot,plate))return null;
            result.upgrades.setStackInSlot(upgradeSlot++,plate.copy());
        }
        for(var source:sources){
            if(source instanceof LiquidCrystalBallBlockEntity liquid){
                var fluid=liquid.tank.getFluid();
                var target=((LiquidCrystalBallBlockEntity)result).tank;
                if(!fluid.isEmpty()&&target.fill(fluid.copy(),net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE)!=fluid.getAmount())return null;
            }
            for(int i=0;i<source.getContainerSize();i++){
                var remainder=source.getItem(i).copy();
                if(sample.liquid){
                    if(remainder.isEmpty())continue;
                    if(!result.getItem(0).isEmpty())return null;
                    result.setItem(0,remainder);continue;
                }
                for(int slot=0;slot<result.getContainerSize()&&!remainder.isEmpty();slot++)
                    remainder=result.itemHandler.insertItem(slot,remainder,false);
                if(!remainder.isEmpty())return null;
            }
        }
        return result;
    }
    private void notifyViews(){
        notifyViews(null);
    }
    private void notifyViews(CrystalBallBlockEntity storage){
        if(server==null||notifying)return;notifying=true;
        try{for(var e:storage==null?endpoints.values():views.getOrDefault(storage,List.of())){
            var ball=loaded(e);if(ball!=null){ball.networkStatus=e.status;ball.getLevel().sendBlockUpdated(e.pos,ball.getBlockState(),ball.getBlockState(),2);}
        }}
        finally{notifying=false;}
    }
    public static CrystalStorageNetworks load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new CrystalStorageNetworks();
        for(var value:tag.getList("Stores",10)){var t=(CompoundTag)value;data.pending.put(t.getUUID("Id"),t.getCompound("Payload"));}
        for(var value:tag.getList("Endpoints",10)){
            var t=(CompoundTag)value;var e=new Endpoint();e.id=t.getUUID("Id");e.store=t.getUUID("Store");
            e.dimension=t.getString("Dimension");e.pos=BlockPos.of(t.getLong("Pos"));e.liquid=t.getBoolean("Liquid");
            e.rejected=t.getBoolean("Rejected");e.status=e.rejected?3:0;
            e.frequency=new ItemStack[]{ItemStack.parseOptional(registries,t.getCompound("F0")),ItemStack.parseOptional(registries,t.getCompound("F1"))};
            data.endpoints.put(e.id,e);
            data.locations.put(new Location(e.dimension,e.pos),e);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var payloads=new ListTag();var all=new HashMap<>(pending);
        stores.forEach((id,ball)->all.put(id,ball.exportLocal()));
        all.forEach((id,payload)->{var t=new CompoundTag();t.putUUID("Id",id);t.put("Payload",payload);payloads.add(t);});
        tag.put("Stores",payloads);var members=new ListTag();
        for(var e:endpoints.values()){
            var t=new CompoundTag();t.putUUID("Id",e.id);t.putUUID("Store",e.store);t.putString("Dimension",e.dimension);
            t.putLong("Pos",e.pos.asLong());t.putBoolean("Liquid",e.liquid);
            t.putBoolean("Rejected",e.rejected);
            t.put("F0",e.frequency[0].saveOptional(registries));t.put("F1",e.frequency[1].saveOptional(registries));members.add(t);
        }
        tag.put("Endpoints",members);return tag;
    }
}
