package com.mcmagic.omnira.travel;

import com.mcmagic.omnira.block.WaymarkBlock;
import com.mcmagic.omnira.block.entity.WaymarkBlockEntity;
import com.mcmagic.omnira.item.RecallCrystalItem;
import com.mcmagic.omnira.mana.ManaCosts;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;
import com.mcmagic.omnira.world.dimension.DreamTransitRules;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class RecallTravel {
    public static final int DURATION=60;
    private record Channel(UUID target,ServerLevel source,Vec3 origin,Entity root,InteractionHand hand,ItemStack stack,long started) {}
    private static final Map<UUID,Channel> CHANNELS=new HashMap<>();
    public static void message(ServerPlayer player,String key) {player.displayClientMessage(Component.translatable("message.omnira.recall."+key),true);}
    public static void cancel(ServerPlayer player,String reason) {
        if(CHANNELS.remove(player.getUUID())!=null) {
            player.setData(ModAttachments.RECALL_TICKS,0);
            if(reason!=null) message(player,reason);
        }
    }
    public static boolean start(ServerPlayer player,InteractionHand hand,UUID target) {
        if(!player.isAlive() || player.isSpectator() || !DreamTransitRules.allowed(player.getRootVehicle()) || player.isSleeping()
                || !(player.getItemInHand(hand).getItem() instanceof RecallCrystalItem crystal)) return false;
        var directory=WaymarkDirectory.get(player.serverLevel());
        var entry=directory.knows(player.getUUID(),target)?directory.get(target):null;
        if(entry==null) {message(player,"missing");return false;}
        double cost=ManaCosts.cost(player,crystal.baseCost(!entry.dimension().equals(player.level().dimension().location())));
        if(!player.getData(ModAttachments.MANA).canSpend(cost)) {message(player,"mana");return false;}
        var root=player.getRootVehicle();
        CHANNELS.put(player.getUUID(),new Channel(target,player.serverLevel(),root.position(),root,hand,player.getItemInHand(hand),player.level().getGameTime()));
        player.setData(ModAttachments.RECALL_TICKS,DURATION);
        return true;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {if(event.getEntity() instanceof ServerPlayer player) advance(player);}
    public static void advance(ServerPlayer player) {
        var channel=CHANNELS.get(player.getUUID());if(channel==null) return;
        if(!player.isAlive() || player.isSpectator() || player.getRootVehicle()!=channel.root() || player.isSleeping() || player.serverLevel()!=channel.source()
                || channel.root().position().distanceToSqr(channel.origin())>0.0001 || player.getItemInHand(channel.hand())!=channel.stack()) {
            cancel(player,"interrupted");return;
        }
        long elapsed=player.level().getGameTime()-channel.started();
        if(elapsed<DURATION) {player.setData(ModAttachments.RECALL_TICKS,(int)(DURATION-elapsed));return;}
        cancel(player,null);
        complete(player,channel);
    }
    private static void complete(ServerPlayer player,Channel channel) {
        var directory=WaymarkDirectory.get(player.serverLevel());var entry=directory.get(channel.target());
        if(!directory.knows(player.getUUID(),channel.target())) {message(player,"missing");return;}
        if(entry==null) {message(player,"missing");return;}
        ServerLevel destination=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,entry.dimension()));
        if(destination==null) {message(player,"missing");return;}
        destination.getChunkAt(entry.pos());
        boolean deathMark=com.mcmagic.omnira.spacetime.AmberDirectory.get(destination).owns(player.getUUID(),entry.id());
        if(!deathMark && (!(destination.getBlockEntity(entry.pos()) instanceof WaymarkBlockEntity mark) || !mark.id().equals(entry.id())
                || !destination.getBlockState(entry.pos().above()).is(ModBlocks.WAYMARK.get())
                || destination.getBlockState(entry.pos().above()).getValue(WaymarkBlock.HALF)!=DoubleBlockHalf.UPPER)) {
            directory.remove(entry.id());message(player,"missing");return;
        }
        Vec3 landing=findLanding(destination,player,entry.pos());
        if(landing==null) {message(player,"blocked");return;}
        var root=channel.root();
        if(!DreamTransitRules.allowed(root)){message(player,"blocked");return;}
        if(destination!=channel.source())for(var member:root.getSelfAndPassengers().toList())
            if(!member.canChangeDimensions(channel.source(),destination)
                    || !net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(member,destination.dimension())){
                message(player,"blocked");return;
            }
        var crystal=(RecallCrystalItem)channel.stack().getItem();
        double cost=ManaCosts.cost(player,crystal.baseCost(destination!=channel.source()));
        var mana=player.getData(ModAttachments.MANA);
        if(!mana.canSpend(cost)) {message(player,"mana");return;}
        player.setData(ModAttachments.MANA,mana.spend(cost));
        boolean success=false;
        try {
            if(root==player){
                success=player.teleportTo(destination,landing.x,landing.y,landing.z,Set.of(),player.getYRot(),player.getXRot());
                success=success && player.serverLevel()==destination && player.position().distanceToSqr(landing)<.01;
            }else{
                var arrived=root.changeDimension(new DimensionTransition(destination,landing,Vec3.ZERO,root.getYRot(),root.getXRot(),entity->{
                    entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;entity.placePortalTicket(entity.blockPosition());
                }));
                success=arrived!=null && player.getRootVehicle()==arrived && player.serverLevel()==destination
                        && arrived.position().distanceToSqr(landing)<.01;
            }
        } finally {
            if(!success) {var current=player.getData(ModAttachments.MANA);player.setData(ModAttachments.MANA,current.withCurrent(current.current()+cost));}
        }
        if(!success) {message(player,"blocked");return;}
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        destination.playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.7F,1.3F);
        destination.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,landing.x,landing.y+1,landing.z,12,.3,.5,.3,.02);
    }
    public static Vec3 findLanding(ServerLevel level,ServerPlayer player,BlockPos base) {
        var root=player.getRootVehicle();
        double lowest=root.getY();
        for(var passenger:root.getIndirectPassengers())lowest=Math.min(lowest,passenger.getBoundingBox().minY);
        double lift=root.getY()-lowest;
        for(int radius=1;radius<=(root==player?3:6);radius++) for(int dy:new int[]{0,1,-1,2,-2})
            for(int x=-radius;x<=radius;x++) for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius) continue;
                var feet=base.offset(x,dy,z);var floor=feet.below();
                if(feet.getY()<level.getMinBuildHeight()+1 || feet.getY()+2>=level.getMaxBuildHeight()) continue;
                level.getChunkAt(feet);
                var support=level.getBlockState(floor);
                if(!support.isFaceSturdy(level,floor,Direction.UP) || dangerous(support) || !level.getFluidState(floor).isEmpty()) continue;
                if(dangerous(level.getBlockState(feet)) || dangerous(level.getBlockState(feet.above()))
                        || !level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) continue;
                var point=Vec3.atBottomCenterOf(feet).add(0,lift,0);
                var box=root.getDimensions(root==player?net.minecraft.world.entity.Pose.STANDING:root.getPose()).makeBoundingBox(point);
                for(var passenger:root.getIndirectPassengers())box=box.minmax(passenger.getBoundingBox().move(point.subtract(root.position())));
                level.getChunkAt(BlockPos.containing(box.minX,box.minY,box.minZ));
                level.getChunkAt(BlockPos.containing(box.maxX,box.maxY,box.maxZ));
                if(box.minY<level.getMinBuildHeight() || box.maxY>=level.getMaxBuildHeight())continue;
                boolean hazard=false;
                for(var at:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ)))
                    if(dangerous(level.getBlockState(at)) || !level.getFluidState(at).isEmpty()){hazard=true;break;}
                if(hazard)continue;
                if(level.getWorldBorder().isWithinBounds(box) && level.noCollision(box)
                        && level.getEntities(root,box,e->e.isAlive() && !e.isSpectator() && e.getRootVehicle()!=root).isEmpty()) return point;
            }
        return null;
    }
    private static boolean dangerous(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(BlockTags.FIRE) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CACTUS) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.getBlock() instanceof CampfireBlock;
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event) {
        if(event.getNewDamage()>0 && event.getEntity() instanceof ServerPlayer player) cancel(player,"interrupted");
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {if(event.getEntity() instanceof ServerPlayer player) cancel(player,null);}
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {CHANNELS.clear();}
}
