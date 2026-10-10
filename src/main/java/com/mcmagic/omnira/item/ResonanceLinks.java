package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Destruction survives chunk unloads, offline owners and terminals stored in containers. */
public final class ResonanceLinks extends SavedData {
    private final Set<UUID> destroyed=new HashSet<>();
    private final java.util.Map<UUID,UUID> removedBindings=new java.util.HashMap<>();
    public void removeBinding(UUID node,UUID terminal){removedBindings.put(node,terminal);setDirty();}
    public boolean consumeRemovedBinding(UUID node,UUID terminal){
        if(terminal!=null&&terminal.equals(removedBindings.get(node))){removedBindings.remove(node);setDirty();return true;}
        return false;
    }
    public static ResonanceLinks get(ServerLevel level){
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new Factory<>(ResonanceLinks::new,ResonanceLinks::load,null),"omnira_resonance_links");
    }
    public void destroyed(UUID identity){removedBindings.remove(identity);if(destroyed.add(identity))setDirty();}
    public boolean isDestroyed(UUID identity){return destroyed.contains(identity);}
    public static ResonanceLinks load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new ResonanceLinks();
        for(var entry:tag.getList("Destroyed",Tag.TAG_COMPOUND)){
            var value=(CompoundTag)entry;if(value.hasUUID("Id"))data.destroyed.add(value.getUUID("Id"));
        }
        for(var entry:tag.getList("RemovedBindings",Tag.TAG_COMPOUND)){
            var value=(CompoundTag)entry;if(value.hasUUID("Node")&&value.hasUUID("Terminal"))data.removedBindings.put(value.getUUID("Node"),value.getUUID("Terminal"));
        }
        return data;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        var list=new ListTag();
        for(var id:destroyed){var value=new CompoundTag();value.putUUID("Id",id);list.add(value);}
        tag.put("Destroyed",list);
        var bindings=new ListTag();
        removedBindings.forEach((node,terminal)->{var value=new CompoundTag();value.putUUID("Node",node);value.putUUID("Terminal",terminal);bindings.add(value);});
        tag.put("RemovedBindings",bindings);return tag;
    }
    public static void prune(ItemStack stack,ServerLevel origin,boolean inspectUnloaded){
        var tag=ResonanceTerminalItem.data(stack);if(!tag.hasUUID("Terminal"))return;
        var ledger=get(origin);boolean changed=false;
        var containers=new HashSet<String>();
        for(int i=0;i<ResonanceTerminalItem.capacity(stack);i++){
            String key="Link"+i;if(!tag.contains(key))continue;
            var link=tag.getCompound(key);boolean invalid=!link.hasUUID("Identity");
            if(!invalid)invalid=ledger.isDestroyed(link.getUUID("Identity"));
            var dimension=ResourceLocation.tryParse(link.getString("Dimension"));
            var level=dimension==null?null:origin.getServer().getLevel(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,dimension));
            var pos=BlockPos.of(link.getLong("Pos"));
            if(level==null||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))invalid=true;
            if(!invalid){
                if(inspectUnloaded)level.getChunk(pos.getX()>>4,pos.getZ()>>4);
                // An unloaded chunk alone is not evidence that its node was destroyed.
                if(level.hasChunkAt(pos)){
                    invalid=!(level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core)
                            ||!link.getUUID("Identity").equals(core.identity)||!tag.getUUID("Terminal").equals(core.terminal);
                    if(!invalid){
                        var core=(ResonanceCoreBlockEntity)level.getBlockEntity(pos);
                        int kind=core.kind();
                        if(kind!=link.getInt("Kind")){link.putInt("Kind",kind);changed=true;}
                    }
                }
            }
            if(!invalid){
                var container=com.mcmagic.omnira.storage.StorageContainerIdentity.terminalKey(level,pos.below());
                if(container!=null){
                    if(!container.equals(link.getString("Container"))){link.putString("Container",container);changed=true;}
                    if(!containers.add(container)){
                        invalid=true;ledger.removeBinding(link.getUUID("Identity"),tag.getUUID("Terminal"));
                        if(level.hasChunkAt(pos)&&level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core)core.refresh();
                    }
                }
            }
            if(invalid){tag.remove(key);changed=true;}
        }
        if(changed)ResonanceTerminalItem.save(stack,tag);
    }
}
