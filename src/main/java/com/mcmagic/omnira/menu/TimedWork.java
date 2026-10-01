package com.mcmagic.omnira.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;

/** Menu-scoped work; changed ingredients or closing the screen cancel without a debit. */
final class TimedWork {
    final DataSlot progress = DataSlot.standalone();
    private final int duration;
    private long started;
    private ItemStack[] snapshot;
    TimedWork(int duration) { this.duration = duration; }
    boolean active() { return progress.get() > 0; }
    void cancel() { progress.set(0); snapshot=null; }
    float fraction() { return Math.min(1F, progress.get() / (float) duration); }
    boolean start(Player player, Container container) {
        if (active()) return false;
        snapshot = new ItemStack[container.getContainerSize()];
        for (int i=0;i<snapshot.length;i++) snapshot[i] = container.getItem(i).copy();
        started = player.level().getGameTime();
        progress.set(1);
        return true;
    }
    void tick(Player player, Container container, boolean valid, Runnable finish) {
        if (player.level().isClientSide || !active()) return;
        for (int i=0; valid && i<snapshot.length; i++)
            valid = ItemStack.matches(snapshot[i],container.getItem(i));
        if (!valid) { progress.set(0); snapshot=null; return; }
        int elapsed = (int)Math.min(duration,player.level().getGameTime()-started);
        if (elapsed >= duration) {
            progress.set(0);
            snapshot=null;
            finish.run();
        } else progress.set(Math.max(1,elapsed));
    }
}
