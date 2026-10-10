package com.mcmagic.omnira.item;

import com.mcmagic.omnira.block.CrystalFamilies;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluids;

public final class ArcaneDustItem extends Item {
    public ArcaneDustItem(Properties properties){super(properties);}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();var player=context.getPlayer();var stack=context.getItemInHand();
        if(stack.isEmpty()||player==null||player.isSpectator()||!level.mayInteract(player,pos))return InteractionResult.PASS;
        if(level.getBlockState(pos).is(com.mcmagic.omnira.space.phase.PhaseContent.CLUSTER.get()))return com.mcmagic.omnira.space.phase.PhaseGrowth.grow(context);
        Block[] family=null;
        for(var candidate:CrystalFamilies.all())if(level.getBlockState(pos).is(candidate[0])){family=candidate;break;}
        if(family==null)return InteractionResult.PASS;
        boolean changed=false;
        for(var face:Direction.values()){
            var at=pos.relative(face);
            if(level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at)||!level.hasChunkAt(at)
                    ||!level.mayInteract(player,at)||!player.mayUseItemAt(at,face,stack))continue;
            var old=level.getBlockState(at);Block next=null;
            if(BuddingAmethystBlock.canClusterGrowAtState(old))next=family[1];
            else if(old.hasProperty(AmethystClusterBlock.FACING)&&old.getValue(AmethystClusterBlock.FACING)==face)
                for(int stage=1;stage<4;stage++)if(old.is(family[stage])){next=family[stage+1];break;}
            if(next==null)continue;
            var grown=next.defaultBlockState().setValue(AmethystClusterBlock.FACING,face)
                    .setValue(AmethystClusterBlock.WATERLOGGED,old.getFluidState().is(Fluids.WATER));
            if(!grown.canSurvive(level,at))continue;
            if(level.isClientSide){changed=true;continue;}
            if(level.setBlockAndUpdate(at,grown)){
                changed=true;
                ((ServerLevel)level).sendParticles(ParticleTypes.END_ROD,at.getX()+.5,at.getY()+.5,at.getZ()+.5,4,.18,.18,.18,.015);
            }
        }
        if(!changed)return InteractionResult.PASS;
        if(!level.isClientSide)stack.consume(1,player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
