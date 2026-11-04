package com.mcmagic.omnira.mixin;

import com.mcmagic.omnira.space.sea.RodStrike;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class StarlightRodAttackMixin extends LivingEntity implements RodStrike.Wielder {
    protected StarlightRodAttackMixin(EntityType<? extends LivingEntity> type,Level level){super(type,level);}
    @Unique private RodStrike omnira$strike;

    public void omnira$offhandStrike(Entity target){
        if(omnira$strike!=null)return;
        var player=(Player)(Object)this;
        omnira$strike=new RodStrike(player,player.getMainHandItem(),player.getOffhandItem());
        try{player.attack(target);}finally{omnira$strike=null;}
    }
    @Inject(method="getItemBySlot",at=@At("HEAD"),cancellable=true)
    private void omnira$rodHand(EquipmentSlot slot,CallbackInfoReturnable<ItemStack> ci){
        if(omnira$strike==null)return;
        if(slot==EquipmentSlot.MAINHAND)ci.setReturnValue(((Player)(Object)this).getInventory().offhand.get(0));
        else if(slot==EquipmentSlot.OFFHAND)ci.setReturnValue(ItemStack.EMPTY);
    }
    @Override public double getAttributeValue(Holder<Attribute> key){
        if(omnira$strike!=null){
            var stat=omnira$strike.attribute(key);
            if(stat!=null)return stat.getValue();
        }
        return super.getAttributeValue(key);
    }
}
