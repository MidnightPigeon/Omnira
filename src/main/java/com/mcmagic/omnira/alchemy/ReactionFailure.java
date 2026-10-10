package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.spell.entity.SpellEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Machine accident: magic damage and knockback only, never a vanilla terrain explosion. */
public final class ReactionFailure {
    public static boolean explosive(int consumed,int planned){return planned>0&&(long)consumed*2>planned;}
    public static void blast(ServerLevel level,BlockPos pos,boolean large){
        var center=Vec3.atCenterOf(pos);double radius=large?20:10;float damage=large?30:15;
        var visual=SpellEntity.spawn(level,null,SpellEntity.Kind.BURST,center);
        visual.configure(1,0);visual.burstRadius(radius);visual.setSpellColor(0xB1EAF2);visual.suppressCloud();
        level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,1,.8F);
        for(var entity:level.getEntities((net.minecraft.world.entity.Entity)null,new AABB(center,center).inflate(radius),e->e.isAlive()&&!e.isSpectator()&&!(e instanceof SpellEntity))){
            var difference=entity.getBoundingBox().getCenter().subtract(center);double distance=difference.length();if(distance>radius)continue;
            if(entity instanceof LivingEntity living)living.hurt(level.damageSources().magic(),damage);
            var direction=distance<.001?new Vec3(0,1,0):difference.normalize();
            entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(3-.5*distance/radius)).add(0,.65,0));entity.hurtMarked=true;
        }
    }
    private ReactionFailure(){}
}
