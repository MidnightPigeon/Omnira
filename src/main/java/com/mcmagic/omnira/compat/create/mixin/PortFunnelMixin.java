package com.mcmagic.omnira.compat.create.mixin;

import com.mcmagic.omnira.space.SpacetimePortState;
import com.simibubi.create.content.logistics.funnel.*;
import com.simibubi.create.foundation.blockEntity.behaviour.inventory.InvManipulationBehaviour;
import com.simibubi.create.foundation.item.ItemHelper.ExtractionCountMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.simibubi.create.content.logistics.funnel.FunnelBlockEntity",remap=false)
abstract class PortFunnelMixin {
    @Shadow private InvManipulationBehaviour invManipulation;
    @Shadow public abstract int getAmountToExtract();
    @Shadow public abstract ExtractionCountMode getModeToExtract();
    @Shadow private int startCooldown(){throw new AssertionError();}
    @Shadow public abstract void flap(boolean inward);
    @Shadow public abstract void onTransfer(ItemStack stack);
    @Inject(method="activateExtractor",at=@At("HEAD"),cancellable=true)
    private void omnira$directOutput(CallbackInfo ci){
        var host=(BlockEntity)(Object)this;var level=host.getLevel();
        var face=AbstractFunnelBlock.getFunnelFacing(host.getBlockState());if(face==null)return;
        var port=SpacetimePortState.at(level,host.getBlockPos().relative(face));
        if(port==null||port.face()!=face)return;
        ci.cancel();var target=port.itemPort(face.getOpposite());if(target==null)return;
        var offered=invManipulation.simulate().extract(getModeToExtract(),getAmountToExtract());
        if(offered.isEmpty()||!ItemHandlerHelper.insertItemStacked(target,offered,true).isEmpty())return;
        var extracted=invManipulation.extract(getModeToExtract(),offered.getCount());if(extracted.isEmpty())return;
        var remainder=ItemHandlerHelper.insertItemStacked(target,extracted,false);
        if(!remainder.isEmpty())net.minecraft.world.Containers.dropItemStack(level,host.getBlockPos().getX()+.5,host.getBlockPos().getY()+.5,host.getBlockPos().getZ()+.5,remainder);
        flap(false);onTransfer(extracted);startCooldown();
    }
}
