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
            Biome.CODEC.fieldOf("plain_biome").forGetter(s->s.plain),
            Biome.CODEC.fieldOf("corridor_biome").forGetter(s->s.corridor),
            Biome.CODEC.optionalFieldOf("forest_biome").forGetter(s->java.util.Optional.of(s.forest)),
            Biome.CODEC.optionalFieldOf("mire_biome").forGetter(s->java.util.Optional.of(s.mire)),
            Biome.CODEC.optionalFieldOf("cliffs_biome").forGetter(s->java.util.Optional.of(s.cliffs)),
            Biome.CODEC.optionalFieldOf("garden_biome").forGetter(s->java.util.Optional.of(s.garden)),
            Biome.CODEC.optionalFieldOf("ruins_biome").forGetter(s->java.util.Optional.of(s.ruins)),
            Biome.CODEC.optionalFieldOf("folded_biome").forGetter(s->java.util.Optional.of(s.folded)),
            Biome.CODEC.optionalFieldOf("starsea_biome").forGetter(s->java.util.Optional.of(s.starsea)),
            Biome.CODEC.optionalFieldOf("geodes_biome").forGetter(s->java.util.Optional.of(s.geodes)),
            Biome.CODEC.optionalFieldOf("abyss_biome").forGetter(s->java.util.Optional.of(s.abyss)),
            LOWER.forGetter(s->new LowerBiomes(s.starsea,s.geodes,s.abyss,s.labyrinth,s.phase)))
            .apply(i,(time,plain,corridor,forest,mire,cliffs,garden,ruins,folded,starsea,geodes,abyss,lower)->{
                var source=new StrataBiomeSource(time,plain,corridor,forest,mire,cliffs,garden,ruins,folded,starsea.or(()->java.util.Optional.of(lower.sea())));
                source.geodes=geodes.orElse(lower.geodes());source.abyss=abyss.orElse(lower.abyss());source.labyrinth=lower.labyrinth();source.phase=lower.phase();return source;
            }));
    private Holder<Biome> geodes;
    private Holder<Biome> abyss;
    private Holder<Biome> labyrinth;
    private Holder<Biome> phase;
    private final Holder<Biome> time,plain,corridor,forest,mire,cliffs,garden,ruins,folded,starsea;
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor){this(time,plain,corridor,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest){this(time,plain,corridor,forest,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire){this(time,plain,corridor,forest,mire,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire,java.util.Optional<Holder<Biome>> cliffs){this(time,plain,corridor,forest,mire,cliffs,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire,java.util.Optional<Holder<Biome>> cliffs,java.util.Optional<Holder<Biome>> garden){this(time,plain,corridor,forest,mire,cliffs,garden,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire,java.util.Optional<Holder<Biome>> cliffs,java.util.Optional<Holder<Biome>> garden,java.util.Optional<Holder<Biome>> ruins){this(time,plain,corridor,forest,mire,cliffs,garden,ruins,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire,java.util.Optional<Holder<Biome>> cliffs,java.util.Optional<Holder<Biome>> garden,java.util.Optional<Holder<Biome>> ruins,java.util.Optional<Holder<Biome>> folded){this(time,plain,corridor,forest,mire,cliffs,garden,ruins,folded,java.util.Optional.empty());}
    public StrataBiomeSource(Holder<Biome> time,Holder<Biome> plain,Holder<Biome> corridor,java.util.Optional<Holder<Biome>> forest,java.util.Optional<Holder<Biome>> mire,java.util.Optional<Holder<Biome>> cliffs,java.util.Optional<Holder<Biome>> garden,java.util.Optional<Holder<Biome>> ruins,java.util.Optional<Holder<Biome>> folded,java.util.Optional<Holder<Biome>> starsea){this.time=time;this.plain=plain;this.corridor=corridor;this.forest=forest.orElse(plain);this.mire=mire.orElse(plain);this.cliffs=cliffs.orElse(plain);this.garden=garden.orElse(plain);this.ruins=ruins.orElse(plain);this.folded=folded.orElse(corridor);this.starsea=starsea.orElse(corridor);this.geodes=corridor;}
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
    @Override protected Stream<Holder<Biome>> collectPossibleBiomes(){return Stream.of(time,plain,corridor,forest,mire,cliffs,garden,ruins,folded,starsea,geodes,abyss==null?corridor:abyss,labyrinth==null?corridor:labyrinth,phase==null?corridor:phase).distinct();}
    @Override public Holder<Biome> getNoiseBiome(int x,int y,int z,Climate.Sampler sampler){
        if(y*4<=CorridorLayout.SPACE_CEILING&&phase!=null&&com.mcmagic.omnira.space.phase.PhaseTerrain.biome(x*4,z*4))return phase;
        if(y*4<=CorridorLayout.SPACE_CEILING&&labyrinth!=null&&com.mcmagic.omnira.space.labyrinth.LabyrinthTerrain.biome(x*4,z*4))return labyrinth;
        if(y*4<=CorridorLayout.SPACE_CEILING&&abyss!=null&&com.mcmagic.omnira.space.abyss.AbyssTerrain.biome(x*4,z*4))return abyss;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.geodes.DislocatedTerrain.biome(x*4,z*4))return geodes;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.sea.VoidSeaTerrain.biome(x*4,z*4))return starsea;
        if(y*4<=CorridorLayout.SPACE_CEILING&&com.mcmagic.omnira.space.FoldedTerrain.biome(x*4,z*4))return folded;
        return y*4>=CorridorLayout.TIME_BASE-2?(frozenTerra(x*4,z*4)?time:fleetingWoods(x*4,z*4)?forest:reversionMire(x*4,z*4)?mire:epochalCliffs(x*4,z*4)?cliffs:recurrenceGarden(x*4,z*4)?garden:eventideRuins(x*4,z*4)?ruins:plain):corridor;
    }
}
