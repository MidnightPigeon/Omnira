package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.ModEffects;
import com.mcmagic.omnira.item.StaffItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class ConstructionLock {
    static final String KEY="OmniraConstructionLock";
    public static boolean locked(LivingEntity entity) {
        return entity.hasEffect(ModEffects.CONSTRUCTION_LOCK) && entity.getPersistentData().getCompound(KEY).getLong("until")>entity.level().getGameTime();
    }
    public static void apply(LivingEntity target,SpellEffect effect) {
        apply(target,effect,null);
    }
    public static void apply(LivingEntity target,SpellEffect effect,ServerPlayer caster) {
        if(target.level().isClientSide||!target.isAlive()||ConstructionPrison.boss(target))return;
        if(!locked(target) && target.getPersistentData().contains(KEY)) tick(target);
        var tag=target.getPersistentData().getCompound(KEY);
        if(!locked(target)) {
            tag=new net.minecraft.nbt.CompoundTag();
            tag.putDouble("x",target.getX());tag.putDouble("y",target.getY());tag.putDouble("z",target.getZ());
            tag.putString("dimension",target.level().dimension().location().toString());
            tag.putBoolean("gravity",target.isNoGravity());
            if(target instanceof Mob mob) tag.putBoolean("no_ai",mob.isNoAi());
            target.stopRiding();
        }
        long until=Math.max(tag.getLong("until"),target.level().getGameTime()+100L*(1<<effect.delay()));
        tag.putLong("until",until);
        tag.putBoolean("mineable",tag.getBoolean("mineable") || effect.infusion()>0);
        tag.putInt("crush",Math.max(tag.getInt("crush"),effect.enhancement()));
        if(tag.getBoolean("mineable")) {
            int ticks=(int)(until-target.level().getGameTime());
            if(tag.hasUUID("seal"))ConstructionPrison.refresh(target,tag,ticks);
            else if(!ConstructionPrison.create(target,caster,tag,ticks))tag.putBoolean("mineable",false);
        }
        target.getPersistentData().put(KEY,tag);
        target.addEffect(new MobEffectInstance(ModEffects.CONSTRUCTION_LOCK,(int)(until-target.level().getGameTime()),tag.getBoolean("mineable")?1:0,false,true,true));
        target.setNoGravity(true);
        if(target instanceof Mob mob) {mob.setNoAi(true);mob.getNavigation().stop();}
        if(target instanceof ServerPlayer player) player.closeContainer();
        target.stopUsingItem();
        target.setDeltaMovement(Vec3.ZERO);
    }
    private static boolean tick(LivingEntity entity) {
        if(entity.level().isClientSide || !entity.getPersistentData().contains(KEY)) return false;
        var tag=entity.getPersistentData().getCompound(KEY);
        long now=entity.level().getGameTime();
        if(!tag.getString("dimension").equals(entity.level().dimension().location().toString()) || now>=tag.getLong("until") || !entity.hasEffect(ModEffects.CONSTRUCTION_LOCK) || !entity.isAlive()) {
            release(entity);return false;
        }
        Vec3 anchor=new Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z"));
        entity.setDeltaMovement(Vec3.ZERO);entity.fallDistance=0;entity.stopUsingItem();
        if(entity instanceof ServerPlayer player) {
            if(entity.position().distanceToSqr(anchor)>.0001) player.teleportTo(anchor.x,anchor.y,anchor.z);
        } else entity.setPos(anchor);
        if(tag.getInt("crush")>0 && now%20==0) entity.hurt(entity.damageSources().inWall(),tag.getInt("crush"));
        return true;
    }
    public static void release(LivingEntity entity) {
        if(!entity.getPersistentData().contains(KEY))return;
        var tag=entity.getPersistentData().getCompound(KEY);
        ConstructionPrison.clear(entity,tag);
        entity.setNoGravity(tag.getBoolean("gravity"));
        if(entity instanceof Mob mob)mob.setNoAi(tag.getBoolean("no_ai"));
        entity.getPersistentData().remove(KEY);entity.removeEffect(ModEffects.CONSTRUCTION_LOCK);
    }
    @SubscribeEvent public static void entityTick(EntityTickEvent.Pre event) {
        if(event.getEntity() instanceof LivingEntity living && !(living instanceof Player) && locked(living)) {
            living.setDeltaMovement(Vec3.ZERO);
            if(living instanceof Mob mob) {mob.setNoAi(true);mob.getNavigation().stop();}
            if(living instanceof net.minecraft.world.entity.monster.Creeper creeper) creeper.setSwellDir(-1);
        }
    }
    @SubscribeEvent public static void afterEntityTick(EntityTickEvent.Post event) {
        if(event.getEntity() instanceof LivingEntity living && !(living instanceof Player)) tick(living);
    }
    @SubscribeEvent public static void playerTick(PlayerTickEvent.Post event) {tick(event.getEntity());}
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if(locked(event.getEntity()) || event.getTarget() instanceof LivingEntity living && locked(living)
                && living.getPersistentData().getCompound(KEY).getBoolean("mineable")) event.setCanceled(true);
    }
    private static void preventInteraction(PlayerInteractEvent event) {
        if(locked(event.getEntity()) && event instanceof ICancellableEvent cancellable) cancellable.setCanceled(true);
    }
    @SubscribeEvent public static void rightBlock(PlayerInteractEvent.RightClickBlock event) {preventInteraction(event);}
    @SubscribeEvent public static void rightItem(PlayerInteractEvent.RightClickItem event) {preventInteraction(event);}
    @SubscribeEvent public static void rightEntity(PlayerInteractEvent.EntityInteract event) {preventInteraction(event);}
    @SubscribeEvent public static void rightEntityAt(PlayerInteractEvent.EntityInteractSpecific event) {preventInteraction(event);}
    @SubscribeEvent public static void breakBlock(BlockEvent.BreakEvent event) {
        if(locked(event.getPlayer()))event.setCanceled(true);
        else if(event.getLevel().getBlockEntity(event.getPos()) instanceof com.mcmagic.omnira.block.entity.VoidCrystalBlockEntity be&&be.prisoner()!=null
                &&(!event.getPlayer().getMainHandItem().is(net.minecraft.tags.ItemTags.PICKAXES)
                    ||!event.getPlayer().isCreative()&&!event.getPlayer().getMainHandItem().isCorrectToolForDrops(event.getState())))event.setCanceled(true);
    }
    @SubscribeEvent public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        var player=event.getEntity();
        if(locked(player)) {event.setCanceled(true);return;}
        if(!event.getLevel().isClientSide && player.getMainHandItem().getItem() instanceof StaffItem
                && (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.mcmagic.omnira.block.VoidCrystalBlock
                    || event.getLevel().getBlockState(event.getPos()).getBlock() instanceof com.mcmagic.omnira.block.PermanentVoidCrystalBlock)) {
            event.setCanceled(true);
            if(player instanceof ServerPlayer server && UtilitySpellEffects.allowed(server,event.getPos())
                    && !net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(server.serverLevel(),server.gameMode.getGameModeForPlayer(),
                    server,event.getPos(),event.getLevel().getBlockState(event.getPos())).isCanceled())
                event.getLevel().removeBlock(event.getPos(),false);
        }
    }
    /** Legacy packets cannot kill a creature without actually breaking its prison. */
    public static void mine(ServerPlayer player,int id) {}
}
