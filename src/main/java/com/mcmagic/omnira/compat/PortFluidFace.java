package com.mcmagic.omnira.compat;

import com.mcmagic.omnira.space.SpacetimePortState;
import com.simibubi.create.content.fluids.FluidPropagator;
import net.createmod.catnip.math.BlockFace;
import net.minecraft.core.*;
import net.minecraft.world.level.Level;

/** A pipe edge whose destination face may be rotated across a paired port. */
public final class PortFluidFace extends BlockFace {
    private final Level level;
    public PortFluidFace(Level level,BlockPos pos,Direction face){super(pos,face);this.level=level;}
    public static BlockFace remote(Level level,BlockPos pos,Direction face){
        var port=SpacetimePortState.at(level,pos.relative(face));
        if(port==null||port.face()!=face)return null;
        var other=port.other();
        if(other==null||!level.hasChunkAt(other.attached()))return null;
        var pipe=FluidPropagator.getPipe(level,other.attached());
        return pipe!=null&&pipe.canHaveFlowToward(level.getBlockState(other.attached()),other.face())
                ?new BlockFace(other.attached(),other.face()):null;
    }
    @Override public BlockPos getConnectedPos(){var remote=remote(level,getPos(),getFace());return remote==null?super.getConnectedPos():remote.getPos();}
    @Override public Direction getOppositeFace(){var remote=remote(level,getPos(),getFace());return remote==null?super.getOppositeFace():remote.getFace();}
    @Override public BlockFace getOpposite(){return new PortFluidFace(level,getConnectedPos(),getOppositeFace());}
}
