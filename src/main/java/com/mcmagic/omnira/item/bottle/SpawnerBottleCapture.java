package com.mcmagic.omnira.item.bottle;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.level.BlockEvent;

/** Preserve the vanilla block entity, including spawn potentials, delay and custom components. */
public final class SpawnerBottleCapture {
    public static boolean capture(Player player,BlockPos pos,ItemStack bottle,Direction face){
        if(!(player.level() instanceof ServerLevel level)||!player.isAlive()||player.isSpectator()||!player.mayBuild()
                ||bottle.isEmpty()||!bottle.is(ModItems.POCKET_MAGIC_BOTTLE.get())||PocketBottleItem.filled(bottle)
                ||!level.hasChunkAt(pos)||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,face,bottle))return false;
        var state=level.getBlockState(pos);
        if(!state.is(Blocks.SPAWNER)||NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,state,player)).isCanceled()
                ||!level.getBlockState(pos).equals(state)||!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner))return false;
        var data=new CompoundTag();data.putString("Kind","spawner");data.putUUID("Owner",player.getUUID());
        data.put("BlockEntity",spawner.saveWithFullMetadata(level.registryAccess()));
        var storage=BottleStorage.get(level);var id=storage.put(data);
        if(!level.removeBlock(pos,false)){storage.remove(id);return false;}
        bottle.set(ModDataComponents.BOTTLE_CAPTURE,id);
        bottle.set(DataComponents.CUSTOM_NAME,Component.translatable("item.omnira.pocket_magic_bottle.filled",Blocks.SPAWNER.getName()));
        return true;
    }
    public static boolean release(ServerLevel level,CompoundTag data,Vec3 impact,Player player){
        if(player==null||player.level()!=level||!player.isAlive()||player.isSpectator()||!player.mayBuild())return false;
        var pos=BlockPos.containing(impact);var saved=data.getCompound("BlockEntity").copy();
        if(!saved.getString("id").equals("minecraft:mob_spawner")||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)
                ||!level.getWorldBorder().isWithinBounds(pos)||!level.getBlockState(pos).canBeReplaced()
                ||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,new ItemStack(ModItems.POCKET_MAGIC_BOTTLE.get()))
                ||!level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(pos),e->e.isAlive()&&e.isPickable()).isEmpty())return false;
        var snapshot=BlockSnapshot.create(level.dimension(),level,pos);
        if(!level.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),2))return false;
        if(NeoForge.EVENT_BUS.post(new BlockEvent.EntityMultiPlaceEvent(java.util.List.of(snapshot),level.getBlockState(pos.below()),player)).isCanceled()
                ||!level.getBlockState(pos).is(Blocks.SPAWNER)||!(level.getBlockEntity(pos) instanceof SpawnerBlockEntity)){
            snapshot.restore(3);return false;
        }
        var restored=level.getBlockEntity(pos);
        saved.putInt("x",pos.getX());saved.putInt("y",pos.getY());saved.putInt("z",pos.getZ());
        restored.loadWithComponents(saved,level.registryAccess());restored.setChanged();
        level.updateNeighborsAt(pos,Blocks.SPAWNER);level.sendBlockUpdated(pos,Blocks.SPAWNER.defaultBlockState(),Blocks.SPAWNER.defaultBlockState(),3);
        return true;
    }
    private SpawnerBottleCapture(){}
}
