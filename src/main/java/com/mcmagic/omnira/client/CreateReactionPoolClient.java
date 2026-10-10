package com.mcmagic.omnira.client;

import com.mcmagic.omnira.alchemy.*;
import com.mcmagic.omnira.compat.CreateReactionPool;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

/** Client-only optional bridge; use Create's clock and phase instead of an independent rotor. */
final class CreateReactionPoolClient {
    static double shaftAngle(ReactionPoolBlockEntity pool,int part){
        var facing=pool.getBlockState().getValue(ReactionPoolBlock.FACING);
        var at=pool.getBlockPos().offset(ReactionPoolBlock.offset(facing,part));
        if(!(pool.getLevel().getBlockEntity(at) instanceof CreateReactionPool.Port port))return 0;
        var right=ReactionPoolBlock.offset(facing,5);
        int sign=right.getX()+right.getZ();
        return Math.toDegrees(KineticBlockEntityRenderer.getAngleForBe(port,at,facing.getClockWise().getAxis()))*sign;
    }
    private CreateReactionPoolClient(){}
}
