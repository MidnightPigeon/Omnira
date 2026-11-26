package com.mcmagic.omnira.spacetime;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/** Keeps the upper continent distinct from the uninhabited mirror corridor. */
public final class StrataBiomeSource extends BiomeSource {
    private record LowerBiomes(Holder<Biome> sea,Holder<Biome> geodes,Holder<Biome> abyss,Holder<Biome> labyrinth,Holder<Biome> phase){}
    private static final MapCodec<LowerBiomes> LOWER=RecordCodecBuilder.mapCodec(i->i.group(
            net.minecraft.resources.RegistryOps.retrieveElement(com.mcmagic.omnira.space.sea.VoidSeaContent.BIOME),
            net.minecraft.resources.RegistryOps.retrieveElement(com.mcmagic.omnira.space.geodes.DeflectionContent.BIOME),
            net.minecraft.resources.RegistryOps.retrieveElement(com.mcmagic.omnira.space.abyss.AbyssContent.BIOME),
            net.minecraft.resources.RegistryOps.retrieveElement(com.mcmagic.omnira.space.labyrinth.LabyrinthContent.BIOME),
            net.minecraft.resources.RegistryOps.retrieveElement(com.mcmagic.omnira.space.phase.PhaseContent.BIOME)).apply(i,LowerBiomes::new));
    public static final int CELL_SIZE=TimeBiomeLayout.SCALE;
    public static TimeBiomeLayout.RegionSample sample(int x,int z){return TimeBiomeLayout.sample(x,z);}
    public static final MapCodec<StrataBiomeSource> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            Biome.CODEC.fieldOf("time_biome").forGetter(s->s.time),
            Biome.CODEC.fieldOf("corridor_biome").forGetter(s->s.corridor),
            timeRegion("forest_biome","fleeting_woods").forGetter(s->s.forest),
            timeRegion("mire_biome","reversion_mire").forGetter(s->s.mire),
            timeRegion("cliffs_biome","epochal_cliffs").forGetter(s->s.cliffs),
            timeRegion("garden_biome","recurrence_garden").forGetter(s->s.garden),
            timeRegion("ruins_biome","eventide_ruins").forGetter(s->s.ruins),
            Biome.CODEC.optionalFieldOf("folded_biome").forGetter(s->java.util.Optional.of(s.folded)),
            Biome.CODEC.optionalFieldOf("starsea_biome").forGetter(s->java.util.Optional.of(s.starsea)),
            Biome.CODEC.optionalFieldOf("geodes_biome").forGetter(s->java.util.Optional.of(s.geodes)),
            Biome.CODEC.optionalFieldOf("abyss_biome").forGetter(s->java.util.Optional.of(s.abyss)),
            LOWER.forGetter(s->new LowerBiomes(s.starsea,s.geodes,s.abyss,s.labyrinth,s.phase)))
            .apply(i,(time,corridor,forest,mire,cliffs,garden,ruins,folded,starsea,geodes,abyss,lower)->{
                var source=new StrataBiomeSource(time,corridor,forest,mire,cliffs,garden,ruins,folded,starsea.or(()->java.util.Optional.of(lower.sea())));
                source.geodes=geodes.orElse(lower.geodes());source.abyss=abyss.orElse(lower.abyss());source.labyrinth=lower.labyrinth();source.phase=lower.phase();return source;
            }));
    private Holder<Biome> geodes;
    private Holder<Biome> abyss;
    private Holder<Biome> labyrinth;
    private Holder<Biome> phase;
    private final Holder<Biome> time,corridor,forest,mire,cliffs,garden,ruins,folded,starsea;
    private static MapCodec<Holder<Biome>> timeRegion(String field,String id){
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira",id));
        return RecordCodecBuilder.mapCodec(i->i.group(
                Biome.CODEC.optionalFieldOf(field).forGetter(java.util.Optional::of),
                net.minecraft.resources.RegistryOps.retrieveElement(key)
        ).apply(i,(configured,current)->configured.orElse(current)));
    }
    private StrataBiomeSource(Holder<Biome> time,Holder<Biome> corridor,Holder<Biome> forest,Holder<Biome> mire,
                              Holder<Biome> cliffs,Holder<Biome> garden,Holder<Biome> ruins,
                              java.util.Optional<Holder<Biome>> folded,java.util.Optional<Holder<Biome>> starsea){
        this.time=time;this.corridor=corridor;this.forest=forest;this.mire=mire;this.cliffs=cliffs;this.garden=garden;this.ruins=ruins;
        this.folded=folded.orElse(corridor);this.starsea=starsea.orElse(corridor);this.geodes=corridor;
    }
    public static boolean frozenTerra(int x,int z){
        return region(x,z)==0;
    }
    public static boolean fleetingWoods(int x,int z){return region(x,z)==1;}
    public static boolean reversionMire(int x,int z){return region(x,z)==2;}
    public static boolean epochalCliffs(int x,int z){return region(x,z)==3;}
    public static boolean recurrenceGarden(int x,int z){return region(x,z)==4;}
    public static boolean eventideRuins(int x,int z){return region(x,z)==5;}
    public static int region(int x,int z){
        return sample(x,z).region();
    }
    @Override protected MapCodec<? extends BiomeSource> codec(){return CODEC;}
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.of(time,corridor,forest,mire,cliffs,garden,ruins,folded,starsea,geodes,abyss==null?corridor:abyss,labyrinth==null?corridor:labyrinth,phase==null?corridor:phase).distinct();}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        if(y*4<=CorridorLayout.SPACE_CEILING&&phase!=null&&com.mcmagic.omnira.space.phase.PhaseTerrain.biome(x*4,z*4))return phase;
        if(y*4<=CorridorLayout.SPACE_CEILING&&labyrinth!=null&&com.mcmagic.omnira.space.labyrinth.LabyrinthTerrain.biome(x*4,z*4))return labyrinth;
        if(y*4<=CorridorLayout.SPACE_CEILING&&abyss!=null&&com.mcmagic.omnira.space.abyss.AbyssTerrain.biome(x*4,z*4))return abyss;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.geodes.DislocatedTerrain.biome(x*4,z*4))return geodes;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.sea.VoidSeaTerrain.biome(x*4,z*4))return starsea;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.FoldedTerrain.biome(x*4,z*4))return folded;
        if(y*4<CorridorLayout.TIME_BASE-2)return corridor;
        return switch(region(x*4,z*4)){
            case 0 -> time;
            case 1 -> forest;
            case 2 -> mire;
            case 3 -> cliffs;
            case 4 -> garden;
            case 5 -> ruins;
            default -> throw new IllegalStateException("Invalid time region");
        };
    }
}
