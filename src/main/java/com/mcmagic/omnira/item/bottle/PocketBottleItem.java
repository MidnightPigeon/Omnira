package com.mcmagic.omnira.item.bottle;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;

public final class PocketBottleItem extends Item {
    public PocketBottleItem(Properties properties) {super(properties);}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        lines.add(Component.translatable(filled(stack)?"tooltip.omnira.bottle.release":"tooltip.omnira.bottle.capture")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        if(filled(stack))lines.add(Component.translatable("tooltip.omnira.bottle.clear").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    public static boolean filled(ItemStack stack) {return stack.has(ModDataComponents.BOTTLE_CAPTURE);}
    public static boolean restricted(ItemStack stack) {return stack.getItem() instanceof PocketBottleItem && filled(stack)
            || com.mcmagic.omnira.item.PortableStorageRules.stabilized(stack);}
    @Override public boolean canFitInsideContainerItems(ItemStack stack) {return !filled(stack);}
    public static void message(Player player,String key) {player.displayClientMessage(Component.translatable("message.omnira.bottle."+key),true);}
    public static boolean capture(Player player,Entity target,ItemStack stack) {
        if(!(player.level() instanceof ServerLevel level) || !stack.is(ModItems.POCKET_MAGIC_BOTTLE.get()) || stack.isEmpty()
                || filled(stack) || !player.isAlive() || !player.mayBuild() || player.isSpectator() || target.level()!=level) return false;
        if(target.getSelfAndPassengers().anyMatch(BottleCaptureRules::forbidden)) {
            message(player,"boss");return false;
        }
        if(target.getSelfAndPassengers().anyMatch(BottleCaptureRules::hostileOrAngry)) {
            message(player,"hostile");return false;
        }
        if(!target.isAlive() || target.isPassenger()
                || target.getSelfAndPassengers().anyMatch(e->!e.isAlive() || BottleCaptureRules.unsupportedStandalone(e)
                        || e instanceof Mob mob && mob.isLeashed())) {
            message(player,"unsupported");return false;
        }
        var tag=new CompoundTag();
        if(!target.save(tag)) {message(player,"unsupported");return false;}
        var data=new CompoundTag();data.putString("Kind","entity");data.put("Entity",tag);
        var id=BottleStorage.get(level).put(data);
        stack.set(ModDataComponents.BOTTLE_CAPTURE,id);
        stack.set(DataComponents.CUSTOM_NAME,Component.translatable("item.omnira.pocket_magic_bottle.filled",target.getName()));
        var entities=target.getSelfAndPassengers().toList();
        for(var entity:entities) entity.discard();
        return true;
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var stack=context.getItemInHand();
        if(player==null) return InteractionResult.PASS;
        if(!filled(stack) && context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.TEMPORAL_MOTE.get())){
            if(!(context.getLevel() instanceof ServerLevel server))return InteractionResult.SUCCESS;
            var pos=context.getClickedPos();
            if(!player.mayBuild() || !server.mayInteract(player,pos))return InteractionResult.FAIL;
            var data=new CompoundTag();data.putString("Kind","temporal_mote");
            var id=BottleStorage.get(server).put(data);
            if(!server.removeBlock(pos,false)){BottleStorage.get(server).remove(id);return InteractionResult.FAIL;}
            stack.set(ModDataComponents.BOTTLE_CAPTURE,id);
            stack.set(DataComponents.CUSTOM_NAME,Component.translatable("item.omnira.pocket_magic_bottle.mote"));
            return InteractionResult.SUCCESS;
        }
        if(filled(stack) || player.isShiftKeyDown()) return use(context.getLevel(),player,context.getHand()).getResult();
        if(ModList.get().isLoaded("sable")) return com.mcmagic.omnira.compat.SableBottleCompat.capture(context);
        return InteractionResult.PASS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player.isShiftKeyDown()) {
            if(player.isSpectator()) return InteractionResultHolder.fail(stack);
            if(!level.isClientSide && filled(stack)) {
                var storage=BottleStorage.get((ServerLevel)level);
                var id=stack.get(ModDataComponents.BOTTLE_CAPTURE);
                // A duplicated item must not delete a capture owned by a flying bottle.
                if(!storage.owns(id,null)) return InteractionResultHolder.fail(stack);
                storage.remove(id);
                stack.remove(ModDataComponents.BOTTLE_CAPTURE);
                stack.remove(DataComponents.CUSTOM_NAME);
                message(player,"cleared");
            }
            return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
        }
        if(!filled(stack)) return InteractionResultHolder.pass(stack);
        if(player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if(!level.isClientSide) {
            if(player.isSpectator()) return InteractionResultHolder.fail(stack);
            var projectile=new ThrownPocketBottle(ModEntityTypes.POCKET_BOTTLE.get(),level);
            var storage=BottleStorage.get((ServerLevel)level);var id=stack.get(ModDataComponents.BOTTLE_CAPTURE);
            boolean reusable=player.isCreative();
            var thrown=stack.copyWithCount(1);
            if(reusable) {
                var snapshot=storage.get(id);
                if(snapshot==null) return InteractionResultHolder.fail(stack);
                snapshot.remove("InFlight");snapshot.putBoolean("CreativeCopy",true);
                id=storage.put(snapshot);thrown.set(ModDataComponents.BOTTLE_CAPTURE,id);
            }
            if(!storage.claim(id,projectile.getUUID())) {
                player.setItemInHand(hand,ItemStack.EMPTY);
                return InteractionResultHolder.consume(ItemStack.EMPTY);
            }
            projectile.setOwner(player);projectile.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
            projectile.setItem(thrown);projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.25F,.5F);
            if(!level.addFreshEntity(projectile)) {if(reusable) storage.remove(id);else storage.unclaim(id,projectile.getUUID());return InteractionResultHolder.fail(stack);}
            // Replace the hand, not merely the count that vanilla creative use restores.
            if(!reusable) player.setItemInHand(hand,ItemStack.EMPTY);
            player.getCooldowns().addCooldown(this,10);
        }
        return InteractionResultHolder.sidedSuccess(player.isCreative()?stack:ItemStack.EMPTY,level.isClientSide);
    }
    @Override public void inventoryTick(ItemStack stack,Level world,Entity holder,int slot,boolean selected) {
        if(world instanceof ServerLevel level && filled(stack) && !BottleStorage.get(level).owns(stack.get(ModDataComponents.BOTTLE_CAPTURE),null)) stack.setCount(0);
    }
    public static boolean release(ServerLevel level,ItemStack stack,Vec3 impact) {
        return release(level,stack,impact,null);
    }
    public static boolean release(ServerLevel level,ItemStack stack,Vec3 impact,java.util.UUID projectile) {
        var id=stack.get(ModDataComponents.BOTTLE_CAPTURE);
        if(id==null) return false;
        var storage=BottleStorage.get(level);var data=storage.get(id);
        if(data==null || !storage.owns(id,projectile)) return false;
        boolean released;
        boolean returnEmpty=false;
        Vec3 returnPoint=impact;
        if(data.getString("Kind").equals("temporal_mote")) {
            var pos=BlockPos.containing(impact);
            if(!level.getBlockState(pos).canBeReplaced())pos=pos.above();
            released=level.hasChunkAt(pos) && level.getWorldBorder().isWithinBounds(pos)
                    && level.getBlockState(pos).canBeReplaced()
                    && level.setBlock(pos,ModBlocks.TEMPORAL_MOTE.get().defaultBlockState(),3);
        } else if(data.getString("Kind").equals("multiblock")) {
            var thrown=projectile==null?null:level.getEntity(projectile);
            Player actor=thrown instanceof ThrownPocketBottle bottle && bottle.getOwner() instanceof Player owner?owner:
                    data.hasUUID("Owner")?level.getServer().getPlayerList().getPlayer(data.getUUID("Owner")):null;
            released=MultiblockBottleCapture.release(level,data,impact,actor);
        } else if(data.getString("Kind").equals("structure")) {
            released=ModList.get().isLoaded("sable") && com.mcmagic.omnira.compat.SableBottleCompat.release(level,data,impact);
        } else {
            var tag=data.getCompound("Entity");
            if(data.getBoolean("CreativeCopy")) renewEntityIds(tag);
            var entity=EntityType.loadEntityRecursive(tag,level,e->e);
            if(entity==null || entity.getSelfAndPassengers().anyMatch(BottleCaptureRules::unsupportedStandalone)) return false;
            var group=entity.getSelfAndPassengers().toList();
            for(var member:group) for(var dimension:level.getServer().getAllLevels())
                if(dimension.getEntity(member.getUUID())!=null) return false;
            Vec3 original=entity.position();
            Vec3 landing=null;
            for(int radius=0;radius<=3 && landing==null;radius++) for(int y=0;y<=3 && landing==null;y++)
                for(int x=-radius;x<=radius && landing==null;x++) for(int z=-radius;z<=radius;z++) {
                    if(Math.max(Math.abs(x),Math.abs(z))!=radius) continue;
                    Vec3 point=impact.add(x,y,z),delta=point.subtract(original);
                    boolean clear=true;
                    for(var member:group) {
                        var box=member.getBoundingBox().move(delta);
                        if(box.minY<level.getMinBuildHeight() || box.maxY>=level.getMaxBuildHeight()
                                || !level.getWorldBorder().isWithinBounds(box) || !level.hasChunkAt(BlockPos.containing(box.getCenter())) || !level.noCollision(box)) clear=false;
                    }
                    if(clear) {landing=point;break;}
                }
            if(landing==null) return false;
            var delta=landing.subtract(original);
            for(var member:group) {member.setPos(member.position().add(delta));member.setDeltaMovement(Vec3.ZERO);member.fallDistance=0;}
            released=level.tryAddFreshEntityWithPassengers(entity);
            returnEmpty=group.stream().anyMatch(BottleCaptureRules::tamed) && !data.getBoolean("CreativeCopy");
            returnPoint=entity.position();
        }
        if(released) {
            storage.remove(id);
            if(returnEmpty) {
                var empty=new net.minecraft.world.entity.item.ItemEntity(level,returnPoint.x,returnPoint.y,returnPoint.z,
                        new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get()));
                empty.setDefaultPickUpDelay();
                level.addFreshEntity(empty);
            }
        }
        return released;
    }
    public static void renewEntityIds(CompoundTag tag) {
        tag.putUUID("UUID",java.util.UUID.randomUUID());
        for(var child:tag.getList("Passengers",net.minecraft.nbt.Tag.TAG_COMPOUND)) renewEntityIds((CompoundTag)child);
    }
}
