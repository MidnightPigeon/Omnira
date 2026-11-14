package com.mcmagic.omnira.weaver;

import com.mcmagic.omnira.entity.WeaverSpirit;
import com.mcmagic.omnira.combat.OmniraDamage;
import com.mcmagic.omnira.registry.ModEntityTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.*;
import java.util.*;

/** Authored attack channels, reused by the boss and its reward. No terrain operations occur here. */
public final class WeaverCombat {
    private static final ThreadLocal<Boolean> REACTING=ThreadLocal.withInitial(()->false);
    public static boolean reacting(){return REACTING.get();}
    public static float damage(LivingEntity owner,float base){
        if(owner instanceof net.minecraft.world.entity.player.Player)return (float)(base*com.mcmagic.omnira.spell.CastAttributes.power(owner,0));
        return base+(owner instanceof WeaverSpirit boss?2*boss.strength():0);
    }
    public static DamageSource source(LivingEntity target,ResourceKey<DamageType> kind,Entity direct,Entity owner,boolean projectile,boolean explosion){
        return new DamageSource(target.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(kind),direct,owner){
            @Override public boolean is(TagKey<DamageType> tag){return (tag==DamageTypeTags.ALWAYS_HURTS_ENDER_DRAGONS && direct instanceof WeaverBolt && owner instanceof WeaverSpirit)||tag==DamageTypeTags.BYPASSES_COOLDOWN||projectile&&tag==DamageTypeTags.IS_PROJECTILE||explosion&&tag==DamageTypeTags.IS_EXPLOSION||super.is(tag);}
        };
    }
    public static WeaverBolt bolt(LivingEntity owner,LivingEntity target,Vec3 origin,Vec3 direction){
        var entity=ModEntityTypes.WEAVER_BOLT.get().create(owner.level());
        entity.launch(owner,target,origin,direction);owner.level().addFreshEntity(entity);return entity;
    }
    public static void bend(LivingEntity owner,boolean outward){
        if(!(owner.level() instanceof ServerLevel level))return;
        Vec3 center=owner.getBoundingBox().getCenter();visual(level,outward?0:1,center,center,15);
        boolean previous=REACTING.get();REACTING.set(true);
        try{
            for(var target:level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(15),t->outward?trueTarget(t,owner):eligible(t,owner))){
                var delta=target.getBoundingBox().getCenter().subtract(center);double length=delta.length();if(length>15)continue;
                var src=source(target,outward?OmniraDamage.TRUE_DAMAGE:DamageTypes.INDIRECT_MAGIC,owner,owner,false,outward);
                if(outward)OmniraDamage.trueDamage(target,damage(owner,5),src);else target.hurt(src,damage(owner,8));
                Vec3 motion=length<.001?new Vec3(0,1,0):delta.scale((outward?2.5:-1.8)/length);
                target.setDeltaMovement(target.getDeltaMovement().add(motion).add(0,outward?.35:.15,0));target.hurtMarked=true;
            }
        }finally{REACTING.set(previous);}
    }
    private static boolean trueTarget(LivingEntity target,LivingEntity owner){return target!=owner&&target.isAlive()&&!(target instanceof ArmorStand);}
    public static boolean eligible(LivingEntity target,LivingEntity owner){return trueTarget(target,owner)&&!target.isSpectator()
            &&(!(target instanceof net.minecraft.world.entity.player.Player p)||!p.isCreative());}
    public static void lockedRay(LivingEntity owner,LivingEntity target){
        if(!(owner.level() instanceof ServerLevel level)||!trueTarget(target,owner))return;
        visual(level,2,owner.getEyePosition(),target.getBoundingBox().getCenter(),0);rayHit(owner,target);
    }
    public static void ray(LivingEntity owner,Vec3 direction){
        if(!(owner.level() instanceof ServerLevel level))return;
        Vec3 start=owner.getEyePosition(),end=start.add(direction.normalize().scale(64));visual(level,2,start,end,0);
        for(var contact:level.getEntities(owner,new AABB(start,end).inflate(1),e->!(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon))){
            var target=com.mcmagic.omnira.spell.SpellCasting.livingTarget(contact);
            if(target==null || !trueTarget(target,owner) || contact!=target && !contact.isPickable())continue;
            // Multipart parents have a broad envelope; only their actual hitboxes count.
            if(contact==target && target.isMultipartEntity())continue;
            var box=contact.getBoundingBox().inflate(.15);
            if(!box.contains(start) && box.clip(start,end).isEmpty())continue;
            if(contact instanceof net.minecraft.world.entity.boss.EnderDragonPart part){
                // Let the dragon phase and struck part scale the hit before living damage bonuses.
                part.hurt(source(target,OmniraDamage.TRUE_DAMAGE,owner,owner,false,false),damage(owner,3));
            }else rayHit(owner,target);
        }
    }
    public static void lockedRay(LivingEntity owner,LivingEntity target,float damage,float wearFraction){
        if(!(owner.level() instanceof ServerLevel level)||!trueTarget(target,owner))return;
        visual(level,2,owner.getEyePosition(),target.getBoundingBox().getCenter(),0);rayHit(owner,target,damage,wearFraction);
    }
    public static void rayHit(LivingEntity owner,LivingEntity target){rayHit(owner,target,damage(owner,3),.1F);}
    private static void rayHit(LivingEntity owner,LivingEntity target,float amount,float wearFraction){
        if(!trueTarget(target,owner)||!OmniraDamage.trueDamage(target,amount,source(target,OmniraDamage.TRUE_DAMAGE,owner,owner,false,false)))return;
        boolean armorBroken=false;var seen=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.world.item.ItemStack,Boolean>());
        for(var slot:List.of(EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND,EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
            var stack=target.getItemBySlot(slot);int max=stack.getMaxDamage();if(stack.isEmpty()||!stack.isDamageableItem()||max<=0||!seen.add(stack))continue;
            int loss=(int)Math.ceil(max*(double)wearFraction-0.00001);long remaining=(long)stack.getDamageValue()+loss;
            if(remaining>=max){
                var item=stack.getItem();target.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);target.onEquippedItemBroken(item,slot);
                if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR)armorBroken=true;
            }else stack.setDamageValue((int)remaining);
        }
        if(armorBroken)armorExplosion((ServerLevel)owner.level(),target.getBoundingBox().getCenter(),target);
    }
    public static void armorExplosion(ServerLevel level,Vec3 center,LivingEntity wearer){
        visual(level,0,center,center,3);
        var targets=new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(3),t->t.isAlive()&&!t.isSpectator()&&!(t instanceof ArmorStand)));
        if(!targets.contains(wearer)&&wearer.isAlive())targets.add(wearer);
        for(var target:targets)if(target.getBoundingBox().getCenter().distanceToSqr(center)<=9)
            target.hurt(source(target,DamageTypes.MAGIC,null,null,false,true),5);
    }
    public static void visual(ServerLevel level,int kind,Vec3 from,Vec3 to,float radius){
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level,null,from.x,from.y,from.z,128,new WeaverVisualPayload(kind,from,to,radius));
    }
    private WeaverCombat(){}
}
