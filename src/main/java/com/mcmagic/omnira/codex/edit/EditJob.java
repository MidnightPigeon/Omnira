package com.mcmagic.omnira.codex.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A replayable plan independent of any source block entity. Owned by CodexEdits. */
public final class EditJob {
    public enum Status { SAMPLING, CLEARING, PLACING, UPDATING, COMPLETE, FAILED }
    enum Kind { BLOCK, DROP, REPLACE_DROP }
    record Change(Kind kind, BlockPos pos, BlockState before, BlockState after, ItemStack item,
                  @Nullable UUID entity, @Nullable ResourceLocation loot, @Nullable ResourceLocation matchItem) {
        Change { pos=pos.immutable(); item=item.copy(); }
    }

    private final UUID id;
    private final BlockPos center;
    private final int diameter;
    @Nullable private final UUID owner;
    private final List<Change> changes;
    private final boolean ecology;
    private final long lootSeed;
    private Status status;
    private int cursor;
    private String error="";
    private String notice="";
    private int splitRemaining;
    private int splitIndex;
    private String advancement="";
    private boolean changed;
    void advancement(String id){advancement=id;}

    EditJob(UUID id, BlockPos center, int diameter, @Nullable UUID owner, List<Change> changes, boolean ecology, long seed) {
        this.id=id; this.center=center.immutable(); this.diameter=diameter; this.owner=owner;
        this.changes=List.copyOf(changes); this.ecology=ecology; this.lootSeed=seed;
        status=ecology ? Status.CLEARING : Status.PLACING;
    }

    public UUID id() { return id; }
    public BlockPos center() { return center; }
    public Status status() { return status; }
    public int cursor() { return cursor; }
    public int plannedChanges() { return changes.size(); }
    public String error() { return error; }
    public String notice() { return notice; }
    void notice(String value) { notice=value; }
    EditJob reidentify(UUID identity, @Nullable UUID ownerId, long seed) {
        var copy=new EditJob(identity,center,diameter,ownerId,changes,ecology,seed);
        copy.notice=notice;
        return copy;
    }
    public boolean active() { return status!=Status.COMPLETE && status!=Status.FAILED; }

    boolean ready(ServerLevel level) {
        // Include the neighbour halo: explicit physics updates must not request unloaded chunks.
        return CodexEditEngine.regionError(level,center,diameter)==null &&
                EcologySampler.loaded(level,center,(diameter-1)/2+1);
    }

    /** Returns plan actions spent, including reads/updates, so empty plans cannot monopolize a tick. */
    public int advance(ServerLevel level, int budget) {
        int spent=0;
        while (active() && spent<budget) {
            if (cursor==changes.size()) {
                cursor=0;
                status=switch(status) {
                    case CLEARING -> Status.PLACING;
                    case PLACING -> Status.UPDATING;
                    default -> Status.COMPLETE;
                };
                if(status==Status.COMPLETE&&changed&&!advancement.isEmpty())
                    com.mcmagic.omnira.progression.BranchProgress.record(level.getServer(),owner,advancement);
                continue;
            }
            var change=changes.get(cursor);
            int cost=status==Status.PLACING && change.kind==Kind.REPLACE_DROP &&
                    (change.item.isEmpty() || change.item.getItem() instanceof net.minecraft.world.item.BlockItem) ? 2 : 1;
            if (cost>budget-spent) break;
            try {
                boolean finished=true;
                if (status==Status.CLEARING) {
                    if (change.kind==Kind.BLOCK) place(level,change.pos,Blocks.AIR.defaultBlockState());
                } else if (status==Status.PLACING) finished=apply(level,change,cursor);
                else if (change.kind==Kind.BLOCK || change.kind==Kind.REPLACE_DROP &&
                        (change.item.isEmpty() || change.item.getItem() instanceof net.minecraft.world.item.BlockItem)) update(level,change);
                if (finished) cursor++;
                spent+=cost;
            } catch (RuntimeException exception) {
                com.mcmagic.omnira.Omnira.LOGGER.error("Codex edit {} failed at {} (phase {}, cursor {})",id,change.pos,status,cursor,exception);
                error="edit_failed:"+exception.getClass().getSimpleName();
                status=Status.FAILED;
            }
        }
        return spent;
    }

