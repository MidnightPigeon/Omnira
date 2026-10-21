package com.mcmagic.omnira.codex.edit;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/** Saved construction intent, including compatibility with old pending sampling requests. */
final class EcologyRequest {
    final UUID id;
    final BlockPos center;
    final int diameter;
    final ResourceLocation biomeId;
    @Nullable final UUID owner;
    final long seed;
    long checks;
    @Nullable EcologySampler.Sample sample;
    String notice="";
    String error="";

    EcologyRequest(UUID id, BlockPos center, int diameter, ResourceLocation biomeId, @Nullable UUID owner, long seed) {
        this.id=id; this.center=center.immutable(); this.diameter=diameter; this.biomeId=biomeId; this.owner=owner; this.seed=seed;
    }

    @Nullable Holder<Biome> biome(ServerLevel destination) {
        return destination.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolder(ResourceKey.create(Registries.BIOME,biomeId)).orElse(null);
    }

    void construct(ServerLevel destination, Runnable dirty) {
        if (sample!=null || !error.isEmpty()) return;
        var biome=biome(destination);
        if (biome==null) error="selected_biome_removed";
        else try {
            sample=EcologyLandscape.build(destination,diameter,biome,seed);
            notice="constructed_landscape:"+biomeId;
        } catch (IllegalArgumentException e) { error=e.getMessage(); }
        dirty.run();
    }

    CompoundTag save() {
        var tag=new CompoundTag();
        tag.putUUID("Id",id); tag.putLong("Center",center.asLong()); tag.putInt("Diameter",diameter);
        tag.putString("Biome",biomeId.toString()); tag.putLong("Seed",seed); tag.putLong("Checks",checks);
        tag.putString("Notice",notice); tag.putString("Error",error);
        if (owner!=null) tag.putUUID("Owner",owner);
        if (sample!=null) {
            tag.putString("SampleDimension",sample.level().dimension().location().toString());
            tag.putLong("Surface",sample.surface().asLong());
            var states=new ListTag(); for (var state:sample.states()) states.add(NbtUtils.writeBlockState(state));
            tag.put("Sample",states);
        }
        return tag;
    }

    static EcologyRequest load(CompoundTag tag, ServerLevel level) {
        var request=new EcologyRequest(tag.getUUID("Id"),BlockPos.of(tag.getLong("Center")),tag.getInt("Diameter"),
                ResourceLocation.parse(tag.getString("Biome")),tag.hasUUID("Owner")?tag.getUUID("Owner"):null,tag.getLong("Seed"));
        request.checks=tag.getLong("Checks"); request.notice=tag.getString("Notice"); request.error=tag.getString("Error");
        if (tag.contains("Sample")) {
            var source=level.getServer().getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(tag.getString("SampleDimension"))));
            if (source!=null) {
                var states=new ArrayList<BlockState>(); var list=tag.getList("Sample",Tag.TAG_COMPOUND);
                for (int i=0;i<list.size();i++) states.add(NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),list.getCompound(i)));
                request.sample=new EcologySampler.Sample(source,BlockPos.of(tag.getLong("Surface")),request.biomeId,request.diameter,states);
            }
        }
        return request;
    }
}
