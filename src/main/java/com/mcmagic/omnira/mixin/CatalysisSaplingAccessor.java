package com.mcmagic.omnira.mixin;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.SaplingBlock.class)
public interface CatalysisSaplingAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("treeGrower")
    net.minecraft.world.level.block.grower.TreeGrower omnira$grower();
}
