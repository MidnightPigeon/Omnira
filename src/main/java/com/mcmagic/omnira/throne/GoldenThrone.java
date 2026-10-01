package com.mcmagic.omnira.throne;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.block.GoldenThroneBlock;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class GoldenThrone {
    private static final String KEY="OmniraGoldenThrone";
    public static final int LOCK_TICKS=12000;
    private static final ThreadLocal<Boolean> RELEASING=ThreadLocal.withInitial(()->false);
    private GoldenThrone(){}

    public static void sit(Player player,BlockPos pos){
        if(!(player instanceof ServerPlayer server) || player.isPassenger()
                || !server.level().getBlockState(pos).is(ModBlocks.GOLDEN_THRONE.get()))return;
        GoldenThroneSeat seat=com.mcmagic.omnira.registry.ModEntityTypes.GOLDEN_THRONE_SEAT.get().create(server.level());
        if(seat==null)return;
        seat.place(pos);
        server.level().addFreshEntity(seat);
        if(!server.startRiding(seat,true)){seat.discard();return;}
        server.setPos(seat.getX(),seat.getY()+.05,seat.getZ());
        CompoundTag lock=new CompoundTag();
        lock.putLong("until",server.level().getGameTime()+LOCK_TICKS);
        lock.putLong("pos",pos.asLong());lock.putString("dimension",server.level().dimension().location().toString());
        server.getPersistentData().put(KEY,lock);
    }

    private static boolean active(ServerPlayer player){
        CompoundTag lock=player.getPersistentData().getCompound(KEY);
        return lock.contains("until") && lock.getLong("until")>player.level().getGameTime()
                && lock.getString("dimension").equals(player.level().dimension().location().toString())
                && player.level().getBlockState(BlockPos.of(lock.getLong("pos"))).is(ModBlocks.GOLDEN_THRONE.get());
    }
    public static boolean empowered(ServerPlayer player){return active(player) && player.getVehicle() instanceof GoldenThroneSeat;}

    public static void release(ServerPlayer player){
        player.getPersistentData().remove(KEY);
        if(player.getVehicle() instanceof GoldenThroneSeat seat){
            RELEASING.set(true);
            try{player.stopRiding();seat.discard();}finally{RELEASING.set(false);}
        }
        clearSeatEffect(player,MobEffects.DAMAGE_RESISTANCE,4);
        clearSeatEffect(player,MobEffects.REGENERATION,4);
        clearSeatEffect(player,MobEffects.SATURATION,0);
    }
    static void clearSeatEffect(ServerPlayer player,net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,int amplifier){
        var current=player.getEffect(effect);
        if(current!=null && current.getAmplifier()==amplifier && current.isAmbient()
                && !current.isVisible() && current.getDuration()<=21)player.removeEffect(effect);
    }

    @SubscribeEvent public static void mount(EntityMountEvent event){
        if(RELEASING.get())return;
        if(event.getEntityMounting() instanceof ServerPlayer player && player.isAlive() && empowered(player)
                && (event.isDismounting() && event.getEntityBeingMounted() instanceof GoldenThroneSeat
                || event.isMounting()))event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player && player.getVehicle() instanceof GoldenThroneSeat){
            var seat=player.getVehicle();RELEASING.set(true);
            try{player.stopRiding();seat.discard();}finally{RELEASING.set(false);}
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player) || !player.getPersistentData().contains(KEY))return;
        CompoundTag lock=player.getPersistentData().getCompound(KEY);
        if(lock.contains("until") && lock.getLong("until")<=player.level().getGameTime()
                && lock.getString("dimension").equals(player.level().dimension().location().toString()))
            GoldenThroneCollapse.queue(player.serverLevel(),BlockPos.of(lock.getLong("pos")));
        if(!player.isAlive() || !active(player)){
            release(player);
            return;
        }
        if(player.getVehicle() instanceof GoldenThroneSeat seat
                && player.position().distanceToSqr(seat.getX(),seat.getY()+.05,seat.getZ())>1){
            release(player);
            return;
        }
        if(!empowered(player)){
            BlockPos pos=BlockPos.of(player.getPersistentData().getCompound(KEY).getLong("pos"));
            if(player.blockPosition().distSqr(pos)>4){release(player);return;}
            sitExisting(player,pos);
        }
        if(empowered(player) && player.tickCount%20==0){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,21,4,true,false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,21,4,true,false));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION,21,0,true,false));
        }
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(KEY))release(player);
    }
    private static void sitExisting(ServerPlayer player,BlockPos pos){
        GoldenThroneSeat seat=com.mcmagic.omnira.registry.ModEntityTypes.GOLDEN_THRONE_SEAT.get().create(player.level());if(seat==null)return;
        seat.place(pos);player.level().addFreshEntity(seat);
        if(!player.startRiding(seat,true))seat.discard();
        else player.setPos(seat.getX(),seat.getY()+.05,seat.getZ());
    }
}
