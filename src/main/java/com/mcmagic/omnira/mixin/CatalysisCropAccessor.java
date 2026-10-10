package com.mcmagic.omnira.mixin;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.world.level.block.CropBlock.class)
public interface CatalysisCropAccessor {
    @org.spongepowered.asm.mixin.gen.Invoker("getGrowthSpeed")
    static float omnira$growthSpeed(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter view,net.minecraft.core.BlockPos pos){throw new AssertionError();}
}
