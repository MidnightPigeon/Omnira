package com.mcmagic.omnira.mire;

import com.mcmagic.omnira.registry.TimeNatureContent;
import com.mcmagic.omnira.time.TimeSaplingBlock;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Tracks authored tree footprints, not arbitrary neighbouring player blocks. */
public final class MireTrees extends SavedData {
    private static final class Tree {
        final BlockPos root;final Set<BlockPos> footprint;final BlockState sapling,trunk;Map<BlockPos,BlockState> original=Map.of();long until;
        Tree(BlockPos root,Collection<BlockPos> footprint,BlockState sapling,BlockState trunk){
            this.root=root;this.footprint=new HashSet<>(footprint);this.sapling=sapling;this.trunk=trunk;
        }
    }
    private final Map<BlockPos,Tree> trees=new HashMap<>();
    private final Map<BlockPos,Set<Tree>> positions=new HashMap<>();
    private boolean internalChanges;
    public static MireTrees get(ServerLevel level){return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(MireTrees::new,MireTrees::load,null),"omnira_mire_trees");}
    public void register(BlockPos root,Collection<BlockPos> shape){register(root,shape,TimeNatureContent.SAPLING.get().defaultBlockState(),TimeNatureContent.LOG.get().defaultBlockState());}
    public void register(BlockPos root,Collection<BlockPos> shape,BlockState sapling,BlockState trunk){
        remove(root);
        var tree=new Tree(root.immutable(),shape.stream().map(BlockPos::immutable).toList(),sapling,trunk);
        trees.put(tree.root,tree);index(tree);setDirty();
    }
    private void index(Tree tree){for(var pos:tree.footprint)positions.computeIfAbsent(pos,p->new HashSet<>()).add(tree);}
    private Tree remove(BlockPos root){
        var tree=trees.remove(root);if(tree==null)return null;
        for(var pos:tree.footprint){var owners=positions.get(pos);if(owners!=null){owners.remove(tree);if(owners.isEmpty())positions.remove(pos);}}
        return tree;
    }
    public void changed(ServerLevel level,BlockPos pos){
        if(internalChanges)return;
        var owners=positions.get(pos);if(owners==null)return;
        for(var tree:List.copyOf(owners)){
            remove(tree.root);
            var state=level.getBlockState(tree.root);
            if(tree.until>0&&state.hasProperty(TimeSaplingBlock.REWINDING)&&state.getValue(TimeSaplingBlock.REWINDING))
                level.setBlockAndUpdate(tree.root,state.setValue(TimeSaplingBlock.REWINDING,false));
        }
        if(owners.isEmpty())positions.remove(pos);setDirty();
    }
    public boolean rewinding(BlockPos pos){var tree=trees.get(pos);return tree!=null&&tree.until>0;}
    private static boolean material(BlockState state){return state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)
            ||state.is(TimeNatureContent.LOG.get())||state.is(TimeNatureContent.LEAVES.get());}
    /** Called only by a successful feature; never infers trees from player buildings. */
    public static void generated(WorldGenLevel level,BlockPos root,Collection<BlockPos> shape){
        if(!level.getBiome(root).is(MireCycle.BIOME))return;
        // Root placers can lift the trunk above the original sapling (e.g. mangroves).
        var origin=root;
        if(!level.getBlockState(root).is(BlockTags.LOGS))root=shape.stream()
                .filter(p->level.getBlockState(p).is(BlockTags.LOGS))
                .min(Comparator.comparingDouble(p->p.distSqr(origin))).orElse(root);
        var trunk=level.getBlockState(root);var id=BuiltInRegistries.BLOCK.getKey(trunk.getBlock());
        String path=id.getPath().replaceFirst("_log$","_sapling");
        if(path.equals("mangrove_sapling"))path="mangrove_propagule";
        var sapling=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),path));
        if(!(sapling instanceof net.minecraft.world.level.block.SaplingBlock))return;
        var footprint=shape.stream().filter(p->material(level.getBlockState(p))).map(BlockPos::immutable).toList();
        if(!footprint.contains(root))return;
        var server=level.getLevel();var savedRoot=root.immutable();var state=sapling.defaultBlockState();
        server.getServer().execute(()->get(server).register(savedRoot,footprint,state,trunk));
    }
    private static boolean loaded(ServerLevel level,Tree tree){return tree.footprint.stream().allMatch(level::hasChunkAt);}
    public void pulse(ServerLevel level,long now){
        internalChanges=true;
        try{pulseInternal(level,now);}finally{internalChanges=false;}
    }
    private void pulseInternal(ServerLevel level,long now){
        var start=new ArrayList<Tree>();var remove=new ArrayList<BlockPos>();var restored=new HashSet<BlockPos>();
        for(var tree:trees.values()){
            if(tree.until==0&&now%MireCycle.PERIOD!=0)continue;
            if(!loaded(level,tree))continue;
            if(tree.until>0){
                if(now>=tree.until){if(restore(level,tree))restored.add(tree.root);remove.add(tree.root);}
            }else if(now>=MireCycle.PERIOD&&now%MireCycle.PERIOD==0&&level.getBiome(tree.root).is(MireCycle.BIOME)){
                if(!level.getBlockState(tree.root).is(tree.trunk.getBlock())){remove.add(tree.root);continue;}
                var snapshot=new LinkedHashMap<BlockPos,BlockState>();
                for(var pos:tree.footprint){var state=level.getBlockState(pos);if(material(state))snapshot.put(pos,state);}
                tree.original=snapshot;tree.until=now+MireCycle.DURATION;start.add(tree);
            }
        }
        for(var tree:start){
            tree.original.keySet().forEach(p->level.setBlock(p,Blocks.AIR.defaultBlockState(),2));
            var sapling=tree.sapling;
            if(sapling.hasProperty(TimeSaplingBlock.REWINDING))sapling=sapling.setValue(TimeSaplingBlock.REWINDING,true);
            level.setBlockAndUpdate(tree.root,sapling);
        }
        // Completed trees are re-registered for the next cycle only after iteration.
        var completed=new ArrayList<Tree>();
        for(var root:remove){var t=remove(root);if(t!=null&&restored.contains(root))completed.add(new Tree(root,t.footprint,t.sapling,t.trunk));}
        completed.forEach(t->{trees.put(t.root,t);index(t);});
        if(!start.isEmpty()||!remove.isEmpty())setDirty();
    }
    private static boolean restore(ServerLevel level,Tree tree){
        var rootState=level.getBlockState(tree.root);
        if(!rootState.is(tree.sapling.getBlock())||(rootState.hasProperty(TimeSaplingBlock.REWINDING)&&!rootState.getValue(TimeSaplingBlock.REWINDING)))return false;
        boolean clear=tree.original.keySet().stream().allMatch(p->p.equals(tree.root)||level.getBlockState(p).canBeReplaced());
        if(!clear){if(rootState.hasProperty(TimeSaplingBlock.REWINDING))level.setBlockAndUpdate(tree.root,rootState.setValue(TimeSaplingBlock.REWINDING,false));return false;}
        tree.original.forEach((p,s)->level.setBlock(p,s,2));
        return true;
    }
    public static MireTrees load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new MireTrees();
        for(var entry:tag.getList("Trees",Tag.TAG_COMPOUND)){
            var n=(CompoundTag)entry;var positions=Arrays.stream(n.getLongArray("Footprint")).mapToObj(BlockPos::of).toList();
            var lookup=registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
            var sapling=n.contains("Sapling")?NbtUtils.readBlockState(lookup,n.getCompound("Sapling")):TimeNatureContent.SAPLING.get().defaultBlockState();
            var trunk=n.contains("Trunk")?NbtUtils.readBlockState(lookup,n.getCompound("Trunk")):TimeNatureContent.LOG.get().defaultBlockState();
            var tree=new Tree(BlockPos.of(n.getLong("Root")),positions,sapling,trunk);tree.until=n.getLong("Until");
            var original=new LinkedHashMap<BlockPos,BlockState>();
            for(var raw:n.getList("Original",Tag.TAG_COMPOUND)){var s=(CompoundTag)raw;original.put(BlockPos.of(s.getLong("Pos")),NbtUtils.readBlockState(registries.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),s.getCompound("State")));}
            tree.original=original;data.trees.put(tree.root,tree);data.index(tree);
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();for(var tree:trees.values()){
            var n=new CompoundTag();n.putLong("Root",tree.root.asLong());n.putLongArray("Footprint",tree.footprint.stream().mapToLong(BlockPos::asLong).toArray());n.putLong("Until",tree.until);
            n.put("Sapling",NbtUtils.writeBlockState(tree.sapling));n.put("Trunk",NbtUtils.writeBlockState(tree.trunk));
            var original=new ListTag();tree.original.forEach((p,s)->{var e=new CompoundTag();e.putLong("Pos",p.asLong());e.put("State",NbtUtils.writeBlockState(s));original.add(e);});n.put("Original",original);list.add(n);
        }tag.put("Trees",list);return tag;
    }
}
