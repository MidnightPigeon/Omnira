package com.mcmagic.omnira.alchemy.catalysis;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class CatalysisStructure {
    public static Block required(int part){return switch(part){case 0->ModBlocks.ROUGH_ALCHEMY_CAULDRON.get();case 1->ModBlocks.ROUGH_ALCHEMICAL_ENERGY_CORE.get();default->ModBlocks.CRYSTAL_BALL.get();};}
    @net.neoforged.bus.api.SubscribeEvent public static void use(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(event.getEntity().isSpectator()||event.getEntity().isShiftKeyDown()||!event.getItemStack().is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())
                ||!event.getLevel().getBlockState(event.getPos()).is(required(1)))return;
        boolean ok=activate(event.getLevel(),event.getPos(),event.getEntity(),event.getItemStack());
        event.setCanceled(true);event.setCancellationResult(ok?InteractionResult.sidedSuccess(event.getLevel().isClientSide):InteractionResult.FAIL);
    }
    public static boolean activate(Level level,BlockPos middle,Player player,ItemStack crystal){
        if(player.isSpectator()||!crystal.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))return false;var base=middle.below();
        for(int i=0;i<3;i++){
            var p=base.above(i);if(!level.hasChunkAt(p)||level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)
                    ||!level.mayInteract(player,p)||!player.mayUseItemAt(p,Direction.UP,crystal)||!level.getBlockState(p).is(required(i)))return false;
            var be=level.getBlockEntity(p);if(be instanceof Container c&&!c.isEmpty())return false;
            if(be instanceof com.mcmagic.omnira.alchemy.RoughAlchemyCauldronBlockEntity cauldron){
                if(!cauldron.tank.isEmpty()||cauldron.working())return false;
                for(int slot=0;slot<cauldron.items.getSlots();slot++)if(!cauldron.items.getStackInSlot(slot).isEmpty())return false;
            }
            if(be instanceof com.mcmagic.omnira.block.entity.CrystalBallBlockEntity ball){
                if(ball.hasPendingLoot())return false;for(int slot=0;slot<3;slot++)if(!ball.upgrades.getStackInSlot(slot).isEmpty())return false;
            }
        }
        if(level.isClientSide)return true;
        if(!CatalysisBlock.place(level,base,player.getDirection().getOpposite()))return false;
        crystal.shrink(1);return true;
    }
    private CatalysisStructure(){}
}
