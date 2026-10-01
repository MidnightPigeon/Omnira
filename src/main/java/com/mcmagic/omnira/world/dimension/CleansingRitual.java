package com.mcmagic.omnira.world.dimension;

import com.mcmagic.omnira.block.RitualEnergyCoreBlock;
import com.mcmagic.omnira.block.entity.CrystalPedestalBlockEntity;
import com.mcmagic.omnira.fate.ManuscriptReward;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

public final class CleansingRitual {
    private CleansingRitual() {}

    public static boolean activate(ServerLevel level,BlockPos core,Player caster,InteractionHand hand) {
        if(!caster.isAlive() || caster.isSpectator() || !level.mayInteract(caster,core)
                || !RitualEnergyCoreBlock.supportsBasicRitual(level.getBlockState(core))
                || !caster.getItemInHand(hand).is(ModItems.PARADOX_DUST.get()))return false;
        for(int[] offset:DreamRitual.ANCHORS) {
            BlockPos pos=core.offset(offset[0],0,offset[1]);
            if(!level.hasChunkAt(pos) || !level.mayInteract(caster,pos)
                    || !(level.getBlockEntity(pos) instanceof CrystalPedestalBlockEntity pedestal)
                    || !pedestal.getItem(0).is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))return false;
        }
        var center=core.getCenter();
        var targets=level.players().stream().filter(player->player.isAlive() && !player.isSpectator()
                && Math.abs(player.getY()-core.getY())<=3
                && (player.getX()-center.x)*(player.getX()-center.x)
                +(player.getZ()-center.z)*(player.getZ()-center.z)<9).toList();
        if(targets.isEmpty())return false;
        caster.getItemInHand(hand).consume(1,caster);
        for(ServerPlayer player:targets) {
            for(int slot=0;slot<player.getInventory().getContainerSize();slot++) {
                var book=player.getInventory().getItem(slot);
                if(!book.is(ModItems.INFUSED_GRIMOIRE.get()))continue;
                var result=new net.minecraft.world.item.ItemStack(level.random.nextBoolean()
                        ?ModItems.SANCTIFIED_GRIMOIRE.get():ModItems.CORRUPTED_GRIMOIRE.get());
                result.applyComponents(book.getComponentsPatch());
                player.getInventory().setItem(slot,result);
            }
            ManuscriptReward.cleanse(player);
            player.removeAllEffects();
            player.setHealth(1);
            player.getFoodData().setFoodLevel(1);
            player.getFoodData().setSaturation(0);
            level.sendParticles(new DustParticleOptions(new Vector3f(.83F,1F,.92F),1),player.getX(),player.getY()+1,player.getZ(),32,.55,.8,.55,.02);
        }
        level.playSound(null,core,SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.BLOCKS,1,1.4F);
        return true;
    }
}
