package com.mcmagic.omnira.mixin;

@org.spongepowered.asm.mixin.Mixin(net.minecraft.server.level.ServerPlayer.class)
public interface CompassRespawnAccessor {
    @org.spongepowered.asm.mixin.gen.Invoker("findRespawnAndUseSpawnBlock")
    static java.util.Optional<net.minecraft.server.level.ServerPlayer.RespawnPosAngle> omnira$respawn(
            net.minecraft.server.level.ServerLevel level,net.minecraft.core.BlockPos pos,float angle,boolean forced,boolean keepInventory){throw new AssertionError();}
}
