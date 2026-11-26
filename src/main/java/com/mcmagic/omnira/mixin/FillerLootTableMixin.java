package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.loot.FillerLootSource;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LootTable.class)
abstract class FillerLootTableMixin {
    @WrapOperation(method="getRandomItems(Lnet/minecraft/world/level/storage/loot/LootContext;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
            at=@At(value="INVOKE", target="Lnet/minecraft/world/level/storage/loot/LootTable;createStackSplitter(Lnet/minecraft/server/level/ServerLevel;Ljava/util/function/Consumer;)Ljava/util/function/Consumer;"))
    private Consumer<ItemStack> omnira$recordDraw(ServerLevel level, Consumer<ItemStack> output,
                                                  Operation<Consumer<ItemStack>> original, LootContext context) {
        var id = ((LootTable)(Object)this).getLootTableId();
        if (id == null || !id.getPath().startsWith("chests/") ||
                !(id.getNamespace().equals("minecraft") || id.getNamespace().equals("omnira"))) return original.call(level, output);
        var draw = new FillerLootSource();
        ((FillerLootSource.Context)context).omnira$filler(draw);
        Consumer<ItemStack> record = stack -> {
            if (draw.choice != null && stack.is(draw.choice)) draw.stacks.add(stack);
            output.accept(stack);
        };
        return original.call(level, record);
    }
}
