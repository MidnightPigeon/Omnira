package com.mcmagic.omnira.throne;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.vehicle.CruiseOrbEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class GoldenToilet {
    private static final String KEY="OmniraGoldenToilet";
    private GoldenToilet(){}

    public static void sit(Player player,BlockPos pos){
        if(!(player instanceof ServerPlayer server) || player.isPassenger()
                || !server.level().getBlockState(pos).is(ModBlocks.GOLDEN_TOILET.get()))return;
        GoldenThroneSeat seat=ModEntityTypes.GOLDEN_THRONE_SEAT.get().create(server.level());
        if(seat==null)return;
        seat.place(pos);server.level().addFreshEntity(seat);
        if(!server.startRiding(seat,true)){seat.discard();return;}
        server.setPos(seat.getX(),seat.getY()+.05,seat.getZ());
        server.getPersistentData().putBoolean(KEY,true);
        refresh(server);
    }
    public static boolean empowered(ServerPlayer player){
        return player.getVehicle() instanceof CruiseOrbEntity orb && orb.storage.hasGoldenToilet()
                || player.getPersistentData().getBoolean(KEY) && player.getVehicle() instanceof GoldenThroneSeat seat
                && seat.isToilet() && player.level().getBlockState(seat.anchor()).is(ModBlocks.GOLDEN_TOILET.get());
    }
    private static void clear(ServerPlayer player){
        player.getPersistentData().remove(KEY);
        GoldenThrone.clearSeatEffect(player,MobEffects.REGENERATION,1);
        GoldenThrone.clearSeatEffect(player,MobEffects.SATURATION,0);
    }
    public static void release(ServerPlayer player){
        if(player.getVehicle() instanceof GoldenThroneSeat seat && seat.isToilet()){
            player.stopRiding();seat.discard();
        }
        clear(player);
    }
    private static void refresh(ServerPlayer player){
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION,21,1,true,false));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION,21,0,true,false));
    }
    @SubscribeEvent public static void mount(EntityMountEvent event){
        if(event.isDismounting() && event.getEntityMounting() instanceof ServerPlayer player
                && (event.getEntityBeingMounted() instanceof GoldenThroneSeat seat && seat.isToilet()
                || event.getEntityBeingMounted() instanceof CruiseOrbEntity orb && orb.storage.hasGoldenToilet()))clear(player);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(event.getEntity() instanceof ServerPlayer player && player.getPersistentData().getBoolean(KEY))release(player);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer player))return;
        if(!player.getPersistentData().getBoolean(KEY) && !(player.getVehicle() instanceof CruiseOrbEntity orb && orb.storage.hasGoldenToilet()))return;
        if(!player.isAlive() || !empowered(player)){release(player);return;}
        if(player.tickCount%20==0)refresh(player);
    }
}
