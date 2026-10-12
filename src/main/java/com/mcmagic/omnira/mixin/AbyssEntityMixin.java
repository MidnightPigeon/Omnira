package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.abyss.AbyssGravity;
import com.mcmagic.omnira.space.abyss.AbyssMovement;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
abstract class AbyssEntityMixin {
    @Shadow public java.util.Optional<BlockPos> mainSupportingBlockPos;
    @Shadow private boolean onGroundNoBlocks;
    @Inject(method="getGravity",at=@At("RETURN"),cancellable=true)
    private void omnira$gravity(CallbackInfoReturnable<Double> ci){
        var e=(Entity)(Object)this;if(!e.isSpectator())ci.setReturnValue(ci.getReturnValue()*com.mcmagic.omnira.space.abyss.GravityTransition.gravity(e));
    }
    @ModifyArg(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZLnet/minecraft/world/phys/Vec3;)V"),index=0)
    private boolean omnira$ceiling(boolean ground){var e=(Entity)(Object)this;return AbyssGravity.inverted(e)?e.verticalCollision&&e.getDeltaMovement().y>=0:ground;}
    @Inject(method="setOnGroundWithMovement",at=@At("HEAD"),cancellable=true)
    private void omnira$ceilingGround(boolean ground,Vec3 movement,CallbackInfo ci){
        var e=(Entity)(Object)this;if(AbyssGravity.inverted(e)&&e.verticalCollision&&(ground||e.getDeltaMovement().y>=0)){e.setOnGround(true);ci.cancel();}
    }
    @Inject(method="checkSupportingBlock",at=@At("HEAD"),cancellable=true)
    private void omnira$ceilingSupport(boolean ground,Vec3 movement,CallbackInfo ci){
        var e=(Entity)(Object)this;if(!AbyssGravity.inverted(e))return;
        var box=e.getBoundingBox();
        mainSupportingBlockPos=ground?e.level().findSupportingBlock(e,new net.minecraft.world.phys.AABB(box.minX,box.maxY,box.minZ,box.maxX,box.maxY+1e-6,box.maxZ)):java.util.Optional.empty();
        onGroundNoBlocks=ground&&mainSupportingBlockPos.isEmpty();ci.cancel();
    }
    @ModifyArg(method="move",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"),index=0)
    private double omnira$fall(double dy){var e=(Entity)(Object)this;return AbyssGravity.direction(e)==0?0:AbyssGravity.inverted(e)?-dy:dy;}
    @Inject(method="getOnPos(F)Lnet/minecraft/core/BlockPos;",at=@At("HEAD"),cancellable=true)
    private void omnira$support(float offset,CallbackInfoReturnable<BlockPos> ci){var e=(Entity)(Object)this;if(AbyssGravity.inverted(e))ci.setReturnValue(BlockPos.containing(e.getX(),e.getBoundingBox().maxY+offset,e.getZ()));}
    @ModifyArgs(method="spawnSprintParticle",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void omnira$runningParticles(org.spongepowered.asm.mixin.injection.invoke.arg.Args args){
        var entity=(Entity)(Object)this;if(!AbyssGravity.inverted(entity))return;
        args.set(2,AbyssGravity.footParticleY(entity,.1));
        args.set(5,-(double)args.get(5));
    }
    @Inject(method="getEyeY",at=@At("RETURN"),cancellable=true)
    private void omnira$eyes(CallbackInfoReturnable<Double> ci){ci.setReturnValue(ci.getReturnValue()+AbyssGravity.eyeOffset((Entity)(Object)this));}
    @Inject(method="getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",at=@At("RETURN"),cancellable=true)
    private void omnira$interpolatedEyes(float partial,CallbackInfoReturnable<Vec3> ci){ci.setReturnValue(ci.getReturnValue().add(0,com.mcmagic.omnira.space.abyss.GravityTransition.eyeOffset((Entity)(Object)this,partial),0));}
    @Inject(method="moveRelative",at=@At("HEAD"),cancellable=true)
    private void omnira$forward(float speed,Vec3 input,CallbackInfo ci){
        var e=(Entity)(Object)this;if(e.isSpectator())return;
        int direction=AbyssGravity.direction(e);
        if(direction==0){e.setDeltaMovement(e.getDeltaMovement().add(AbyssGravity.input(e,input,speed)));ci.cancel();}
        else if(com.mcmagic.omnira.space.abyss.GravityTransition.roll(e,0)!=0&&e instanceof net.minecraft.world.entity.player.Player){
            double length=input.length();if(length>1e-7){
                var v=input.scale(speed/Math.max(1,length));double yaw=Math.toRadians(e.getYRot());
                double strafe=v.x*Math.cos(Math.toRadians(com.mcmagic.omnira.space.abyss.GravityTransition.roll(e,0)));
                e.setDeltaMovement(e.getDeltaMovement().add(strafe*Math.cos(yaw)-v.z*Math.sin(yaw),v.y,strafe*Math.sin(yaw)+v.z*Math.cos(yaw)));
            }ci.cancel();
        }
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="turn")
    private void omnira$mouse(double dx,double dy,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original){
        double roll=Math.toRadians(com.mcmagic.omnira.space.abyss.GravityTransition.roll((Entity)(Object)this,0));
        original.call(dx*Math.cos(roll)-dy*Math.sin(roll),dx*Math.sin(roll)+dy*Math.cos(roll));
    }
    @Inject(method="collide",at=@At("RETURN"),cancellable=true)
    private void omnira$step(Vec3 desired,CallbackInfoReturnable<Vec3> ci){ci.setReturnValue(AbyssMovement.ceilingStep((Entity)(Object)this,desired,ci.getReturnValue()));}
}
