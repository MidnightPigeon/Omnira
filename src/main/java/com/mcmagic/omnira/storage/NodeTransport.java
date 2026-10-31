package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.*;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class NodeTransport {
    private static int speed(com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity node){
        var level=node.getLevel();var pos=node.getBlockPos().below();
        var chamber=com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.controller(level,pos);
        if(chamber!=null)return LogisticsRates.multiplier(chamber.upgrades);
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,pos);
        if(pool!=null)return LogisticsRates.multiplier(pool.upgrades);
        var be=level.getBlockEntity(pos);
        if(be instanceof CrystalBallBlockEntity ball)return ball.upgrades.speedMultiplier();
        return 1;
    }
    private static net.neoforged.neoforge.items.IItemHandler items(ResonanceCoreBlockEntity node){
        var level=node.getLevel();var pos=node.getBlockPos().below();
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,pos);if(pool!=null)return pool.nodeItems;
        if(level.getBlockEntity(pos) instanceof LiquidCrystalBallBlockEntity)return null;
        if(level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball)return new OrbItemHandler(ball,false);
        return node.items();
    }
    private static net.neoforged.neoforge.fluids.capability.IFluidHandler fluids(ResonanceCoreBlockEntity node){
        var level=node.getLevel();var pos=node.getBlockPos().below();
        var pool=com.mcmagic.omnira.alchemy.ReactionPoolBlock.controller(level,pos);if(pool!=null)return pool.nodeFluids;
        if(level.getBlockEntity(pos) instanceof LiquidCrystalBallBlockEntity ball)return ball.transportFluids;
        return node.fluids();
    }
    public static void unpair(ResonanceCoreBlockEntity node){
        if(node.getLevel() instanceof net.minecraft.server.level.ServerLevel server)TuningOutputs.get(server).release(node);
        if(node.peer!=null&&node.getLevel()!=null&&node.getLevel().hasChunkAt(node.peer)
            &&node.getLevel().getBlockEntity(node.peer) instanceof ResonanceCoreBlockEntity other
            &&node.identity.equals(other.peerIdentity)){
            other.peer=null;other.peerIdentity=null;other.setChanged();other.refresh();
            if(other.getLevel() instanceof net.minecraft.server.level.ServerLevel server)TuningOutputs.get(server).release(other);
        }
        node.peer=null;node.peerIdentity=null;node.selectedUntil=0;node.setChanged();node.refresh();
    }
    public static boolean pair(ResonanceCoreBlockEntity source,ResonanceCoreBlockEntity destination){
        if(!(source.getLevel() instanceof net.minecraft.server.level.ServerLevel level)||source.getLevel()!=destination.getLevel()
                ||source.getBlockPos().distSqr(destination.getBlockPos())>32*32
                ||StorageContainerIdentity.sameStorage(source,destination)
                ||!TuningOutputs.get(level).available(source))return false;
        unpair(source);unpair(destination);
        source.peer=destination.getBlockPos();source.peerIdentity=destination.identity;source.output=true;
        destination.peer=source.getBlockPos();destination.peerIdentity=source.identity;destination.output=false;
        source.setChanged();destination.setChanged();source.refresh();destination.refresh();
        TuningOutputs.get(level).claim(source);return true;
    }
    public static void tick(ResonanceCoreBlockEntity source){
        if(!source.output||source.peer==null)return;
        LogisticsRates.transfer(()->run(source));
    }
    private static void run(ResonanceCoreBlockEntity source){
        if(source.output&&source.peer!=null&&source.getLevel() instanceof net.minecraft.server.level.ServerLevel server&&!TuningOutputs.get(server).renew(source)){unpair(source);return;}
        if(!source.output||source.peer==null||!source.getLevel().hasChunkAt(source.peer))return;
        if(!(source.getLevel().getBlockEntity(source.peer) instanceof ResonanceCoreBlockEntity target)
            ||!source.peerIdentity.equals(target.identity)||!source.identity.equals(target.peerIdentity)
            ||source.getBlockPos().distSqr(target.getBlockPos())>32*32){unpair(source);return;}
        if(StorageContainerIdentity.sameStorage(source,target)){unpair(source);return;}
        var from=items(source);var to=items(target);
        if(from!=null&&to!=null&&from!=to){
            int remaining=LogisticsRates.ITEMS/4*speed(source);boolean moved=false;
            for(int i=0;i<from.getSlots()&&remaining>0;i++){
                var sample=from.extractItem(i,remaining,true);
                if(sample.isEmpty())continue;
                int accepted=sample.getCount()-ItemHandlerHelper.insertItemStacked(to,sample,true).getCount();if(accepted<=0)continue;
                var extracted=from.extractItem(i,accepted,false);var rest=ItemHandlerHelper.insertItemStacked(to,extracted,false);
                int count=extracted.getCount()-rest.getCount();remaining-=count;moved|=count>0;
                com.mcmagic.omnira.progression.BranchProgress.automated(source,extracted.getCount()-rest.getCount());
                com.mcmagic.omnira.progression.BranchProgress.automated(target,extracted.getCount()-rest.getCount());
                if(!rest.isEmpty())rest=from.insertItem(i,rest,false);
                if(!rest.isEmpty())net.minecraft.world.level.block.Block.popResource(source.getLevel(),source.getBlockPos(),rest);
            }
            if(moved)return;
        }
        var fluids=fluids(source);var receiving=fluids(target);
        if(fluids==null||receiving==null||fluids==receiving)return;
        for(int i=0;i<fluids.getTanks();i++){
            var sample=fluids.getFluidInTank(i).copyWithAmount(LogisticsRates.FLUID/4*speed(source));if(sample.isEmpty())continue;
            var available=fluids.drain(sample,IFluidHandler.FluidAction.SIMULATE);
            int amount=receiving.fill(available,IFluidHandler.FluidAction.SIMULATE);if(amount<=0)continue;
            var drained=fluids.drain(available.copyWithAmount(amount),IFluidHandler.FluidAction.EXECUTE);
            int accepted=receiving.fill(drained,IFluidHandler.FluidAction.EXECUTE);
            com.mcmagic.omnira.progression.BranchProgress.automated(source,accepted);
            com.mcmagic.omnira.progression.BranchProgress.automated(target,accepted);
            if(accepted<drained.getAmount())fluids.fill(drained.copyWithAmount(drained.getAmount()-accepted),IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }
    private NodeTransport(){}
}
