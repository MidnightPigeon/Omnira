package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.mcmagic.omnira.fate.PredatorFate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
abstract class PredatorLootMixin {
    /** Re-roll the same resolved table/context before Mob clears a custom table. */
    @WrapOperation(method="dropFromLootTable",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V"))
    private void omnira$predatorLoot(LootTable table,LootParams params,long seed,java.util.function.Consumer<ItemStack> consumer,Operation<Void> original){
        original.call(table,params,seed,consumer);
        var attacker=params.getOptionalParameter(LootContextParams.ATTACKING_ENTITY);
        var credited=attacker instanceof net.minecraft.server.level.ServerPlayer?attacker:params.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
        if(PredatorFate.extraLoot(credited))original.call(table,params,0L,consumer);
    }
}
