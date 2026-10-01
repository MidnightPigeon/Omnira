package com.mcmagic.omnira.item;

import com.mcmagic.omnira.mana.ManaCosts;
import com.mcmagic.omnira.menu.ArquebusMenu;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import java.util.List;

public final class ArcaneArquebusItem extends Item {
    public ArcaneArquebusItem(Properties properties) {super(properties.stacksTo(1));}
    public static boolean accepts(ItemStack stack) {
        if(!CrystalGridMenu.isCrystal(stack)) return false;
        var pattern=stack.get(ModDataComponents.SPELL_PATTERN);
        return pattern.shapeKeyword()==SpellShapeKeyword.PROJECTILE
                && (pattern.targetKeyword()==SpellTargetKeyword.TARGET || pattern.targetKeyword()==SpellTargetKeyword.AIM);
    }
    public static ItemStack crystal(ItemStack gun) {
        return gun.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).stream().findFirst().orElse(ItemStack.EMPTY);
    }
    public static boolean upgraded(ItemStack gun) {return ArquebusPlugin.of(gun)!=ArquebusPlugin.NONE;}
    public static double manaCost(Player player,ItemStack gun,SpellPayload payload) {
        return ManaCosts.cost(payload.baseCost(),ArquebusPlugin.of(gun).costMultiplier,CastAttributes.reduction(player,0));
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var gun=player.getItemInHand(hand);
        if(hand!=InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(gun);
        if(player.isShiftKeyDown()) {
            if(level.isClientSide) return InteractionResultHolder.success(gun);
            int source=player.getInventory().selected;
            player.openMenu(new SimpleMenuProvider((id,inventory,p)->new ArquebusMenu(id,inventory,source),gun.getHoverName()),b->b.writeInt(source));
            return InteractionResultHolder.success(gun);
        }
        if(!IndependentSpellCooldown.ready(player,gun)) return InteractionResultHolder.pass(gun);
        if(level.isClientSide) return InteractionResultHolder.success(gun);
        if(player.containerMenu instanceof ArquebusMenu) return InteractionResultHolder.fail(gun);
        return castLoaded((ServerPlayer)player,gun)?InteractionResultHolder.success(gun):InteractionResultHolder.fail(gun);
    }
    public static boolean castLoaded(ServerPlayer player,ItemStack gun) {
        if(!IndependentSpellCooldown.ready(player,gun) || !fire(player,gun)) return false;
        var profile=ArquebusPlugin.of(gun);
        IndependentSpellCooldown.start(player,gun,SpellCooldowns.ticks(profile.cooldownTicks,CastAttributes.cooldownReduction(player,0),profile.shots));
        ArquebusEvents.queue(player,gun);
        return true;
    }
    static boolean fire(ServerPlayer player,ItemStack gun) {
        if(player.getMainHandItem()!=gun || !(gun.getItem() instanceof ArcaneArquebusItem) || !player.isAlive() || player.isSpectator()) return false;
        var crystal=crystal(gun);
        if(!accepts(crystal)) {player.displayClientMessage(Component.translatable("message.omnira.arquebus.empty"),true);return false;}
        var payload=SpellPayload.of(crystal);
        double cost=manaCost(player,gun,payload);
        var mana=player.getData(ModAttachments.MANA);
        if(!mana.canSpend(cost)) {player.displayClientMessage(Component.translatable("message.omnira.mana.insufficient"),true);return false;}
        if(!SpellCasting.castArquebus(player,crystal.get(ModDataComponents.SPELL_PATTERN),payload,
                CastAttributes.power(player,ArquebusPlugin.of(gun).powerBonus),ArquebusPlugin.of(gun))) {
            player.displayClientMessage(Component.translatable("message.omnira.crystal.invalid_target"),true);return false;
        }
        player.setData(ModAttachments.MANA,mana.spend(cost));
        player.swing(InteractionHand.MAIN_HAND,true);
        return true;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        var plugin=ArquebusPlugin.of(stack);
        boolean ghost=plugin==ArquebusPlugin.KINGS_NEW_CLOTHES;
        lines.add(Component.translatable("tooltip.omnira.arquebus.flavor."+plugin.getSerializedName())
                .withStyle(style->style.withItalic(true).withColor(ghost?0x99CCFF:plugin==ArquebusPlugin.ANCESTOR_LAUNCHER?0xFF8888:0xFFFFFF)));
        if(upgraded(stack)) lines.add(Component.translatable("tooltip.omnira.arquebus."+(ghost?"sense":"explosion"))
                .withStyle(ghost?ChatFormatting.AQUA:ChatFormatting.RED));
        lines.add(Component.translatable("tooltip.omnira.arquebus."+(ghost?"ghost_stats":"stats")).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.omnira.arquebus."+(ghost?"ghost_cost":plugin==ArquebusPlugin.ANCESTOR_LAUNCHER?"ancestor_cost":"cost")).withStyle(ChatFormatting.GRAY));
        if(upgraded(stack)) lines.add((ghost?ModItems.KINGS_NEW_CLOTHES:ModItems.ANCESTOR_LAUNCHER).get().getDescription().copy()
                .withStyle(ghost?ChatFormatting.BLUE:ChatFormatting.DARK_RED));
        var crystal=crystal(stack);
        if(!crystal.isEmpty()) lines.add(crystal.getHoverName().copy().withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.arquebus.load").withStyle(ChatFormatting.DARK_GRAY));
    }
}
