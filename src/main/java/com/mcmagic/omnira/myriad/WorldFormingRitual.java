package com.mcmagic.omnira.myriad;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import java.util.*;

/** Components leave their pedestals after absorption and remain in escrow until completion. */
public final class WorldFormingRitual {
    public static final int ASPECT_TICKS = 40;
    public static final int ASSEMBLY_TICKS = 80;
    public static final int TOTAL_TICKS = 8 * ASPECT_TICKS + ASSEMBLY_TICKS;
    private final RitualEnergyCoreBlockEntity core;
    private ItemStack seed = ItemStack.EMPTY;
    private int elapsed;
    private int[] anchors = new int[0];
    private final NonNullList<ItemStack> absorbed = NonNullList.withSize(8, ItemStack.EMPTY);
    private boolean changingOfferings;

    public WorldFormingRitual(RitualEnergyCoreBlockEntity core) { this.core = core; }
    public boolean active() { return !seed.isEmpty(); }
    public int elapsed() { return elapsed; }
    public int absorbedCount() { return (int) absorbed.stream().filter(stack -> !stack.isEmpty()).count(); }
    public void tickVisual() { if (active() && elapsed < TOTAL_TICKS) elapsed++; }
    public int anchor(int aspect) { return aspect >= 0 && aspect < anchors.length ? anchors[aspect] : -1; }

