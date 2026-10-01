package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

@EventBusSubscriber(modid="omnira")
public final class AnchoringSigilItem extends Item implements ICurioItem {
    public AnchoringSigilItem(Properties properties){super(properties.stacksTo(1));}
    public static boolean equipped(LivingEntity wearer){
        var handler=CuriosApi.getCuriosInventory(wearer).flatMap(inv->inv.getStacksHandler("charm"));
        if(handler.isEmpty())return false;
        var stacks=handler.get().getStacks();
        for(int i=0;i<stacks.getSlots();i++)if(stacks.getStackInSlot(i).is(ModItems.SPACETIME_ANCHORING_SIGIL.get()))return true;
        return false;
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack){return context.identifier().equals("charm");}
    @Override public void curioTick(SlotContext context,ItemStack stack){
        if(!context.cosmetic() && !context.entity().level().isClientSide){
            context.entity().removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            context.entity().removeEffect(MobEffects.WITHER);
        }
    }
    @SubscribeEvent public static void effect(MobEffectEvent.Applicable event){
        var effect=event.getEffectInstance().getEffect();
        if((effect.equals(MobEffects.MOVEMENT_SLOWDOWN)||effect.equals(MobEffects.WITHER)) && equipped(event.getEntity()))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    @SubscribeEvent public static void witherDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event){
        if(event.getSource().is(net.minecraft.world.damagesource.DamageTypes.WITHER) && equipped(event.getEntity()))event.setCanceled(true);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.spacetime_anchoring_sigil.immunity").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.spacetime_anchoring_sigil.time_immunity").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.spacetime_anchoring_sigil.death").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
