package com.mcmagic.omnira.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.*;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.Optional;

public final class RuinsStructure extends Structure {
    public static final MapCodec<RuinsStructure> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            settingsCodec(i),Codec.intRange(0,2).fieldOf("variant").forGetter(s->s.variant)).apply(i,RuinsStructure::new));
    private static final DeferredRegister<StructureType<?>> TYPES=DeferredRegister.create(Registries.STRUCTURE_TYPE,"omnira");
    private static final DeferredRegister<StructurePieceType> PIECES=DeferredRegister.create(Registries.STRUCTURE_PIECE,"omnira");
    public static final java.util.function.Supplier<StructureType<RuinsStructure>> TYPE=TYPES.register("overworld_ruins",()->()->CODEC);
    public static final java.util.function.Supplier<StructurePieceType> PIECE=PIECES.register("overworld_ruins",()->RuinsPiece::new);
    public static final java.util.function.Supplier<StructureType<RadiantCourtyardStructure>> COURTYARD_TYPE=
            TYPES.register("radiant_courtyard",()->()->RadiantCourtyardStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> COURTYARD_PIECE=
            PIECES.register("radiant_courtyard",()->RadiantCourtyardPiece::new);
    private final int variant;
    public static final java.util.function.Supplier<StructureType<com.mcmagic.omnira.archaeology.DrillingPlatformStructure>> DRILL_PLATFORM_TYPE=
            TYPES.register("hyperdimensional_drilling_platform",()->()->com.mcmagic.omnira.archaeology.DrillingPlatformStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> DRILL_PLATFORM_PIECE=
            PIECES.register("hyperdimensional_drilling_platform",()->com.mcmagic.omnira.archaeology.DrillingPlatformPiece::new);
    public static final java.util.function.Supplier<StructureType<com.mcmagic.omnira.mire.MillStructure>> MILL_TYPE=
            TYPES.register("rusty_lake_mill",()->()->com.mcmagic.omnira.mire.MillStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> MILL_PIECE=
            PIECES.register("rusty_lake_mill",()->com.mcmagic.omnira.mire.MillPiece::new);
    public static final java.util.function.Supplier<StructureType<com.mcmagic.omnira.shop.KirisameShopStructure>> KIRISAME_TYPE=
            TYPES.register("kirisame_magic_shop",()->()->com.mcmagic.omnira.shop.KirisameShopStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> KIRISAME_PIECE=
            PIECES.register("kirisame_magic_shop",()->com.mcmagic.omnira.shop.KirisameShopPiece::new);
    public static final java.util.function.Supplier<StructureType<com.mcmagic.omnira.time.LookingGlassGardenStructure>> GARDEN_TYPE=
            TYPES.register("looking_glass_garden",()->()->com.mcmagic.omnira.time.LookingGlassGardenStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> GARDEN_PIECE=
            PIECES.register("looking_glass_garden",()->com.mcmagic.omnira.time.LookingGlassGardenPiece::new);
    public static final java.util.function.Supplier<StructureType<com.mcmagic.omnira.time.DoomsdayRiteStructure>> DOOMSDAY_TYPE=
            TYPES.register("doomsday_rite",()->()->com.mcmagic.omnira.time.DoomsdayRiteStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> DOOMSDAY_PIECE=
            PIECES.register("doomsday_rite",()->com.mcmagic.omnira.time.DoomsdayRitePiece::new);
    public static final java.util.function.Supplier<StructureType<MirrorGalleryStructure>> GALLERY_TYPE=
            TYPES.register("mirror_gallery",()->()->MirrorGalleryStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> GALLERY_PIECE=
            PIECES.register("mirror_gallery",()->MirrorGalleryPiece::new);
    public static final java.util.function.Supplier<StructureType<ShadowLibraryStructure>> LIBRARY_TYPE=
            TYPES.register("shadow_library",()->()->ShadowLibraryStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> LIBRARY_PIECE=
            PIECES.register("shadow_library",()->ShadowLibraryPiece::new);
    public static final java.util.function.Supplier<StructureType<FrozenTerraPalaceStructure>> TERRA_PALACE_TYPE=
            TYPES.register("frozen_terra_palace",()->()->FrozenTerraPalaceStructure.CODEC);
    public static final java.util.function.Supplier<StructurePieceType> TERRA_PALACE_PIECE=
            PIECES.register("frozen_terra_palace",()->FrozenTerraPalacePiece::new);
    public RuinsStructure(StructureSettings settings,int variant) {super(settings);this.variant=variant;}
    public static void register(IEventBus bus) {TYPES.register(bus);PIECES.register(bus);}
    @Override public StructureType<?> type() {return TYPE.get();}
    @Override public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x=context.chunkPos().getMinBlockX(),z=context.chunkPos().getMinBlockZ();
        int low=Integer.MAX_VALUE,high=Integer.MIN_VALUE;
        int extent=Math.max(RuinsPiece.width(variant),RuinsPiece.depth(variant))-1;
        for(int dx:new int[]{0,extent/2,extent}) for(int dz:new int[]{0,extent/2,extent}) {
            int surface=context.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.WORLD_SURFACE_WG,context.heightAccessor(),context.randomState());
            int floor=context.chunkGenerator().getBaseHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,context.heightAccessor(),context.randomState());
            if(surface>floor || floor<context.chunkGenerator().getSeaLevel()) return Optional.empty();
            low=Math.min(low,surface);high=Math.max(high,surface);
        }
        if(high-low>4 || high+RuinsPiece.height(variant)>=context.heightAccessor().getMaxBuildHeight()) return Optional.empty();
        int y=generationOriginY(variant,high);
        Direction facing=Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
        long seed=context.random().nextLong();
        int anchorX=x+(facing==Direction.WEST?RuinsPiece.depth(variant)-1:0);
        int anchorZ=z+(facing==Direction.NORTH?RuinsPiece.depth(variant)-1:0);
        return Optional.of(new GenerationStub(new BlockPos(x+6,y,z+6),builder->builder.addPiece(new RuinsPiece(variant,anchorX,y,anchorZ,facing,seed))));
    }
    public static int generationOriginY(int variant,int surfaceHeight){
        return surfaceHeight-(variant==0?2:variant==1?1:0);
    }
}
