package com.mcmagic.omnira.codex.edit;

import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Pure planning boundary: begin never changes the destination world or consumes inputs. */
public final class CodexEditEngine {
    public record BeginResult(@Nullable EditJob job, @Nullable String error) {
        public boolean accepted() { return job!=null; }
    }
    private static final ResourceLocation CODEX=ResourceLocation.parse("omnira:myriad_codex");
    private static final ResourceLocation HEART=ResourceLocation.parse("omnira:myriad_heart");
    private CodexEditEngine() {}

    public static BeginResult begin(ServerLevel level, BlockPos center, int diameter, Operation op,
                                    Direction.Axis plane, boolean hollow, BlockState source, BlockState target,
                                    @Nullable Holder<Biome> biome, @Nullable ServerPlayer owner) {
        if (forbidden(target)) return fail("codex_target");
        return plan(level,center,diameter,op,plane,hollow,new ItemStack(source.getBlock()),
                new ItemStack(target.getBlock()),com.mcmagic.omnira.codex.CodexMaterials.itemOutput(new ItemStack(target.getBlock()))?null:target,biome,owner,null);
    }

    public static BeginResult begin(ServerLevel level, BlockPos center, int diameter, Operation op,
                                    Direction.Axis plane, boolean hollow, ItemStack source, ItemStack target,
                                    @Nullable Holder<Biome> biome, @Nullable ServerPlayer owner) {
        return begin(level,center,diameter,op,plane,hollow,source,target,biome,owner,null);
    }

    /** Optional detached sample preserves old saved plans and explicit test fixtures. */
    public static BeginResult begin(ServerLevel level, BlockPos center, int diameter, Operation op,
                                    Direction.Axis plane, boolean hollow, ItemStack source, ItemStack target,
                                    @Nullable Holder<Biome> biome, @Nullable ServerPlayer owner,
                                    @Nullable EcologySampler.Sample sample) {
        var state=target.getItem() instanceof BlockItem block&&!com.mcmagic.omnira.codex.CodexMaterials.itemOutput(target) ? block.getBlock().defaultBlockState() : null;
        if(target.isEmpty()&&op!=Operation.ECOLOGY)
            state=Blocks.AIR.defaultBlockState();
        return plan(level,center,diameter,op,plane,hollow,source,target,state,biome,owner,sample);
    }

