package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import com.mcmagic.omnira.loot.FillerLootSource;
import java.util.List;
import java.util.IdentityHashMap;
import net.minecraft.world.item.Item;
import java.util.function.Consumer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.entries.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootPool.class)
abstract class FillerLootPoolMixin {
    @Shadow @Final private List<LootPoolEntryContainer> entries;
    @Unique private IdentityHashMap<LootPoolEntry, Item> omnira$items;

    @Unique private Item omnira$choice(LootPoolEntry selected, List<LootPoolEntry> choices) {
        if (choices.size() < 2) return null;
        if (omnira$items == null) {
            omnira$items = new IdentityHashMap<>();
            for (var container : entries) {
                if (container.getClass() == LootItem.class) {
                    var entry = ((FillerEntryAccessor)container).omnira$entry();
                    omnira$items.put(entry, ((FillerItemAccessor)container).omnira$item().value());
                }
            }
        }
        Item item = omnira$items.get(selected);
        if (item == null) return null;
        for (var choice : choices) {
            Item alternative = omnira$items.get(choice);
            if (alternative != null && alternative != item) return item;
        }
        return null;
    }

    @WrapOperation(method="addRandomItem", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/storage/loot/entries/LootPoolEntry;createItemStack(Ljava/util/function/Consumer;Lnet/minecraft/world/level/storage/loot/LootContext;)V"))
    private void omnira$recordChoice(LootPoolEntry entry, Consumer<ItemStack> output, LootContext context,
                                      Operation<Void> original, @Local List<LootPoolEntry> choices) {
        var draw = ((FillerLootSource.Context)context).omnira$filler();
        if (draw == null) {
            original.call(entry, output, context);
            return;
        }
        Item outer = draw.choice;
        draw.choice = omnira$choice(entry, choices);
        try {
            original.call(entry, output, context);
        } finally {
            draw.choice = outer;
        }
    }
}
