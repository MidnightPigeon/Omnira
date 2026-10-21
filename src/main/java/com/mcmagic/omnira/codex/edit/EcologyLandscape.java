package com.mcmagic.omnira.codex.edit;

import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

/** Detached, bounded landscape construction. Never executes features or requests world chunks. */
public final class EcologyLandscape {
    private record Decoration(String kind, BlockState block, BlockState crown) {
        static final Codec<Decoration> CODEC=RecordCodecBuilder.create(i->i.group(
                Codec.STRING.fieldOf("kind").forGetter(Decoration::kind),
                BlockState.CODEC.fieldOf("block").forGetter(Decoration::block),
                BlockState.CODEC.optionalFieldOf("crown",Blocks.AIR.defaultBlockState()).forGetter(Decoration::crown)
        ).apply(i,Decoration::new));
    }
    private record Profile(BlockState base, BlockState surface, List<Decoration> decorations) {
        static final Codec<Profile> CODEC=RecordCodecBuilder.create(i->i.group(
                BlockState.CODEC.fieldOf("base").forGetter(Profile::base),
                BlockState.CODEC.fieldOf("surface").forGetter(Profile::surface),
                Decoration.CODEC.listOf().optionalFieldOf("decorations",List.of()).forGetter(Profile::decorations)
        ).apply(i,Profile::new));
    }
    private EcologyLandscape() {}

    public static EcologySampler.Sample build(ServerLevel level, int diameter, Holder<Biome> biome, long seed) {
        if (!EditGeometry.validDiameter(diameter)) throw new IllegalArgumentException("invalid_diameter");
        var id=biome.unwrapKey().orElseThrow().location();
        var random=RandomSource.create(seed);
        var profile=profile(level,biome,random);
        if (safe(profile.base).isAir() || safe(profile.surface).isAir() || profile.decorations.size()>8)
            throw new IllegalArgumentException("invalid_ecology_profile:"+id);
        int radius=(diameter-1)/2;
        var states=new ArrayList<BlockState>(Collections.nCopies(diameter*diameter*diameter,Blocks.AIR.defaultBlockState()));
        boolean liquid=!profile.surface.getFluidState().isEmpty();
        for (int i=0;i<states.size();i++) {
            int y=EditGeometry.offset(i,diameter).getY();
            // Liquids fill the shallow basin, not just a film on a grass/dirt template.
            if (y<=0) states.set(i,safe(y==0 || liquid && y==-1 ? profile.surface : profile.base));
        }
        int reach=Math.max(2,radius-2), ground=-radius+diameter/3;
        var occupied=new HashSet<BlockPos>();
        int count=Math.min(5,Math.max(2,diameter/3));
        for (int n=0;n<count && !profile.decorations.isEmpty();n++) {
            // Fixed attempts and feature count keep uncommon biomes as cheap as ordinary plains.
            for (int attempt=0;attempt<16;attempt++) {
                int x=random.nextInt(2*reach+1)-reach, z=random.nextInt(2*reach+1)-reach;
                if (x*x+z*z<4 || x*x+z*z>reach*reach || occupied.stream().anyMatch(p->p.distSqr(new BlockPos(x,0,z))<5)) continue;
                occupied.add(new BlockPos(x,0,z));
                var deco=profile.decorations.get(random.nextInt(profile.decorations.size()));
                decorate(states,diameter,ground,x,z,deco,liquid,random);
                break;
            }
        }
        // The planner places the unique treasure here, with no plants covering or occupying it.
        put(states,diameter,ground,0,1,0,Blocks.AIR.defaultBlockState());
        return new EcologySampler.Sample(level,BlockPos.ZERO,id,diameter,states);
    }

