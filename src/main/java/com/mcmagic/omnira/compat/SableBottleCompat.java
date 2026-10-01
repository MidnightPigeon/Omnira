package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.item.bottle.*;
import com.mcmagic.omnira.registry.ModDataComponents;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.*;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import dev.ryanhcode.sable.sublevel.storage.serialization.*;
import dev.ryanhcode.sable.util.SableNBTUtils;
import dev.ryanhcode.sable.api.schematic.SubLevelSchematicSerializationContext;
import dev.ryanhcode.sable.companion.math.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.*;
import java.util.*;

/** Loaded only behind the Sable mod check. Uses its complete persistence format, not a block copy. */
public final class SableBottleCompat {
    public static InteractionResult capture(UseOnContext context) {
        var world=context.getLevel();var player=context.getPlayer();
        var container=SubLevelContainer.getContainer(world);
        if(container==null || !container.inBounds(context.getClickedPos())) return InteractionResult.PASS;
        var plot=container.getPlot(new net.minecraft.world.level.ChunkPos(context.getClickedPos()));
        if(plot==null) return InteractionResult.PASS;
        if(world.isClientSide) return InteractionResult.SUCCESS;
        if(!(world instanceof ServerLevel level) || player==null || player.isSpectator() || !player.mayBuild()) return InteractionResult.FAIL;
        var linked=SubLevelHelper.getConnectedChain(plot.getSubLevel());
        var members=new ArrayList<ServerSubLevel>();
        for(var ship:linked) {
            if(!(ship instanceof ServerSubLevel server) || server.getLevel()!=level || ship.isRemoved()) return InteractionResult.FAIL;
            members.add(server);
        }
        if(!members.contains(plot.getSubLevel())) members.add((ServerSubLevel)plot.getSubLevel());
        var entities=new ArrayList<Entity>();
        for(var entity:level.getAllEntities()) {
            boolean local=members.stream().anyMatch(s->s.getPlot().contains(entity.position()));
            if(entity instanceof Player) {
                boolean aboard=members.stream().anyMatch(s->{
                    if(s.getPlot().contains(entity.position())) {
                        var point=s.logicalPose().transformPosition(new org.joml.Vector3d(entity.getX(),entity.getY(),entity.getZ()));
                        return s.boundingBox().toMojang().inflate(.25,2,.25).contains(point.x,point.y,point.z);
                    }
                    return s.boundingBox().toMojang().inflate(.25,2,.25).intersects(entity.getBoundingBox());
                });
                if(aboard) {PocketBottleItem.message(player,"disembark");return InteractionResult.FAIL;}
                continue;
            }
            if(local && !entity.isPassenger()) entities.add(entity);
        }
        var savedEntities=new ListTag();
        for(var entity:entities) {
            var tag=new CompoundTag();
            if(entity.getSelfAndPassengers().anyMatch(e->e instanceof Player || com.mcmagic.omnira.item.bottle.BottleCaptureRules.forbidden(e)
                    || com.mcmagic.omnira.item.bottle.BottleCaptureRules.hostileOrAngry(e)) || !entity.save(tag)) {
                PocketBottleItem.message(player,"unsupported");return InteractionResult.FAIL;
            }
            savedEntities.add(tag);
        }
        var ids=members.stream().map(SubLevel::getUniqueId).toList();
        var snapshots=new ListTag();
        try {
            for(var ship:members) snapshots.add(SubLevelSerializer.toData(ship,ids).fullTag().copy());
        } catch(RuntimeException error) {
            com.mcmagic.omnira.Omnira.LOGGER.error("Could not snapshot a bottled structure",error);
            PocketBottleItem.message(player,"unsupported");return InteractionResult.FAIL;
        }
        var data=new CompoundTag();data.putString("Kind","structure");data.put("Ships",snapshots);data.put("Entities",savedEntities);
        data.putInt("SourceMinSection",level.getMinSection());
        data.putString("SourceDimension",level.dimension().location().toString());
        var storage=BottleStorage.get(level);var id=storage.put(data);
        // Keep source coordinates in the snapshot; each release maps them into fresh plots.
        for(var ship:members) container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
        context.getItemInHand().set(ModDataComponents.BOTTLE_CAPTURE,id);
        context.getItemInHand().set(DataComponents.CUSTOM_NAME,Component.translatable("item.omnira.pocket_magic_bottle.structure"));
        return InteractionResult.CONSUME;
    }
    public static boolean release(ServerLevel level,CompoundTag data,Vec3 point) {
        var container=SubLevelContainer.getContainer(level);
        if(container==null) return false;
        var tags=data.getList("Ships",Tag.TAG_COMPOUND);
        if(tags.isEmpty()) return false;
        int sourceMinSection;
        if(data.contains("SourceMinSection",Tag.TAG_INT)) sourceMinSection=data.getInt("SourceMinSection");
        else {
            // Older bottles lack height metadata. Only migrate when the stored pivot identifies it unambiguously.
            var candidates=new HashSet<Integer>();
            for(var dimension:level.getServer().getAllLevels()) candidates.add(dimension.getMinSection());
            candidates.removeIf(min->tags.stream().anyMatch(tag->!StructureSections.containsPivot((CompoundTag)tag,min)));
            if(candidates.size()!=1) return false;
            sourceMinSection=candidates.iterator().next();
        }
        var prepared=new ArrayList<CompoundTag>();
        AABB bounds=null;
        for(var value:tags) {
            var tag=((CompoundTag)value).copy();var plot=tag.getCompound("plot");
            try {StructureSections.remap(tag,sourceMinSection,level.getMinSection(),level.getSectionsCount());}
            catch(IllegalArgumentException error) {
                com.mcmagic.omnira.Omnira.LOGGER.warn("Cannot fit bottled structure in destination height: {}",error.getMessage());
                return false;
            }
            var box=SableNBTUtils.readBoundingBox(tag.getCompound("world_bounds")).toMojang();
            bounds=bounds==null?box:bounds.minmax(box);prepared.add(tag);
        }
        Vec3 delta=new Vec3(point.x-bounds.getCenter().x,point.y+.2-bounds.minY,point.z-bounds.getCenter().z);
        Vec3 landing=null;
        search: for(int y=0;y<=6;y++) for(int radius=0;radius<=3;radius++)
            for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius) continue;
                var offset=delta.add(x,y,z);var target=bounds.move(offset);
                if(clear(level,target)) {landing=offset;break search;}
            }
        if(landing==null) return false;
        delta=landing;
        var entities=new ArrayList<Entity>();
        var allocated=new ArrayList<ServerSubLevel>();
        var offsets=new ArrayList<BlockPos>();
        var context=new SubLevelSchematicSerializationContext(SubLevelSchematicSerializationContext.Type.PLACE,new BoundingBox3i());
        context.setPlaceTransform(p->(BlockPos)p);context.setSetupTransform(p->(BlockPos)p);
        var previous=SubLevelSchematicSerializationContext.getCurrentContext();
        try {
            // Allocate every member before loading any blocks so inter-structure links can be remapped together.
            for(var tag:prepared) {
                var pose=SableNBTUtils.readPose3d(tag.getCompound("pose"));pose.position().add(delta.x,delta.y,delta.z);
                var ship=(ServerSubLevel)container.allocateNewSubLevel(pose);
                allocated.add(ship);
                var plot=tag.getCompound("plot");var origin=container.getOrigin();
                int shift=container.getLogPlotSize()+4;
                var oldCorner=new BlockPos((plot.getInt("plot_x")+origin.x)<<shift,0,(plot.getInt("plot_z")+origin.y)<<shift);
                var newPlot=ship.getPlot().save();
                var offset=new BlockPos((newPlot.getInt("plot_x")-plot.getInt("plot_x"))<<shift,0,
                        (newPlot.getInt("plot_z")-plot.getInt("plot_z"))<<shift);
                offsets.add(offset);
                context.getMappings().put(tag.getUUID("uuid"),new SubLevelSchematicSerializationContext.SchematicMapping(
                        new org.joml.Vector3d(oldCorner.getX()+offset.getX(),0,oldCorner.getZ()+offset.getZ()),pose.orientation(),ship.getUniqueId(),p->((BlockPos)p).offset(offset)));
                plot.putInt("plot_x",newPlot.getInt("plot_x"));plot.putInt("plot_z",newPlot.getInt("plot_z"));
                pose.rotationPoint().add(offset.getX(),0,offset.getZ());
                tag.put("pose",SableNBTUtils.writePose3d(pose));
                movePlotPositions(plot,offset);
            }
            SubLevelSchematicSerializationContext.setCurrentContext(context);
            for(int i=0;i<prepared.size();i++) {
                var tag=prepared.get(i);var ship=allocated.get(i);
                ship.getPlot().load(tag.getCompound("plot"));
                var pose=SableNBTUtils.readPose3d(tag.getCompound("pose"));ship.logicalPose().set(pose);
                container.physicsSystem().getPipeline().teleport(ship,pose.position(),pose.orientation());
                ship.updateLastPose();
                if(tag.contains("display_name")) ship.setName(tag.getString("display_name"));
                if(tag.contains("user_data")) ship.setUserDataTag(tag.getCompound("user_data"));
                ship.updateBoundingBox();ship.forceUpdateGlobalBounds();
            }
            for(var value:data.getList("Entities",Tag.TAG_COMPOUND)) {
                var tag=((CompoundTag)value).copy();PocketBottleItem.renewEntityIds(tag);
                var entity=EntityType.loadEntityRecursive(tag,level,e->e);
                if(entity==null || entity.getSelfAndPassengers().anyMatch(e->e instanceof Player || com.mcmagic.omnira.item.bottle.BottleCaptureRules.forbidden(e))) throw new IllegalStateException("Unsupported structure passenger");
                for(var member:entity.getSelfAndPassengers().toList()) {
                    var local=BlockPos.containing(member.position());
                    for(int i=0;i<tags.size();i++) {
                        var plot=tags.getCompound(i).getCompound("plot");int shift=container.getLogPlotSize()+4;
                        if((local.getX()>>shift)-container.getOrigin().x==plot.getInt("plot_x")
                                && (local.getZ()>>shift)-container.getOrigin().y==plot.getInt("plot_z")) {
                            var offset=offsets.get(i);member.setPos(member.position().add(offset.getX(),0,offset.getZ()));break;
                        }
                    }
                    member.setDeltaMovement(Vec3.ZERO);member.fallDistance=0;
                }
                entities.add(entity);
            }
            for(var entity:entities) if(!level.tryAddFreshEntityWithPassengers(entity)) throw new IllegalStateException("Could not restore structure entities");
            return true;
        } catch(RuntimeException error) {
            for(var entity:entities) for(var member:entity.getSelfAndPassengers().toList()) member.discard();
            for(var ship:allocated) if(!ship.isRemoved()) container.removeSubLevel(ship,SubLevelRemovalReason.REMOVED);
            com.mcmagic.omnira.Omnira.LOGGER.error("Structure release failed; retaining filled bottle",error);
            return false;
        } finally {SubLevelSchematicSerializationContext.setCurrentContext(previous);}
    }
    private static boolean clear(ServerLevel level,AABB target) {
        if(target.minY<level.getMinBuildHeight() || target.maxY>=level.getMaxBuildHeight() || !level.getWorldBorder().isWithinBounds(target)) return false;
        for(int x=((int)Math.floor(target.minX))>>4;x<=((int)Math.floor(target.maxX))>>4;x++)
            for(int z=((int)Math.floor(target.minZ))>>4;z<=((int)Math.floor(target.maxZ))>>4;z++) if(!level.hasChunk(x,z)) return false;
        return level.noCollision(target) && !SubLevelContainer.getContainer(level).queryIntersecting(new BoundingBox3d(target)).iterator().hasNext();
    }
    private static void movePlotPositions(CompoundTag plot,BlockPos offset) {
        var chunks=plot.getCompound("chunks");
        for(String key:chunks.getAllKeys()) {
            var chunk=chunks.getCompound(key);
            for(String list:List.of("block_entities","block_ticks","fluid_ticks"))
                for(var value:chunk.getList(list,Tag.TAG_COMPOUND)) {
                    var tag=(CompoundTag)value;
                    tag.putInt("x",tag.getInt("x")+offset.getX());tag.putInt("z",tag.getInt("z")+offset.getZ());
                }
        }
    }
}
