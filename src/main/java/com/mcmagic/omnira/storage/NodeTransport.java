package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.*;
import net.neoforged.neoforge.items.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class NodeTransport {
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
                ||StorageContainerIdentity.sameStorage(source,destination)
                ||!TuningOutputs.get(level).available(source))return false;
        unpair(source);unpair(destination);
        source.peer=destination.getBlockPos();source.peerIdentity=destination.identity;source.output=true;
        destination.peer=source.getBlockPos();destination.peerIdentity=source.identity;destination.output=false;
        source.setChanged();destination.setChanged();source.refresh();destination.refresh();
        TuningOutputs.get(level).claim(source);return true;
    }
    public static void tick(ResonanceCoreBlockEntity source){
        if(source.output&&source.peer!=null&&source.getLevel() instanceof net.minecraft.server.level.ServerLevel server&&!TuningOutputs.get(server).renew(source)){unpair(source);return;}
        if(!source.output||source.peer==null||!source.getLevel().hasChunkAt(source.peer))return;
        if(!(source.getLevel().getBlockEntity(source.peer) instanceof ResonanceCoreBlockEntity target)
            ||!source.peerIdentity.equals(target.identity)||!source.identity.equals(target.peerIdentity)
            ||source.getBlockPos().distSqr(target.getBlockPos())>32*32){unpair(source);return;}
        if(StorageContainerIdentity.sameStorage(source,target)){unpair(source);return;}
        var from=source.items();var to=target.items();
        if(from!=null&&to!=null&&from!=to){
            for(int i=0;i<from.getSlots();i++){
                var sample=from.extractItem(i,1,true);
                if(sample.isEmpty())continue;
                if(!ItemHandlerHelper.insertItemStacked(to,sample,true).isEmpty())continue;
                var extracted=from.extractItem(i,1,false);var rest=ItemHandlerHelper.insertItemStacked(to,extracted,false);
                if(!rest.isEmpty())rest=from.insertItem(i,rest,false);
                if(!rest.isEmpty())net.minecraft.world.level.block.Block.popResource(source.getLevel(),source.getBlockPos(),rest);
                return;
            }
        }
        var fluids=source.fluids();var receiving=target.fluids();
        if(fluids==null||receiving==null||fluids==receiving)return;
        for(int i=0;i<fluids.getTanks();i++){
            var sample=fluids.getFluidInTank(i).copyWithAmount(500);if(sample.isEmpty())continue;
            var available=fluids.drain(sample,IFluidHandler.FluidAction.SIMULATE);
            int amount=receiving.fill(available,IFluidHandler.FluidAction.SIMULATE);if(amount<=0)continue;
            var drained=fluids.drain(available.copyWithAmount(amount),IFluidHandler.FluidAction.EXECUTE);
            int accepted=receiving.fill(drained,IFluidHandler.FluidAction.EXECUTE);
            if(accepted<drained.getAmount())fluids.fill(drained.copyWithAmount(drained.getAmount()-accepted),IFluidHandler.FluidAction.EXECUTE);
            return;
        }
    }
    private NodeTransport(){}
}
