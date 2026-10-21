package com.mcmagic.omnira.pulse;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;

/** One shared external bus, regardless of how many relay inlets it touches. */
public record PulseImport(BlockPos source,List<PulseOrbEntity> receivers,List<Source> sources,DoubleConsumer settle){
    /** Native sources keep their physical graph identity; relays never mint a new budget. */
    public record Source(BlockPos pos,int output,DoubleSupplier supply,boolean nativeDevice){}
}
