package com.mcmagic.omnira.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import java.util.List;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public final class FateCurioItem extends Item implements ICurioItem {
    public enum Kind { PSYKER, KHORNE, TZEENTCH, NURGLE, SLAANESH, LAND_KING, SEA_KING, SKY_KING, TIME_DISTORTION;
        public boolean talent(){return this==PSYKER||this==LAND_KING||this==SEA_KING||this==SKY_KING;}
    }
    private final Kind kind;
    public FateCurioItem(Properties properties,Kind kind) {super(properties.stacksTo(1));this.kind=kind;}
    public Kind kind() {return kind;}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        super.appendHoverText(stack,context,lines,flag);
        if(kind!=Kind.KHORNE)lines.add(Component.translatable("tooltip.omnira.fate."+kind.name().toLowerCase(java.util.Locale.ROOT))
                .withStyle(net.minecraft.ChatFormatting.GRAY));
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack) {
        return context.identifier().equals(kind.talent()?"talent":"curse");
    }
    @Override public boolean canUnequip(SlotContext context,ItemStack stack) {return false;}
    @Override public List<Component> getAttributesTooltip(List<Component> lines,TooltipContext context,ItemStack stack) {
        String header="curios.modifiers."+(kind.talent()?"talent":"curse");
        return lines.stream().filter(line->!(line.getContents() instanceof TranslatableContents translation
                && translation.getKey().equals(header))).toList();
    }
    @Override public ICurio.DropRule getDropRule(SlotContext context,net.minecraft.world.damagesource.DamageSource source,
                                                  boolean recentlyHit,ItemStack stack) {return ICurio.DropRule.ALWAYS_KEEP;}
    @Override public Multimap<Holder<Attribute>,AttributeModifier> getAttributeModifiers(
            SlotContext context,ResourceLocation id,ItemStack stack) {
        var modifiers=LinkedHashMultimap.<Holder<Attribute>,AttributeModifier>create();
        if(context.identifier().equals("talent")) {
            if(kind==Kind.LAND_KING)modifiers.put(ModAttributes.DAMAGE_DEALT,new AttributeModifier(id,.1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            if(kind==Kind.SEA_KING){
                modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,new AttributeModifier(id,.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                modifiers.put(net.neoforged.neoforge.common.NeoForgeMod.SWIM_SPEED,new AttributeModifier(id,.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                modifiers.put(net.minecraft.world.entity.ai.attributes.Attributes.SUBMERGED_MINING_SPEED,new AttributeModifier(id,4,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            }
            if(kind==Kind.SKY_KING)modifiers.put(ModAttributes.GLIDING_SPEED,new AttributeModifier(id,.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if(kind==Kind.PSYKER && context.identifier().equals("talent")) {
            modifiers.put(ModAttributes.COOLDOWN_REDUCTION,new AttributeModifier(id,.1,AttributeModifier.Operation.ADD_VALUE));
            modifiers.put(ModAttributes.MANA_REGEN,new AttributeModifier(id,.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            modifiers.put(ModAttributes.SPELL_POWER,new AttributeModifier(id,.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        if(kind==Kind.KHORNE && context.identifier().equals("curse")) {
            modifiers.put(ModAttributes.DAMAGE_DEALT,new AttributeModifier(id,.2,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            modifiers.put(ModAttributes.DAMAGE_TAKEN,new AttributeModifier(id,.5,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
        return modifiers;
    }
}
