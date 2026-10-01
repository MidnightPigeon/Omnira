package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.compat.ForgeMovement;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.simibubi.create.content.contraptions.Contraption",remap=false)
public abstract class ForgeCreateAssemblyMixin implements com.mcmagic.omnira.compat.StormContraptionState {
    @Shadow public BlockPos anchor;
    @Shadow protected Map<BlockPos,net.minecraft.nbt.CompoundTag> updateTags;
    @Shadow protected com.google.common.collect.Multimap<BlockPos,StructureTemplate.StructureBlockInfo> capturedMultiblocks;
    @Shadow public abstract Map<BlockPos,StructureTemplate.StructureBlockInfo> getBlocks();
    public void omnira$eraseStormState(Set<BlockPos> positions){
        updateTags.keySet().removeAll(positions);
        capturedMultiblocks.entries().removeIf(entry->positions.contains(entry.getKey()) || positions.contains(entry.getValue().pos()));
        for(var info:getBlocks().values())if(info.nbt()!=null && info.state().is(com.mcmagic.omnira.registry.ModBlocks.ADVANCED_FORGE.get())){
            var master=com.mcmagic.omnira.forging.ForgeLayout.master(info.pos(),info.state());
            var facing=info.state().getValue(com.mcmagic.omnira.forging.AdvancedForgeBlock.FACING);
            for(int i=0;i<18;i++)if(positions.contains(master.offset(com.mcmagic.omnira.forging.ForgeLayout.offset(facing,i)))){
                info.nbt().putBoolean("StormDamaged",true);break;
            }
        }
    }
    @Inject(method="searchMovedStructure",at=@At("RETURN"),cancellable=true)
    private void omnira$rejectFragments(Level level,BlockPos start,Direction forced,CallbackInfoReturnable<Boolean> callback){
        if(!callback.getReturnValueZ())return;
        Set<BlockPos> positions=new HashSet<>();for(var pos:getBlocks().keySet())positions.add(pos.offset(anchor));
        if(!ForgeMovement.completeSelection(level,positions))callback.setReturnValue(false);
    }
}
