package com.mcmagic.omnira.energy;

import net.minecraft.world.item.ItemStack;

/** Native potential uses the stress field; the legacy FE field remains for source/API compatibility. */
public interface EngineCore {
    Output engineOutput(ItemStack stack);

    record Output(int stress,int fePerSecond) {
        public Output {
            if(stress<0 || fePerSecond<0 || (stress==0 && fePerSecond==0))
                throw new IllegalArgumentException("An engine core needs a positive output");
        }
    }
}
