package com.mcmagic.omnira.dream;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.travel.RecallTravel;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid="omnira")
public final class DreamRabbitTravel {
    public static final int DURATION=60,COOLDOWN=1200;
    private static final ResourceKey<net.minecraft.world.level.biome.Biome> MIRROR=ResourceKey.create(Registries.BIOME,ResourceLocation.parse("omnira:mirror_dream_border"));
    private record Channel(ServerLevel source,Vec3 origin,InteractionHand hand,ItemStack stack,long started,boolean sigil){}
    private static final Map<UUID,Channel> CHANNELS=new HashMap<>();
    private DreamRabbitTravel(){}
    private static void message(ServerPlayer player,String key){player.displayClientMessage(Component.translatable("message.omnira.rabbit."+key),true);}
    public static boolean start(ServerPlayer player,InteractionHand hand,DreamRabbitItem item){
        if(!player.isAlive()||player.isSpectator()||player.isSleeping()||player.isPassenger()||CHANNELS.containsKey(player.getUUID()))return false;
        if(item.sigil()){
            if(player.getCooldowns().isOnCooldown(ModItems.ALICE_DREAM_RABBIT_SIGIL.get())){message(player,"cooldown");return false;}
            var mana=player.getData(ModAttachments.MANA).withMaximum(player.getAttributeValue(ModAttributes.MAX_MANA));
            if(mana.current()<mana.maximum()/2){player.setHealth(0);player.die(player.damageSources().genericKill());return false;}
        }
        if(!supported(player.level().dimension())){message(player,"dimension");return false;}
        CHANNELS.put(player.getUUID(),new Channel(player.serverLevel(),player.position(),hand,player.getItemInHand(hand),player.level().getGameTime(),item.sigil()));
        player.setData(ModAttachments.RABBIT_TICKS,DURATION);
        return true;
    }
    static boolean supported(ResourceKey<Level> dimension){return dimension==Level.OVERWORLD||dimension==ModDimensions.DREAM_REALM||dimension==ModDimensions.SPACETIME_CORRIDOR;}
    private static void cancel(ServerPlayer player){if(CHANNELS.remove(player.getUUID())!=null)player.setData(ModAttachments.RABBIT_TICKS,0);}
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        var channel=CHANNELS.get(player.getUUID());if(channel==null)return;
        if(!player.isAlive()||player.isSpectator()||player.isPassenger()||player.isSleeping()||player.serverLevel()!=channel.source()
                ||player.position().distanceToSqr(channel.origin())>.0001||player.getItemInHand(channel.hand())!=channel.stack()){
            cancel(player);return;
        }
        long elapsed=player.level().getGameTime()-channel.started();
        if(elapsed<DURATION){player.setData(ModAttachments.RABBIT_TICKS,(int)(DURATION-elapsed));
            if(elapsed%5==0)player.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,player.getX(),player.getY()+1,player.getZ(),8,.4,.7,.4,.03);
            return;}
        cancel(player);complete(player,channel);
    }
    private static void complete(ServerPlayer player,Channel channel){
        ResourceKey<Level> from=channel.source().dimension();
        ResourceKey<Level> to=from==Level.OVERWORLD?ModDimensions.DREAM_REALM:from==ModDimensions.DREAM_REALM?ModDimensions.SPACETIME_CORRIDOR:Level.OVERWORLD;
        ServerLevel destination=player.server.getLevel(to);
        if(destination==null){message(player,"blocked");return;}
        Vec3 landing=findLanding(destination,player,to);
        if(landing==null || !player.canChangeDimensions(channel.source(),destination)
                || !net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(player,to)){
            message(player,"blocked");return;
        }
        var mana=player.getData(ModAttachments.MANA).withMaximum(player.getAttributeValue(ModAttributes.MAX_MANA));
        if(channel.sigil() && mana.current()<mana.maximum()/2){player.setHealth(0);player.die(player.damageSources().genericKill());return;}
        if(!player.teleportTo(destination,landing.x,landing.y,landing.z,Set.of(),player.getYRot(),player.getXRot())
                ||player.serverLevel()!=destination){message(player,"blocked");return;}
        player.setDeltaMovement(Vec3.ZERO);player.fallDistance=0;
        if(channel.sigil()){
            player.setData(ModAttachments.MANA,mana.withCurrent(0));
            player.getCooldowns().addCooldown(ModItems.ALICE_DREAM_RABBIT_SIGIL.get(),COOLDOWN);
        }else channel.stack().shrink(1);
        destination.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,landing.x,landing.y+1,landing.z,32,.6,1,.6,.08);
        destination.playSound(null,player.blockPosition(),SoundEvents.ENDERMAN_TELEPORT,SoundSource.PLAYERS,.8F,1.1F);
    }
    private static Vec3 findLanding(ServerLevel level,ServerPlayer player,ResourceKey<Level> destination){
        if(destination==Level.OVERWORLD)return RecallTravel.findLanding(level,player,level.getSharedSpawnPos());
        if(destination==ModDimensions.SPACETIME_CORRIDOR){
            int z=level.random.nextInt(2049)-1024;
            return RecallTravel.findLanding(level,player,new BlockPos(0,129,z));
        }
        for(int attempt=0;attempt<24;attempt++){
            int x=level.random.nextInt(4097)-2048,z=level.random.nextInt(4097)-2048;
            var column=new BlockPos(x,0,z);
            // The dream's mirror stratum is vertical; the highest island can belong to a different biome.
            if(!level.getBiome(column.atY(112)).is(MIRROR))continue;
            level.getChunkAt(column);
            for(int y=132;y>=92;y--){
                var feet=column.atY(y);
                if(!level.getBiome(feet).is(MIRROR) || !level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP))continue;
                var landing=RecallTravel.findLanding(level,player,feet);
                if(landing!=null && level.getBiome(BlockPos.containing(landing)).is(MIRROR))return landing;
            }
        }
        return null;
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent.Post event){if(event.getNewDamage()>0&&event.getEntity() instanceof ServerPlayer player)cancel(player);}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)cancel(player);}
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){CHANNELS.clear();}
}
