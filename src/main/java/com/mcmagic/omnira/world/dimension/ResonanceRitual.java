package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class ResonanceRitual {
    private ResonanceRitual() {}
    public static boolean activate(ServerLevel level,BlockPos pos,Player player,InteractionHand hand) {
        if(!player.isAlive() || player.isSpectator() || !level.mayInteract(player,pos)
                || !com.mcmagic.omnira.block.RitualEnergyCoreBlock.supportsBasicRitual(level.getBlockState(pos))
                || !player.getItemInHand(hand).is(ModItems.SPACETIME_KNOT.get()) || !level.isEmptyBlock(pos.above()))return false;
        var pedestals=new java.util.ArrayList<CrystalPedestalBlockEntity>();
        for(var offset:DreamRitual.ANCHORS) {
            var anchor=pos.offset(offset[0],0,offset[1]);
            if(!level.hasChunkAt(anchor) || !level.mayInteract(player,anchor)
                    || !(level.getBlockEntity(anchor) instanceof CrystalPedestalBlockEntity pedestal)
                    || !pedestal.getItem(0).is(ModItems.RESONANCE_CORE.get()))return false;
            pedestals.add(pedestal);
        }
        var output=new net.minecraft.world.entity.item.ItemEntity(level,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,new ItemStack(ModItems.RESONANCE_TERMINAL.get()));
        output.setDeltaMovement(0,.2,0);
        if(!level.addFreshEntity(output))return false;
        for(var pedestal:pedestals) {
            pedestal.removeItem(0,1);
            var p=pedestal.getBlockPos();level.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,p.getX()+.5,p.getY()+1,p.getZ()+.5,12,.15,.2,.15,.1);
        }
        player.getItemInHand(hand).consume(1,player);
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
        return true;
    }
}
