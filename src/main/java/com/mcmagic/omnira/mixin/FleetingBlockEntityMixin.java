package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.time.FleetingTime;
import com.mcmagic.omnira.time.MachineBiomeSamples;
import com.mcmagic.omnira.time.MachineTimeRegions;
import com.mcmagic.omnira.time.StilledTime;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Level.class)
abstract class FleetingBlockEntityMixin implements MachineTimeRegions.Owner {
    @Unique private final MachineTimeRegions omnira$regions=new MachineTimeRegions();
    public MachineTimeRegions omnira$timeRegions(){return omnira$regions;}
    @Unique private MachineBiomeSamples omnira$machineBiomes;
    @WrapOperation(method="tickBlockEntities",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/entity/TickingBlockEntity;tick()V"))
    private void omnira$machine(TickingBlockEntity ticker,Operation<Void> original){
        var level=(Level)(Object)this;
        var pos=omnira$regions.active()?ticker.getPos():null;
        if(omnira$regions.nearby(pos)&&omnira$matches(level,pos,StilledTime.BIOME)
                && !StilledTime.shipPlot(level,pos) && StilledTime.freezesBlock(level,pos))return;
        original.call(ticker);
        if(!level.isClientSide&&omnira$regions.active()&&!ticker.isRemoved()){
            var after=ticker.getPos();
            if(omnira$regions.nearby(after)&&omnira$matches(level,after,FleetingTime.BIOME))original.call(ticker);
        }
    }
    @Unique private boolean omnira$matches(Level level,net.minecraft.core.BlockPos pos,net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome){
        if(omnira$machineBiomes==null)omnira$machineBiomes=new MachineBiomeSamples(level.getBiomeManager());
        return omnira$machineBiomes.matches(level,pos,biome);
    }
}
