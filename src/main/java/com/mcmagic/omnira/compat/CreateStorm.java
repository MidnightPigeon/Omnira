package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.spacetime.SpacetimeStorm;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.StructureBlockInfo;
import net.minecraft.world.phys.*;
import java.util.*;

public final class CreateStorm {
    private CreateStorm(){}
    public static boolean erase(Entity entity,Vec3 center){
        if(!(entity instanceof AbstractContraptionEntity moving))return false;
        var contraption=moving.getContraption();if(contraption==null)return true;
        var local=moving.toLocalVector(center,1);
        Set<BlockPos> erased=new HashSet<>();
        for(var pos:contraption.getBlocks().keySet())if(SpacetimeStorm.intersects(local,new AABB(pos)))erased.add(pos);
        if(erased.isEmpty())return true;
        // Rebuild mounted storage through its codec so erased containers cannot return on disassembly.
        var storage=new CompoundTag();contraption.getStorage().write(storage,entity.registryAccess(),false);
        for(String key:List.of("items","fluids"))storage.getList(key,Tag.TAG_COMPOUND).removeIf(tag->NbtUtils.readBlockPos((CompoundTag)tag,"pos").filter(erased::contains).isPresent());
        contraption.getStorage().read(storage,entity.registryAccess(),false,contraption);
        ((StormContraptionState)contraption).omnira$eraseStormState(erased);
        contraption.getActors().removeIf(actor->erased.contains(actor.getLeft().pos()));
        contraption.getInteractors().keySet().removeAll(erased);
        var seats=contraption.getSeats();
        for(var passenger:List.copyOf(moving.getPassengers())){
            var index=contraption.getSeatMapping().get(passenger.getUUID());
            if(index!=null && index>=0 && index<seats.size() && erased.contains(seats.get(index)))passenger.stopRiding();
        }
        contraption.getSeatMapping().entrySet().removeIf(entry->entry.getValue()>=0 && entry.getValue()<seats.size() && erased.contains(seats.get(entry.getValue())));
        for(var pos:erased){
            moving.setBlock(pos,new StructureBlockInfo(pos,Blocks.AIR.defaultBlockState(),null));
            contraption.getBlocks().remove(pos);
        }
        contraption.invalidateColliders();
        if(contraption.getBlocks().isEmpty())moving.discard();
        return true;
    }
}
