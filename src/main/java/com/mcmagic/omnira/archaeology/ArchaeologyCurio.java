package com.mcmagic.omnira.archaeology;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

/** Archaeological accessories share the normal additive attribute pipeline. */
public final class ArchaeologyCurio extends Item implements ICurioItem {
    public enum Kind { CALLING_CARD, ENHANCED_CARD, D, C, B, A, JADE_RING, LUST_RING, GREED_RING }
    private final Kind kind;
    public ArchaeologyCurio(Properties properties, Kind kind) { super(properties.stacksTo(1)); this.kind=kind; }
    @Override public boolean canEquip(SlotContext context, ItemStack stack) {
        return context.identifier().equals(kind==Kind.JADE_RING||kind==Kind.LUST_RING||kind==Kind.GREED_RING?"ring":"charm");
    }
    @Override public Multimap<Holder<Attribute>,AttributeModifier> getAttributeModifiers(SlotContext context, ResourceLocation id, ItemStack stack) {
        var out=LinkedHashMultimap.<Holder<Attribute>,AttributeModifier>create();
        if(!canEquip(context,stack))return out;
        if(kind==Kind.LUST_RING){
            out.put(ModAttributes.MELEE_DAMAGE,new AttributeModifier(id,.25,AttributeModifier.Operation.ADD_VALUE));
            out.put(ModAttributes.LIFESTEAL,new AttributeModifier(id,.25,AttributeModifier.Operation.ADD_VALUE));
        }
        if(kind==Kind.GREED_RING)out.put(ModAttributes.SPELL_POWER,new AttributeModifier(id,.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if(kind==Kind.CALLING_CARD||kind==Kind.ENHANCED_CARD||kind==Kind.JADE_RING)
            out.put(Attributes.MOVEMENT_SPEED,new AttributeModifier(id,kind==Kind.JADE_RING?-.2:kind==Kind.ENHANCED_CARD?.15:.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        if(kind==Kind.D||kind==Kind.C||kind==Kind.B||kind==Kind.A) {
            out.put(ModAttributes.DAMAGE_TAKEN,new AttributeModifier(id,kind==Kind.D||kind==Kind.C?-.1:-.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if(kind!=Kind.D)out.put(ModAttributes.SPELL_POWER,new AttributeModifier(id,.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if(kind==Kind.B||kind==Kind.A)out.put(ModAttributes.COST_REDUCTION,new AttributeModifier(id,10,AttributeModifier.Operation.ADD_VALUE));
        }
        return out;
    }
    @Override public void curioTick(SlotContext context,ItemStack stack) {
        LivingEntity wearer=context.entity();
        if(wearer.level().isClientSide||!canEquip(context,stack))return;
        if(kind==Kind.JADE_RING&&(!wearer.hasEffect(MobEffects.ABSORPTION)||wearer.tickCount%200==0))wearer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,1,false,false));
        if(kind==Kind.A&&(!wearer.hasEffect(MobEffects.REGENERATION)||wearer.tickCount%200==0))wearer.addEffect(new MobEffectInstance(MobEffects.REGENERATION,600,1,false,false));
        if(kind==Kind.A&&wearer.tickCount%600==0)wearer.addEffect(new MobEffectInstance(MobEffects.SATURATION,1,0,false,false));
    }
    public static int lootBonus(Entity entity) {
        if(!(entity instanceof LivingEntity living))return 0;
        return CuriosApi.getCuriosInventory(living).map(inv->inv.findCurios(s->s.getItem() instanceof ArchaeologyCurio)
                .stream().filter(s->s.slotContext().identifier().equals("charm"))
                .mapToInt(s->switch(((ArchaeologyCurio)s.stack().getItem()).kind){case CALLING_CARD->1;case ENHANCED_CARD->2;default->0;}).sum()).orElse(0);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        if(kind==Kind.CALLING_CARD||kind==Kind.ENHANCED_CARD||kind==Kind.JADE_RING||kind==Kind.A)
            lines.add(net.minecraft.network.chat.Component.translatable("tooltip.omnira.archaeology."+kind.name().toLowerCase(java.util.Locale.ROOT)).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
