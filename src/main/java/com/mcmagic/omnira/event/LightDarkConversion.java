package com.mcmagic.omnira.event;

import com.mcmagic.omnira.entity.ShadowMist;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid="omnira")
public final class LightDarkConversion {
    private LightDarkConversion() {}
    public static boolean consumeLightBlock(Level level,BlockPos pos) {
        return !level.isClientSide && level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                && level.getBlockState(pos).is(DreamContent.LIGHT_CRYSTAL_CORE.get())
                && level.setBlockAndUpdate(pos,DreamContent.LIGHT_CONDENSATE.get().defaultBlockState());
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if(event.getUseBlock()==net.neoforged.neoforge.common.util.TriState.FALSE
                || event.getUseItem()==net.neoforged.neoforge.common.util.TriState.FALSE) return;
        interact(event);
    }
    @SubscribeEvent public static void useItem(PlayerInteractEvent.RightClickItem event) {interact(event);}
    private static void interact(PlayerInteractEvent event) {
        var player=event.getEntity();var level=event.getLevel();var stack=event.getItemStack();
        if(player.isSpectator() || !player.isAlive() || !player.mayBuild()) return;
        BooleanSupplier consume;Item result;
        if(stack.is(ModItems.DARK_MICROCORE.get()) && event instanceof PlayerInteractEvent.RightClickBlock block) {
            var pos=block.getPos();
            if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,block.getFace(),stack)
                    || !level.getBlockState(pos).is(DreamContent.LIGHT_CRYSTAL_CORE.get())) return;
            consume=()->consumeLightBlock(level,pos);result=ModItems.LIGHT_MICROCORE.get();
        } else if(stack.is(ModItems.LIGHT_MICROCORE.get())) {
            var start=player.getEyePosition();var end=start.add(player.getLookAngle().scale(player.blockInteractionRange()));
            end=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,player)).getLocation();
            ShadowMist selected=null;double distance=Double.MAX_VALUE;
            for(var mist:level.getEntitiesOfClass(ShadowMist.class,new AABB(start,end).inflate(.01),m->m.isAlive()&&!m.collapsing())) {
                var bounds=mist.getBoundingBox();var hit=bounds.contains(start)?java.util.Optional.of(start):bounds.clip(start,end);
                if(hit.isPresent() && start.distanceToSqr(hit.get())<distance && level.mayInteract(player,mist.blockPosition())) {
                    distance=start.distanceToSqr(hit.get());selected=mist;
                }
            }
            if(selected==null) return;
            consume=selected::consumeForConversion;result=ModItems.DARK_MICROCORE.get();
        } else return;
        if(event instanceof PlayerInteractEvent.RightClickBlock block) {
            block.setCanceled(true);block.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        } else if(event instanceof PlayerInteractEvent.RightClickItem item) {
            item.setCanceled(true);item.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        }
        if(!level.isClientSide && consume.getAsBoolean()) {
            // A transformation consumes one input in creative too, rather than duplicating it.
            var converted=stack.transmuteCopy(result,1);
            if(stack.getCount()==1) player.setItemInHand(event.getHand(),converted);
            else {stack.shrink(1);if(!player.getInventory().add(converted)) player.drop(converted,false);}
        }
    }
    @SubscribeEvent public static void tick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof ItemEntity item) convertDropped(item);
    }
    public static boolean convertDropped(ItemEntity entity) {
        if(entity.level().isClientSide || !entity.isAlive()) return false;
        var stack=entity.getItem();var level=entity.level();
        if(stack.is(ModItems.DARK_MICROCORE.get())) {
            // Tiny inflation includes a dropped item resting exactly on a block face.
            var bounds=entity.getBoundingBox().inflate(.015);
            for(var pos:BlockPos.betweenClosed(BlockPos.containing(bounds.minX,bounds.minY,bounds.minZ),BlockPos.containing(bounds.maxX,bounds.maxY,bounds.maxZ))) {
                if(level.hasChunkAt(pos) && level.getBlockState(pos).is(DreamContent.LIGHT_CRYSTAL_CORE.get())) {
                    var source=pos.immutable();
                    return exchangeDropped(entity,ModItems.LIGHT_MICROCORE.get(),()->consumeLightBlock(level,source));
                }
            }
        } else if(stack.is(ModItems.LIGHT_MICROCORE.get())) {
            for(var mist:level.getEntitiesOfClass(ShadowMist.class,entity.getBoundingBox(),m->m.isAlive()&&!m.collapsing())) {
                double r=mist.diameter(0)/2+entity.getBbWidth()/2,dx=mist.getX()-entity.getX(),dz=mist.getZ()-entity.getZ();
                if(dx*dx+dz*dz<=r*r) return exchangeDropped(entity,ModItems.DARK_MICROCORE.get(),mist::consumeForConversion);
            }
        }
        return false;
    }
    private static boolean exchangeDropped(ItemEntity source,Item target,BooleanSupplier consume) {
        var converted=source.getItem().transmuteCopy(target,1);
        if(source.getItem().getCount()==1) {
            if(!consume.getAsBoolean()) return false;
            source.setItem(converted);return true;
        }
        var output=new ItemEntity(source.level(),source.getX(),source.getY(),source.getZ(),converted);
        output.setDeltaMovement(source.getDeltaMovement());output.setDefaultPickUpDelay();
        if(!source.level().addFreshEntity(output)) return false;
        if(!consume.getAsBoolean()) {output.discard();return false;}
        var remainder=source.getItem().copy();remainder.shrink(1);source.setItem(remainder);return true;
    }
}
