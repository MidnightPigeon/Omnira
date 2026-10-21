package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.registry.ModBlocks;
import com.simibubi.create.api.contraption.BlockMovementChecks;
import com.simibubi.create.api.contraption.BlockMovementChecks.CheckResult;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Optional Create integration: sealed terrain never moves; forges must remain complete. */
public final class ForgeMovement {
    private ForgeMovement(){}
    public static void register(){
        BlockMovementChecks.registerMovementAllowedCheck((state,level,pos)->state.is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get())
                ?CheckResult.FAIL:CheckResult.PASS);
        BlockMovementChecks.registerMovementAllowedCheck((state,level,pos)->state.is(ModBlocks.ADVANCED_FORGE.get())
                ?CheckResult.of(fullyGlued(level,pos,state)):CheckResult.PASS);
        BlockMovementChecks.registerMovementNecessaryCheck((state,level,pos)->state.is(ModBlocks.ADVANCED_FORGE.get())?CheckResult.SUCCESS:CheckResult.PASS);
    }
    public static Set<BlockPos> parts(Level level,BlockPos pos,BlockState state){
        return ForgeLayout.formedParts(level,pos,state);
    }
    public static boolean fullyGlued(Level level,BlockPos pos,BlockState state){
        var parts=parts(level,pos,state);if(parts.isEmpty())return false;
        var center=ForgeLayout.master(pos,state);
        var bounds=new AABB(center).inflate(1,0,1).expandTowards(0,1,0);
        // One glue selection must enclose all 18 cells, including the invisible entrance cells.
        return level.getEntities((net.minecraft.world.entity.Entity)null,bounds,glue->glue.isAlive()
                && (glue instanceof SuperGlueEntity || net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(glue.getType()).toString().equals("simulated:honey_glue")))
                .stream().anyMatch(glue->parts.stream().allMatch(at->glue.getBoundingBox().contains(net.minecraft.world.phys.Vec3.atCenterOf(at))));
    }
    public static boolean completeSelection(Level level,Set<BlockPos> selected){
        Set<BlockPos> checked=new HashSet<>();
        for(var pos:selected){
            var state=level.getBlockState(pos);
            if(state.is(ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()))return false;
            if(state.getBlock() instanceof com.mcmagic.omnira.item.bottle.BottleMultiblock machine){
                if(!checked.add(machine.bottleOrigin(pos,state)))continue;
                var parts=machine.bottleParts(level,pos,state);
                if(parts.isEmpty() || !selected.containsAll(parts))return false;
                if(state.is(ModBlocks.ADVANCED_FORGE.get())&&!fullyGlued(level,pos,state))return false;
            }else if(state.getBlock() instanceof com.mcmagic.omnira.myriad.MiniatureWorldBlock){
                var base=com.mcmagic.omnira.myriad.MiniatureWorldBlock.origin(pos,state);
                if(!checked.add(base))continue;
                int side=com.mcmagic.omnira.myriad.MiniatureWorldBlock.size(state);
                for(int i=0;i<side*side*side;i++){
                    var at=base.offset(com.mcmagic.omnira.myriad.MiniatureWorldBlock.offset(i,side));
                    var part=level.getBlockState(at);
                    if(!selected.contains(at)||!part.is(state.getBlock())||part.getValue(com.mcmagic.omnira.myriad.MiniatureWorldBlock.PART)!=i
                            ||part.getValue(com.mcmagic.omnira.myriad.MiniatureWorldBlock.COMPACT)!=state.getValue(com.mcmagic.omnira.myriad.MiniatureWorldBlock.COMPACT))return false;
                }
            }
        }
        return true;
    }
}
