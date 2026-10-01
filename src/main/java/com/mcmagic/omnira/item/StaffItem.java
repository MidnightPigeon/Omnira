package com.mcmagic.omnira.item;

import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.item.staff.StaffCastContext;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import net.minecraft.world.level.Level;
import java.util.List;

public final class StaffItem extends Item {
    public static final int CHAIN_COOLDOWN_TICKS=80;
    public StaffItem(Properties properties) {super(properties.stacksTo(1));}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack held=player.getItemInHand(hand);
        // Offhand casting is dispatched by the physical-use click packet, not held-use repeats.
        if(hand==InteractionHand.OFF_HAND) return InteractionResultHolder.success(held);
        if(level.isClientSide) return InteractionResultHolder.success(held);
        if(player.isShiftKeyDown()) {CrystalGridItem.open(player,-2);return InteractionResultHolder.success(held);}
        return castHeld((ServerPlayer)player,held);
    }
    public InteractionResultHolder<ItemStack> castHeld(ServerPlayer player,ItemStack held) {
        if(!player.isAlive() || player.isSpectator() || player.getCooldowns().isOnCooldown(this) || player.containerMenu!=player.inventoryMenu) return InteractionResultHolder.fail(held);
        if(held.getItem()!=this || held!=player.getMainHandItem() && held!=player.getOffhandItem()) return InteractionResultHolder.fail(held);
        var context=StaffCastContext.capture(player,held);
        var stats=context.stats();
        int baseCooldown=sequenceCooldown(CrystalGridMenu.locate(player,-2),stats.shots(),stats.cooldownTicks());
        int successes=castSequenceContents(player,context,1);
        if(successes==0) return InteractionResultHolder.fail(held);
        int cooldown=SpellCooldowns.ticks(baseCooldown,context.cooldownReduction(),1);
        player.getCooldowns().addCooldown(this,cooldown+(stats.shots()-1)*com.mcmagic.omnira.item.staff.StaffEvents.SHOT_INTERVAL);
        com.mcmagic.omnira.item.staff.StaffEvents.queue(player,held,stats.shots()-1,cooldown);
        return InteractionResultHolder.success(held);
    }
    public static int sequenceCooldown(ItemStack grid,int shots,int staffCooldown) {
        if(!(grid.getItem() instanceof CrystalGridItem item)) return staffCooldown;
        ItemStack preview=grid.copy();
        for(int i=0;i<shots;i++) {
            int slot=CrystalGridMenu.next(preview,item.capacity);
            if(slot<0) return staffCooldown;
            preview.set(ModDataComponents.GRID_CURSOR,(slot+1)%item.capacity);
            int next=CrystalGridMenu.next(preview,item.capacity);
            if(next<=slot) return CHAIN_COOLDOWN_TICKS;
        }
        return staffCooldown;
    }
    public static int castSequence(ServerPlayer player,ItemStack staff,int count) {
        if(!(staff.getItem() instanceof StaffItem)) return 0;
        return castSequenceContents(player,StaffCastContext.capture(player,staff),count);
    }
    private static int castSequenceContents(ServerPlayer player,StaffCastContext context,int count) {
        ItemStack grid=CrystalGridMenu.locate(player,-2);
        if(!(grid.getItem() instanceof CrystalGridItem item)) {
            player.displayClientMessage(Component.translatable("message.omnira.grid.missing"),true);return 0;
        }
        NonNullList<ItemStack> list=NonNullList.withSize(item.capacity,ItemStack.EMPTY);
        grid.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(list);
        int successes=0;
        for(int shot=0;shot<count;shot++) {
            int slot=CrystalGridMenu.next(grid,item.capacity);
            if(slot<0) {player.displayClientMessage(Component.translatable("message.omnira.grid.empty"),true);break;}
            ItemStack crystal=list.get(slot);
            SpellPayload payload=com.mcmagic.omnira.item.staff.StaffEnhancements.apply(SpellPayload.of(crystal),context.enhancements());
            double cost=ManaCosts.cost(payload.baseCost(),1,context.reduction());
            if(!player.getData(ModAttachments.MANA).canSpend(cost)) {
                player.displayClientMessage(Component.translatable("message.omnira.mana.insufficient"),true);break;
            }
            if(!SpellCasting.cast(player,crystal.get(ModDataComponents.SPELL_PATTERN),payload,context.power(),
                    new SpellCasting.Modifiers(context.stats().speedMultiplier(),context.stats().rangeMultiplier()))) {
                player.displayClientMessage(Component.translatable("message.omnira.crystal.invalid_target"),true);break;
            }
            player.setData(ModAttachments.MANA,player.getData(ModAttachments.MANA).spend(cost));
            grid.set(ModDataComponents.GRID_CURSOR,(slot+1)%item.capacity);
            successes++;
        }
        return successes;
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        var assembly=com.mcmagic.omnira.item.staff.StaffAssembly.of(stack);
        boolean details=net.neoforged.fml.loading.FMLEnvironment.dist.isClient() && net.minecraft.client.gui.screens.Screen.hasShiftDown();
        if(details) {
            for(var part:assembly.components()) lines.add(part.getHoverName().copy().withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
            for(var kind:com.mcmagic.omnira.item.staff.StaffPart.SlotKind.values())
                lines.add(Component.translatable("tooltip.omnira.staff.slots."+kind.getSerializedName(),
                        assembly.usedSlots(kind),assembly.slotCapacity(kind)).withStyle(net.minecraft.ChatFormatting.GRAY));
        } else lines.add(Component.translatable("tooltip.omnira.staff.details").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        var stats=assembly.stats();
        if(stats.rangeMultiplier()>1) lines.add(Component.translatable("tooltip.omnira.staff.range").withStyle(net.minecraft.ChatFormatting.AQUA));
        if(stats.speedMultiplier()>1) lines.add(Component.translatable("tooltip.omnira.staff.speed",
                Math.round((stats.speedMultiplier()-1)*100)).withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.omnira.staff.base_cycle",stats.shots(),com.mcmagic.omnira.item.staff.StaffPartItem.format(stats.cooldownTicks()/20D)).withStyle(net.minecraft.ChatFormatting.GRAY));
        if(stats.shots()>1) lines.add(Component.translatable("tooltip.omnira.staff.interval","0.3").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.staff.edit").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
