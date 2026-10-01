package com.mcmagic.omnira.energy;

import net.minecraft.world.item.ItemStack;

/** A core owns independent base mechanical and electrical outputs, before lattice multipliers. */
public interface EngineCore {
    Output engineOutput(ItemStack stack);

    record Output(int stress,int fePerSecond) {
        public Output {
            if(stress<0 || fePerSecond<0 || (stress==0 && fePerSecond==0))
                throw new IllegalArgumentException("An engine core needs a positive output");
        }
    }
}
