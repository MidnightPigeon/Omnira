package com.mcmagic.omnira.space.geodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class DeflectionBlock extends TransparentBlock {
    public DeflectionBlock(Properties properties){super(properties);}
    @Override public void stepOn(Level level,BlockPos pos,BlockState state,Entity entity){
        super.stepOn(level,pos,state,entity);
        if(level.isClientSide||!active(entity,pos))return;
        var data=entity.getPersistentData();long now=level.getGameTime();
        if(!data.contains("DeflectionStepAt")||data.getLong("DeflectionStepAt")!=pos.asLong()
                ||now-data.getLong("DeflectionStepTick")>1)
            DeflectionMotion.turn(entity,level.random.nextBoolean()?10:-10);
        data.putLong("DeflectionStepAt",pos.asLong());data.putLong("DeflectionStepTick",now);
    }
    public static boolean active(Entity entity,BlockPos pos){
        return !entity.isSpectator()&&entity.level().getBiome(pos).is(DeflectionContent.BIOME)
                &&!(entity instanceof net.minecraft.world.entity.player.Player player
                &&com.mcmagic.omnira.spacetime.AnchoringSigilItem.equipped(player));
    }
}
