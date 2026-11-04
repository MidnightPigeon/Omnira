package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.mire.MireContent;
import com.mcmagic.omnira.mire.MireFishingEffects;
import com.mcmagic.omnira.space.sea.StarlightContent;
import com.mcmagic.omnira.space.sea.StarlightFloat;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.Unique;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
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
abstract class MireFishingMixin implements StarlightFloat {
    @Unique private static final EntityDataAccessor<Boolean> omnira$starFloat =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BOOLEAN);

    @Inject(method="defineSynchedData", at=@At("TAIL"))
    private void omnira$defineFloat(SynchedEntityData.Builder data, CallbackInfo ci) {
        data.define(omnira$starFloat, false);
        data.define(omnira$phase,(byte)0);
    }

    @Override public boolean omnira$isStarlight() {
        return ((FishingHook)(Object)this).getEntityData().get(omnira$starFloat);
    }

    @Override public void omnira$markStarlight() {
        ((FishingHook)(Object)this).getEntityData().set(omnira$starFloat, true);
    }
    @Shadow private int timeUntilLured;
    @Shadow private int timeUntilHooked;
    @Shadow private int nibble;
    @Unique private ItemStack omnira$castingRod=ItemStack.EMPTY;
    @Override public void omnira$castWith(ItemStack rod){
        omnira$markStarlight();
        omnira$castingRod=rod;
    }
    @Unique private void omnira$strike(net.minecraft.world.entity.Entity target){
        var hook=(FishingHook)(Object)this;
        var player=hook.getPlayerOwner();
        if(hook.level().isClientSide||!omnira$isStarlight()||omnira$castingRod.isEmpty()||player==null||player.isSpectator()
                ||target==player||!target.isAlive()||target.isSpectator())return;
        if(target instanceof net.minecraft.world.entity.player.Player other&&!player.canHarmPlayer(other))return;
        if(player.getMainHandItem()==omnira$castingRod)player.attack(target);
        else if(player.getOffhandItem()==omnira$castingRod)((com.mcmagic.omnira.space.sea.RodStrike.Wielder)player).omnira$offhandStrike(target);
    }
    @Inject(method="onHitEntity",at=@At("TAIL"))
    private void omnira$hookStrike(net.minecraft.world.phys.EntityHitResult hit,CallbackInfo ci){
        omnira$strike(hit.getEntity());
    }
    @Inject(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/projectile/FishingHook;pullEntity(Lnet/minecraft/world/entity/Entity;)V"))
    private void omnira$reelStrike(ItemStack rod,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> ci){
        var target=((FishingHook)(Object)this).getHookedIn();
        if(rod==omnira$castingRod&&target!=null)omnira$strike(target);
    }
    // Roll each bonus independently before ItemFishedEvent, so cancellation discards the whole catch.
    @ModifyExpressionValue(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"))
    private ObjectArrayList<ItemStack> omnira$starlightCatch(ObjectArrayList<ItemStack> catchItems, ItemStack rod) {
        var hook=(FishingHook)(Object)this;
        if (!catchItems.isEmpty() && rod.is(StarlightContent.ROD.get()) && omnira$isStarlight()) {
            if(hook.getRandom().nextInt(5)==0)catchItems.add(StarlightContent.DUST_ITEM.get().getDefaultInstance());
            if(hook.getRandom().nextInt(100)<3)catchItems.add(com.mcmagic.omnira.registry.ModItems.NIGHT_HERON_STATUE.get().getDefaultInstance());
        }
        return catchItems;
    }
    @WrapOperation(method="catchingFish",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean omnira$timeflowSurface(BlockState state,Block block,Operation<Boolean> original){
        var hook=(FishingHook)(Object)this;
        if(block==Blocks.WATER&&com.mcmagic.omnira.space.sea.VoidSeaContent.fluid(hook.level().getFluidState(hook.blockPosition())))return state.is(Blocks.WATER)||com.mcmagic.omnira.space.sea.VoidSeaContent.fluid(state.getFluidState());
        return block==Blocks.WATER&&MireContent.fluid(hook.level().getFluidState(hook.blockPosition()))
                ?MireFishingEffects.surface(state):original.call(state,block);
    }
    @WrapOperation(method="catchingFish",at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int omnira$timeflowParticles(ServerLevel level,ParticleOptions type,double x,double y,double z,int count,double dx,double dy,double dz,double speed,Operation<Integer> original){
        var hook=(FishingHook)(Object)this;
        if(com.mcmagic.omnira.space.sea.VoidSeaContent.fluid(level.getFluidState(hook.blockPosition())))return com.mcmagic.omnira.space.sea.StarflowFishing.emit(level,type,x,y,z,count,dx,dy,dz,speed);
        if(!MireContent.fluid(level.getFluidState(hook.blockPosition())))return original.call(level,type,x,y,z,count,dx,dy,dz,speed);
        return MireFishingEffects.emit(level,type,x,y,z,count,dx,dy,dz,speed);
    }
    @Unique private static final EntityDataAccessor<Byte> omnira$phase =
            SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BYTE);

    @Override public int omnira$fishingPhase(){return ((FishingHook)(Object)this).getEntityData().get(omnira$phase);}

    @org.spongepowered.asm.mixin.injection.ModifyConstant(method="shouldStopFishing",
            constant=@org.spongepowered.asm.mixin.injection.Constant(doubleValue=1024.0))
    private double omnira$lineLength(double vanilla){
        return omnira$isStarlight()?64.0*64.0:vanilla;
    }

    @Inject(method="catchingFish",at=@At("RETURN"))
    private void omnira$syncFishingPhase(BlockPos pos,CallbackInfo ci){
        var hook=(FishingHook)(Object)this;
        var fluid=hook.level().getFluidState(pos);
        if(!omnira$isStarlight() && !MireContent.fluid(fluid) && !com.mcmagic.omnira.space.sea.VoidSeaContent.fluid(fluid))return;
        hook.getEntityData().set(omnira$phase,(byte)(nibble>0?3:timeUntilHooked>0?2:1));
    }

    @Unique private int omnira$biteAge;

    @Inject(method="tick",at=@At("TAIL"))
    private void omnira$showFishingPhase(CallbackInfo ci){
        var hook=(FishingHook)(Object)this;
        if(hook.level().isClientSide && !hook.isRemoved()){
            int phase=omnira$fishingPhase();
            omnira$biteAge=phase==3?omnira$biteAge+1:0;
            com.mcmagic.omnira.space.sea.StarflowFishing.showPhase(hook,phase,omnira$isStarlight(),omnira$biteAge);
        }
    }
    @ModifyArg(method="retrieve",at=@At(value="INVOKE",target="Lnet/minecraft/server/ReloadableServerRegistries$Holder;getLootTable(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/world/level/storage/loot/LootTable;"),index=0)
    private ResourceKey<LootTable> omnira$mireFishing(ResourceKey<LootTable> original){
        var hook=(FishingHook)(Object)this;
        if(com.mcmagic.omnira.space.sea.VoidSeaContent.fluid(hook.level().getFluidState(hook.blockPosition())))return ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:fishing/void_starflow"));
        return MireContent.fluid(hook.level().getFluidState(hook.blockPosition()))
                ?ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.parse("omnira:fishing/reversion_mire")):original;
    }
}
