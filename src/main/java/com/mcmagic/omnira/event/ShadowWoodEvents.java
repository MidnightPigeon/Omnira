package com.mcmagic.omnira.event;

import com.mcmagic.omnira.registry.*;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid="omnira")
public final class ShadowWoodEvents {
    @SubscribeEvent public static void strip(BlockEvent.BlockToolModificationEvent event) {
        if(event.getItemAbility()!=ItemAbilities.AXE_STRIP || !event.getContext().getItemInHand().canPerformAction(ItemAbilities.AXE_STRIP)) return;
        var state=event.getState();
        var target=state.is(DreamContent.SHADOW_LOG.get())?ShadowWoodContent.STRIPPED_LOG:state.is(ShadowWoodContent.WOOD.get())?ShadowWoodContent.STRIPPED_WOOD:null;
        if(target!=null) event.setFinalState(target.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,state.getValue(RotatedPillarBlock.AXIS)));
        var timeTarget=state.is(TimeNatureContent.LOG.get())?TimeNatureContent.STRIPPED_LOG:state.is(TimeNatureContent.WOOD.get())?TimeNatureContent.STRIPPED_WOOD:null;
        if(timeTarget!=null)event.setFinalState(timeTarget.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,state.getValue(RotatedPillarBlock.AXIS)));
    }
}
