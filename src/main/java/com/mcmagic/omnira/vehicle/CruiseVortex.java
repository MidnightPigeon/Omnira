package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

public final class CruiseVortex {
    public static final int DURATION=160, HALF=80;
    public static final double MAX_RADIUS=4;
    public static double radius(double remaining){return MAX_RADIUS*Math.clamp(Math.min(DURATION-remaining,remaining)/HALF,0,1);}
    public static final class Active {
        final ServerPlayer caster;
        final java.util.Set<java.util.UUID> pushed=new java.util.HashSet<>();
        final java.util.Set<java.util.UUID> drops=new java.util.HashSet<>();
        final java.util.Set<BlockPos> swept=new java.util.HashSet<>();
        public Active(ServerPlayer caster){this.caster=caster;}
    }
    private CruiseVortex() {}

    public static boolean activate(CruiseOrbEntity orb,ServerPlayer player) {
        if(!(orb.level() instanceof ServerLevel level) || player.level()!=level || !orb.isAlive()
                || !player.isAlive() || player.isSpectator() || !player.mayBuild()
                || orb.getFirstPassenger()!=player || orb.vortexTicks()>0)return false;
        var mana=player.getData(ModAttachments.MANA);
        if(!mana.canSpend(300))return false;
        player.setData(ModAttachments.MANA,mana.spend(300));
        // Set the cooldown before calling protection hooks, which may invoke other actions.
        orb.beginVortex(player);
        return true;
    }

