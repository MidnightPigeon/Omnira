package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.mire.MireFishingEffects;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
abstract class MireFishingMixin {
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Shadow private int nibble;
    @WrapOperation(method="catchingFish",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean omnira$timeflowSurface(BlockState state,Block block,Operation<Boolean> original){
        var hook=(FishingHook)(Object)this;
        return block==Blocks.WATER&&MireContent.fluid(hook.level().getFluidState(hook.blockPosition()))
                ?MireFishingEffects.surface(state):original.call(state,block);
    }
    @WrapOperation(method="catchingFish",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int omnira$timeflowParticles(ServerLevel level,ParticleOptions type,double x,double y,double z,int count,double dx,double dy,double dz,double speed,Operation<Integer> original){
        var hook=(FishingHook)(Object)this;
        if(!MireContent.fluid(level.getFluidState(hook.blockPosition())))return original.call(level,type,x,y,z,count,dx,dy,dz,speed);
        return MireFishingEffects.emit(level,type,x,y,z,count,dx,dy,dz,speed);
    }
    @Inject(method="catchingFish",at=@At("HEAD"))
    private void omnira$waitingRipple(BlockPos pos,CallbackInfo ci){
        var hook=(FishingHook)(Object)this;
        if(timeUntilLured<=0||timeUntilHooked>0||nibble>0||hook.tickCount%16!=0||!MireContent.fluid(hook.level().getFluidState(pos)))return;
        double angle=hook.tickCount*.17, radius=.45+.15*Math.sin(hook.tickCount*.07);
        MireFishingEffects.emit((ServerLevel)hook.level(),ParticleTypes.SPLASH,hook.getX()+Math.cos(angle)*radius,hook.getY()+.5,hook.getZ()+Math.sin(angle)*radius,1,0,0,0,0);
    }
    @ModifyArg(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"),index=0)
    private ResourceKey<LootTable> omnira$mireFishing(ResourceKey<LootTable> original){
        var hook=(FishingHook)(Object)this;
        return MireContent.fluid(hook.level().getFluidState(hook.blockPosition()))
                ?ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:fishing/reversion_mire")):original;
    }
}
