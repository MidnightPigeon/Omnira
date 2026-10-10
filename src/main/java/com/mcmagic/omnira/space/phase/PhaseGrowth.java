package com.mcmagic.omnira.space.phase;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

public final class PhaseGrowth {
    private static final Set<BlockPos> FORM=create();
    private static Set<BlockPos> create(){
        var cells=new LinkedHashSet<BlockPos>();
        for(int y=0;y<7;y++)cells.add(new BlockPos(0,y,0));
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++){
            if(dx==0&&dz==0)continue;
            for(int step=0;step<=4;step++){
                int y=step;cells.add(new BlockPos(dx*step,y,dz*step));
                if(step>0){cells.add(new BlockPos(dx*(step-1),y,dz*step));cells.add(new BlockPos(dx*step,y,dz*(step-1)));cells.add(new BlockPos(dx*(step-1),y,dz*(step-1)));}
                if(step<=2)cells.add(new BlockPos(dx*step,y+1,dz*step));
            }
        }
        return Collections.unmodifiableSet(cells);
    }
    public static Set<BlockPos> form(){return FORM;}
    public static InteractionResult grow(UseOnContext context){return grow(context,context.getLevel().random);}
    public static InteractionResult grow(UseOnContext context,net.minecraft.util.RandomSource random){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();var stack=context.getItemInHand();
        if(player==null||!level.getBlockState(pos).is(PhaseContent.CLUSTER.get()))return InteractionResult.PASS;
        var replacements=new LinkedHashSet<BlockPos>();
        for(var local:FORM){
            var at=pos.offset(local);if(level.isOutsideBuildHeight(at)||!level.hasChunkAt(at)||!level.getWorldBorder().isWithinBounds(at)
                    ||!level.mayInteract(player,at)||!player.mayUseItemAt(at,Direction.UP,stack))return InteractionResult.PASS;
            var state=level.getBlockState(at);
            if(state.getDestroySpeed(level,at)<0){if(at.equals(pos.above()))return InteractionResult.PASS;continue;}
            if(!level.isClientSide&&!state.isAir()){
                var event=new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(level,at,state,player);
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);if(event.isCanceled())return InteractionResult.PASS;
            }
            replacements.add(at);
        }
        if(!level.isClientSide){
            var server=(ServerLevel)level;stack.consume(1,player);
            if(random.nextInt(4)!=0){server.sendParticles(ParticleTypes.END_ROD,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3,.2,.2,.2,.01);return InteractionResult.SUCCESS;}
            var occupants=PhaseImpact.occupants(server,replacements);
            for(var at:replacements){
                if(!at.equals(pos)&&!level.getBlockState(at).isAir())level.destroyBlock(at,false,player);
                level.setBlockAndUpdate(at,PhaseContent.BLOCK.get().defaultBlockState());
                server.sendParticles(ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.1,at.getZ()+.5,2,.05,.15,.05,.08);
            }
            PhaseImpact.impale(occupants);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    private PhaseGrowth(){}
}
