package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class ThrownAggregate extends ThrowableItemProjectile {
    public ThrownAggregate(EntityType<? extends ThrownAggregate> type,Level level){super(type,level);}
    @Override protected Item getDefaultItem(){return ModItems.UNSTABLE_SPACETIME_AGGREGATE.get();}
    private void detonate(net.minecraft.world.phys.Vec3 point){
        if(!isRemoved() && level() instanceof ServerLevel server){discard();SpacetimeStorm.trigger(server,point);}
    }
    @Override protected void onHit(HitResult hit){detonate(hit.getLocation());}
    @Override public void tick(){
        super.tick();
        if(!isRemoved() && !level().isClientSide){
            var stack=getItem().copy();boolean expired=UnstableAggregateItem.wear(stack);setItem(stack);
            if(expired || getY()<level().getMinBuildHeight())detonate(position());
        }
    }
}