    private static Profile profile(ServerLevel level, Holder<Biome> biome, RandomSource random) {
        var id=biome.unwrapKey().orElseThrow().location();
        var resource=level.getServer().getResourceManager().getResource(ResourceLocation.fromNamespaceAndPath(
                "omnira","codex/ecology/"+id.getNamespace()+"/"+id.getPath()+".json"));
        if (resource.isPresent()) try (var reader=resource.get().openAsReader()) {
            return Profile.CODEC.parse(JsonOps.INSTANCE,JsonParser.parseReader(reader)).getOrThrow();
        } catch (java.io.IOException | RuntimeException e) {
            // Bad pack data is visible, not an unbounded retry or a lost, already consumed input.
            throw new IllegalArgumentException("invalid_ecology_profile:"+id,e);
        }
        var fallback=EcologySampler.palette(level,7,biome);
        BlockState base=fallback.at(0,-1,0), top=fallback.at(0,0,0);
        var decorations=new ArrayList<Decoration>();
        if (top.getFluidState().isEmpty()) {
            // Read providers, never invoke configured feature placement in the live world.
            var features=biome.value().getGenerationSettings().features().stream().flatMap(HolderSet::stream)
                    .limit(128).flatMap(p->p.value().getFeatures()).limit(256).iterator();
            while (features.hasNext() && decorations.size()<8) {
                if (features.next().config() instanceof TreeConfiguration tree) {
                    var trunk=safe(tree.trunkProvider.getState(random,BlockPos.ZERO));
                    var leaves=safe(tree.foliageProvider.getState(random,BlockPos.ZERO));
                    if (!trunk.isAir() && !leaves.isAir()) decorations.add(new Decoration("tree",trunk,leaves));
                }
            }
        }
        if (decorations.isEmpty()) {
            if (biome.is(BiomeTags.IS_END)) decorations.add(deco("spire",Blocks.OBSIDIAN,Blocks.END_STONE));
            else if (biome.is(BiomeTags.IS_NETHER)) decorations.add(deco("spire",Blocks.BASALT,Blocks.GLOWSTONE));
            else if (biome.is(BiomeTags.IS_OCEAN)) decorations.add(deco("patch",Blocks.PRISMARINE,Blocks.PRISMARINE));
            else if (biome.value().getBaseTemperature()<=0.15F) decorations.add(deco("spire",Blocks.PACKED_ICE,Blocks.BLUE_ICE));
            else if (biome.value().getBaseTemperature()>=1) decorations.add(deco("column",Blocks.CACTUS,Blocks.CACTUS));
            else decorations.add(deco("patch",Blocks.SHORT_GRASS,Blocks.POPPY));
        }
        return new Profile(base,top,decorations);
    }

    private static Decoration deco(String kind, Block block, Block crown) {
        return new Decoration(kind,block.defaultBlockState(),crown.defaultBlockState());
    }
    private static BlockState safe(BlockState state) {
        if (state.is(com.mcmagic.omnira.registry.ModBlocks.PURE_SOLIDIFIED_SPACETIME.get()))
            return com.mcmagic.omnira.registry.DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState();
        if (state.hasBlockEntity() || CodexEditEngine.forbidden(state)) return Blocks.AIR.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.PERSISTENT)) state=state.setValue(BlockStateProperties.PERSISTENT,true);
        return state;
    }
    private static void decorate(List<BlockState> states,int diameter,int ground,int x,int z,
                                 Decoration d,boolean liquid,RandomSource random) {
        if (liquid) {
            // Supported seabed accents stay below the liquid surface; custom liquids keep their own palette.
            put(states,diameter,ground,x,-1,z,d.block);
            return;
        }
        int height=diameter==7?2:3+random.nextInt(2);
        switch (d.kind) {
            case "tree" -> {
                for (int y=1;y<height;y++) put(states,diameter,ground,x,y,z,d.block);
                for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++)
                    if (dx*dx+dz*dz<=1) put(states,diameter,ground,x+dx,height-1,z+dz,d.crown);
                put(states,diameter,ground,x,height,z,d.crown);
                for (int y=1;y<height;y++) put(states,diameter,ground,x,y,z,d.block);
            }
            case "spire" -> {
                for (int dx=-1;dx<=1;dx++) for (int dz=-1;dz<=1;dz++)
                    if (dx*dx+dz*dz<=1) put(states,diameter,ground,x+dx,1,z+dz,d.block);
                for (int y=2;y<height;y++) put(states,diameter,ground,x,y,z,d.block);
                put(states,diameter,ground,x,height,z,d.crown.isAir()?d.block:d.crown);
            }
            case "column" -> {
                for (int y=1;y<=height;y++) put(states,diameter,ground,x,y,z,d.block);
                if (!d.crown.isAir()) put(states,diameter,ground,x,height,z,d.crown);
            }
            case "patch" -> {
                put(states,diameter,ground,x,1,z,d.block);
                put(states,diameter,ground,x+1,1,z,d.crown.isAir()?d.block:d.crown);
            }
            default -> throw new IllegalArgumentException("invalid_ecology_decoration:"+d.kind);
        }
    }
    private static void put(List<BlockState> states,int diameter,int ground,int x,int y,int z,BlockState state) {
        int r=(diameter-1)/2, worldY=y+ground;
        if (Math.abs(x)>r || Math.abs(y)>r || Math.abs(z)>r ||
                x*x+worldY*worldY+z*z>r*r || EditGeometry.ecologyShell(r,x,worldY,z)) return;
        // Leave the central treasure's vertical column clear of overhanging decoration.
        if (x==0 && z==0 && y>=1 && !state.isAir()) return;
        states.set((y+r)*diameter*diameter+(z+r)*diameter+x+r,safe(state));
    }
}
