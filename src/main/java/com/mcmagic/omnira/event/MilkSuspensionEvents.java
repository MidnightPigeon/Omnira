package com.mcmagic.omnira.event;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid="omnira")
public final class MilkSuspensionEvents {
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) {
        fill(event);
    }

    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if(inject(event)) return;
        fill(event);
    }

    private static boolean inject(PlayerInteractEvent.RightClickBlock event) {
        var player=event.getEntity();var stack=event.getItemStack();var level=event.getLevel();var pos=event.getPos();
        if(event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE
                || event.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE) return false;
        if(!stack.is(ModItems.MILK_SUSPENSION.get()) || player.isSpectator() || !player.isAlive()
                || !player.mayBuild() || !level.mayInteract(player,pos) || !level.hasChunkAt(pos)) return false;
        var tank=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,pos,event.getFace());
        if(tank==null) return false;
        // Intercept before a tank opens its GUI; only the server mutates fluid and inventory.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if(!level.isClientSide && injectBucket(tank,stack,player.getAbilities().instabuild))
            level.playSound(null,pos,SoundEvents.BUCKET_EMPTY,SoundSource.PLAYERS,1,1);
        return true;
    }

    /** Any positive transfer consumes the portion, even when the remaining capacity is smaller. */
    public static boolean injectBucket(net.neoforged.neoforge.fluids.capability.IFluidHandler tank,ItemStack stack,boolean creative) {
        if(!stack.is(ModItems.MILK_SUSPENSION.get()) || stack.isEmpty()) return false;
        var candidates=new java.util.ArrayList<net.neoforged.neoforge.fluids.FluidStack>();
        for(int i=0;i<tank.getTanks();i++) {
            var existing=tank.getFluidInTank(i);
            if(!existing.isEmpty() && isMilk(existing.getFluid())) candidates.add(existing.copyWithAmount(1000));
        }
        var standard=net.neoforged.neoforge.common.NeoForgeMod.MILK.get();
        for(var fluid:net.minecraft.core.registries.BuiltInRegistries.FLUID) {
            if(fluid!=standard && fluid.defaultFluidState().isSource() && isMilk(fluid))
                candidates.add(new net.neoforged.neoforge.fluids.FluidStack(fluid,1000));
        }
        candidates.add(new net.neoforged.neoforge.fluids.FluidStack(standard,1000));
        var simulate=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;
        var execute=net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
        for(var milk:candidates) {
            int accepted=Math.min(1000,tank.fill(milk.copy(),simulate));
            if(accepted<=0 || tank.fill(milk.copyWithAmount(accepted),execute)<=0) continue;
            if(!creative) stack.shrink(1);
            return true;
        }
        return false;
    }

    private static boolean isMilk(net.minecraft.world.level.material.Fluid fluid) {
        return fluid==net.neoforged.neoforge.common.NeoForgeMod.MILK.get()
                || fluid.defaultFluidState().is(net.neoforged.neoforge.common.Tags.Fluids.MILK)
                || net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).getPath().equals("milk");
    }

    private static void fill(PlayerInteractEvent event) {
        var player=event.getEntity();
        if(!event.getItemStack().is(Items.BUCKET) || player.isSpectator() || !player.isAlive()) return;
        var level=event.getLevel();
        var start=player.getEyePosition();
        var end=start.add(player.getLookAngle().scale(player.blockInteractionRange()));
        // Dropped items are not vanilla interaction targets; stop our ray at solid terrain.
        end=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
        ItemEntity selected=null;
        double nearest=Double.MAX_VALUE;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,new AABB(start,end).inflate(.2),
                entity->entity.isAlive() && entity.getItem().is(ModItems.MILK_SUSPENSION.get()))) {
            var bounds=item.getBoundingBox().inflate(.15);
            var hit=bounds.contains(start)?java.util.Optional.of(start):bounds.clip(start,end);
            if(hit.isPresent() && start.distanceToSqr(hit.get())<nearest) {
                nearest=start.distanceToSqr(hit.get());
                selected=item;
            }
        }
        if(selected==null) return;
        var result=InteractionResult.sidedSuccess(level.isClientSide);
        if(event instanceof PlayerInteractEvent.RightClickItem itemEvent) {
            itemEvent.setCanceled(true);
            itemEvent.setCancellationResult(result);
        } else if(event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
            blockEvent.setCanceled(true);
            blockEvent.setCancellationResult(result);
        }
        if(level.isClientSide) return;
        player.setItemInHand(event.getHand(),ItemUtils.createFilledResult(event.getItemStack(),player,new ItemStack(Items.MILK_BUCKET)));
        var remainder=selected.getItem().copy();
        remainder.shrink(1);
        if(remainder.isEmpty()) selected.discard();
        else selected.setItem(remainder);
        level.playSound(null,player.blockPosition(),SoundEvents.BUCKET_FILL,SoundSource.PLAYERS,1,1);
    }
}
