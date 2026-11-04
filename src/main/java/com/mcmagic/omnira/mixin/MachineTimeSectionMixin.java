package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.time.*;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunkSection.class)
abstract class MachineTimeSectionMixin implements MachineTimeRegions.Section {
    @Unique private MachineTimeRegions omnira$regions;
    @Unique private long omnira$key;
    @Unique private boolean omnira$hasLocalTime;

    public void omnira$trackTime(MachineTimeRegions regions,long key) {
        if(omnira$regions==regions && omnira$key==key)return;
        if(omnira$hasLocalTime)omnira$regions.change(omnira$key,-1);
        omnira$hasLocalTime=false;
        omnira$regions=regions;
        omnira$key=key;
        omnira$refreshTime();
    }

    @Inject(method={"fillBiomesFromNoise","readBiomes","read"},at=@At("RETURN"))
    private void omnira$biomesChanged(CallbackInfo ci){omnira$refreshTime();}

    @Unique private void omnira$refreshTime(){
        if(omnira$regions==null)return;
        var palette=((LevelChunkSection)(Object)this).getBiomes();
        boolean present=palette.maybeHas(b->b.is(FleetingTime.BIOME)||b.is(StilledTime.BIOME));
        if(present!=omnira$hasLocalTime){
            omnira$hasLocalTime=present;
            omnira$regions.change(omnira$key,present?1:-1);
        }
    }
}