    private boolean apply(ServerLevel level, Change change, int index) {
        if (change.kind==Kind.REPLACE_DROP) {
            if (splitRemaining>0) {
                int count=Math.min(splitRemaining,change.item.getMaxStackSize());
                var identity=UUID.nameUUIDFromBytes((id+":"+index+":"+splitIndex).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                spawn(level,change.pos,change.item.copyWithCount(count),identity);
                splitRemaining-=count; splitIndex++;
                return splitRemaining==0;
            }
            var entity=level.getEntity(change.entity);
            if (!(entity instanceof ItemEntity drop) || !drop.isAlive() || !drop.blockPosition().equals(change.pos) ||
                    !BuiltInRegistries.ITEM.getKey(drop.getItem().getItem()).equals(change.matchItem)) return true;
            changed=true;
            if (change.item.isEmpty() || change.item.getItem() instanceof net.minecraft.world.item.BlockItem&&!com.mcmagic.omnira.codex.CodexMaterials.itemOutput(change.item)) {
                drop.discard();
                if (level.getBlockState(change.pos)!=change.after) place(level,change.pos,change.after);
            } else {
                int count=Math.min(drop.getItem().getCount(),change.item.getMaxStackSize());
                splitRemaining=drop.getItem().getCount()-count; splitIndex=1;
                drop.setItem(change.item.copyWithCount(count));
                return splitRemaining==0;
            }
        } else if (change.kind==Kind.DROP) {
            spawn(level,change,change.entity);
            changed=true;
        } else {
            changed|=level.getBlockState(change.pos)!=change.after;
            place(level,change.pos,change.after);
            if (change.loot!=null) {
                if (!(level.getBlockEntity(change.pos) instanceof RandomizableContainerBlockEntity ball))
                    throw new IllegalStateException("missing_loot_ball");
                ball.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,change.loot));
                ball.setLootTableSeed(lootSeed^index);
                ball.setChanged();
            }
        }
        return true;
    }

    private static void spawn(ServerLevel level, Change change, UUID identity) {
        spawn(level,change.pos,change.item,identity);
    }

    private static void spawn(ServerLevel level, BlockPos pos, ItemStack stack, UUID identity) {
        if (level.getEntity(identity)!=null) return;
        var drop=new ItemEntity(level,pos.getX()+0.5,pos.getY()+0.5,
                pos.getZ()+0.5,stack.copy(),0,0,0);
        drop.setUUID(identity);
        drop.setDefaultPickUpDelay();
        if (!level.addFreshEntity(drop)) throw new IllegalStateException("item_spawn_rejected");
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        if (CodexEditEngine.forbidden(state)) throw new IllegalArgumentException("codex_target");
        // Intentionally overwrite filled and incomplete block entities without spilling inventory.
        var entity=level.getBlockEntity(pos);
        if (entity!=null) {
            Clearable.tryClear(entity);
            level.removeBlockEntity(pos);
        }
        var old=level.getBlockState(pos);
        if (old!=state) {
            var chunk=level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4);
            if (chunk==null) throw new IllegalStateException("chunk_unloaded");
            // NeoForge's synchronous capture guard defers onPlace (including falling-block timers).
            // Direct chunk placement maintains lighting/heightmaps without accumulating capture entries.
            boolean capturing=level.captureBlockSnapshots;
            try {
                level.captureBlockSnapshots=true;
                if (chunk.setBlockState(pos,state,false)==null) throw new IllegalStateException("placement_rejected");
            } finally { level.captureBlockSnapshots=capturing; }
            level.markAndNotifyBlock(pos,chunk,old,state,
                    Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS,512);
        }
        if (state.hasBlockEntity() && level.getBlockEntity(pos)==null && state.getBlock() instanceof EntityBlock block) {
            var created=block.newBlockEntity(pos,state);
            if (created!=null) level.setBlockEntity(created);
        }
    }

    private static void update(ServerLevel level, Change change) {
        var state=level.getBlockState(change.pos);
        state.onPlace(level,change.pos,change.before,false);
        var resolved=Block.updateFromNeighbourShapes(state,level,change.pos);
        if (resolved!=state) level.setBlock(change.pos,resolved,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE|Block.UPDATE_SUPPRESS_DROPS);
        change.before.updateIndirectNeighbourShapes(level,change.pos,Block.UPDATE_ALL,1);
        resolved.updateNeighbourShapes(level,change.pos,Block.UPDATE_ALL,1);
        resolved.updateIndirectNeighbourShapes(level,change.pos,Block.UPDATE_ALL,1);
        level.updateNeighborsAt(change.pos,change.before.getBlock());
        level.scheduleTick(change.pos,resolved.getBlock(),1);
        var fluid=resolved.getFluidState();
        if (!fluid.isEmpty()) level.scheduleTick(change.pos,fluid.getType(),fluid.getType().getTickDelay(level));
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        var tag=new CompoundTag();
        tag.putInt("Version",1); tag.putUUID("Id",id); tag.putLong("Center",center.asLong());
        tag.putInt("Diameter",diameter); tag.putBoolean("Ecology",ecology); tag.putLong("Seed",lootSeed);
        if (owner!=null) tag.putUUID("Owner",owner);
        tag.putString("Status",status.name()); tag.putInt("Cursor",cursor); tag.putString("Error",error);
        tag.putString("Notice",notice);
        tag.putString("Advancement",advancement);tag.putBoolean("Changed",changed);
        tag.putInt("SplitRemaining",splitRemaining); tag.putInt("SplitIndex",splitIndex);
        var list=new ListTag();
        for (var change:changes) {
            var cell=new CompoundTag();
            cell.putString("Kind",change.kind.name()); cell.putLong("Pos",change.pos.asLong());
            cell.put("Before",NbtUtils.writeBlockState(change.before)); cell.put("After",NbtUtils.writeBlockState(change.after));
            if (!change.item.isEmpty()) cell.put("Item",change.item.save(registries));
            if (change.entity!=null) cell.putUUID("Entity",change.entity);
            if (change.loot!=null) cell.putString("Loot",change.loot.toString());
            if (change.matchItem!=null) cell.putString("MatchItem",change.matchItem.toString());
            list.add(cell);
        }
        tag.put("Changes",list);
        return tag;
    }

    public static EditJob load(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.getInt("Version")!=1 || !EditGeometry.validDiameter(tag.getInt("Diameter")))
            throw new IllegalArgumentException("invalid_edit_snapshot");
        var changes=new ArrayList<Change>();
        var list=tag.getList("Changes",Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            var cell=list.getCompound(i);
            var after=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),cell.getCompound("After"));
            var item=ItemStack.parseOptional(registries,cell.getCompound("Item"));
            var kind=Kind.valueOf(cell.getString("Kind"));
            if (CodexEditEngine.forbidden(after) || CodexEditEngine.forbidden(item))
                throw new IllegalArgumentException("codex_target");
            if(!com.mcmagic.omnira.codex.CodexMaterials.allowed(item))throw new IllegalArgumentException("filled_material");
            changes.add(new Change(kind,BlockPos.of(cell.getLong("Pos")),
                    NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),cell.getCompound("Before")),after,item,
                    cell.hasUUID("Entity")?cell.getUUID("Entity"):null,
                    cell.contains("Loot")?ResourceLocation.parse(cell.getString("Loot")):null,
                    cell.contains("MatchItem")?ResourceLocation.parse(cell.getString("MatchItem")):null));
        }
        var job=new EditJob(tag.getUUID("Id"),BlockPos.of(tag.getLong("Center")),tag.getInt("Diameter"),
                tag.hasUUID("Owner")?tag.getUUID("Owner"):null,changes,tag.getBoolean("Ecology"),tag.getLong("Seed"));
        job.status=Status.valueOf(tag.getString("Status")); job.cursor=tag.getInt("Cursor"); job.error=tag.getString("Error");
        job.notice=tag.getString("Notice");
        job.advancement=tag.getString("Advancement");job.changed=tag.getBoolean("Changed");
        job.splitRemaining=tag.getInt("SplitRemaining"); job.splitIndex=tag.getInt("SplitIndex");
        if (job.cursor<0 || job.cursor>changes.size()) throw new IllegalArgumentException("invalid_edit_cursor");
        int radius=(job.diameter-1)/2;
        for (var change:changes) {
            if (change.pos.distSqr(job.center)>radius*radius || change.kind!=Kind.BLOCK &&
                    (change.entity==null || change.item.isEmpty()) || change.kind==Kind.REPLACE_DROP && change.matchItem==null)
                throw new IllegalArgumentException("invalid_edit_change");
        }
        if (job.status==Status.SAMPLING) throw new IllegalArgumentException("invalid_edit_phase");
        if (job.splitRemaining<0 || job.splitIndex<0 || job.splitRemaining>0 &&
                (job.status!=Status.PLACING || job.cursor>=changes.size() || changes.get(job.cursor).kind!=Kind.REPLACE_DROP))
            throw new IllegalArgumentException("invalid_item_split");
        return job;
    }
}
