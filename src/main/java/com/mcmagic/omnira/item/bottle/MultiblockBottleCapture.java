package com.mcmagic.omnira.item.bottle;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.*;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class MultiblockBottleCapture {
    private MultiblockBottleCapture(){}
    @net.neoforged.bus.api.SubscribeEvent
    public static void use(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        var stack=event.getItemStack();var player=event.getEntity();
        if(!stack.is(ModItems.POCKET_MAGIC_BOTTLE.get()) || PocketBottleItem.filled(stack) || player.isShiftKeyDown()
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof BottleMultiblock))return;
        boolean ok=event.getLevel().isClientSide || capture(player,event.getPos(),stack);
        event.setCancellationResult(ok?InteractionResult.sidedSuccess(event.getLevel().isClientSide):InteractionResult.FAIL);
        event.setCanceled(true);
    }
    public static boolean capture(Player player,BlockPos clicked,ItemStack bottle){
        if(!(player.level() instanceof ServerLevel level) || player.isSpectator() || !player.mayBuild()
                || !bottle.is(ModItems.POCKET_MAGIC_BOTTLE.get()) || PocketBottleItem.filled(bottle))return false;
        var state=level.getBlockState(clicked);
        if(!(state.getBlock() instanceof BottleMultiblock machine))return false;
        var origin=machine.bottleOrigin(clicked,state);var parts=machine.bottleParts(level,clicked,state);
        if(parts.isEmpty())return false;
        var entries=new ListTag();
        for(var pos:parts){
            if(!level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,bottle))return false;
            var before=level.getBlockState(pos);
            if(NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,before,player)).isCanceled())return false;
            var tag=new CompoundTag();tag.putLong("Offset",pos.subtract(origin).asLong());tag.put("State",NbtUtils.writeBlockState(before));
            var be=level.getBlockEntity(pos);if(be!=null)tag.put("Entity",be.saveWithFullMetadata(level.registryAccess()));
            entries.add(tag);
        }
        // Protection callbacks must not turn a snapshot into a partial capture.
        if(!parts.equals(machine.bottleParts(level,clicked,state)))return false;
        var data=new CompoundTag();data.putString("Kind","multiblock");data.put("Blocks",entries);data.putUUID("Owner",player.getUUID());
        var id=BottleStorage.get(level).put(data);
        for(var pos:parts)level.removeBlockEntity(pos);
        for(var pos:parts)level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        for(var pos:parts)level.updateNeighborsAt(pos,Blocks.AIR);
        bottle.set(ModDataComponents.BOTTLE_CAPTURE,id);
        bottle.set(DataComponents.CUSTOM_NAME,Component.translatable("item.omnira.pocket_magic_bottle.filled",state.getBlock().getName()));
        return true;
    }
    public static boolean release(ServerLevel level,CompoundTag data,Vec3 impact,Player player){
        if(player==null || player.level()!=level || player.isSpectator() || !player.mayBuild())return false;
        var origin=BlockPos.containing(impact);var entries=data.getList("Blocks",Tag.TAG_COMPOUND);
        if(entries.isEmpty() || entries.size()>512)return false;
        var states=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        var snapshots=new ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(var entry:entries){
            var tag=(CompoundTag)entry;var pos=origin.offset(BlockPos.of(tag.getLong("Offset")));
            var state=NbtUtils.readBlockState(level.holderLookup(net.minecraft.core.registries.Registries.BLOCK),tag.getCompound("State"));
            if(!(state.getBlock() instanceof BottleMultiblock) || states.put(pos,state)!=null || !level.hasChunkAt(pos)
                    || level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).isAir() || !level.mayInteract(player,pos)
                    || !player.mayUseItemAt(pos,Direction.UP,new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get()))
                    || !level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(pos),e->e.isAlive() && e.isPickable()).isEmpty())return false;
            snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,pos));
        }
        for(var pos:states.keySet())if(!level.getBlockState(pos).isAir())return false;
        // Install the whole shape before notifying neighbours; restore inventories without dropping them.
        for(var entry:states.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
        if(NeoForge.EVENT_BUS.post(new BlockEvent.EntityMultiPlaceEvent(snapshots,level.getBlockState(origin.below()),player)).isCanceled()){
            for(var snapshot:snapshots)snapshot.restore(2);
            return false;
        }
        for(var entry:entries){
            var tag=(CompoundTag)entry;var pos=origin.offset(BlockPos.of(tag.getLong("Offset")));var be=level.getBlockEntity(pos);
            if(be!=null && tag.contains("Entity")){
                var saved=tag.getCompound("Entity").copy();saved.putInt("x",pos.getX());saved.putInt("y",pos.getY());saved.putInt("z",pos.getZ());
                be.loadWithComponents(saved,level.registryAccess());be.setChanged();
            }
        }
        for(var entry:states.entrySet()){
            var pos=entry.getKey();level.updateNeighborsAt(pos,entry.getValue().getBlock());
            level.sendBlockUpdated(pos,entry.getValue(),entry.getValue(),3);
        }
        return true;
    }
}
