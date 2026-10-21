package com.mcmagic.omnira.codex.edit;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/** Per-level durable scheduler. Source block entities store only the returned UUID. */
@EventBusSubscriber(modid="omnira")
public final class CodexEdits extends SavedData {
    public static final int ACTIONS_PER_TICK=256;
    public static final int TERMINAL_RETENTION_TICKS=1200;
    public record Submission(@Nullable UUID jobId, @Nullable String error) {
        public boolean accepted() { return jobId!=null; }
    }
    public record Progress(EditJob.Status status, int cursor, int plannedChanges, String error, boolean waitingForChunks, String notice) {}
    private final LinkedHashMap<UUID,EditJob> jobs=new LinkedHashMap<>();
    private int nextJob;
    private int nextSample;
    private final LinkedHashMap<UUID,EcologyRequest> requests=new LinkedHashMap<>();
    private final List<CompoundTag> pendingRequests=new ArrayList<>();
    private final Map<UUID,Long> terminalSince=new HashMap<>();
    private ServerLevel level;

    public static CodexEdits get(ServerLevel level) {
        var data=level.getDataStorage().computeIfAbsent(new Factory<>(CodexEdits::new,CodexEdits::load,null),"omnira_codex_edits");
        data.level=level;
        for (var tag:data.pendingRequests) {
            var request=EcologyRequest.load(tag,level);
            data.requests.put(request.id,request);
        }
        data.pendingRequests.clear();
        return data;
    }

    public Submission submit(BlockPos center, int diameter, Operation op, Direction.Axis plane,
                             boolean hollow, ItemStack source, ItemStack target, @Nullable Holder<Biome> biome,
                             @Nullable ServerPlayer owner) {
        return submit(level,center,diameter,op,plane,hollow,source,target,biome,owner);
    }

    public Submission submit(ServerLevel level, BlockPos center, int diameter, Operation op, Direction.Axis plane,
                             boolean hollow, ItemStack source, ItemStack target, @Nullable Holder<Biome> biome,
                             @Nullable ServerPlayer owner) {
        return submit(level,center,diameter,op,plane,hollow,source,target,biome,owner,null);
    }

    public Submission submit(ServerLevel level, BlockPos center, int diameter, Operation op, Direction.Axis plane,
                             boolean hollow, ItemStack source, ItemStack target, @Nullable Holder<Biome> biome,
                             @Nullable ServerPlayer owner, @Nullable EcologySampler.Sample sample) {
        if (get(level)!=this) return new Submission(null,"wrong_level_manager");
        if (op==Operation.ECOLOGY && sample==null) {
            if (!EditGeometry.validDiameter(diameter)) return new Submission(null,"invalid_diameter");
            if (plane==null) return new Submission(null,"invalid_operation");
            if (CodexEditEngine.forbidden(target)) return new Submission(null,"codex_target");
            String region=CodexEditEngine.regionError(level,center,diameter);
            if (region!=null) return new Submission(null,region);
            if (biome==null || biome.unwrapKey().isEmpty() || level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolder(biome.unwrapKey().get()).isEmpty())
                return new Submission(null,"unregistered_biome");
            var request=new EcologyRequest(UUID.randomUUID(),center,diameter,biome.unwrapKey().get().location(),
                    owner==null?null:owner.getUUID(),level.random.nextLong());
            requests.put(request.id,request); setDirty();
            return new Submission(request.id,null);
        }
        var result=CodexEditEngine.begin(level,center,diameter,op,plane,hollow,source,target,biome,owner,sample);
        if (!result.accepted()) return new Submission(null,result.error());
        var job=result.job();
        job.advancement(op==Operation.MATTER?"transmute_matter":op==Operation.ECOLOGY?"pocket_ecology":target.isEmpty()?"":"shape_at_will");
        jobs.put(job.id(),job); setDirty();
        return new Submission(job.id(),null);
    }

    public boolean active(UUID id) {
        var request=requests.get(id); if (request!=null) return request.error.isEmpty();
        var job=jobs.get(id); return job!=null && job.active();
    }
    public Optional<Progress> progress(ServerLevel level, UUID id) {
        var job=jobs.get(id);
        var request=requests.get(id);
        if (request!=null) return Optional.of(new Progress(request.error.isEmpty()?EditJob.Status.SAMPLING:EditJob.Status.FAILED,
                (int)Math.min(Integer.MAX_VALUE,request.checks),0,request.error,
                CodexEditEngine.regionError(level,request.center,request.diameter)!=null,request.notice));
        return job==null?Optional.empty():Optional.of(new Progress(job.status(),job.cursor(),job.plannedChanges(),
                job.error(),job.active()&&!job.ready(level),job.notice()));
    }
    public boolean forget(UUID id) {
        if (active(id)) return false;
        boolean removed=jobs.remove(id)!=null;
        removed=requests.remove(id)!=null || removed;
        if (!removed) return false;
        terminalSince.remove(id);
        setDirty(); return true;
    }

