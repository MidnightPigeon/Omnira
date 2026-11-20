package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.combat.OmniraDamage;
import com.mcmagic.omnira.item.*;
import com.mcmagic.omnira.mana.ManaEvents;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.weaver.WeaverCombat;
import com.mcmagic.omnira.network.ChaosBeamState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class ChaosBeam {
    public static final int TRAVEL_TICKS=6;
    private record Channel(ItemStack carrier, InteractionHand hand, int slot, ItemStack grid, boolean wheel, boolean spendsCrystal,
                           int started, int range, int radius, int strength, double power, double cost, double areaScale, boolean abyss) {}
    private static final Map<UUID,Channel> ACTIVE = new HashMap<>();
    public static boolean active(ServerPlayer player){return ACTIVE.containsKey(player.getUUID());}
    public static void stop(ServerPlayer player){
        var channel=ACTIVE.remove(player.getUUID());
        if(channel==null)return;
        if(channel.spendsCrystal)channel.carrier.shrink(1);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ChaosBeamState(player.getUUID(),channel.hand,0,0));
    }
    public static void beforeDrop(ServerPlayer p,ItemStack stack){
        var channel=ACTIVE.get(p.getUUID());
        if(channel!=null && channel.carrier==stack && channel.spendsCrystal)stop(p);
    }
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void died(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){
        if(e.getEntity() instanceof ServerPlayer p)stop(p);
    }
    private static ChaosBeamState visual(ServerPlayer p,Channel c){
        long launched=p.level().getGameTime()-Math.floorMod(p.tickCount-c.started,20);
        return new ChaosBeamState(p.getUUID(),c.hand,c.range,launched);
    }
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){
        if(e.getTarget() instanceof ServerPlayer caster && e.getEntity() instanceof ServerPlayer observer){
            var channel=ACTIVE.get(caster.getUUID());
            if(channel==null)return;
            PacketDistributor.sendToPlayer(observer,visual(caster,channel));
        }
    }
    public static void release(ServerPlayer player,int session){
        var channel=ACTIVE.get(player.getUUID());
        if(channel!=null && channel.started==session && !channel.wheel)stop(player);
    }

    public static boolean start(ServerPlayer p, InteractionHand hand, ItemStack grid, SpellPayload payload, double power, double cost) {
        return start(p,hand,grid,payload,power,cost,1);
    }

    public static boolean start(ServerPlayer p, InteractionHand hand, ItemStack grid, SpellPayload payload, double power, double cost, double rangeScale) {
        if(active(p) || !p.isAlive() || p.isSpectator() || !ManaEvents.trySpendExact(p,cost))return false;
        var held=p.getItemInHand(hand);
        boolean wheel=held.getItem() instanceof IndustrialWheelItem;
        var source=grid.isEmpty()?held:grid;
        power=com.mcmagic.omnira.fate.FateEffects.spellPower(p,power+com.mcmagic.omnira.fate.ArcanistFate.bonus(p,source));
        com.mcmagic.omnira.fate.ArcanistFate.completed(p,source);
        boolean spendsCrystal=grid.isEmpty() && held.getItem() instanceof LowTierMagicCrystalItem && !p.getAbilities().instabuild;
        var keys=payload.keywords();
        var channel=new Channel(p.getItemInHand(hand),hand,p.getInventory().selected,grid,wheel,spendsCrystal,p.tickCount,
                (int)Math.round(32*(1+Collections.frequency(keys,"delay"))*rangeScale),Collections.frequency(keys,"infusion"),
                Collections.frequency(keys,"enhancement"),power,cost,rangeScale,keys.contains("omnira:abyss_tip"));
        ACTIVE.put(p.getUUID(),channel);
        if(!wheel)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p,new com.mcmagic.omnira.network.ChaosBeamInput(channel.started));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,visual(p,channel));
        return true;
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p))return;
        var channel=ACTIVE.get(p.getUUID());
        if(channel==null)return;
        boolean sameHand=p.getItemInHand(channel.hand)==channel.carrier ||
                channel.carrier.isEmpty() && p.getItemInHand(channel.hand).isEmpty();
        if(!p.isAlive() || p.isSpectator() || p.containerMenu!=p.inventoryMenu || !sameHand
                || channel.hand==InteractionHand.MAIN_HAND && channel.slot!=p.getInventory().selected
                || !channel.grid.isEmpty() && CrystalGridMenu.locate(p,-2)!=channel.grid){
            end(p,channel);return;
        }
        int age=p.tickCount-channel.started;
        if(age<=0)return;
        if(age%20==0){
            if(!ManaEvents.trySpendExact(p,channel.cost)){end(p,channel);return;}
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(p,visual(p,channel));
        }else if(age%20==TRAVEL_TICKS)impact(p,channel);
    }
    private static void end(ServerPlayer p,Channel channel){
        stop(p);
        if(channel.wheel)IndustrialWheelItem.stop(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void shutdown(net.neoforged.neoforge.event.server.ServerStoppedEvent e){ACTIVE.clear();}

    private static void impact(ServerPlayer p,Channel channel){
        var hit=SpellCasting.target(p,channel.range,true,true);
        if(hit.getType()==HitResult.Type.MISS)return;
        applyImpact(p,channel,hit);
        com.mcmagic.omnira.mana.ChaosAffinity.echo(p,player->applyImpact(player,channel,hit));
    }

    private static void applyImpact(ServerPlayer p,Channel channel,HitResult hit){
        var center=hit.getLocation();
        var level=p.serverLevel();
        var living=new LinkedHashSet<LivingEntity>();
        var blocks=new LinkedHashSet<BlockPos>();
        if(hit instanceof EntityHitResult entity)living.add(SpellCasting.livingTarget(entity.getEntity()));
        if(hit instanceof BlockHitResult block && !level.isEmptyBlock(block.getBlockPos()))blocks.add(block.getBlockPos());
        int radius=channel.radius;
        if(radius>0){
            // Count the struck cell itself toward the diameter (3, 5, 7 ...).
            var origin=hit instanceof BlockHitResult block ? block.getBlockPos() : BlockPos.containing(center);
            var blastCenter=origin.getCenter();
            double reach=(radius+.5)*channel.areaScale;
            int extent=(int)Math.ceil(reach);
            WeaverCombat.visual(level,0,blastCenter,blastCenter,(float)reach);
            var bounds=new AABB(blastCenter,blastCenter).inflate(reach);
            living.addAll(level.getEntitiesOfClass(LivingEntity.class,bounds,e->e!=p && e.isAlive() && !e.isSpectator()
                    && e.getBoundingBox().distanceToSqr(blastCenter)<=reach*reach));
            for(var pos:BlockPos.betweenClosed(origin.offset(-extent,-extent,-extent),origin.offset(extent,extent,extent)))
                if(pos.getCenter().distanceToSqr(blastCenter)<=reach*reach && level.hasChunkAt(pos) && !level.isEmptyBlock(pos))
                    blocks.add(pos.immutable());
        }
        int failures=0;
        for(var target:living){
            if(target==null || !target.isAlive())continue;
            boolean damaged=OmniraDamage.trueDamage(target,(float)((5+3*channel.strength)*channel.power),SpellDamageSource.of(target,net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC,p,p,"spell"));
            if(damaged && channel.abyss)com.mcmagic.omnira.compat.AbyssTipEffects.apply(target,p);
            if(target.isAlive())failures++;
        }
        int tier=2+channel.strength+(int)Math.floor(Math.max(0,channel.power-1)*4+1e-9);
        var tool=new ItemStack(tier==2?Items.IRON_PICKAXE:tier==3?Items.DIAMOND_PICKAXE:Items.NETHERITE_PICKAXE);
        for(var pos:blocks)if(!destroy(p,pos,tool))failures++;
        for(int i=0;i<failures && p.isAlive();i++)backlash(p);
    }
    private static boolean destroy(ServerPlayer p,BlockPos pos,ItemStack tool){
        var level=p.serverLevel();
        if(!UtilitySpellEffects.allowed(p,pos))return false;
        var state=level.getBlockState(pos);
        if(state.isAir() || state.getDestroySpeed(level,pos)<0 || state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state))return false;
        if(CommonHooks.fireBlockBreak(level,p.gameMode.getGameModeForPlayer(),p,pos,state).isCanceled())return false;
        var be=level.getBlockEntity(pos);
        if(!level.removeBlock(pos,false))return false;
        level.levelEvent(2001,pos,Block.getId(state));
        state.getBlock().playerDestroy(level,p,pos,state,be,tool.copy());
        return true;
    }
    public static void backlash(ServerPlayer player){
        var mana=player.getData(ModAttachments.MANA);
        double amount=Math.max(10,mana.current()*.1);
        player.setData(ModAttachments.MANA,mana.withCurrent(Math.max(0,mana.current()-amount)));
        if(amount>mana.current())OmniraDamage.overdraw(player);
        else if(!com.mcmagic.omnira.mana.ChaosAffinity.active(player))
            OmniraDamage.trueDamage(player,2,player.damageSources().indirectMagic(player,player));
    }
    private ChaosBeam(){}
}
