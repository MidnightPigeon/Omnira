package com.mcmagic.omnira.time;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;

public final class TimeSeedItem extends Item {
    public TimeSeedItem(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext context){
        var level=context.getLevel();var pos=context.getClickedPos();
        if(!level.getBlockState(pos).is(DreamContent.EXCITED_SPATIAL_CRYSTAL.get()))return InteractionResult.PASS;
        if(!level.isClientSide){
            level.setBlockAndUpdate(pos,TimeNatureContent.SPATIAL_MATRIX.get().defaultBlockState());
            if(context.getPlayer()==null||!context.getPlayer().getAbilities().instabuild)context.getItemInHand().shrink(1);
            level.levelEvent(1505,pos,0);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