    @SubscribeEvent public static void tick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) get(level).advance(level);
    }

    private void advance(ServerLevel level) {
        prune(level.getGameTime());
        sample(level);
        var active=jobs.values().stream().filter(EditJob::active).toList();
        if (active.isEmpty()) return;
        int budget=ACTIONS_PER_TICK;
        int start=Math.floorMod(nextJob,active.size());
        for (int i=0;i<active.size() && budget>0;i++) {
            var job=active.get((start+i)%active.size());
            if (!job.ready(level)) continue;
            int spent=job.advance(level,Math.min(64,budget));
            budget-=spent;
            setDirty();
        }
        nextJob=(start+1)%active.size();
    }

    private void prune(long now) {
        int recorded=terminalSince.size();
        for (var job:jobs.values()) if (!job.active()) terminalSince.putIfAbsent(job.id(),now);
        for (var request:requests.values()) if (!request.error.isEmpty()) terminalSince.putIfAbsent(request.id,now);
        var expired=terminalSince.entrySet().stream().filter(entry->now-entry.getValue()>=TERMINAL_RETENTION_TICKS)
                .map(Map.Entry::getKey).toList();
        for (var id:expired) { jobs.remove(id); requests.remove(id); terminalSince.remove(id); }
        if (!expired.isEmpty() || recorded!=terminalSince.size()) setDirty();
    }

    private void sample(ServerLevel level) {
        var pending=requests.values().stream().filter(request->request.error.isEmpty()).toList();
        if (pending.isEmpty()) return;
        int start=Math.floorMod(nextSample,pending.size());
        // At most two detached 15^3 constructions per tick; mutations retain their own budget.
        for (int i=0;i<Math.min(2,pending.size());i++) {
            var request=pending.get((start+i)%pending.size());
            request.construct(level,this::setDirty);
            if (request.sample==null || CodexEditEngine.regionError(level,request.center,request.diameter)!=null) continue;
            var result=CodexEditEngine.begin(level,request.center,request.diameter,Operation.ECOLOGY,Direction.Axis.Y,
                    false,ItemStack.EMPTY,ItemStack.EMPTY,request.biome(level),null,request.sample);
            if (result.accepted()) {
                var job=result.job().reidentify(request.id,request.owner,request.seed);
                job.advancement("pocket_ecology");
                job.notice(request.notice); jobs.put(request.id,job); requests.remove(request.id);
            } else request.error=result.error();
            setDirty();
        }
        nextSample=(start+1)%pending.size();
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        var list=new ListTag();
        for (var job:jobs.values()) list.add(job.save(registries));
        tag.put("Jobs",list); tag.putInt("NextJob",nextJob);
        var sampling=new ListTag(); for (var request:requests.values()) sampling.add(request.save());
        for (var request:pendingRequests) sampling.add(request.copy());
        tag.put("Sampling",sampling); tag.putInt("NextSample",nextSample);
        var terminal=new CompoundTag(); terminalSince.forEach((id,time)->terminal.putLong(id.toString(),time));
        tag.put("TerminalSince",terminal);
        return tag;
    }

    private static CodexEdits load(CompoundTag tag, HolderLookup.Provider registries) {
        var data=new CodexEdits();
        var list=tag.getList("Jobs",Tag.TAG_COMPOUND);
        for (int i=0;i<list.size();i++) {
            var job=EditJob.load(list.getCompound(i),registries);
            data.jobs.put(job.id(),job);
        }
        data.nextJob=tag.getInt("NextJob");
        var sampling=tag.getList("Sampling",Tag.TAG_COMPOUND);
        for (int i=0;i<sampling.size();i++) data.pendingRequests.add(sampling.getCompound(i).copy());
        data.nextSample=tag.getInt("NextSample");
        var terminal=tag.getCompound("TerminalSince");
        for (var id:terminal.getAllKeys()) data.terminalSince.put(UUID.fromString(id),terminal.getLong(id));
        return data;
    }
}
