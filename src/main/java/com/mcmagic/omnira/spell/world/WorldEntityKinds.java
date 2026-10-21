package com.mcmagic.omnira.spell.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.animal.*;
import net.neoforged.neoforge.common.Tags;

/** Stable creature families; neutral anger does not change a reload's family. */
public final class WorldEntityKinds {
    public enum Kind { FRIENDLY, NEUTRAL, HOSTILE, BOSS }
    public static final TagKey<EntityType<?>> IMMUNE=tag("world_magic_immune"),NEUTRAL=tag("reload_neutral"),HOSTILE=tag("reload_hostile"),FRIENDLY=tag("reload_friendly");
    private static TagKey<EntityType<?>> tag(String id){return TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("omnira",id));}
    private static final ClassValue<Boolean> BOSS_FIELD=new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type){
            for(var current=type;current!=null&&current!=Entity.class;current=current.getSuperclass())
                for(var field:current.getDeclaredFields())if(net.minecraft.world.BossEvent.class.isAssignableFrom(field.getType()))return true;
            return false;
        }
    };
    public static boolean boss(LivingEntity entity){return entity.getType().is(Tags.EntityTypes.BOSSES)
            ||entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon
            ||entity instanceof net.minecraft.world.entity.boss.wither.WitherBoss||BOSS_FIELD.get(entity.getClass());}
    public static boolean eligible(LivingEntity entity){return entity instanceof Mob&&entity.isAlive()
            &&!entity.getType().is(IMMUNE)&&!entity.isSpectator()
            &&(entity.getType().getCategory()!=MobCategory.MISC||boss(entity));}
    public static Kind kind(LivingEntity entity){
        if(boss(entity))return Kind.BOSS;
        if(entity.getType().is(NEUTRAL)||entity instanceof NeutralMob||entity instanceof Spider
                ||entity instanceof net.minecraft.world.entity.monster.piglin.Piglin||entity instanceof Dolphin||entity instanceof Pufferfish
                ||entity instanceof net.minecraft.world.entity.animal.goat.Goat||entity instanceof net.minecraft.world.entity.animal.horse.Llama)return Kind.NEUTRAL;
        if(entity.getType().is(FRIENDLY))return Kind.FRIENDLY;
        return entity.getType().is(HOSTILE)||entity instanceof Enemy||entity.getType().getCategory()==MobCategory.MONSTER?Kind.HOSTILE:Kind.FRIENDLY;
    }
    public static boolean hostileNow(LivingEntity entity){
        if(!eligible(entity))return false;var family=kind(entity);
        if(family==Kind.HOSTILE||family==Kind.BOSS)return true;
        if(family!=Kind.NEUTRAL)return false;
        if(entity instanceof NeutralMob neutral&&(neutral.getRemainingPersistentAngerTime()>0||neutral.getPersistentAngerTarget()!=null))return true;
        var mob=(Mob)entity;var brain=mob.getBrain();
        return mob.getTarget()!=null||mob.isAggressive()
                ||brain.hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)
                ||brain.hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ANGRY_AT);
    }
    private WorldEntityKinds(){}
}
