package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import com.mcmagic.omnira.travel.WaymarkDirectory;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

public final class WaymarkBlockEntity extends BlockEntity {
    private UUID id=UUID.randomUUID();
    private String name="";
    private boolean unclaimed=true;
    public WaymarkBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.WAYMARK.get(),pos,state);}
    public UUID id() {return id;}
    public String name() {return name;}
    public WaymarkDirectory.Entry entry() {return new WaymarkDirectory.Entry(id,level.dimension().location(),worldPosition,name);}
    public void register() {if(level instanceof ServerLevel server) WaymarkDirectory.get(server).put(entry());}
    public void placedByPlayer() {unclaimed=false;setChanged();}
    public void discover(net.minecraft.server.level.ServerPlayer player) {
        if(level instanceof ServerLevel server) {
            var directory=WaymarkDirectory.get(server);
            if(unclaimed) {
                unclaimed=false;
                rename(directory.nextLostName(player.getUUID()));
            }
            register();directory.discover(player.getUUID(),id);
        }
    }
    public void rename(String value) {
        name=value.codePoints().filter(c->!Character.isISOControl(c) && c!=0x00a7).limit(32)
                .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().strip();
        setChanged();register();
        if(level!=null) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    @Override public void onLoad() {super.onLoad();register();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putUUID("Id",id);tag.putString("Name",name);tag.putBoolean("Unclaimed",unclaimed);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);if(tag.hasUUID("Id")) id=tag.getUUID("Id");name=tag.getString("Name");
        unclaimed=tag.contains("Unclaimed")?tag.getBoolean("Unclaimed"):name.isBlank();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}
