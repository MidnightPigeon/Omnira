package com.mcmagic.omnira.space.sea;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

@EventBusSubscriber(modid="omnira")
public final class StarlightEntity extends BlockEntity {
    private static final Map<ServerLevel, Set<BlockPos>> SOURCES = new WeakHashMap<>();
    private static final int SIDE = 129, VOLUME = SIDE*SIDE*SIDE;
    private int cursor, cooldown = 200;
    private BlockPos candidate;
    public StarlightEntity(BlockPos p, BlockState s) { super(StarlightContent.ENTITY.get(), p, s); }
    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && getBlockState().is(StarlightContent.SOURCE.get()))
            SOURCES.computeIfAbsent(server, k -> new HashSet<>()).add(worldPosition);
    }
    @Override public void setRemoved() {
        if (level instanceof ServerLevel server && SOURCES.containsKey(server)) SOURCES.get(server).remove(worldPosition);
        super.setRemoved();
    }
    public static boolean protectedAt(ServerLevel level, Vec3 p) {
        for (BlockPos source : SOURCES.getOrDefault(level, Set.of()))
            if (level.hasChunkAt(source) && level.getBlockState(source).is(StarlightContent.SOURCE.get())
                    && Vec3.atCenterOf(source).distanceToSqr(p) <= 64*64) return true;
        return false;
    }
    @SubscribeEvent public static void preventSpawn(MobSpawnEvent.PositionCheck e) {
        if ((e.getEntity().getType().getCategory() == MobCategory.MONSTER || e.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)
                && protectedAt(e.getLevel().getLevel(), e.getEntity().position())) e.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }
    public static boolean eligible(ServerLevel l, BlockPos source, BlockPos p) {
        if (source.distSqr(p) > 64*64 || !l.isInWorldBounds(p) || !l.hasChunkAt(p) || !l.hasChunkAt(p.below())) return false;
        BlockState s = l.getBlockState(p);
        boolean water = s.is(Blocks.WATER);
        boolean star = s.is(VoidSeaContent.LIQUID.get());
        return (s.isAir() || water || star) && l.getMaxLocalRawBrightness(p) < 5
                && l.getBlockState(p.below()).isFaceSturdy(l, p.below(), Direction.UP);
    }
    public static boolean place(ServerLevel l, BlockPos source, BlockPos p) {
        if (!eligible(l, source, p)) return false;
        var fluid = l.getFluidState(p);
        int water = fluid.getType().isSame(Fluids.WATER) ? 1 : VoidSeaContent.fluid(fluid) ? 2 : 0;
        int amount=l.getBlockState(p).getOptionalValue(LiquidBlock.LEVEL).orElse(0);
        return l.setBlockAndUpdate(p, StarlightContent.DUST.get().defaultBlockState().setValue(StarlightBlock.WATER, water)
                .setValue(StarlightBlock.FLUID_LEVEL,amount));
    }
    public static void tick(Level l, BlockPos p, BlockState s, StarlightEntity e) {
        if (!(l instanceof ServerLevel server)) return;
        if (e.cooldown > 0) e.cooldown--;
        if (e.candidate != null && !eligible(server,p,e.candidate)) e.candidate = null;
        if (e.candidate == null) e.search(server,p);
        if (e.cooldown == 0 && e.candidate != null) {
            place(server,p,e.candidate);e.candidate=null;e.cooldown=200;
        }
    }
    private static int offset(int n) { return n%2==0 ? -n/2 : (n+1)/2; }
    private void search(ServerLevel server,BlockPos p) {
        // Walk the sphere incrementally; unsuccessful scans never force-load terrain.
        for (int n = 0; n < 2048; n++) {
            int i = cursor++;
            if (cursor == VOLUME) cursor = 0;
            BlockPos target = p.offset(offset(i % SIDE), offset(i / (SIDE*SIDE)), offset(i / SIDE % SIDE));
            if (eligible(server, p, target)) { candidate=target; return; }
            if (cursor == 0) return;
        }
    }
}