    public static List<CrystalPedestalBlockEntity> offerings(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()) || !GenesisRitual.loaded(level, pos)) return List.of();
        var result = new ArrayList<CrystalPedestalBlockEntity>();
        var seen = EnumSet.noneOf(WorldComponent.class);
        for (var offset : DreamRitual.ANCHORS) {
            var at = pos.offset(offset[0], 0, offset[1]);
            if (!level.getBlockState(at).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                    || !(level.getBlockEntity(at) instanceof CrystalPedestalBlockEntity pedestal)
                    || pedestal.getItem(0).getCount() != 1) return List.of();
            var aspect = WorldComponent.of(pedestal.getItem(0));
            if (aspect == null || !seen.add(aspect)) return List.of();
            result.add(pedestal);
        }
        return seen.size() == 8 ? result : List.of();
    }

    public boolean activate(Player player, InteractionHand hand) {
        if (!(core.getLevel() instanceof ServerLevel level) || active() || core.genesisFluid() > 0
                || player.isSpectator() || !player.isAlive() || !level.mayInteract(player, core.getBlockPos())
                || !level.mayInteract(player, core.getBlockPos().above()) || !level.isEmptyBlock(core.getBlockPos().above())
                || !player.getItemInHand(hand).is(WorldSeedContent.ACTIVE.get())) return false;
        var offerings = offerings(level, core.getBlockPos());
        if (offerings.size() != 8 || offerings.stream().anyMatch(p -> !level.mayInteract(player, p.getBlockPos()))) return false;
        anchors = new int[8];
        for (int i = 0; i < 8; i++) anchors[Objects.requireNonNull(WorldComponent.of(offerings.get(i).getItem(0))).ordinal()] = i;
        seed = player.getItemInHand(hand).copyWithCount(1);
        player.getItemInHand(hand).consume(1, player);
        elapsed = 0;
        sync();
        chime(0);
        return true;
    }

    public void validate() {
        var level = core.getLevel();
        if (level == null || level.isClientSide || !active() || changingOfferings || !GenesisRitual.loaded(level, core.getBlockPos())) return;
        if (!level.getBlockState(core.getBlockPos()).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()) || anchors.length != 8) { cancel(); return; }
        int seen = 0;
        for (var aspect : WorldComponent.values()) {
            int index = anchor(aspect.ordinal());
            if (index < 0 || index >= 8 || (seen & (1 << index)) != 0) { cancel(); return; }
            seen |= 1 << index;
            var pedestal = pedestal(index);
            if (pedestal == null) { cancel(); return; }
            var stored = absorbed.get(aspect.ordinal());
            var remaining = pedestal.getItem(0);
            boolean valid = stored.isEmpty() ? remaining.is(aspect.item.get()) && remaining.getCount() == 1
                    : stored.is(aspect.item.get()) && stored.getCount() == 1 && remaining.isEmpty();
            if (!valid) { cancel(); return; }
        }
    }

    public void cancel() {
        if (core.getLevel() == null || core.getLevel().isClientSide || !active()) return;
        var refund = seed;
        var components = absorbed.stream().map(ItemStack::copy).toList();
        var locations = anchors.clone();
        clear();
        for (int i = 0; i < components.size(); i++) {
            var stack = components.get(i);
            if (stack.isEmpty()) continue;
            var pedestal = i < locations.length ? pedestal(locations[i]) : null;
            if (pedestal != null && pedestal.getItem(0).isEmpty()) { pedestal.setItem(0, stack); pedestal.setChanged(); }
            else Block.popResource(core.getLevel(), core.getBlockPos().above(), stack);
        }
        Block.popResource(core.getLevel(), core.getBlockPos().above(), refund);
        sync();
    }

    public void tick() {
        var level = core.getLevel();
        if (level == null || level.isClientSide || !active() || !GenesisRitual.loaded(level, core.getBlockPos())) return;
        validate();
        if (!active()) return;
        if (elapsed < TOTAL_TICKS) elapsed++;
        // This also catches up saves made before per-component absorption existed.
        for (var aspect : WorldComponent.values()) {
            if (elapsed < (aspect.ordinal() + 1) * ASPECT_TICKS || !absorbed.get(aspect.ordinal()).isEmpty()) continue;
            var pedestal = pedestal(anchor(aspect.ordinal()));
            changingOfferings = true;
            try { absorbed.set(aspect.ordinal(), pedestal.removeItem(0, 1)); }
            finally { changingOfferings = false; }
            if (absorbed.get(aspect.ordinal()).isEmpty()) { cancel(); return; }
            pedestal.setChanged();
        }
        if (elapsed < TOTAL_TICKS) {
            if (elapsed % ASPECT_TICKS == 0 && elapsed < 8 * ASPECT_TICKS) chime(elapsed / ASPECT_TICKS);
            if (elapsed % 5 == 0) sync(); else core.setChanged();
            return;
        }
        if (!level.isEmptyBlock(core.getBlockPos().above())) { core.setChanged(); return; }
        if (absorbedCount() != 8) return;
        clear();
        Block.popResource(level, core.getBlockPos().above(), new ItemStack(MiniatureWorldContent.ITEM.get()));
        chime(8);
        sync();
    }

    private CrystalPedestalBlockEntity pedestal(int index) {
        if (index < 0 || index >= DreamRitual.ANCHORS.length || core.getLevel() == null) return null;
        var offset = DreamRitual.ANCHORS[index];
        var at = core.getBlockPos().offset(offset[0], 0, offset[1]);
        return core.getLevel().hasChunkAt(at) && core.getLevel().getBlockState(at).is(ModBlocks.CRYSTAL_PEDESTAL.get())
                && core.getLevel().getBlockEntity(at) instanceof CrystalPedestalBlockEntity pedestal ? pedestal : null;
    }
    private void clear() { seed = ItemStack.EMPTY; elapsed = 0; anchors = new int[0]; Collections.fill(absorbed, ItemStack.EMPTY); }
    private void chime(int step) {
        core.getLevel().playSound(null, core.getBlockPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1, .6F + step * .08F);
    }
    private void sync() {
        core.setChanged();
        if (core.getLevel() != null) core.getLevel().sendBlockUpdated(core.getBlockPos(), core.getBlockState(), core.getBlockState(), 2);
    }

    public void save(CompoundTag tag, HolderLookup.Provider lookup) {
        if (!active()) return;
        var data = new CompoundTag();
        data.put("Seed", seed.save(lookup)); data.putInt("Elapsed", elapsed); data.putIntArray("Anchors", anchors);
        var components = new CompoundTag(); ContainerHelper.saveAllItems(components, absorbed, lookup); data.put("Absorbed", components);
        tag.put("WorldForming", data);
    }
    public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        clear();
        if (!tag.contains("WorldForming")) return;
        var data = tag.getCompound("WorldForming");
        var loaded = ItemStack.parseOptional(lookup, data.getCompound("Seed"));
        if (!loaded.is(WorldSeedContent.ACTIVE.get())) return;
        seed = loaded.copyWithCount(1); elapsed = Math.clamp(data.getInt("Elapsed"), 0, TOTAL_TICKS); anchors = data.getIntArray("Anchors");
        ContainerHelper.loadAllItems(data.getCompound("Absorbed"), absorbed, lookup);
    }
}
