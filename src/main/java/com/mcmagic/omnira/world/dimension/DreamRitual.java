package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class DreamRitual {
    public static final int[][] ANCHORS={{0,-3},{-2,-2},{2,-2},{-3,0},{3,0},{-2,2},{2,2},{0,3}};
    private DreamRitual() {}
    public static boolean anchorsIntact(Level level,BlockPos portal) {
        if(!com.mcmagic.omnira.block.RitualEnergyCoreBlock.supportsBasicRitual(level.getBlockState(portal.below()))) return false;
        for(int[] offset:ANCHORS) {
            BlockPos anchor=portal.offset(offset[0],-1,offset[1]);
            if(!level.hasChunkAt(anchor)) continue;
            if(!level.getBlockState(anchor).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                    || !(level.getBlockEntity(anchor) instanceof CrystalPedestalBlockEntity pedestal)
                    || !pedestal.getItem(0).is(com.mcmagic.omnira.registry.ModItems.SPIRITUAL_CRYSTAL.get())) return false;
        }
        return true;
    }
    public static InteractionResult activate(Level world,BlockPos core,Player player,net.minecraft.world.InteractionHand hand) {
        if(!(world instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        if(player.isSpectator() || !player.isAlive()) return InteractionResult.PASS;
        var bed=player.getItemInHand(hand);
        if(!bed.is(ItemTags.BEDS)) return InteractionResult.PASS;
        BlockPos center=core.above();
        if(level.getBlockState(center).is(ModBlocks.DREAM_PORTAL.get())) return InteractionResult.CONSUME;
        String failure=null;
        if(!com.mcmagic.omnira.block.RitualEnergyCoreBlock.supportsBasicRitual(level.getBlockState(core)) || !level.mayInteract(player,core)
                || !level.mayInteract(player,center))
            failure="structure";
        if(failure==null) for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
            BlockPos floor=center.offset(x,-2,z);
            if(!level.hasChunkAt(floor) || !level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP))
                failure="floor";
        }
        if(failure==null && !anchorsIntact(level,center)) failure="structure";
        if(failure==null && (!level.getBlockState(center).isAir() || !level.getBlockState(center.above()).isAir())) failure="space";
        if(failure==null && level.getServer().getLevel(ModDimensions.DREAM_REALM)==null) failure="unavailable";
        if(failure!=null) {
            player.displayClientMessage(Component.translatable("message.omnira.dream."+failure),true);
            return InteractionResult.CONSUME;
        }
        if(!level.setBlockAndUpdate(center,ModBlocks.DREAM_PORTAL.get().defaultBlockState())) return InteractionResult.CONSUME;
        bed.consume(1,player);
        if(level.getBlockEntity(center) instanceof DreamPortalBlockEntity portal) portal.initialize(false);
        level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,1,.6F);
        return InteractionResult.CONSUME;
    }
}
