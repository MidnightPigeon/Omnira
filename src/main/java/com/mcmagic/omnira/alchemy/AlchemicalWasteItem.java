package com.mcmagic.omnira.alchemy;

import net.minecraft.sounds.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public final class AlchemicalWasteItem extends Item {
    public AlchemicalWasteItem(Properties properties){super(properties);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!level.isClientSide){
            var projectile=new ThrownAlchemicalWaste(AlchemyMaterials.THROWN_WASTE.get(),level);
            projectile.setOwner(player);projectile.setPos(player.getX(),player.getEyeY()-.1,player.getZ());
            projectile.setItem(stack.copyWithCount(1));
            projectile.shootFromRotation(player,player.getXRot(),player.getYRot(),0,1.2F,1F);
            if(!level.addFreshEntity(projectile))return InteractionResultHolder.fail(stack);
            level.playSound(null,player.getX(),player.getY(),player.getZ(),SoundEvents.SNOWBALL_THROW,SoundSource.PLAYERS,.5F,1F);
            player.awardStat(Stats.ITEM_USED.get(this));
            if(!player.getAbilities().instabuild)stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
}
