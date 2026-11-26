package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mcmagic.omnira.compat.SophisticatedBackpackMenus.BoundContext;
import com.mcmagic.omnira.storage.RemoteContainerAccess;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext$Block",remap=false)
abstract class BoundBackpackContextMixin implements BoundContext {
    @Unique private BlockEntity omnira$target;
    public void omnira$bind(BlockEntity backpack){omnira$target=backpack;}
    public BlockEntity omnira$backpack(){return omnira$target;}

    @WrapOperation(method={"getBackpackWrapper","onUpgradeChanged"},at=@At(value="INVOKE",
            target="Lnet/p3pp3rf1y/sophisticatedcore/util/WorldHelper;getBlockEntity(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Ljava/lang/Class;)Ljava/util/Optional;"))
    private Optional<?> omnira$resolve(BlockGetter local,BlockPos pos,Class<?> type,Operation<Optional<?>> original){
        if(omnira$target==null)return original.call(local,pos,type);
        return !omnira$target.isRemoved() && type.isInstance(omnira$target) && pos.equals(omnira$target.getBlockPos())
                ?Optional.of(omnira$target):Optional.empty();
    }
    @Inject(method="canInteractWith",at=@At("HEAD"),cancellable=true)
    private void omnira$validate(Player player,CallbackInfoReturnable<Boolean> result){
        if(omnira$target!=null)
            result.setReturnValue(player.level().isClientSide ? !omnira$target.isRemoved() : RemoteContainerAccess.permits(player,omnira$target));
    }
    @Inject(method="getSubBackpackContext",at=@At("RETURN"))
    private void omnira$child(int slot,boolean save,CallbackInfoReturnable<?> result){
        if(omnira$target!=null && result.getReturnValue() instanceof BoundContext child)child.omnira$bind(omnira$target);
    }
}
