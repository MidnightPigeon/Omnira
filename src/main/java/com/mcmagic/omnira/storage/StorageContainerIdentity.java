package com.mcmagic.omnira.storage;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;

public final class StorageContainerIdentity {
    public static boolean sameStorage(ResonanceCoreBlockEntity a,ResonanceCoreBlockEntity b){
        var key=terminalKey(a);
        return key!=null&&key.equals(terminalKey(b))||same(a,b);
    }
    public static String terminalKey(ResonanceCoreBlockEntity node){return terminalKey(node.getLevel(),node.getBlockPos().below());}
    public static String terminalKey(Level level,BlockPos container){
        if(level instanceof net.minecraft.server.level.ServerLevel server){
            var shared=CrystalStorageNetworks.get(server).terminalStorageKey(server,container);
            if(shared!=null)return shared;
        }
        return key(level,container);
    }
    public static boolean same(ResonanceCoreBlockEntity a,ResonanceCoreBlockEntity b){
        if(a.getLevel()!=b.getLevel())return false;
        var key=key(a);if(key!=null&&key.equals(key(b)))return true;
        var items=a.items();if(items!=null&&items==b.items())return true;
        var fluids=a.fluids();return fluids!=null&&fluids==b.fluids();
    }
    public static String key(ResonanceCoreBlockEntity node){return key(node.getLevel(),node.getBlockPos().below());}
    public static String key(Level level,BlockPos pos){
        if(level==null||!level.hasChunkAt(pos))return null;
        var state=level.getBlockState(pos);var anchor=pos;
        if(state.getBlock() instanceof com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock)
            return level.dimension().location()+"@"+com.mcmagic.omnira.alchemy.catalysis.CatalysisBlock.master(pos,state).asLong();
        if(state.getBlock() instanceof com.mcmagic.omnira.alchemy.ReactionPoolBlock){
            anchor=com.mcmagic.omnira.alchemy.ReactionPoolBlock.master(pos,state);
        }else if(state.getBlock() instanceof ChestBlock&&state.getValue(ChestBlock.TYPE)!=ChestType.SINGLE){
            var other=pos.relative(ChestBlock.getConnectedDirection(state));
            if(level.hasChunkAt(other)){
                var neighbor=level.getBlockState(other);
                if(neighbor.is(state.getBlock())&&neighbor.getValue(ChestBlock.FACING)==state.getValue(ChestBlock.FACING)
                        &&neighbor.getValue(ChestBlock.TYPE)!=ChestType.SINGLE&&neighbor.getValue(ChestBlock.TYPE)!=state.getValue(ChestBlock.TYPE))
                    anchor=pos.asLong()<other.asLong()?pos:other;
            }
        }else if(net.neoforged.fml.ModList.get().isLoaded("create")){
            anchor=CreateStorageIdentity.controller(level.getBlockEntity(pos),pos);
        }
        return level.dimension().location()+"@"+anchor.asLong();
    }
    private StorageContainerIdentity(){}
}
