package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public final class ShadowMistItem extends Item {
    public ShadowMistItem() {super(new Properties());}
    @Override public InteractionResult useOn(UseOnContext context) {
        var player=context.getPlayer();var level=context.getLevel();var pos=context.getClickedPos().relative(context.getClickedFace());
        if(player==null || player.isSpectator() || !level.mayInteract(player,context.getClickedPos())
                || !player.mayUseItemAt(pos,context.getClickedFace(),context.getItemInHand())
                || !level.getWorldBorder().isWithinBounds(pos)) return InteractionResult.FAIL;
        if(!level.isClientSide) {
            var mist=ModEntityTypes.SHADOW_MIST.get().create(level);
            if(mist==null) return InteractionResult.FAIL;
            var point=context.getClickLocation().add(net.minecraft.world.phys.Vec3.atLowerCornerOf(context.getClickedFace().getNormal()).scale(.51));
            mist.setPos(point.x,point.y-.3,point.z);
            if(!level.addFreshEntity(mist)) return InteractionResult.FAIL;
            if(!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
