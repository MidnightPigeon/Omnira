package com.mcmagic.omnira.item.bottle;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.particles.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

public final class ThrownPocketBottle extends ThrowableItemProjectile {
    private boolean resolved;
    public ThrownPocketBottle(EntityType<? extends ThrownPocketBottle> type,Level level) {super(type,level);}
    @Override protected Item getDefaultItem() {return ModItems.POCKET_MAGIC_BOTTLE.get();}
    @Override protected void onHit(HitResult hit) {
        if(!(level() instanceof ServerLevel level) || resolved) return;
        Vec3 point=hit.getLocation();
        if(hit instanceof BlockHitResult block) point=point.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.6));
        if(PocketBottleItem.release(level,getItem(),point,getUUID())) {
            resolved=true;
            level.playSound(null,point.x,point.y,point.z,SoundEvents.GLASS_BREAK,SoundSource.PLAYERS,.8F,1.1F);
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM,new ItemStack(Items.GLASS_BOTTLE)),point.x,point.y,point.z,12,.15,.15,.15,.05);
            discard();
        } else recover(point);
    }
    private void recover(Vec3 point) {
        if(resolved || level().isClientSide) return;
        var id=getItem().get(com.mcmagic.omnira.registry.ModDataComponents.BOTTLE_CAPTURE);
        var storage=BottleStorage.get((ServerLevel)level());
        if(id==null || !storage.owns(id,getUUID())) {resolved=true;discard();return;}
        if(storage.get(id).getBoolean("CreativeCopy")) {
            storage.remove(id);
            if(getOwner() instanceof net.minecraft.world.entity.player.Player player) PocketBottleItem.message(player,"retained");
            resolved=true;discard();return;
        }
        var dropped=new ItemEntity(level(),point.x,Math.max(level().getMinBuildHeight()+2,point.y),point.z,getItem().copy());
        dropped.setUnlimitedLifetime();
        if(level().addFreshEntity(dropped)) {
            storage.unclaim(id,getUUID());
            if(getOwner() instanceof net.minecraft.world.entity.player.Player player) PocketBottleItem.message(player,"retained");
            resolved=true;discard();
        }
    }
    @Override public void tick() {
        super.tick();
        if(!isRemoved() && !level().isClientSide && (tickCount>200 || getY()<level().getMinBuildHeight()+1)) recover(position());
    }
}
