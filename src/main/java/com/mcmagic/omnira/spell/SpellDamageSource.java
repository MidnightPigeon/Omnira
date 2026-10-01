package com.mcmagic.omnira.spell;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;

/** Presentation is independent of damage tags, armor rules and attribution. */
public final class SpellDamageSource extends DamageSource {
    private final String message;
    public SpellDamageSource(Holder<DamageType> type,Entity source,Entity owner,String message) {
        super(type,source,owner);this.message=message;
    }
    public static SpellDamageSource of(LivingEntity target,ResourceKey<DamageType> type,Entity source,Entity owner,String message) {
        return new SpellDamageSource(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type),source,owner,message);
    }
    public static SpellDamageSource physical(LivingEntity target,Entity source,Entity owner,String message) {
        boolean burst=source instanceof com.mcmagic.omnira.spell.entity.SpellEntity spell&&spell.kind()==com.mcmagic.omnira.spell.entity.SpellEntity.Kind.BURST;
        return of(target,burst?(owner instanceof net.minecraft.world.entity.player.Player?DamageTypes.PLAYER_EXPLOSION:DamageTypes.EXPLOSION):DamageTypes.MOB_PROJECTILE,source,owner,message);
    }
    @Override public Component getLocalizedDeathMessage(LivingEntity victim) {
        Entity attacker=getEntity()!=null?getEntity():getDirectEntity();
        return attacker==null?Component.translatable("death.attack.omnira."+message+".unattributed",victim.getDisplayName())
                :Component.translatable("death.attack.omnira."+message,victim.getDisplayName(),attacker.getDisplayName());
    }
}
