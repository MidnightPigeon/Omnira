package com.mcmagic.omnira.codex.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Detached state volume, climate fallback palette and loaded-only snapshot fixtures. */
public final class EcologySampler {
    public record Sample(ServerLevel level, BlockPos surface, ResourceLocation biomeId, int diameter, List<BlockState> states) {
        public Sample {
            if (!EditGeometry.validDiameter(diameter) || states.size()!=diameter*diameter*diameter)
                throw new IllegalArgumentException("invalid_sample");
            surface = surface.immutable(); states = List.copyOf(states);
        }
        public BlockState at(int x, int y, int z) {
            int r = (diameter-1)/2;
            if (Math.abs(x)>r || Math.abs(y)>r || Math.abs(z)>r) return Blocks.AIR.defaultBlockState();
            return states.get((y+r)*diameter*diameter + (z+r)*diameter + x+r);
        }
    }
    private EcologySampler() {}

    /** Climate/tag baseline for biomes without an explicit landscape profile. */
    public static Sample palette(ServerLevel level, int diameter, Holder<Biome> biome) {
        boolean ocean=biome.is(net.minecraft.tags.BiomeTags.IS_OCEAN);
        float temperature=biome.value().getBaseTemperature();
        BlockState stone=Blocks.STONE.defaultBlockState(), soil=Blocks.DIRT.defaultBlockState();
        BlockState top=Blocks.GRASS_BLOCK.defaultBlockState();
        if (biome.is(net.minecraft.tags.BiomeTags.IS_NETHER)) { stone=Blocks.NETHERRACK.defaultBlockState(); soil=stone; top=stone; }
        else if (biome.is(net.minecraft.tags.BiomeTags.IS_END)) { stone=Blocks.END_STONE.defaultBlockState(); soil=stone; top=stone; }
        else if (ocean) { soil=Blocks.SAND.defaultBlockState(); top=Blocks.WATER.defaultBlockState(); }
        else if (temperature<=0.15F) top=Blocks.SNOW_BLOCK.defaultBlockState();
        else if (temperature>=1.0F) { stone=Blocks.SANDSTONE.defaultBlockState(); soil=Blocks.SAND.defaultBlockState(); top=soil; }
        // Top-layer override: omnira:codex/ecology/palette/<registered block namespace>/<path>.
        var override=biome.tags().map(net.minecraft.tags.TagKey::location)
                .filter(id->id.getNamespace().equals("omnira") && id.getPath().startsWith("codex/ecology/palette/"))
                .sorted().findFirst();
        if (override.isPresent()) {
            String rest=override.get().getPath().substring("codex/ecology/palette/".length()); int slash=rest.indexOf('/');
            if (slash>0) {
                var id=ResourceLocation.tryParse(rest.substring(0,slash)+":"+rest.substring(slash+1));
                if (id!=null && net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(id)) {
                    var candidate=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(id).defaultBlockState();
                    if (!candidate.isAir() && !candidate.hasBlockEntity() && !CodexEditEngine.forbidden(candidate)) top=candidate;
                }
            }
        }
        var states=new ArrayList<BlockState>();
        for (int i=0;i<diameter*diameter*diameter;i++) {
            int y=EditGeometry.offset(i,diameter).getY();
            states.add(y<-1?stone:y==-1?soil:y==0?top:Blocks.AIR.defaultBlockState());
        }
        return new Sample(level,BlockPos.ZERO,biome.unwrapKey().orElseThrow().location(),diameter,states);
    }

    public static Optional<Sample> capture(ServerLevel level, BlockPos surface, int diameter, Holder<Biome> biome) {
        int r = (diameter-1)/2;
        if (!EditGeometry.validDiameter(diameter) || !loaded(level,surface,r)) return Optional.empty();
        if (!level.getBiome(surface).is(biome.unwrapKey().orElseThrow())) return Optional.empty();
        var states = new ArrayList<BlockState>();
        for (int i=0;i<diameter*diameter*diameter;i++) {
            var p = surface.offset(EditGeometry.offset(i,diameter));
            var state = level.getBlockState(p);
            states.add(state.hasBlockEntity() || CodexEditEngine.forbidden(state)
                    ? Blocks.AIR.defaultBlockState() : state);
        }
        return Optional.of(new Sample(level,surface,biome.unwrapKey().orElseThrow().location(),diameter,states));
    }

    static boolean loaded(ServerLevel level, BlockPos center, int radius) {
        if (center.getY()-radius < level.getMinBuildHeight() || center.getY()+radius >= level.getMaxBuildHeight()) return false;
        for (int x=(center.getX()-radius)>>4;x<=((center.getX()+radius)>>4);x++)
            for (int z=(center.getZ()-radius)>>4;z<=((center.getZ()+radius)>>4);z++)
                if (level.getChunkSource().getChunkNow(x,z)==null) return false;
        return true;
    }
}
