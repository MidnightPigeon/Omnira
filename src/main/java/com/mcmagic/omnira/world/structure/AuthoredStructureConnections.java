package com.mcmagic.omnira.world.structure;

import com.mcmagic.omnira.shop.KirisameShopPlan;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.util.Collection;

/** Persist a one-time reconciliation until every structure chunk has finished generation. */
public final class AuthoredStructureConnections {
    private static final String ORIGIN="OmniraConnectionOrigin",KIND="OmniraConnectionKind";
    private AuthoredStructureConnections(){}
    public static void defer(BlockEntity anchor,BlockPos origin,String kind){
        anchor.getPersistentData().putLong(ORIGIN,origin.asLong());
        anchor.getPersistentData().putString(KIND,kind);
        anchor.setChanged();
    }
    public static void initialize(BlockEntity anchor){
        if(!(anchor.getLevel() instanceof ServerLevel level)||!anchor.getPersistentData().contains(ORIGIN))return;
        var origin=BlockPos.of(anchor.getPersistentData().getLong(ORIGIN));
        String kind=anchor.getPersistentData().getString(KIND);
        int width,depth;
        Collection<BlockPos> positions;
        if(kind.equals("kirisame")){
            width=KirisameShopPlan.WIDTH;depth=KirisameShopPlan.DEPTH;
            positions=KirisameShopPlan.cells().stream().map(KirisameShopPlan.Cell::pos).toList();
        }else if(kind.equals("palace")){
            width=FrozenTerraPalacePlan.WIDTH;depth=FrozenTerraPalacePlan.DEPTH;
            positions=FrozenTerraPalacePlan.create().keySet();
        }else return;
        for(int x=origin.getX()>>4;x<=(origin.getX()+width-1)>>4;x++)
            for(int z=origin.getZ()>>4;z<=(origin.getZ()+depth-1)>>4;z++)
                if(level.getChunkSource().getChunkNow(x,z)==null)return;
        reconcile(level,origin,positions);
        for(var local:positions){
            var be=level.getBlockEntity(local.offset(origin));
            if(be!=null&&be.getPersistentData().contains(ORIGIN)&&be.getPersistentData().getLong(ORIGIN)==origin.asLong()
                    &&be.getPersistentData().getString(KIND).equals(kind)){
                be.getPersistentData().remove(ORIGIN);be.getPersistentData().remove(KIND);be.setChanged();
            }
        }
    }
    public static void reconcile(ServerLevel level,BlockPos origin,Collection<BlockPos> positions){
        for(var local:positions){
            var p=local.offset(origin);var state=level.getBlockState(p);var block=state.getBlock();
            if(block instanceof StairBlock||block instanceof FenceBlock||block instanceof WallBlock||block instanceof IronBarsBlock){
                var connected=Block.updateFromNeighbourShapes(state,level,p);
                if(connected!=state)level.setBlock(p,connected,Block.UPDATE_CLIENTS);
            }
        }
    }
}
