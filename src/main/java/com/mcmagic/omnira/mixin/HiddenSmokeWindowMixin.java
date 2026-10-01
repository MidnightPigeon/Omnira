package com.mcmagic.omnira.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the explicitly requested development smoke run suppresses its GLFW window. */
@Mixin(Window.class)
public abstract class HiddenSmokeWindowMixin {
    @Inject(method="<init>",at=@At(value="INVOKE",target="Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J",remap=false))
    private void omnira$hiddenSmoke(CallbackInfo ci) {
        if(Boolean.getBoolean("omnira.clientSmoke"))org.lwjgl.glfw.GLFW.glfwWindowHint(org.lwjgl.glfw.GLFW.GLFW_VISIBLE,org.lwjgl.glfw.GLFW.GLFW_FALSE);
    }
}
