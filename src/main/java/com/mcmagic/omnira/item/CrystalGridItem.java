package com.mcmagic.omnira.item;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.ModAttributes;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import java.util.List;

public final class CrystalGridItem extends Item implements ICurioItem {
    public final int capacity;
    private final double mana,regen,reduction,power,cooldownReduction;
    public CrystalGridItem(Properties properties,int capacity,double mana,double regen,double reduction,double power,double cooldownReduction) {
        super(properties.stacksTo(1)); this.capacity=capacity;this.mana=mana;this.regen=regen;this.reduction=reduction;this.power=power;this.cooldownReduction=cooldownReduction;
    }
    @Override public boolean canEquip(SlotContext context,ItemStack stack) { return context.identifier().equals("crystal_grid"); }
    @Override public Multimap<Holder<Attribute>,AttributeModifier> getAttributeModifiers(SlotContext context,ResourceLocation id,ItemStack stack) {
        Multimap<Holder<Attribute>,AttributeModifier> result=LinkedHashMultimap.create();
        if(!context.identifier().equals("crystal_grid") || !PrimarySpellAccessory.primary(context)) return result;
        id=PrimarySpellAccessory.modifierId("crystal_grid");
        add(result,ModAttributes.MAX_MANA,id,mana,AttributeModifier.Operation.ADD_VALUE);
        add(result,ModAttributes.MANA_REGEN,id,regen,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(result,ModAttributes.COST_REDUCTION,id,reduction,AttributeModifier.Operation.ADD_VALUE);
        add(result,ModAttributes.SPELL_POWER,id,power,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        add(result,ModAttributes.COOLDOWN_REDUCTION,id,cooldownReduction,AttributeModifier.Operation.ADD_VALUE);
        return result;
    }
    private static void add(Multimap<Holder<Attribute>,AttributeModifier> map,Holder<Attribute> attribute,ResourceLocation id,double amount,AttributeModifier.Operation op) {
        if(amount!=0) map.put(attribute,new AttributeModifier(id,amount,op));
    }
    public static void open(Player player,int source) {
        ItemStack grid=CrystalGridMenu.locate(player,source);
        if(!(grid.getItem() instanceof CrystalGridItem item)) return;
        player.openMenu(new SimpleMenuProvider((id,inventory,p)->new CrystalGridMenu(id,inventory,source),grid.getHoverName()),
                buffer->{buffer.writeInt(source);buffer.writeInt(item.capacity);});
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        if(!level.isClientSide) open(player,hand==InteractionHand.MAIN_HAND?player.getInventory().selected:-1);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.omnira.grid.capacity",capacity).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
