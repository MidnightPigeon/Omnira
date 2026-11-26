package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.client.RemoteContainerView;
import com.mcmagic.omnira.compat.SophisticatedBackpackMenus.BoundContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContext$Block",remap=false)
abstract class BoundBackpackClientContextMixin {
    @Inject(method="<init>",at=@At("RETURN"))
    private void omnira$snapshot(BlockPos pos,CallbackInfo ci){
        if(!Minecraft.getInstance().isSameThread())return;
        var snapshot=RemoteContainerView.entity(pos);
        if(snapshot!=null)((BoundContext)this).omnira$bind(snapshot);
    }
}
