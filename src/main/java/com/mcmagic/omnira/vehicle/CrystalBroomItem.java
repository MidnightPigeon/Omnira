package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class CrystalBroomItem extends Item {
    public CrystalBroomItem(Properties properties){super(properties.stacksTo(1));}
    @Override public InteractionResult useOn(UseOnContext context){
        var player=context.getPlayer();var level=context.getLevel();
        if(player==null || !player.mayBuild() || !level.mayInteract(player,context.getClickedPos())
                || !player.mayUseItemAt(context.getClickedPos(),context.getClickedFace(),context.getItemInHand()))return InteractionResult.FAIL;
        var broom=ModEntityTypes.CRYSTAL_BROOM.get().create(level);if(broom==null)return InteractionResult.FAIL;
        var pos=context.getClickLocation().add(0,.02,0);
        broom.moveTo(pos.x,pos.y,pos.z,player.getYRot()+180,0);
        if(!level.noCollision(broom,broom.getBoundingBox()) || !level.getWorldBorder().isWithinBounds(broom.getBoundingBox()))return InteractionResult.FAIL;
        if(!level.isClientSide){if(!level.addFreshEntity(broom))return InteractionResult.FAIL;context.getItemInHand().consume(1,player);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
