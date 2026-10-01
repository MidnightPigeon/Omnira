package com.mcmagic.omnira.item;

import com.mcmagic.omnira.mire.MemoryCubeRewards;
import com.mcmagic.omnira.registry.ModAttachments;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** Single-use extracted memory, separate from the placeable cube. */
public final class MemoryCubeItem extends Item {
    private final boolean peaceful;
    public MemoryCubeItem(Properties properties,boolean peaceful){super(properties.stacksTo(16));this.peaceful=peaceful;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(!(level instanceof ServerLevel server))return InteractionResultHolder.success(stack);
        if(!player.isAlive()||player.isSpectator())return InteractionResultHolder.fail(stack);
        var mana=player.getData(ModAttachments.MANA).withMaximum(player.getAttributeValue(ModAttributes.MAX_MANA));
        boolean fatal=peaceful?mana.current()<mana.maximum()/2:player.getHealth()<player.getMaxHealth()/2;
        stack.shrink(1);
        for(var reward:MemoryCubeRewards.roll(server,player.position(),server.getRandom(),peaceful))player.drop(reward,false);
        if(peaceful)player.setData(ModAttachments.MANA,mana.withCurrent(0));
        else {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,200,2));
            if(!fatal)player.setHealth(player.getHealth()-(float)Math.ceil(player.getMaxHealth()/2));
        }
        if(fatal||!player.isAlive()){
            player.setHealth(0);
            player.die(player.damageSources().genericKill());
        }
        return InteractionResultHolder.success(stack);
    }
}
