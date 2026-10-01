package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mcmagic.omnira.mana.DreamAffinityLoot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class AffinityBlockLootMixin {
    @WrapMethod(method="getDrops")
    private java.util.List<ItemStack> omnira$mining(LootParams.Builder params,Operation<java.util.List<ItemStack>> original) {
        return DreamAffinityLoot.mining(params.getOptionalParameter(LootContextParams.THIS_ENTITY),()->original.call(params));
    }
}
