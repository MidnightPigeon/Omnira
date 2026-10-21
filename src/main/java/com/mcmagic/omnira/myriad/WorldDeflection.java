package com.mcmagic.omnira.myriad;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Samples real dimension terrain, budgeting biome search and deferring chunk generation. */
@EventBusSubscriber(modid="omnira")
public final class WorldDeflection {
    private record Source(ServerLevel level,Holder<Biome> biome) {}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final class Pending {
        final ServerPlayer player;final ServerLevel target;final BlockPos min;
        final InteractionHand hand;final Source source;
        int attempts,age,samples;boolean generating;
        Pending(ServerPlayer player,BlockPos min,InteractionHand hand,Source source) {
            this.player=player;this.target=player.serverLevel();this.min=min;this.hand=hand;this.source=source;
        }
    }
    public static BlockPos origin(BlockPos clicked) {
        return new BlockPos(Math.floorDiv(clicked.getX(),4)*4,Math.floorDiv(clicked.getY(),4)*4,Math.floorDiv(clicked.getZ(),4)*4);
    }
    public static BlockPos placementOrigin(BlockPos clicked) { return origin(clicked).above(2); }
    public static boolean request(ServerPlayer player,BlockPos clicked,InteractionHand hand) {
        var min=placementOrigin(clicked);var stack=player.getItemInHand(hand);
        if(PENDING.containsKey(player.getUUID())||PENDING.size()>=4||!allowed(player.serverLevel(),min,player,stack))return false;
        var options=new LinkedHashMap<Holder<Biome>,List<ServerLevel>>();
        for(var level:player.server.getAllLevels())for(var biome:level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes())
            options.computeIfAbsent(biome,k->new ArrayList<>()).add(level);
        if(options.isEmpty())return false;
        var biomes=new ArrayList<>(options.keySet());var biome=biomes.get(player.getRandom().nextInt(biomes.size()));
        var levels=options.get(biome);var source=new Source(levels.get(player.getRandom().nextInt(levels.size())),biome);
        PENDING.put(player.getUUID(),new Pending(player,min,hand,source));
        player.displayClientMessage(Component.translatable("message.omnira.world_seed.searching"),true);return true;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        for(var pending:List.copyOf(PENDING.values())) {
            if(pending.target.getServer()!=event.getServer())continue;
            if(++pending.age>600||!valid(pending)){finish(pending,false);continue;}
            if(pending.generating)continue;
            var source=pending.source;var level=source.level();var cache=level.getChunkSource();
            for(int i=0;i<32;i++) {
                if(++pending.attempts>2048){finish(pending,false);break;}
                int x=(pending.min.getX()>>4)+level.random.nextInt(1025)-512;
                int z=(pending.min.getZ()>>4)+level.random.nextInt(1025)-512;
                int y=level.getMinBuildHeight()+level.random.nextInt(level.getHeight());
                var at=new BlockPos(x*16+8,y,z*16+8);
                if(!level.getWorldBorder().isWithinBounds(at)||!cache.getGenerator().getBiomeSource()
                        .getNoiseBiome(at.getX()>>2,y>>2,at.getZ()>>2,cache.randomState().sampler()).equals(source.biome()))continue;
                pending.generating=true;
                cache.getChunkFuture(x,z,ChunkStatus.FULL,true).whenComplete((result,error)->event.getServer().execute(()->{
                    if(PENDING.get(pending.player.getUUID())!=pending)return;
                    if(error!=null||result==null||result.orElse(null)==null||!valid(pending)){finish(pending,false);return;}
                    var snapshot=sample(level,at,source.biome());
                    if(snapshot==null){
                        if(++pending.samples>=3)finish(pending,false);else pending.generating=false;
                        return;
                    }
                    finish(pending,apply(pending.target,pending.min,source.biome(),snapshot,pending.player,pending.player.getItemInHand(pending.hand)));
                }));
                break;
            }
        }
    }
    private static boolean valid(Pending p) {
        return p.target.getServer().getPlayerList().getPlayer(p.player.getUUID())==p.player&&p.player.isAlive()
                &&p.player.serverLevel()==p.target&&p.player.distanceToSqr(p.min.getX()+2,p.min.getY()+2,p.min.getZ()+2)<144
                &&p.player.getItemInHand(p.hand).is(WorldSeedContent.ACTIVE.get());
    }
    public static BlockState[] sample(ServerLevel level,BlockPos at,Holder<Biome> biome) {
        // Find the nearest walkable/fluid surface in this biome, not the Nether roof or high sky.
        for(int distance=0;distance<level.getHeight();distance++)for(int direction:new int[]{-1,1}) {
            int y=at.getY()+distance*direction;
            if(y<level.getMinBuildHeight()+1||y>=level.getMaxBuildHeight()-2)continue;
            var floor=new BlockPos(at.getX(),y,at.getZ());var state=level.getBlockState(floor);
            if(state.isAir()||state.getDestroySpeed(level,floor)<0||!level.getBlockState(floor.above()).isAir()
                    ||!level.getBiome(floor.above()).equals(biome))continue;
            var min=floor.offset(-2,-1,-2);var snapshot=new BlockState[64];
            for(int i=0;i<64;i++)snapshot[i]=level.getBlockState(min.offset(i%4,i/16,(i/4)%4));
            return snapshot;
        }
        return null;
    }
    private static boolean allowed(ServerLevel level,BlockPos min,Player player,ItemStack stack) {
        if(player.isSpectator()||stack.isEmpty()||!stack.is(WorldSeedContent.ACTIVE.get()))return false;
        for(int i=0;i<64;i++) {
            var pos=min.offset(i%4,i/16,(i/4)%4);
            if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.mayInteract(player,pos)||!player.mayUseItemAt(pos,Direction.UP,stack))return false;
        }
        return true;
    }
    public static boolean apply(ServerLevel level,BlockPos min,Holder<Biome> biome,BlockState[] snapshot,Player player,ItemStack stack) {
        if(snapshot.length!=64||!min.equals(placementOrigin(min.below(2)))||!allowed(level,min,player,stack))return false;
        for(var state:snapshot)if(state==null)return false;
        for(int i=0;i<64;i++) {
            var pos=min.offset(i%4,i/16,(i/4)%4);
            if(level.getBlockState(pos).getDestroySpeed(level,pos)>=0) {
                if(level.getBlockEntity(pos)!=null) {
                    level.removeBlockEntity(pos);
                    level.setBlock(pos,net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
                }
                level.setBlock(pos,snapshot[i],2);
            }
        }
        var chunk=level.getChunkAt(min);int qx=min.getX()>>2,qy=min.getY()>>2,qz=min.getZ()>>2,lastY=(min.getY()+3)>>2;
        // The two-block lift intersects two vertical biome cells, sometimes across a section edge.
        for(int sectionY=min.getY()>>4;sectionY<=(min.getY()+3)>>4;sectionY++) {
            var section=chunk.getSections()[level.getSectionIndex(sectionY*16)];var old=new ArrayList<Holder<Biome>>(64);
            for(int i=0;i<64;i++)old.add(section.getNoiseBiome(i%4,i/16,(i/4)%4));
            section.fillBiomesFromNoise((x,y,z,s)->
                    x==qx&&y>=qy&&y<=lastY&&z==qz?biome:old.get(Math.floorMod(y,4)*16+Math.floorMod(z,4)*4+Math.floorMod(x,4)),
                    level.getChunkSource().randomState().sampler(),qx&~3,sectionY*4,qz&~3);
        }
        chunk.setUnsaved(true);level.getChunkSource().chunkMap.resendBiomesForChunks(List.of(chunk));
        stack.shrink(1);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,min.getX()+2,min.getY()+2,min.getZ()+2,16,1.5,1.5,1.5,.02);
        return true;
    }
    private static void finish(Pending pending,boolean success) {
        PENDING.remove(pending.player.getUUID(),pending);
        pending.player.displayClientMessage(Component.translatable(success?"message.omnira.world_seed.changed":"message.omnira.world_seed.failed"),true);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PENDING.values().removeIf(p->p.target.getServer()==event.getServer()); }
    private WorldDeflection() {}
}
