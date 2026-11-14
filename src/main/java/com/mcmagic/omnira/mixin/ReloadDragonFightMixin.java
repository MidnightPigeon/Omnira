package com.mcmagic.omnira.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mcmagic.omnira.spell.world.ReloadDragonEncounter;
import java.util.List;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(EndDragonFight.class)
abstract class ReloadDragonFightMixin {
    @ModifyExpressionValue(method={"findOrCreateDragon","scanState"}, at=@At(value="INVOKE",
            target="Lnet/minecraft/server/level/ServerLevel;getDragons()Ljava/util/List;"))
    private List<? extends EnderDragon> omnira$eligibleDragons(List<? extends EnderDragon> dragons) {
        return dragons.stream().filter(dragon -> !ReloadDragonEncounter.unrelated(dragon)).toList();
    }
}
