package com.mcmagic.omnira.codex;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Tokens are references, never independent copies of the source or its power. */
public record HeartLink(ResourceKey<Level> dimension,BlockPos origin,UUID identity,UUID lease) {
    public CompoundTag save(){var tag=new CompoundTag();tag.putString("Dimension",dimension.location().toString());tag.putLong("Origin",origin.asLong());tag.putUUID("Identity",identity);tag.putUUID("Lease",lease);return tag;}
    public static HeartLink load(CompoundTag tag){
        var dim=ResourceLocation.tryParse(tag.getString("Dimension"));
        return dim==null||!tag.hasUUID("Identity")||!tag.hasUUID("Lease")?null:new HeartLink(ResourceKey.create(Registries.DIMENSION,dim),BlockPos.of(tag.getLong("Origin")),tag.getUUID("Identity"),tag.getUUID("Lease"));
    }
    public ItemStack stack(){var stack=new ItemStack(MyriadCodexContent.HEART_ITEM.get());stack.set(DataComponents.CUSTOM_DATA,CustomData.of(save()));return stack;}
    public static HeartLink of(ItemStack stack){return stack.is(MyriadCodexContent.HEART_ITEM.get())?load(stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag()):null;}
    public MyriadCodexEntity find(MinecraftServer server){
        var level=server.getLevel(dimension);
        var codex=level!=null&&level.hasChunkAt(origin)?MyriadCodexEntity.from(level.getBlockEntity(origin)):null;
        return codex!=null&&codex.identity.equals(identity)&&codex.lease.equals(lease)?codex:null;
    }
    public boolean matches(ItemStack stack){return equals(of(stack));}
}
