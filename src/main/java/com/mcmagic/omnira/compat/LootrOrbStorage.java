package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.menu.CrystalBallMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.ILootrInfoProvider;

/** Lootr owns player/team persistence; detached native orbs retain the native item/fluid UI. */
final class LootrOrbStorage {
    private LootrOrbStorage() {}
    private record Session(CrystalBallBlockEntity orb,java.util.Set<ServerPlayer> viewers) {}
    // Only active menus are retained. Lootr team inventories share one live view as well as one save.
    private static final java.util.Map<noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory,Session> SESSIONS=new java.util.IdentityHashMap<>();
    static void clear(){SESSIONS.clear();}
    static boolean mayBreak(net.minecraft.world.entity.player.Player player){
        return LootrAPI.canDestroyOrBreak(player)||player.isShiftKeyDown()
                &&(!LootrAPI.isBreakDisabled()||player.getAbilities().instabuild);
    }
    static void warnBreak(net.minecraft.world.entity.player.Player player){
        String key=LootrAPI.isBreakDisabled()?(player.getAbilities().instabuild?"cannot_break_sneak":"cannot_break"):"should_sneak";
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable("lootr.message."+key).setStyle(LootrAPI.getChatStyle()),true);
    }
    static boolean opened(CrystalBallBlockEntity source,ServerPlayer player){
        var data=LootrAPI.getData(ILootrInfoProvider.of(source,source.storageIdentity));
        return data!=null&&data.getInventory(player)!=null;
    }
    static void sync(CrystalBallBlockEntity source,ServerPlayer player){
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new com.mcmagic.omnira.network.LootrOrbAppearancePayload(
                source.getLevel().dimension().location(),source.getBlockPos(),source.storageIdentity,opened(source,player)));
    }
    static boolean isLootr(net.minecraft.world.level.block.entity.BlockEntity be){return LootrAPI.resolveBlockEntity(be)!=null;}
    static boolean manage(CrystalBallBlockEntity orb) {
        if(orb.lootrManaged)return true;
        if(!LootrAPI.isReady() || LootrAPI.isDisabled() || LootrAPI.isDimensionBlocked(orb.getLevel().dimension())
                || !orb.hasPendingLoot() || orb.resonant())return false;
        if(orb.getLootTable()!=null && LootrAPI.isLootTableBlacklisted(orb.getLootTable()))return false;
        orb.lootrManaged=true;orb.getLevel().invalidateCapabilities(orb.getBlockPos());orb.setChanged();
        if(orb.getLevel() instanceof net.minecraft.server.level.ServerLevel server)
            for(var player:server.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(orb.getBlockPos()),false))sync(orb,player);
        return true;
    }
    private static CrystalBallBlockEntity view(CrystalBallBlockEntity source,CompoundTag data){
        CrystalBallBlockEntity view=source instanceof LiquidCrystalBallBlockEntity
                ?new LiquidCrystalBallBlockEntity(source.getBlockPos(),source.getBlockState())
                :new CrystalBallBlockEntity(source.getBlockPos(),source.getBlockState());
        view.virtualStorage=true;view.setLevel(source.getLevel());
        data=data.copy();data.remove("LootrManaged");data.remove("LootrPersonalView");data.remove("PendingLoot");
        view.loadWithComponents(data,source.getLevel().registryAccess());return view;
    }
    static AbstractContainerMenu menu(int id,Inventory inventory,ServerPlayer player,CrystalBallBlockEntity source){
        if(player.isSpectator())return null;
        if(source instanceof LiquidCrystalBallBlockEntity && LiquidCrystalBallBlockEntity.treasureFluids().isEmpty())return null;
        var registries=source.getLevel().registryAccess();
        var provider=ILootrInfoProvider.of(source,source.storageIdentity);
        var personal=LootrAPI.getInventory(provider,player,(info,opener,container)->{
            var initial=view(source,source.saveWithoutMetadata(registries));
            initial.unpackLootTable(opener);
            var marker=new ItemStack(Items.GLASS_BOTTLE);
            marker.set(DataComponents.CUSTOM_DATA,CustomData.of(initial.saveWithoutMetadata(registries)));
            container.setItem(0,marker);container.setChanged();
        });
        if(personal==null || !personal.getItem(0).has(DataComponents.CUSTOM_DATA))return null;
        // Team members may share this inventory; notify all watchers using their own Lootr owner.
        if(source.getLevel() instanceof net.minecraft.server.level.ServerLevel server)
            for(var watcher:server.getChunkSource().chunkMap.getPlayers(new net.minecraft.world.level.ChunkPos(source.getBlockPos()),false))sync(source,watcher);
        var existing=SESSIONS.get(personal);
        if(existing!=null){
            existing.viewers().add(player);
            LootrAPI.award(provider,player);return new CrystalBallMenu(id,inventory,existing.orb());
        }
        var orb=view(source,personal.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag());
        var viewers=new java.util.HashSet<ServerPlayer>();viewers.add(player);
        SESSIONS.put(personal,new Session(orb,viewers));
        orb.lootrPersonalView=true;
        orb.personalValid=()->!source.isRemoved() && source.getLevel().getBlockEntity(source.getBlockPos())==source;
        final CompoundTag[] last={null};
        orb.storageChanged=()->{
            var state=orb.saveWithoutMetadata(registries);
            if(state.equals(last[0]))return;
            last[0]=state.copy();
            var marker=new ItemStack(Items.GLASS_BOTTLE);marker.set(DataComponents.CUSTOM_DATA,CustomData.of(state));
            personal.setItem(0,marker);personal.setChanged();
        };
        orb.personalClose=closing->{
            orb.storageChanged.run();viewers.remove(closing);
            if(closing instanceof ServerPlayer viewer)sync(source,viewer);
            if(viewers.isEmpty())SESSIONS.remove(personal);
        };
        orb.storageChanged.run();LootrAPI.award(provider,player);
        return new CrystalBallMenu(id,inventory,orb);
    }
}
