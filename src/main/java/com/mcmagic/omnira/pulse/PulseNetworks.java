package com.mcmagic.omnira.pulse;

import com.mcmagic.omnira.space.SpacetimePortState;
import net.minecraft.core.*;
import net.minecraft.server.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Loaded-only topology cache shared by all consumers; no per-machine world scan per tick. */
public final class PulseNetworks {
    public record Power(boolean connected,boolean running,double allocated,double occupied) {
        public double factor(int base){return running?Math.clamp(allocated/base,1,8):0;}
        public String reason(){return connected&&!running?"gui.omnira.pulse.insufficient":"";}
    }
    private record Key(ServerLevel level,BlockPos pos,int output) {
        String persistent(){return level.dimension().location()+":"+pos.asLong()+":"+output;}
    }
    private record Located(Key key,PulseDevices.Device device) {}
    private record Step(Key from,Key to){}
    private record Quote(Key output,int requested){}
    private record Frequency(net.minecraft.world.item.ItemStack first,net.minecraft.world.item.ItemStack second){
        Frequency(PulseOrbEntity orb){this(orb.frequency[0].copy(),orb.frequency[1].copy());}
        @Override public boolean equals(Object value){return value instanceof Frequency f&&ItemStack.isSameItemSameComponents(first,f.first)&&ItemStack.isSameItemSameComponents(second,f.second);}
        @Override public int hashCode(){return Objects.hash(first.getItem(),first.getComponents(),second.getItem(),second.getComponents());}
    }
    private record Channel(Frequency frequency,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {}
    private static final Map<MinecraftServer,PulseNetworks> SERVERS=new WeakHashMap<>();
    private final Map<Key,Component> components=new HashMap<>();
    private final Set<PulseOrbEntity> orbs=Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Frequency,List<PulseOrbEntity>> frequencyGroups=new HashMap<>();private boolean frequencyDirty=true;
    private final MinecraftServer server;
    private Map<Key,List<ServerPlayer>> absorbers=Map.of();
    private PulseNetworks(MinecraftServer server){this.server=server;}
    private static PulseNetworks get(ServerLevel level){return SERVERS.computeIfAbsent(level.getServer(),PulseNetworks::new);}
    public static void clear(MinecraftServer server){SERVERS.remove(server);}
    public static List<PulseOrbEntity> relays(ServerLevel level){return get(level).orbs.stream().filter(o->o.getLevel()==level&&!o.isRemoved()).toList();}
    public static boolean covered(ServerLevel level,BlockPos source,int output,BlockPos consumer){
        var component=get(level).components.get(new Key(level,source,output));
        var device=PulseDevices.consumer(level,consumer);
        return component!=null&&device!=null&&component.index.containsKey(new Key(level,device.root(),device.output()));
    }
    /** Quote a consumer against the same graph, reclaiming dynamic extras but never machine minima. */
    public static int conversion(PulseOrbEntity orb,int output,int requested){
        if(!(orb.getLevel() instanceof ServerLevel level)||requested<=0)return 0;
        var networks=get(level);var root=new Key(level,orb.getBlockPos(),0);
        networks.read(root,PulseDevices.at(level,orb.getBlockPos(),Direction.UP));var c=networks.components.get(root);
        var key=new Key(level,orb.getBlockPos(),PulseConversions.EXPORT+output);int found=c.index.getOrDefault(key,-1);
        var quote=new Quote(key,requested);var cached=c.quotes.get(quote);if(cached!=null)return cached;
        var nodes=c.nodes();var edges=new ArrayList<>(c.edges);int at=found;
        if(at<0){at=nodes.size();nodes.add(new PulseTransport.Node(0,0,0,0,0,false,false));edges.add(new PulseTransport.Link(c.index.get(root),at,true,false));}
        int low=0,high=requested;
        while(low<high){int amount=low+(high-low+1)/2;
            nodes.set(at,new PulseTransport.Node(0,0,amount,amount,0,false,false));
            if(PulseTransport.solve(nodes,edges).running())low=amount;else high=amount-1;
        }if(c.quotes.size()>=256)c.quotes.clear();c.quotes.put(quote,low);return low;
    }
    /** Inspect only the six cells beside each player's feet, never scan loaded machines. */
    public static void updatePlayers(MinecraftServer server,List<ServerPlayer> players){
        var networks=SERVERS.computeIfAbsent(server,PulseNetworks::new);
        var next=new HashMap<Key,List<ServerPlayer>>();
        for(var player:players){
            if(!player.isAlive()||player.isSpectator())continue;
            var level=player.serverLevel();
            var feet=new HashSet<BlockPos>();feet.add(player.blockPosition());
            if(net.neoforged.fml.ModList.get().isLoaded("sable"))feet.addAll(com.mcmagic.omnira.compat.SablePulseFeet.positions(player));
            for(var cell:feet)for(var face:Direction.values()){
                var pos=cell.relative(face.getOpposite());var device=PulseDevices.at(level,pos,face);
                if(device==null||device.base()>0||device.throughput()>0||device.supply().getAsDouble()<=0
                        ||PulseOutput.consumptionMode(level,pos,face)!=PulseOutput.PULSE)continue;
                var key=new Key(level,device.root(),device.output());
                var occupants=next.computeIfAbsent(key,k->new ArrayList<>());
                if(!occupants.contains(player))occupants.add(player);
            }
        }
        if(!networks.absorbers.keySet().equals(next.keySet())){
            var changed=new HashSet<>(networks.absorbers.keySet());changed.addAll(next.keySet());
            for(var key:changed){var component=networks.components.get(key);if(component!=null){component.signature=null;component.checked=Long.MIN_VALUE;}}
        }
        networks.absorbers=next;
    }
    public static void absorbPlayers(MinecraftServer server){
        var networks=SERVERS.get(server);if(networks==null)return;
        var amounts=new HashMap<ServerPlayer,Double>();
        for(var entry:networks.absorbers.entrySet()){
            var key=entry.getKey();PulseDevices.Device device=null;
            for(var face:Direction.values()){
                var candidate=PulseDevices.at(key.level,key.pos,face);
                if(candidate!=null&&candidate.output()==key.output&&candidate.base()==0&&candidate.throughput()==0){device=candidate;break;}
            }
            if(device==null)continue;
            networks.read(key,device);var component=networks.components.get(key);
            double amount=component.result.absorbed()[component.index.get(key)]/entry.getValue().size();
            if(amount<=1e-7)continue;
            for(var player:entry.getValue())if(player.isAlive()&&!player.isSpectator())amounts.merge(player,Math.ceil(amount/10),Double::sum);
        }
        amounts.forEach(PulseAbsorption::receive);
    }
    public static Power power(net.minecraft.world.level.Level level,BlockPos pos){
        if(!(level instanceof ServerLevel server))return new Power(false,false,0,0);
        var device=PulseDevices.consumer(server,pos);return device==null?new Power(false,false,0,0):get(server).read(new Key(server,device.root(),device.output()),device);
    }
    public static int base(net.minecraft.world.level.Level level,BlockPos pos){if(!(level instanceof ServerLevel server))return 0;var device=PulseDevices.consumer(server,pos);return device==null?0:device.base();}
    /** Inspect settled positive flow only when processing completes, not every machine tick. */
    public static void processed(net.minecraft.world.level.Level world,BlockPos pos){
        if(!(world instanceof ServerLevel level))return;var device=PulseDevices.consumer(level,pos);if(device==null)return;
        var networks=get(level);var key=new Key(level,device.root(),device.output());
        if(!networks.read(key,device).running()){
            if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.PulseKinetics.processed(level.getBlockEntity(device.root()));return;
        }var c=networks.components.get(key);
        processing(c,c.index.get(key),level.getBlockEntity(device.root()));
    }
    public static void convertedProcessing(PulseOrbEntity orb,net.minecraft.world.level.block.entity.BlockEntity host){
        if(!(orb.getLevel() instanceof ServerLevel level))return;
        var networks=get(level);var root=new Key(level,orb.getBlockPos(),0);
        networks.read(root,PulseDevices.at(level,orb.getBlockPos(),Direction.UP));var c=networks.components.get(root);
        var target=c.index.get(new Key(level,orb.getBlockPos(),PulseConversions.EXPORT+PulseConversions.KINETIC));
        if(target!=null&&c.result.running()&&c.result.demand()[target]>1e-7)processing(c,target,host);
    }
    private static void processing(Component c,int target,net.minecraft.world.level.block.entity.BlockEntity host){
        if(c.progressEdges==null){
            c.progressEdges=new ArrayList<>();for(int i=0;i<c.devices.size();i++)c.progressEdges.add(new ArrayList<>());
            for(var flow:c.result.transfers())if(flow.amount()>1e-7)c.progressEdges.get(flow.from()).add(flow);
        }
        var traces=c.progressPaths.computeIfAbsent(target,k->new HashMap<>());
        for(var entry:c.portLinks.entrySet()){
            var a=c.index.get(entry.getKey().from);var b=c.index.get(entry.getKey().to);
            if(a!=null&&b!=null&&c.result.transfers().stream().anyMatch(flow->flow.amount()>1e-7&&(flow.from()==a&&flow.to()==b||flow.from()==b&&flow.to()==a)))
                for(var endpoint:entry.getValue())com.mcmagic.omnira.progression.BranchProgress.port(endpoint,1);
        }
        for(int source=0;source<c.devices.size();source++){
            if(c.result.generation()[source]-c.result.absorbed()[source]<=1e-7)continue;
            var origin=c.devices.get(source).key;
            int reached=traces.getOrDefault(source,-1);
            if(reached<0){
                record Path(int at,int flags){}
                var queue=new ArrayDeque<Path>();var seen=new HashSet<Path>();queue.add(new Path(source,0));reached=0;
                while(!queue.isEmpty()){
                    var path=queue.remove();if(!seen.add(path))continue;
                    if(path.at==target){reached|=1<<path.flags;continue;}
                    for(var flow:c.progressEdges.get(path.at))if(flow.source()<0||flow.source()==source){
                        var next=c.devices.get(flow.to());int flags=path.flags;
                        if(c.wires.contains(next.key))flags|=1;
                        if(next.device.throughput()>0)flags|=2;
                        var port=SpacetimePortState.at(next.key.level,next.key.pos);
                        if(port!=null)flags|=4;
                        queue.add(new Path(flow.to(),flags));
                    }
                }
                traces.put(source,reached);
            }
            if(reached==0)continue;
            var state=origin.level.getBlockState(origin.pos);
            boolean engine=state.is(com.mcmagic.omnira.registry.ModBlocks.MANA_ENGINE.get())
                    ||state.is(com.mcmagic.omnira.engine.SpacetimeEngineContent.BLOCK.get());
            if(engine)com.mcmagic.omnira.progression.BranchProgress.machine(host,"first_pulse");
            for(int flags=0;flags<8;flags++)if((reached&(1<<flags))!=0){
                if((flags&3)==3)com.mcmagic.omnira.progression.BranchProgress.machine(host,"branching_pulse");
                if((flags&4)!=0)com.mcmagic.omnira.progression.BranchProgress.machine(host,"adjacent_worlds");
            }
            if(origin.output>=PulseChanneling.FIRST_SOURCE_OUTPUT)for(var session:PulseChanneling.sources(origin.level,origin.pos))
                if(session.output==origin.output)com.mcmagic.omnira.progression.BranchProgress.record(session.player,"personal_power");
        }
    }
    /** World-facing live inlets, distinct from the machine's overall powered state. */
    public static int inputFaces(net.minecraft.world.level.Level world,BlockPos pos){
        if(!(world instanceof ServerLevel level))return 0;
        var device=PulseDevices.consumer(level,pos);if(device==null)return 0;
        var networks=get(level);var key=new Key(level,device.root(),device.output());
        if(!networks.read(key,device).running())return 0;
        var c=networks.components.get(key);int mask=0;
        for(var entry:c.inletFaces.getOrDefault(key,Map.of()).entrySet()){
            var other=entry.getValue();int i=c.index.get(other);var d=c.devices.get(i).device;
            if(c.source(other)?c.result.generation()[i]>1e-7:d.base()==0&&c.inputReading(other)>1e-7)mask|=1<<entry.getKey().ordinal();
        }
        for(var player:PulseChanneling.sources(level,device.root()))if(player.supply()>0)mask|=1<<player.target.face().ordinal();
        return mask;
    }
    public static double available(PulseOrbEntity orb){
        if(!(orb.getLevel() instanceof ServerLevel level))return 0;
        var networks=get(level);networks.register(orb);var device=PulseDevices.at(level,orb.getBlockPos(),Direction.UP);
        var key=new Key(level,orb.getBlockPos(),0);networks.read(key,device);var component=networks.components.get(key);
        return component==null?0:component.availableReading(key);
    }
    public static double occupied(PulseOrbEntity orb){
        if(!(orb.getLevel() instanceof ServerLevel level))return 0;
        var networks=get(level);var device=PulseDevices.at(level,orb.getBlockPos(),Direction.UP);var key=new Key(level,orb.getBlockPos(),0);
        networks.read(key,device);var component=networks.components.get(key);
        return component==null?0:component.received(key);
    }
    public static double sourceOccupied(net.minecraft.world.level.Level world,BlockPos pos,Direction face){
        if(!(world instanceof ServerLevel level))return 0;
        var device=PulseDevices.at(level,pos,face);if(device==null||device.base()>0||device.throughput()>0)return 0;
        var key=new Key(level,device.root(),device.output());var networks=get(level);networks.read(key,device);var component=networks.components.get(key);
        double extra=net.neoforged.fml.ModList.get().isLoaded("create")?com.mcmagic.omnira.compat.create.PulseKinetics.externalLoad(level.getBlockEntity(pos)):0;
        return (component==null?0:component.result.generation()[component.index.get(key)])+extra;
    }
    public static boolean wirePowered(PulseWireEntity wire){
        return input(wire)>0;
    }
    public static int visualFlow(PulseWireEntity wire){
        if(!(wire.getLevel() instanceof ServerLevel level))return 0;
        input(wire);var key=new Key(level,wire.getBlockPos(),0);var component=get(level).components.get(key);
        return visualFlow(component,key);
    }
    public static int visualFlow(PulseOrbEntity orb){
        if(!(orb.getLevel() instanceof ServerLevel level))return 0;
        available(orb);var key=new Key(level,orb.getBlockPos(),0);
        return visualFlow(get(level).components.get(key),key);
    }
    private static int visualFlow(Component component,Key key){
        if(component==null||component.visualDistance==null||component.inputReading(key)<=0)return 0;
        int at=component.index.get(key),mask=0;
        for(var edge:component.wireFaces.getOrDefault(key,Map.of()).entrySet()){
            Key other=edge.getValue();var face=edge.getKey();
            int next=component.index.get(other);
            if(component.visualDistance[next]<component.visualDistance[at])mask|=1<<face.ordinal();
            else if(component.visualDistance[next]>component.visualDistance[at]&&!component.source(other)&&component.inputReading(other)>0)mask|=1<<(face.ordinal()+6);
        }return mask;
    }
    public static double input(PulseWireEntity wire){
        if(!(wire.getLevel() instanceof ServerLevel level))return 0;
        var networks=get(level);var key=new Key(level,wire.getBlockPos(),0);
        var sockets=Arrays.stream(Direction.values()).map(d->new PulseDevices.Socket(key.pos,d)).toList();
        networks.read(key,new PulseDevices.Device(key.pos,0,sockets,()->0,0,0,0));
        var component=networks.components.get(key);return component==null?0:component.inputReading(key);
    }
    public static double loss(net.minecraft.world.level.block.entity.BlockEntity entity){
        if(!(entity.getLevel() instanceof ServerLevel level))return 0;
        var orb=PulseOrbEntity.from(entity);
        if(orb!=null)available(orb);else if(entity instanceof PulseWireEntity wire)input(wire);else return 0;
        var key=new Key(level,entity.getBlockPos(),0);var component=get(level).components.get(key);
        return component==null?0:component.result.loss()[component.index.get(key)];
    }
    private void register(PulseOrbEntity orb){if(orbs.add(orb)){components.clear();frequencyDirty=true;}}
    public static void changed(net.minecraft.world.level.Level level,BlockPos pos){
        if(!(level instanceof ServerLevel server))return;var networks=SERVERS.get(server.getServer());if(networks==null)return;
        var affected=new HashSet<Component>();
        for(var component:new HashSet<>(networks.components.values()))if(component.watched.contains(new Key(server,pos,0)))affected.add(component);
        networks.components.values().removeIf(affected::contains);
    }
    public static void orbChanged(PulseOrbEntity orb){
        if(orb.getLevel() instanceof ServerLevel level){var networks=get(level);networks.register(orb);networks.frequencyDirty=true;networks.components.clear();}
    }
    public static void removed(PulseOrbEntity orb){
        if(orb.getLevel() instanceof ServerLevel level){var networks=SERVERS.get(level.getServer());if(networks!=null){networks.orbs.remove(orb);networks.frequencyDirty=true;networks.components.clear();}}
    }
    private Power read(Key key,PulseDevices.Device device){
        var component=components.get(key);
        if(component==null||component.topologyChanged()){
            if(component!=null)components.values().removeIf(component::equals);
            component=build(key,device);for(var member:component.index.keySet())components.put(member,component);
        }
        component.solve();int index=component.index.getOrDefault(key,-1);
        if(!component.connected){if(device.base()>0)PulseOrder.get(server).disconnected(key.persistent());return new Power(false,false,0,0);}
        double amount=index<0?0:component.result.demand()[index];
        return new Power(true,component.result.running(),amount,amount);
    }
    private Component build(Key start,PulseDevices.Device first){
        if(orbs.removeIf(orb->orb.isRemoved()||orb.getLevel()==null))frequencyDirty=true;
        if(frequencyDirty){frequencyGroups.clear();for(var orb:orbs)if(orb.resonant()&&orb.hasFrequency())frequencyGroups.computeIfAbsent(new Frequency(orb),f->new ArrayList<>()).add(orb);frequencyDirty=false;}
        var component=new Component(start.level.getGameTime());var pending=new ArrayDeque<Located>();pending.add(new Located(start,first));
        if(start.level.getBlockState(start.pos).is(PulseContent.WIRE.get()))component.wires.add(start);
        var links=new HashSet<String>();
        while(!pending.isEmpty()){
            var current=pending.remove();if(component.index.containsKey(current.key))continue;
            int index=component.devices.size();component.index.put(current.key,index);component.devices.add(current);
            var device=current.device;var level=current.key.level;
            if(device.base()==0)component.connected=true;
            if(current.key.output==0)for(var player:PulseChanneling.sources(level,device.root())){
                var source=new Located(new Key(level,device.root(),player.output),
                        new PulseDevices.Device(device.root(),player.output,List.of(),player::supply,0,0,0));
                add(component,pending,links,current,source);
            }
            for(var socket:device.sockets()){
                watch(component,level,socket.pos());var next=socket.pos().relative(socket.face());watch(component,level,next);
                connect(component,pending,links,current,next,socket.face().getOpposite());
            }
            var orb=current.key.output==0?PulseOrbEntity.from(level.getBlockEntity(device.root())):null;
            if(orb!=null){
                for(int output=0;output<=PulseConversions.KINETIC;output++){
                    final int channel=output;int amount=orb.conversion.claim(output);if(amount<=0)continue;
                    var sink=new Located(new Key(level,device.root(),PulseConversions.EXPORT+output),new PulseDevices.Device(device.root(),PulseConversions.EXPORT+output,List.of(),()->0,0,amount,amount));
                    add(component,pending,links,current,sink);component.blocked.add(new Step(sink.key,current.key));
                }
                if(orb.conversion.imported(orb.inlet())>0){
                    var source=new Located(new Key(level,device.root(),PulseConversions.IMPORT),new PulseDevices.Device(device.root(),PulseConversions.IMPORT,List.of(),()->orb.conversion.imported(orb.inlet()),0,0,0));
                    add(component,pending,links,current,source);
                }
                if(net.neoforged.fml.ModList.get().isLoaded("create")){
                    var group=com.mcmagic.omnira.compat.create.PulseOrbKinetics.inputGroup(orb);
                    if(group!=null){
                        var hub=new Located(new Key(level,group.source(),PulseConversions.IMPORT+1),new PulseDevices.Device(group.source(),PulseConversions.IMPORT+1,List.of(),()->0,0,0,0));
                        component.imports.put(hub.key,group);
                        add(component,pending,links,current,hub);component.blocked.add(new Step(current.key,hub.key));
                        for(var origin:group.sources()){
                            PulseDevices.Device found=null;
                            if(origin.nativeDevice())for(var face:Direction.values()){
                                var candidate=PulseDevices.at(level,origin.pos(),face);
                                if(candidate!=null&&candidate.output()==origin.output()){found=candidate;break;}
                            }
                            if(found==null&&!origin.nativeDevice())found=new PulseDevices.Device(origin.pos(),origin.output(),List.of(),origin.supply(),0,0,0);
                            if(found!=null){var source=new Located(new Key(level,origin.pos(),origin.output()),found);
                                pending.add(source);
                                add(component,pending,links,source,hub);component.blocked.add(new Step(hub.key,source.key));
                            }
                        }
                        for(var member:group.receivers()){
                            var target=PulseDevices.at(level,member.getBlockPos(),Direction.UP);
                            if(target!=null){var key=new Key(level,target.root(),0);add(component,pending,links,hub,new Located(key,target));component.blocked.add(new Step(key,hub.key));}
                        }
                    }
                }
            }
            if(orb!=null&&orb.resonant()&&orb.hasFrequency()){
                var frequency=new Frequency(orb);
                for(boolean global:new boolean[]{false,true}){
                    if(global&&!orb.stabilized())continue;var channel=new Channel(frequency,global?null:level.dimension());
                    var hub=component.hubs.get(channel);
                    if(hub==null){
                        int id=-1-component.hubs.size();hub=new Located(new Key(level,BlockPos.ZERO,id),new PulseDevices.Device(BlockPos.ZERO,id,List.of(),()->0,0,0,0));component.hubs.put(channel,hub);pending.add(hub);
                        for(var other:frequencyGroups.getOrDefault(frequency,List.of())){
                            if(!(other.getLevel() instanceof ServerLevel remote)||global&&!other.stabilized()||!global&&!remote.dimension().equals(level.dimension()))continue;
                            var target=PulseDevices.at(remote,other.getBlockPos(),Direction.UP);if(target==null)continue;
                            add(component,pending,links,hub,new Located(new Key(remote,target.root(),0),target));
                        }
                    }
                    add(component,pending,links,current,hub);
                }
            }
        }
        mergeFrequencyNodes(component);
        var protectedWires=new HashSet<Key>();
        for(var pair:component.links){
            if(component.source(pair[0])&&component.wires.contains(pair[1]))protectedWires.add(pair[1]);
            if(component.source(pair[1])&&component.wires.contains(pair[0]))protectedWires.add(pair[0]);
        }
        for(var wire:component.wires)if(!protectedWires.contains(wire))component.lossyWires.add(wire);
        for(var pair:component.links){boolean forward=!component.blocked.contains(new Step(pair[0],pair[1])),backward=!component.blocked.contains(new Step(pair[1],pair[0]));
            if(forward||backward)component.edges.add(new PulseTransport.Link(component.index.get(pair[0]),component.index.get(pair[1]),forward,backward));}
        for(var key:component.watched)component.snapshot.put(key,new Object[]{key.level.hasChunkAt(key.pos)?key.level.getBlockState(key.pos):null,key.level.hasChunkAt(key.pos)?key.level.getBlockEntity(key.pos):null});
        return component;
    }
    /** Frequency endpoints are aliases of one relay, not spare-budget destinations. */
    private static void mergeFrequencyNodes(Component c){
        if(c.hubs.isEmpty())return;
        var parents=new HashMap<Key,Key>();
        for(var key:c.index.keySet())parents.put(key,key);
        var hubKeys=new HashSet<Key>();for(var hub:c.hubs.values())hubKeys.add(hub.key);
        for(var link:c.links)if(hubKeys.contains(link[0])||hubKeys.contains(link[1])){
            var a=representative(parents,link[0]);var b=representative(parents,link[1]);parents.put(a,b);
        }
        var aliases=new HashMap<Key,Key>();var merged=new LinkedHashMap<Key,Located>();
        for(var located:c.devices){
            var group=representative(parents,located.key);if(hubKeys.contains(located.key))continue;
            var existing=merged.get(group);
            if(existing==null||located.device.throughput()>existing.device.throughput())merged.put(group,located);
        }
        c.index.clear();c.devices.clear();
        for(var entry:merged.entrySet()){int i=c.devices.size();c.devices.add(entry.getValue());c.index.put(entry.getValue().key,i);}
        for(var key:parents.keySet()){
            var canonical=merged.get(representative(parents,key)).key;aliases.put(key,canonical);c.index.put(key,c.index.get(canonical));
        }
        var portLinks=new HashMap<Step,List<net.minecraft.world.level.block.entity.BlockEntity>>();
        c.portLinks.forEach((step,endpoints)->{var a=aliases.get(step.from);var b=aliases.get(step.to);if(a!=null&&b!=null&&!a.equals(b))portLinks.put(new Step(a,b),endpoints);});
        c.portLinks.clear();c.portLinks.putAll(portLinks);
        var links=new ArrayList<Key[]>();var seen=new HashSet<Set<Key>>();var allowed=new HashSet<Step>();
        for(var link:c.links){var a=aliases.get(link[0]);var b=aliases.get(link[1]);if(a.equals(b))continue;
            if(!c.blocked.contains(new Step(link[0],link[1])))allowed.add(new Step(a,b));
            if(!c.blocked.contains(new Step(link[1],link[0])))allowed.add(new Step(b,a));
            if(seen.add(Set.of(a,b)))links.add(new Key[]{a,b});}
        c.links.clear();c.links.addAll(links);
        c.blocked.clear();for(var link:links){
            if(!allowed.contains(new Step(link[0],link[1])))c.blocked.add(new Step(link[0],link[1]));
            if(!allowed.contains(new Step(link[1],link[0])))c.blocked.add(new Step(link[1],link[0]));
        }
    }
    private static Key representative(Map<Key,Key> parents,Key key){
        var root=key;while(!parents.get(root).equals(root))root=parents.get(root);
        while(!key.equals(root)){var next=parents.put(key,root);key=next;}return root;
    }
    private void connect(Component c,ArrayDeque<Located> pending,Set<String> links,Located from,BlockPos pos,Direction face){
        Direction localFace=face.getOpposite();
        var level=from.key.level;watch(c,level,pos);if(!level.hasChunkAt(pos))return;
        var port=SpacetimePortState.at(level,pos);
        if(port!=null){
            if(port.face().getOpposite()!=face)return;var other=port.other();if(other==null)return;
            watch(c,level,other.host.getBlockPos());pos=other.attached();face=other.face();watch(c,level,pos);
        }
        if(level.getBlockState(pos).is(PulseContent.WIRE.get())){
            var wireKey=new Key(level,pos,0);c.wires.add(wireKey);c.connected=true;
            if(port!=null)c.portLinks.put(new Step(from.key,wireKey),List.of(port.host,port.other().host));
            var here=from.key.output==0?PulseOrbEntity.from(level.getBlockEntity(from.device.root())):null;
            if(here!=null&&localFace==here.inlet())c.blocked.add(new Step(from.key,wireKey));
            var sockets=Arrays.stream(Direction.values()).map(d->new PulseDevices.Socket(wireKey.pos,d)).toList();
            if(c.wires.contains(from.key)||here!=null)c.wireFaces.computeIfAbsent(from.key,k->new EnumMap<>(Direction.class)).put(localFace,wireKey);
            if(from.device.base()>0)c.inletFaces.computeIfAbsent(from.key,k->new EnumMap<>(Direction.class)).put(localFace,wireKey);
            add(c,pending,links,from,new Located(wireKey,new PulseDevices.Device(pos,0,sockets,()->0,0,0,0)));return;
        }
        var device=PulseDevices.at(level,pos,face);if(device!=null){
            var target=new Key(level,device.root(),device.output());
            if(port!=null)c.portLinks.put(new Step(from.key,target),List.of(port.host,port.other().host));
            var here=from.key.output==0?PulseOrbEntity.from(level.getBlockEntity(from.device.root())):null;
            var there=PulseOrbEntity.from(level.getBlockEntity(device.root()));
            if(here!=null&&localFace==here.inlet())c.blocked.add(new Step(from.key,target));
            if(there!=null&&face==there.inlet())c.blocked.add(new Step(target,from.key));
            if(c.wires.contains(from.key)||here!=null)c.wireFaces.computeIfAbsent(from.key,k->new EnumMap<>(Direction.class)).put(localFace,target);
            if(from.device.base()>0)c.inletFaces.computeIfAbsent(from.key,k->new EnumMap<>(Direction.class)).put(localFace,target);
            add(c,pending,links,from,new Located(target,device));
        }
    }
    private static void watch(Component c,ServerLevel level,BlockPos pos){c.watched.add(new Key(level,pos.immutable(),0));}
    private static void add(Component c,ArrayDeque<Located> pending,Set<String> links,Located from,Located to){
        if(from.key.equals(to.key))return;var a=from.key.persistent();var b=to.key.persistent();String link=a.compareTo(b)<0?a+"/"+b:b+"/"+a;
        if(links.add(link))c.links.add(new Key[]{from.key,to.key});pending.add(to);
    }
    private final class Component {
        final long built;final List<Located> devices=new ArrayList<>();final Map<Key,Integer> index=new HashMap<>();
        final Set<Key> wires=new HashSet<>(),watched=new HashSet<>();final List<Key[]> links=new ArrayList<>();final List<PulseTransport.Link> edges=new ArrayList<>();
        final Map<Key,Map<Direction,Key>> wireFaces=new HashMap<>();
        final Map<Key,Map<Direction,Key>> inletFaces=new HashMap<>();
        final Map<Step,List<net.minecraft.world.level.block.entity.BlockEntity>> portLinks=new HashMap<>();
        final Map<Key,Object[]> snapshot=new HashMap<>();long inspected;
        final Set<Key> lossyWires=new HashSet<>();
        final Set<Step> blocked=new HashSet<>();final Map<Key,PulseImport> imports=new HashMap<>();
        final Map<Quote,Integer> quotes=new HashMap<>();
        final Map<Integer,Map<Integer,Integer>> progressPaths=new HashMap<>();
        List<List<PulseTransport.Transfer>> progressEdges;
        final Map<Channel,Located> hubs=new HashMap<>();
        boolean connected;double[] signature;PulseTransport.Result result;int[] visualDistance;long checked=Long.MIN_VALUE;
        Component(long built){this.built=built;}
        boolean source(Key key){if(imports.containsKey(key))return false;var d=devices.get(index.get(key)).device;return key.output>=PulseChanneling.FIRST_SOURCE_OUTPUT
                ||key.output>=0&&d.base()==0&&d.throughput()==0&&!wires.contains(key)&&!d.sockets().isEmpty();}
        double inputReading(Key target){
            int i=index.get(target);return result==null?0:result.used()[i]+result.free()[i];
        }
        double received(Key target){
            return result==null?0:result.used()[index.get(target)];
        }
        double availableReading(Key target){
            return result==null?0:result.free()[index.get(target)];
        }
        boolean topologyChanged(){
            long now=server.overworld().getGameTime();if(now-inspected<20)return false;inspected=now;
            for(var entry:snapshot.entrySet()){
                var key=entry.getKey();var old=entry.getValue();
                var state=key.level.hasChunkAt(key.pos)?key.level.getBlockState(key.pos):null;
                var entity=key.level.hasChunkAt(key.pos)?key.level.getBlockEntity(key.pos):null;
                if(old[1]!=entity){
                    var i=index.get(key);
                    if(key.level.hasChunkAt(key.pos)&&i!=null&&devices.get(i).device.base()>0&&state!=old[0])PulseOrder.get(server).disconnected(key.persistent());
                    return true;
                }
                if(old[0]!=state&&!(state!=null&&state.is(PulseContent.WIRE.get())&&old[0] instanceof net.minecraft.world.level.block.state.BlockState previous&&previous.is(PulseContent.WIRE.get())))return true;
            }return false;
        }
        void solve(){
            if(!connected){int n=devices.size();result=new PulseTransport.Result(false,new double[n],new double[n],new double[n],new double[n],new double[n]);return;}
            long now=server.overworld().getGameTime();if(checked==now)return;checked=now;
            var supplies=new double[devices.size()];for(int i=0;i<supplies.length;i++)supplies[i]=devices.get(i).device.supply().getAsDouble();
            if(result!=null&&Arrays.equals(signature,supplies))return;signature=supplies;quotes.clear();
            var nodes=nodes();
            boolean[] absorbing=new boolean[devices.size()];
            for(int i=0;i<absorbing.length;i++)absorbing[i]=absorbers.containsKey(devices.get(i).key);
            result=PulseTransport.solve(nodes,edges,absorbing);
            progressEdges=null;progressPaths.clear();
            visualDistance=PulseFlowDirections.distances(nodes,edges);
            for(var entry:imports.entrySet())entry.getValue().settle().accept(result.used()[index.get(entry.getKey())]);
        }
        ArrayList<PulseTransport.Node> nodes(){
            var nodes=new ArrayList<PulseTransport.Node>();var order=PulseOrder.get(server);
            for(int i=0;i<devices.size();i++){
                var located=devices.get(i);var d=located.device;
                nodes.add(new PulseTransport.Node(d.supply().getAsDouble(),d.throughput(),d.base(),d.maximum(),d.base()>0?order.order(located.key.persistent()):0,source(located.key),lossyWires.contains(located.key)));
            }
            return nodes;
        }
    }
}