    public static void tick(CruiseOrbEntity orb,Active active){
        if(!(orb.level() instanceof ServerLevel level) || active==null)return;
        var player=active.caster;
        if(!player.isAlive() || player.level()!=level || player.isSpectator())return;
        int remaining=orb.vortexTicks();boolean expanding=remaining>HALF;
        double radius=radius(remaining-1);
        var center=orb.position().add(0,1.5,0);var blockCenter=BlockPos.containing(center);
        var cube=new AABB(center,center).inflate(MAX_RADIUS);
        if(expanding || remaining%4==0 || remaining==1)
        for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++) {
            var pos=blockCenter.offset(x,y,z);
            double distance=net.minecraft.world.phys.Vec3.atCenterOf(pos).distanceToSqr(center);
            if(distance>MAX_RADIUS*MAX_RADIUS || expanding && (distance>radius*radius || !active.swept.add(pos)))continue;
            if(!level.hasChunkAt(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || level.isOutsideBuildHeight(pos))continue;
            var state=level.getBlockState(pos);
            if(expanding && state.is(com.mcmagic.omnira.registry.ModBlocks.TIME_WARP_POINT.get())){
                if(orb.storage.fitsItems(java.util.List.of(com.mcmagic.omnira.registry.ModItems.TIME_WARP_POINT.get().getDefaultInstance()))
                        && allowed(player,level,pos,state))
                    com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(level,pos,player);
                continue;
            }
            if(state.isAir() || state.hasBlockEntity() || level.getBlockEntity(pos)!=null
                    || state.getDestroySpeed(level,pos)<0 || state.getMenuProvider(level,pos)!=null
                    || PoiTypes.hasPoi(state))continue;
            // A source inside a plant or waterlogged structure is not a standalone liquid block.
            // Preserve it together with its containing block, avoiding accidental structure deletion.
            if(!state.getFluidState().isEmpty()) {
                if(!expanding && distance>=radius*radius && state.getBlock() instanceof LiquidBlock && state.getFluidState().isSource())
                    collectSource(orb,player,level,pos,state);
                continue;
            }
            if(!expanding || Block.isShapeFullBlock(state.getShape(level,pos)))continue;
            if(!allowed(player,level,pos,state))continue;
            var drops=Block.getDrops(state,level,pos,null,player,ItemStack.EMPTY);
            if(!orb.storage.fitsItems(drops) || !level.getBlockState(pos).equals(state))continue;
            if(!level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL))continue;
            // Empty-hand loot naturally omits shears-only vines and similar plants.
            for(var drop:drops)Block.popResource(level,pos,drop);
        }
        for(var entity:level.getEntities(orb,cube,e->e.isAlive() && !e.isSpectator() && !e.isPassengerOfSameVehicle(orb)
                && (e.isPushable() || e instanceof net.minecraft.world.entity.LivingEntity || e instanceof ItemEntity
                    || e instanceof net.minecraft.world.entity.projectile.Projectile || e.canBeCollidedWith()))){
            var delta=entity.getBoundingBox().getCenter().subtract(center);double distance=delta.length();
            if(distance<.001 || distance>MAX_RADIUS || entity.isPassenger())continue;
            if(expanding){
                if(distance>radius || !active.pushed.add(entity.getUUID()))continue;
                if(entity instanceof ItemEntity)active.drops.add(entity.getUUID());
                entity.setDeltaMovement(entity.getDeltaMovement().add(delta.scale(.12/distance)));
            }else{
                if(entity instanceof ItemEntity){active.drops.add(entity.getUUID());continue;}
                double toward=entity.getDeltaMovement().dot(delta.scale(-1/distance));
                if(toward<.12)entity.setDeltaMovement(entity.getDeltaMovement().add(delta.scale(-.006/distance)));
            }
            entity.hasImpulse=true;entity.hurtMarked=true;
        }
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))
            com.mcmagic.omnira.compat.SableVortex.apply(level,center,expanding,radius,active.pushed);
        if(expanding)return;
        for(var item:level.getEntitiesOfClass(ItemEntity.class,cube))
            if(item.position().distanceToSqr(center)<=MAX_RADIUS*MAX_RADIUS)active.drops.add(item.getUUID());
        for(var iterator=active.drops.iterator();iterator.hasNext();) {
            if(!(level.getEntity(iterator.next()) instanceof ItemEntity item) || !item.isAlive()) {iterator.remove();continue;}
            double distance=item.position().distanceToSqr(center);
            // Retain swept drops after our outward impulse, but never chase distant/teleported items.
            if(distance>4*MAX_RADIUS*MAX_RADIUS){iterator.remove();continue;}
            var delta=center.subtract(item.getBoundingBox().getCenter());
            if(delta.lengthSqr()>.000001){
                // Swallow drops immediately toward the center, canceling drift and compensating gravity.
                var motion=delta.normalize().scale(Math.min(.75,delta.length()))
                        .add(0,item.isNoGravity()?0:.04,0);
                item.setDeltaMovement(motion);item.hasImpulse=true;item.hurtMarked=true;
            }
            if(distance>1 && distance<radius*radius
                    || item.getTarget()!=null && !item.getTarget().equals(player.getUUID())
                    || !level.hasChunkAt(item.blockPosition())
                    || !level.mayInteract(player,item.blockPosition()))continue;
            if(!orb.storage.items.isItemValid(0,item.getItem()))continue;
            var pickup=new ItemEntityPickupEvent.Pre(player,item);
            NeoForge.EVENT_BUS.post(pickup);
            if(pickup.canPickup()==TriState.FALSE
                    || pickup.canPickup()==TriState.DEFAULT && item.hasPickUpDelay())continue;
            // Protection callbacks may change the entity or its stack; recheck before moving anything.
            if(!item.isAlive() || item.level()!=level || item.position().distanceToSqr(center)>4*MAX_RADIUS*MAX_RADIUS
                    || item.getTarget()!=null && !item.getTarget().equals(player.getUUID())
                    || !level.hasChunkAt(item.blockPosition()) || !level.mayInteract(player,item.blockPosition())
                    || !orb.storage.items.isItemValid(0,item.getItem()))continue;
            var original=item.getItem();var rest=orb.storage.insert(original,false);
            if(rest.getCount()==original.getCount())continue;
            if(rest.isEmpty())item.discard();else item.setItem(rest);
        }
    }

    private static boolean allowed(ServerPlayer player,ServerLevel level,BlockPos pos,BlockState state) {
        if(!player.mayBuild() || !level.mayInteract(player,pos)
                || !player.mayUseItemAt(pos,Direction.UP,ItemStack.EMPTY))return false;
        var event=new BlockEvent.BreakEvent(level,pos,state,player);
        NeoForge.EVENT_BUS.post(event);
        // Hooks may replace the block while deciding permission.
        return !event.isCanceled() && level.getBlockState(pos).equals(state);
    }

    private static void collectSource(CruiseOrbEntity orb,ServerPlayer player,ServerLevel level,BlockPos pos,BlockState state) {
        var fluid=new FluidStack(state.getFluidState().getType(),1000);var tank=orb.storage.tank;
        boolean compatible=tank.isFluidValid(fluid);
        if(compatible && tank.fill(fluid,FluidAction.SIMULATE)!=1000)return;
        if(!allowed(player,level,pos,state))return;
        // Recheck after callbacks so a newly filled tank cannot consume a source without room.
        compatible=tank.isFluidValid(fluid);
        if(compatible && tank.fill(fluid,FluidAction.SIMULATE)!=1000)return;
        if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL) && compatible)
            tank.fill(fluid,FluidAction.EXECUTE);
    }
}
