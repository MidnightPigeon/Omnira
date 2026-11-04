package com.mcmagic.omnira.space.sea;

/** Cast identity is set once by the rod and tracked along with the vanilla hook. */
public interface StarlightFloat {
    boolean omnira$isStarlight();
    void omnira$markStarlight();
    void omnira$castWith(net.minecraft.world.item.ItemStack rod);
    int omnira$fishingPhase();
}
