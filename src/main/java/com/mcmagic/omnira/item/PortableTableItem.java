package com.mcmagic.omnira.item;

import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.menu.PortableTableMenu;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModDataComponents;
import com.mcmagic.omnira.spell.SpellPattern;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;

public final class PortableTableItem extends Item {
    public static final int SIZE = 5;
    public PortableTableItem(Properties properties) { super(properties); }

    public static SimpleContainer load(ItemStack table) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        table.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(items);
        return new SimpleContainer(items.toArray(ItemStack[]::new));
    }

    public static void store(ItemStack table, Container slots) {
        var items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        for (int i = 0; i < SIZE; i++) items.set(i, slots.getItem(i));
        table.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static boolean accepts(Container slots, int i, ItemStack item) {
        boolean valid = switch (i) {
            case 0 -> CrystalProcessingTableMenu.isTargetMicrocore(item);
            case 1 -> CrystalProcessingTableMenu.isShapeMaterial(item);
            case 2 -> item.is(ModItems.LINGERING_THREAD) || item.is(ModItems.HOLY_HALO)
                    || item.is(ModItems.AMPLIFICATION_STAR) || item.is(ModItems.INFUSION_BUD);
            case 3 -> CrystalProcessingTableMenu.isElement(item);
            case 4 -> item.is(ModItems.SPIRITUAL_CRYSTAL);
            default -> false;
        };
        return valid && SpellPattern.compatibleSlots(slots,i,item)
                && CrystalProcessingTableMenu.chaosSlots(slots,i,item,3,4,2,3);
    }

    public static final int DURATION = 60;

    private static ItemStack result(ItemStack table) {
        var slots = load(table);
        if (!slots.getItem(4).is(ModItems.SPIRITUAL_CRYSTAL)
                || SpellPattern.fromMicrocores(slots.getItem(0), slots.getItem(1)).isEmpty()) return ItemStack.EMPTY;

        for (int i = 0; i < 4; i++) {
            var stack = slots.getItem(i);
            if (stack.isEmpty()) continue;
            if (!accepts(slots,i,stack)) return ItemStack.EMPTY;

        }
        return CrystalProcessingTableMenu.previewCrystal(slots,2,3,3,4,ModItems.LOW_TIER_MAGIC_CRYSTAL.get());
    }

    public static boolean start(ServerPlayer player, ItemStack table) {
        if (table.has(ModDataComponents.PORTABLE_WORK) || player.getMainHandItem() != table) return false;
        var crystal = result(table);
        if (crystal.isEmpty() || !com.mcmagic.omnira.mana.ManaCosts.canSpend(player, cost(crystal))) return false;
        table.set(ModDataComponents.PORTABLE_WORK, DURATION);
        return true;
    }

    private static int cost(ItemStack crystal) {
        return 2 * com.mcmagic.omnira.spell.SpellPayload.of(crystal).baseCost();
    }

    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !newStack.is(this);
    }

    @Override public void inventoryTick(ItemStack table, Level level, net.minecraft.world.entity.Entity entity, int index, boolean selected) {
        Integer remaining = table.get(ModDataComponents.PORTABLE_WORK);
        if (remaining == null || !(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != table) {
            table.remove(ModDataComponents.PORTABLE_WORK);
            return;
        }
        if (remaining > 1) {
            table.set(ModDataComponents.PORTABLE_WORK,remaining-1);
            return;
        }
        table.remove(ModDataComponents.PORTABLE_WORK);
        var crystal = result(table);
        if (crystal.isEmpty() || !com.mcmagic.omnira.mana.ManaEvents.trySpend(player,cost(crystal))) {
            player.displayClientMessage(Component.translatable("message.omnira.portable_table_failed"),true);
            return;
        }
        var slots = load(table);
        for (int i = 0; i < SIZE; i++) slots.removeItem(i,1);
        store(table,slots);
        player.drop(crystal,false);
        com.mcmagic.omnira.progression.BranchProgress.record(player,"spell_writing");
    }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var table = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(table);
        if (table.has(ModDataComponents.PORTABLE_WORK)) return InteractionResultHolder.sidedSuccess(table,level.isClientSide);
        if (player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                if (!start(server,table)) player.displayClientMessage(Component.translatable("message.omnira.portable_table_failed"),true);
            } else {
                int index = hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : 40;
                player.openMenu(new SimpleMenuProvider((id,inv,p) -> new PortableTableMenu(id,inv,index),table.getHoverName()),
                        buf -> buf.writeInt(index));
            }
        }
        return InteractionResultHolder.sidedSuccess(table,level.isClientSide);
    }
}
