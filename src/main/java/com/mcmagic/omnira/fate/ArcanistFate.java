package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.item.FateCurioItem.Kind;
import com.mcmagic.omnira.registry.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Curse state is persistent fate data, never a milk-curable mob effect. */
@EventBusSubscriber(modid="omnira")
public final class ArcanistFate {
    public static final String KEY="Arcanist";
    public static final int PERIOD=6000,DURATION=300,WARNING=200;
    public static CompoundTag data(ServerPlayer p){
        var root=FateRuntime.data(p);
        if(!root.contains(KEY,10))root.put(KEY,new CompoundTag());
        return root.getCompound(KEY);
    }
    public static boolean storm(ServerPlayer p){
        if(ManuscriptReward.equipped(p,"curse")!=Kind.STORM)return false;
        var s=data(p);return s.getInt("Rain")>0||s.contains("RainTick")&&s.getLong("RainTick")==p.level().getGameTime();
    }
    private static String identity(net.minecraft.world.item.ItemStack carrier){
        if(carrier.isEmpty())return "";
        var id=carrier.get(ModDataComponents.ARCANIST_CARRIER);
        if(id==null){id=java.util.UUID.randomUUID();carrier.set(ModDataComponents.ARCANIST_CARRIER,id);}
        return id.toString();
    }
    public static double bonus(Player player,net.minecraft.world.item.ItemStack carrier){
        if(!(player instanceof ServerPlayer p)||ManuscriptReward.equipped(p,"talent")!=Kind.ARCANIST||storm(p))return 0;
        var s=data(p);
        return !carrier.isEmpty()&&s.getString("Carrier").equals(identity(carrier))?Math.floorMod(s.getInt("Casts"),3)*.25:0;
    }
    public static void completed(Player player,net.minecraft.world.item.ItemStack carrier){
        if(!(player instanceof ServerPlayer p)||ManuscriptReward.equipped(p,"talent")!=Kind.ARCANIST)return;
        var s=data(p);
        if(storm(p)){s.remove("Carrier");s.remove("Casts");return;}
        if(carrier.isEmpty())return;
        String id=identity(carrier);
        int stage=s.getString("Carrier").equals(id)?Math.floorMod(s.getInt("Casts"),3)+1:1;
        s.putString("Carrier",id);s.putInt("Casts",stage%3);
        p.displayClientMessage(Component.translatable("message.omnira.arcanist.rank",stage),true);
    }
    public static boolean cast(Player player,net.minecraft.world.item.ItemStack carrier,java.util.function.DoublePredicate action){
        boolean success=action.test(bonus(player,carrier));
        if(success)completed(player,carrier);
        return success;
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Pre event){
        if(event.getEntity() instanceof ServerPlayer p)advance(p);
    }
    public static void advance(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||p.isCreative()||ManuscriptReward.equipped(p,"curse")!=Kind.STORM)return;
        var s=data(p);int rain=s.getInt("Rain");
        int clock=s.getInt("Clock")+1;s.putInt("Clock",clock);
        if(rain<=0){
            if(clock==PERIOD-WARNING)p.displayClientMessage(Component.translatable("message.omnira.storm.warning"),false);
            if(clock>=PERIOD-WARNING&&clock<PERIOD&&clock%5==0)rain(p,true);
            if(clock<PERIOD)return;
            s.putInt("Clock",0);rain=DURATION;
            s.remove("Carrier");s.remove("Casts");
            p.displayClientMessage(Component.translatable("message.omnira.storm.begin"),false);
        }
        s.putLong("RainTick",p.level().getGameTime());
        if(rain%20==0)drain(p);
        if(rain%5==0)rain(p,false);
        s.putInt("Rain",rain-1);
    }
    private static void rain(ServerPlayer p,boolean warning){
        for(int i=0;i<2;i++)p.serverLevel().sendParticles(warning?ModParticles.STORM_WARNING.get():ModParticles.STORM_RAIN.get(),
                p.getX()+(p.getRandom().nextDouble()-.5)*1.7,p.getY()+.03,p.getZ()+(p.getRandom().nextDouble()-.5)*1.7,0,0,.09,0,1);
    }
    public static void drain(ServerPlayer p){
        var mana=p.getData(ModAttachments.MANA);double max=p.getAttributeValue(ModAttributes.MAX_MANA);
        if(mana.current()>0)p.setData(ModAttachments.MANA,mana.withCurrent(Math.max(0,mana.current()-max*.05)));
        else com.mcmagic.omnira.combat.OmniraDamage.payHealth(p,(float)Math.ceil(max*.01),"storm");
    }
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        if(event.getOriginal() instanceof ServerPlayer old&&event.getEntity() instanceof ServerPlayer next){
            var state=FateRuntime.data(old);
            if(state.contains(KEY,10)){var copy=state.getCompound(KEY).copy();copy.remove("RainTick");FateRuntime.data(next).put(KEY,copy);}
        }
    }
    private ArcanistFate(){}
}
