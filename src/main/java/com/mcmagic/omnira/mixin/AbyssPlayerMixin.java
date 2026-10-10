package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.*;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class AbyssPlayerMixin {
    @Inject(method="maybeBackOffFromEdge",at=@At("HEAD"),cancellable=true)
    private void omnira$ceilingEdge(Vec3 desired,MoverType type,CallbackInfoReturnable<Vec3> ci){
        var p=(Player)(Object)this;if(!AbyssGravity.inverted(p))return;
        boolean crouching=p.isShiftKeyDown()&&!p.getAbilities().flying&&p.onGround()&&desired.y>=0&&(type==MoverType.SELF||type==MoverType.PLAYER);
        ci.setReturnValue(crouching?AbyssMovement.ceilingEdge(p,desired):desired);
    }
}