    private static BeginResult plan(ServerLevel level, BlockPos center, int diameter, Operation op,
                                     Direction.Axis plane, boolean hollow, ItemStack source, ItemStack target,
                                     @Nullable BlockState targetState, @Nullable Holder<Biome> biome,
                                     @Nullable ServerPlayer owner, @Nullable EcologySampler.Sample suppliedSample) {
        if (!EditGeometry.validDiameter(diameter)) return fail("invalid_diameter");
        if(!com.mcmagic.omnira.codex.CodexMaterials.allowed(source)||!com.mcmagic.omnira.codex.CodexMaterials.allowed(target))return fail("filled_material");
        if (op==null || plane==null) return fail("invalid_operation");
        if (forbidden(target) || targetState!=null && forbidden(targetState)) return fail("codex_target");
        if (op!=Operation.ECOLOGY && target.isEmpty() && targetState==null) return fail("missing_target");
        if (op==Operation.MATTER && source.isEmpty()) return fail("missing_source");
        String region=regionError(level,center,diameter);
        if (region!=null) return fail(region);
        EcologySampler.Sample sample=null;
        ResourceLocation loot=null;
        BlockState shell=null, ball=null;
        if (op==Operation.ECOLOGY) {
            if (biome==null || biome.unwrapKey().isEmpty() || level.registryAccess().registryOrThrow(Registries.BIOME)
                    .getHolder(biome.unwrapKey().get()).isEmpty()) return fail("unregistered_biome");
            sample=suppliedSample!=null ? suppliedSample : EcologyLandscape.build(level,diameter,biome,level.random.nextLong());
            if (sample.diameter()!=diameter || sample.level().getServer()!=level.getServer() ||
                    !sample.biomeId().equals(biome.unwrapKey().get().location()))
                return fail("sample_biome_mismatch");
            loot=EcologyLoot.table(sample.level(),biome).location();
            shell=registeredBlock("omnira:spiritual_crystal_block"); ball=registeredBlock("omnira:crystal_ball");
            if (shell==null || ball==null) return fail("missing_ecology_blocks");
        }
        UUID id=UUID.randomUUID();
        var changes=new ArrayList<EditJob.Change>();
        int radius=(diameter-1)/2, ground=-radius+diameter/3;
        for (int i=0;i<diameter*diameter*diameter;i++) {
            var offset=EditGeometry.offset(i,diameter);
            int x=offset.getX(), y=offset.getY(), z=offset.getZ();
            if (!EditGeometry.contains(op,radius,plane,hollow,x,y,z)) continue;
            var pos=center.offset(offset);
            var before=level.getBlockState(pos);
            if (op==Operation.MATTER && (!(source.getItem() instanceof BlockItem block) || !before.is(block.getBlock()))) continue;
            BlockState after=targetState;
            ResourceLocation cellLoot=null;
            if (op==Operation.ECOLOGY) {
                if (EditGeometry.ecologyShell(radius,x,y,z)) after=shell;
                else {
                    after=sample.at(x,y-ground,z);
                    if (y<=ground && after.isAir()) after=sample.at(0,0,0);
                    if (forbidden(after) || after.hasBlockEntity()) after=Blocks.AIR.defaultBlockState();
                    if (x==0 && z==0 && y==ground+1) { after=ball; cellLoot=loot; }
                }
            }
            changes.add(block(pos,before,after==null?Blocks.AIR.defaultBlockState():after,cellLoot));
            if (op!=Operation.ECOLOGY && targetState==null) changes.add(drop(id,changes.size(),pos,target));
        }
        if (op==Operation.MATTER) {
            var box=new AABB(center).inflate(radius+1);
            var drops=level.getEntitiesOfClass(ItemEntity.class,box,
                    entity -> entity.getItem().is(source.getItem()) && entity.isAlive() &&
                            entity.position().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(center))<=radius*radius &&
                            EditGeometry.contains(Operation.MATTER,radius,plane,false,
                                    entity.blockPosition().getX()-center.getX(),entity.blockPosition().getY()-center.getY(),
                                    entity.blockPosition().getZ()-center.getZ()));
            drops.sort(Comparator.comparing(entity->entity.getUUID().toString()));
            for (var entity:drops) {
                var pos=entity.blockPosition();
                changes.add(new EditJob.Change(EditJob.Kind.REPLACE_DROP,pos,level.getBlockState(pos),
                        targetState==null?Blocks.AIR.defaultBlockState():targetState,target.copyWithCount(1),
                        entity.getUUID(),null,BuiltInRegistries.ITEM.getKey(source.getItem())));
            }
        }
        Comparator<EditJob.Change> radial=Comparator.comparingDouble(change->change.pos().distSqr(center));
        changes.sort(op==Operation.ECOLOGY
                ? Comparator.<EditJob.Change>comparingInt(change->change.loot()!=null?2:change.pos().getY()<=center.getY()+ground?0:1)
                    .thenComparing(radial)
                : radial);
        return new BeginResult(new EditJob(id,center,diameter,owner==null?null:owner.getUUID(),changes,
                op==Operation.ECOLOGY,level.random.nextLong()),null);
    }

    private static EditJob.Change block(BlockPos pos, BlockState before, BlockState after, @Nullable ResourceLocation loot) {
        return new EditJob.Change(EditJob.Kind.BLOCK,pos,before,after,ItemStack.EMPTY,null,loot,null);
    }
    private static EditJob.Change drop(UUID id, int index, BlockPos pos, ItemStack target) {
        var entity=UUID.nameUUIDFromBytes((id+":"+index).getBytes(StandardCharsets.UTF_8));
        return new EditJob.Change(EditJob.Kind.DROP,pos,Blocks.AIR.defaultBlockState(),Blocks.AIR.defaultBlockState(),
                target.copyWithCount(1),entity,null,null);
    }
    private static BeginResult fail(String error) { return new BeginResult(null,error); }

    @Nullable static String regionError(ServerLevel level, BlockPos center, int diameter) {
        int radius=(diameter-1)/2;
        if (center.getY()-radius-1<level.getMinBuildHeight() || center.getY()+radius+1>=level.getMaxBuildHeight())
            return "outside_build_height";
        if (!level.getWorldBorder().isWithinBounds(center.offset(-radius,0,-radius)) ||
                !level.getWorldBorder().isWithinBounds(center.offset(radius,0,radius))) return "outside_world_border";
        return EcologySampler.loaded(level,center,radius+1)?null:"region_not_loaded";
    }

    static boolean forbidden(BlockState state) {
        var id=BuiltInRegistries.BLOCK.getKey(state.getBlock()); return id.equals(CODEX) || id.equals(HEART);
    }
    static boolean forbidden(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var id=BuiltInRegistries.ITEM.getKey(stack.getItem()); return id.equals(CODEX) || id.equals(HEART);
    }
    @Nullable private static BlockState registeredBlock(String id) {
        var key=ResourceLocation.parse(id);
        return BuiltInRegistries.BLOCK.containsKey(key)?BuiltInRegistries.BLOCK.get(key).defaultBlockState():null;
    }
}
