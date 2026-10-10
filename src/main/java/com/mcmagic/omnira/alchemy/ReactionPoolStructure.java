package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;

/** Uses the finished pool's local part layout; the dream crystal defines its front. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class ReactionPoolStructure {
    public static Block required(int part){
        return switch(part){
            case 3,5 -> ModBlocks.MANA_ENGINE.get();
            case 4 -> ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get();
            case 6 -> ModBlocks.CRYSTAL_BALL.get();
            case 8 -> ModBlocks.LIQUID_CRYSTAL_BALL.get();
            case 10 -> DreamContent.DREAM_CRYSTAL_BLOCK.get();
            case 12,14,16 -> ModBlocks.INFUSED_CRYSTAL_CASING.get();
            case 13 -> ModBlocks.PRIMITIVE_REACTION_CAULDRON.get();
            default -> ModBlocks.CRYSTAL_CASING.get();
        };
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void use(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        var level=event.getLevel();var pos=event.getPos();var player=event.getEntity();var crystal=event.getItemStack();
        if(player.isSpectator()||player.isShiftKeyDown()||!crystal.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())
                ||!level.getBlockState(pos).is(DreamContent.DREAM_CRYSTAL_BLOCK.get()))return;
        boolean success=activate(level,pos,player,crystal);
        if(!success&&!level.isClientSide)player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.reaction_pool_structure"),true);
        event.setCanceled(true);event.setCancellationResult(success?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.FAIL);
    }
    public static boolean activate(Level level,BlockPos clicked,Player player,ItemStack crystal){
        if(player.isSpectator()||player.isShiftKeyDown()||!crystal.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())
                ||!level.getBlockState(clicked).is(DreamContent.DREAM_CRYSTAL_BLOCK.get()))return false;
        for(var facing:Direction.Plane.HORIZONTAL){
            var center=clicked.subtract(ReactionPoolBlock.offset(facing,10));
            if(!matches(level,center,facing,player,crystal))continue;
            if(level.isClientSide)return true;
            if(!ReactionPoolBlock.place(level,center,facing))return false;
            crystal.shrink(1);
            level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
            return true;
        }
        return false;
    }
    private static boolean matches(Level level,BlockPos center,Direction facing,Player player,ItemStack crystal){
        for(int part=0;part<18;part++){
            var pos=center.offset(ReactionPoolBlock.offset(facing,part));
            if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,crystal))return false;
            var state=level.getBlockState(pos);var expected=required(part);
            if(expected==Blocks.AIR?!state.isAir():!state.is(expected))return false;
            var entity=level.getBlockEntity(pos);
            if(entity instanceof Container container&&!container.isEmpty())return false;
            if(entity instanceof CrystalBallBlockEntity ball){
                if(ball.hasPendingLoot())return false;
                for(int slot=0;slot<3;slot++)if(!ball.upgrades.getStackInSlot(slot).isEmpty())return false;
                if(ball instanceof LiquidCrystalBallBlockEntity liquid&&!liquid.tank.isEmpty())return false;
            }
            if(entity instanceof ManaEngineAccess engine&&!engine.engineState().inventory.isEmpty())return false;
            if(entity instanceof RoughAlchemyCauldronBlockEntity vessel){
                if(vessel.working()||!vessel.tank.isEmpty())return false;
                for(int slot=0;slot<vessel.items.getSlots();slot++)if(!vessel.items.getStackInSlot(slot).isEmpty())return false;
                if(!vessel.campfireData().getList("Items",10).isEmpty())return false;
            }
        }
        return true;
    }
    private ReactionPoolStructure(){}
}
