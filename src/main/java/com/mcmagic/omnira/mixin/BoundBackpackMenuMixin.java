package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.compat.SophisticatedBackpackMenus;
import java.util.Optional;
import java.util.function.Function;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

/** Native opener counts on construction and close resolve independently of the inventory context. */
@Pseudo
@Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer",remap=false)
abstract class BoundBackpackMenuMixin {
    @WrapOperation(method={"<init>","removed"},at=@At(value="INVOKE",
            target="Ljava/util/Optional;flatMap(Ljava/util/function/Function;)Ljava/util/Optional;"))
    private Optional<?> omnira$openers(Optional<?> position,Function<?,?> lookup,Operation<Optional<?>> original){
        var target=SophisticatedBackpackMenus.boundBackpack(this);
        if(target==null)return original.call(position,lookup);
        return position.filter(target.getBlockPos()::equals).map(pos->target);
    }
}
