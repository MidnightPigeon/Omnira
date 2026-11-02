package com.mcmagic.omnira.workstation;

import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class CondensationChannel {
    public static final int SHORT_TICKS=5,HEARTBEAT=2;
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final class Session {
        final ServerPlayer player;final SimpleCondensationTableBlockEntity table;final long started;final boolean center;
        long heartbeat;boolean stopped,startedWork;
        Session(ServerPlayer p,SimpleCondensationTableBlockEntity t,boolean center){player=p;table=t;this.center=center;started=heartbeat=p.level().getGameTime();}
    }
    private static BlockHitResult aim(ServerPlayer p,BlockPos pos){
        var hit=p.pick(p.blockInteractionRange(),1,false);
        return hit instanceof BlockHitResult b&&b.getType()==HitResult.Type.BLOCK&&b.getBlockPos().equals(pos)?b:null;
    }
    public static void input(ServerPlayer p,boolean held,BlockPos pos){
        var s=SESSIONS.get(p.getUUID());
        if(!held){
            if(s!=null){
                boolean click=!s.stopped&&p.isAlive()&&!p.isSpectator()&&!s.table.isRemoved()&&p.level()==s.table.getLevel()&&p.level().mayInteract(p,s.table.getBlockPos())&&p.level().getGameTime()-s.started<=SHORT_TICKS&&aim(p,s.table.getBlockPos())!=null;
                stop(p);if(click&&s.center)shortClick(p,s.table);
            }return;
        }
        if(s!=null){s.heartbeat=p.level().getGameTime();return;}
        var hit=aim(p,pos);
        if(hit==null||p.isSpectator()||!p.isAlive()||!p.level().mayInteract(p,pos)
                ||!(p.level().getBlockEntity(pos) instanceof SimpleCondensationTableBlockEntity table))return;
        var local=hit.getLocation().subtract(Vec3.atLowerCornerOf(pos));
        SESSIONS.put(p.getUUID(),new Session(p,table,local.x>=.25&&local.x<=.75&&local.z>=.25&&local.z<=.75&&local.y>=.65));
    }
    public static void shortClick(ServerPlayer p,SimpleCondensationTableBlockEntity table){
        var current=table.getItem(0);var held=p.getMainHandItem();
        if(!current.isEmpty()){
            table.setItem(0,ItemStack.EMPTY);if(!p.getInventory().add(current))p.drop(current,false);return;
        }
        if(!(table instanceof AdvancedCondensationTableBlockEntity)||held.isEmpty()||!table.canPlaceItem(0,held))return;
        int count=p.level().getRecipeManager().getAllRecipesFor(com.mcmagic.omnira.registry.ModRecipes.CONDENSATION_TYPE.get()).stream()
                .map(h->h.value()).filter(r->r.advanced()&&r.ingredient().test(held)).mapToInt(r->r.inputCount()).max().orElse(1);
        table.setItem(0,held.split(Math.min(count,held.getCount())));p.getInventory().setChanged();
    }
    public static void stop(ServerPlayer p){var s=SESSIONS.remove(p.getUUID());if(s!=null&&s.table.work.ownedBy(p))s.table.work.cancel();}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        for(var s:List.copyOf(SESSIONS.values())){
            var p=s.player;long now=p.level().getGameTime();
            if(p.hasDisconnected()||!p.isAlive()||p.isSpectator()||p.level()!=s.table.getLevel()||s.table.isRemoved()
                    ||!p.level().mayInteract(p,s.table.getBlockPos())||now-s.heartbeat>HEARTBEAT*3||aim(p,s.table.getBlockPos())==null){stop(p);continue;}
            if(s.stopped||now-s.started<=SHORT_TICKS)continue;
            var work=s.table.work;
            if(!work.active()){
                if(!work.start(p,s.startedWork?now:s.started)){s.stopped=true;continue;}
                s.startedWork=true;
            }
            if(!work.ownedBy(p)||!work.tick())s.stopped=true;
        }
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)stop(p);}
    @SubscribeEvent public static void clear(net.neoforged.neoforge.event.server.ServerStoppedEvent e){for(var s:SESSIONS.values())s.table.work.cancel();SESSIONS.clear();}
    private CondensationChannel(){}
}
