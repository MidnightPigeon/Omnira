package com.mcmagic.omnira.mixin;

import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCooldowns.class)
public interface ItemCooldownMapAccessor {
    @Accessor("cooldowns") Map<Item, ?> omnira$cooldownEntries();
}
