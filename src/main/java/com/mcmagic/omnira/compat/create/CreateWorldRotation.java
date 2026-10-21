package com.mcmagic.omnira.compat.create;

import net.minecraft.world.level.block.entity.BlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.kinetics.millstone.MillstoneBlockEntity;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;

/** Loaded only behind the Create presence check; native recipe ticks retain their validation. */
public final class CreateWorldRotation {
    public static void complete(BlockEntity entity){
        if(entity instanceof com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity press){
            var work=press.getPressingBehaviour();
            if(work.running&&press.getSpeed()!=0){
                if(work.runningTicks<120){work.runningTicks=120;work.tick();}
                else if(work.runningTicks>120){work.runningTicks=241;work.tick();}
                press.sendData();
            }return;
        }
        if(entity instanceof MechanicalMixerBlockEntity mixer&&mixer.running&&mixer.processingTicks>0){mixer.runningTicks=20;mixer.processingTicks=1;mixer.tick();mixer.sendData();}
        else if(entity instanceof MillstoneBlockEntity mill&&mill.timer>0&&mill.getSpeed()!=0){mill.timer=1;mill.tick();mill.sendData();}
        else if(entity instanceof SawBlockEntity saw&&saw.inventory.remainingTime>0&&saw.getSpeed()!=0&&!saw.inventory.appliedRecipe){saw.inventory.remainingTime=1;saw.tick();saw.sendData();}
        else if(entity instanceof CrushingWheelControllerBlockEntity crusher&&crusher.crushingspeed>0&&crusher.inventory.remainingTime>0&&!crusher.inventory.appliedRecipe){crusher.inventory.remainingTime=1;crusher.tick();crusher.sendData();}
    }
    private CreateWorldRotation(){}
}
