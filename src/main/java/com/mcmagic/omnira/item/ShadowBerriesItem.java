package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public final class ShadowBerriesItem extends BlockItem {
    public ShadowBerriesItem(Block block,Properties properties){
        super(block,properties.food(new FoodProperties.Builder().nutrition(3).saturationModifier(2F/3F).build()));
    }
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
        var result=super.finishUsingItem(stack,level,entity);
        if(!level.isClientSide&&entity instanceof Player player){
            var mana=player.getData(ModAttachments.MANA).withMaximum(player.getAttributeValue(ModAttributes.MAX_MANA));
            player.setData(ModAttachments.MANA,mana.withCurrent(mana.current()+mana.maximum()*.2));
        }
        return result;
    }
}
