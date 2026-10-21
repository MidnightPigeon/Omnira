package com.mcmagic.omnira.myriad;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ActiveWorldSeedItem extends Item {
    public ActiveWorldSeedItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        var level=context.getLevel();var pos=context.getClickedPos();
        if(context.getClickedFace()!=Direction.UP)return InteractionResult.PASS;
        var shape=level.getBlockState(pos).getCollisionShape(level,pos);
        if(shape.isEmpty())return InteractionResult.PASS;
        double top=shape.max(Direction.Axis.Y);
        var surface=net.minecraft.world.phys.shapes.Shapes.box(0,Math.max(0,top-.001),0,1,top,1);
        if(net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(surface,shape,net.minecraft.world.phys.shapes.BooleanOp.ONLY_FIRST))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        return context.getPlayer() instanceof ServerPlayer player
                && WorldDeflection.request(player,pos,context.getHand()) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
}
