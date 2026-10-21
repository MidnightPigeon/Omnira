package com.mcmagic.omnira.compat.create.mixin;

import com.mcmagic.omnira.compat.create.PulseOrbKinetics;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Transit debits are rebuilt from loaded topology, not saved as phantom unloaded stress. */
@Mixin(value=KineticBlockEntity.class,remap=false)
public abstract class PulseStressSaveMixin {
    @Inject(method="write",at=@At("TAIL"))
    private void omnira$transientDebit(CompoundTag tag,HolderLookup.Provider registries,boolean packet,CallbackInfo ci){
        var member=(KineticBlockEntity)(Object)this;
        if(packet||!member.hasNetwork()||!tag.contains("Network"))return;
        float debit=PulseOrbKinetics.importedStress(member.getOrCreateNetwork());
        if(debit<=0)return;var network=tag.getCompound("Network");
        network.putFloat("Stress",Math.max(0,network.getFloat("Stress")-debit));
    }
}
