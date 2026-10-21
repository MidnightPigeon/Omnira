package com.mcmagic.omnira.pulse;

import net.minecraft.core.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;

/** Client-safe socket geometry, without consulting or rebuilding the server network. */
public final class PulseVisualConnections {
    public static boolean wireConnects(BlockGetter level,BlockPos pos,Direction face){
        var at=pos.relative(face);if(level instanceof Level world&&!world.hasChunkAt(at))return false;
        return level.getBlockState(at).is(PulseContent.WIRE.get())||level.getBlockEntity(at) instanceof PulseOrbHost
                ||connects(level,at,face.getOpposite());
    }
    public static boolean connects(BlockGetter level,BlockPos pos,Direction face){
        var state=level.getBlockState(pos);var be=level.getBlockEntity(pos);
        if(be instanceof com.mcmagic.omnira.codex.CodexAccess)return state.getValue(com.mcmagic.omnira.codex.MyriadCodexBlock.FACING).getOpposite()==face;
        if(be instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess)return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==face;
        if(be instanceof com.mcmagic.omnira.block.entity.AssemblyAccess)return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getOpposite()==face;
        if(state.getBlock() instanceof com.mcmagic.omnira.workstation.CrystalWorkstationBlock block)return block.shaft(state,face);
        if(state.is(com.mcmagic.omnira.registry.ModBlocks.REACTION_POOL.get())){int part=state.getValue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.PART);return (part==3||part==5)&&face==com.mcmagic.omnira.alchemy.ReactionPoolBlock.outward(state.getValue(com.mcmagic.omnira.alchemy.ReactionPoolBlock.FACING),part);}
        if(state.is(com.mcmagic.omnira.engine.SpacetimeEngineContent.BLOCK.get())){int side=com.mcmagic.omnira.engine.SpacetimeEngineBlock.side(state.getValue(com.mcmagic.omnira.engine.SpacetimeEngineBlock.PART));return side>=0&&face==com.mcmagic.omnira.engine.SpacetimeEngineBlock.OUTPUTS[side];}
        return be instanceof com.mcmagic.omnira.space.SpacetimePortHost port&&port.port().face().getOpposite()==face;
    }
    private PulseVisualConnections(){}
}
