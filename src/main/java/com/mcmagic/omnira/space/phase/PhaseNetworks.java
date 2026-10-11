package com.mcmagic.omnira.space.phase;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

/** One random trial per loaded six-face component, not one trial per block. */
@EventBusSubscriber(modid="omnira")
public final class PhaseNetworks {
    public static final int PERIOD=200;
    public static final int TRANSITION_TICKS=80;
    private static final Map<ServerLevel,Visit> VISITS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<BlockPos,Component>> COMPONENTS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<Component,List<BlockPos>>> CONTROLS=new WeakHashMap<>();
    public static void invalidateControls(Level level){if(level instanceof ServerLevel server)CONTROLS.remove(server);}
    public static void invalidate(Level level){if(level instanceof ServerLevel server){COMPONENTS.remove(server);VISITS.remove(server);CONTROLS.remove(server);}}
    private static final class Visit {long time;final Set<BlockPos> seen=new HashSet<>();}
    public record Component(Set<BlockPos> blocks,boolean complete){}
    public static void schedule(Level level,BlockPos pos){
        if(level.getBlockState(pos).is(PhaseContent.BLOCK.get()))level.scheduleTick(pos,PhaseContent.BLOCK.get(),5-(int)Math.floorMod(level.getGameTime(),5));
    }
    public static Component component(ServerLevel level,BlockPos start){
        var cache=COMPONENTS.computeIfAbsent(level,k->new HashMap<>());
        var cached=cache.get(start);if(cached!=null)return cached;
        var found=new HashSet<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(start.immutable());boolean complete=true;
        while(!queue.isEmpty()){
            var pos=queue.removeFirst();if(found.contains(pos)||!level.getBlockState(pos).is(PhaseContent.BLOCK.get()))continue;
            found.add(pos);
            for(var face:Direction.values()){
                var next=pos.relative(face);if(level.isOutsideBuildHeight(next))continue;
                if(!level.hasChunkAt(next)){complete=false;continue;}
                if(!found.contains(next)&&level.getBlockState(next).is(PhaseContent.BLOCK.get()))queue.addLast(next);
            }
        }
        var result=new Component(Set.copyOf(found),complete);
        if(complete&&found.size()<=65536){if(cache.size()+found.size()>65536){cache.clear();CONTROLS.remove(level);}for(var p:found)cache.put(p,result);}
        return result;
    }
    public static void tick(ServerLevel level,BlockPos pos){
        tick(level,pos,level.random,true);
    }
    public static void tick(ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random){
        tick(level,pos,random,false);
    }
    private static void tick(ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random,boolean periodic){
        var visit=VISITS.computeIfAbsent(level,k->new Visit());long now=level.getGameTime();
        if(visit.time!=now){visit.time=now;visit.seen.clear();}if(visit.seen.contains(pos))return;
        var group=component(level,pos);visit.seen.addAll(group.blocks());
        if(!group.complete()||group.blocks().isEmpty()){schedule(level,pos);return;}
        var leader=group.blocks().stream().min(BlockPos::compareTo).orElseThrow();
        boolean moving=group.blocks().stream().anyMatch(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING));
        int delay=moving?5-(int)Math.floorMod(now,5):PERIOD-(int)Math.floorMod(now,PERIOD);
        level.scheduleTick(leader,PhaseContent.BLOCK.get(),delay);
        if(periodic&&!moving&&Math.floorMod(now,PERIOD)!=0)return;
        if(control(level,group)){
            if(group.blocks().stream().anyMatch(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING)))advance(level,group);
            return;
        }
        var anchor=group.blocks().stream().min(BlockPos::compareTo).orElseThrow();
        if(group.blocks().stream().anyMatch(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING))){advance(level,group);return;}
        boolean inverted=group.blocks().size()>9&&level.getBlockState(anchor).getValue(PhaseBlock.INVERTED);
        if(group.blocks().size()>9&&random.nextInt(5)==0)inverted=!inverted;
        boolean target=inverted;
        if(group.blocks().stream().anyMatch(p->level.getBlockState(p).getValue(PhaseBlock.INVERTED)!=target))begin(level,group,target);
    }
    public static void begin(ServerLevel level,Component group,boolean target){
        for(var pos:group.blocks()){
            var state=level.getBlockState(pos);
            if(state.getValue(PhaseBlock.INVERTED)!=target||state.getValue(PhaseBlock.PHASE)!=(target?16:0))
                level.setBlock(pos,state.setValue(PhaseBlock.TARGET,target).setValue(PhaseBlock.TRANSITIONING,true),3);
            else if(state.getValue(PhaseBlock.TRANSITIONING))level.setBlock(pos,state.setValue(PhaseBlock.TARGET,target).setValue(PhaseBlock.TRANSITIONING,false),3);
        }
        if(!group.blocks().isEmpty())schedule(level,group.blocks().stream().min(BlockPos::compareTo).orElseThrow());
    }
    public static boolean control(ServerLevel level,Component group){
        var cache=CONTROLS.computeIfAbsent(level,k->new IdentityHashMap<>());
        var panels=cache.computeIfAbsent(group,g->{
            var found=new TreeSet<BlockPos>();
            for(var p:g.blocks())for(var face:Direction.values()){
                var at=p.relative(face);var state=level.getBlockState(at);
                if(state.is(PhaseContent.CONTROL.get())&&at.relative(state.getValue(PhaseControlBlock.FACING).getOpposite()).equals(p))found.add(at);
            }
            return List.copyOf(found);
        });
        if(panels.isEmpty())return false;
        begin(level,group,level.getBlockState(panels.getFirst()).getValue(PhaseControlBlock.INVERTED));return true;
    }
    public static void advance(ServerLevel level,Component group){
        var anchor=group.blocks().stream().filter(p->level.getBlockState(p).getValue(PhaseBlock.TRANSITIONING)).min(BlockPos::compareTo).orElseThrow();
        var state=level.getBlockState(anchor);boolean target=state.getValue(PhaseBlock.TARGET);
        int phase=Math.clamp(state.getValue(PhaseBlock.PHASE)+(target?1:-1),0,16);
        if(phase==(target?16:0)){apply(level,group,target);return;}
        for(var pos:group.blocks()){
            var current=level.getBlockState(pos);
            if(current.getValue(PhaseBlock.TRANSITIONING))level.setBlock(pos,current.setValue(PhaseBlock.PHASE,phase)
                    .setValue(PhaseBlock.TARGET,target),2);
        }
    }
    public static void apply(ServerLevel level,Component group,boolean inverted){
        var restored=new ArrayList<BlockPos>();
        for(var pos:group.blocks()){
            var state=level.getBlockState(pos);
            if(state.is(PhaseContent.BLOCK.get())){
                if(!inverted&&state.getValue(PhaseBlock.INVERTED))restored.add(pos);
                level.setBlock(pos,state.setValue(PhaseBlock.INVERTED,inverted).setValue(PhaseBlock.TARGET,inverted).setValue(PhaseBlock.PHASE,inverted?16:0).setValue(PhaseBlock.TRANSITIONING,false),3);
            }
        }
        if(!restored.isEmpty())PhaseImpact.impale(PhaseImpact.occupants(level,restored));
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event){
        if(event.getLevel().isClientSide()||!(event.getChunk() instanceof LevelChunk chunk))return;
        var level=chunk.getLevel();invalidate(level);var sections=chunk.getSections();
        for(int i=0;i<sections.length;i++){
            var section=sections[i];if(!section.maybeHas(s->s.is(PhaseContent.BLOCK.get())))continue;
            int baseY=chunk.getSectionYFromSectionIndex(i)<<4;
            for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)if(section.getBlockState(x,y,z).is(PhaseContent.BLOCK.get()))
                schedule(level,new BlockPos(chunk.getPos().getMinBlockX()+x,baseY+y,chunk.getPos().getMinBlockZ()+z));
        }
    }
    @SubscribeEvent public static void unloaded(ChunkEvent.Unload event){if(event.getChunk() instanceof LevelChunk chunk)invalidate(chunk.getLevel());}
    private PhaseNetworks(){}
}
