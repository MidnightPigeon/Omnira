package com.mcmagic.omnira.alchemy;

import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class ThrownAlchemicalWaste extends ThrowableItemProjectile {
    public ThrownAlchemicalWaste(EntityType<? extends ThrownAlchemicalWaste> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return AlchemyMaterials.WASTE.get();}
    @Override protected void onHitEntity(EntityHitResult hit){
        super.onHitEntity(hit);
        if(!level().isClientSide&&hit.getEntity() instanceof LivingEntity target)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,100,1),getOwner());
    }
    @Override protected void onHit(HitResult hit){super.onHit(hit);if(!level().isClientSide)discard();}
}
