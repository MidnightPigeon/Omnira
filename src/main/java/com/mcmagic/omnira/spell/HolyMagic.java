package com.mcmagic.omnira.spell;

import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;

public final class HolyMagic {
    private HolyMagic() {}
    public static boolean enabled(SpellPayload payload){return payload.keywords().contains("holy");}
    public static void touch(Entity owner,LivingEntity target) {
        if(target.level().isClientSide || !target.isAlive())return;
        boolean allied=owner!=null && (target==owner || target.isAlliedTo(owner));
        boolean hostile=!allied && (target instanceof NeutralMob neutral ? neutral.isAngry()
                : target instanceof net.minecraft.world.entity.monster.Enemy || target instanceof Mob mob && mob.getTarget()!=null);
        if(hostile)target.igniteForSeconds(10);
        else target.addEffect(new MobEffectInstance(MobEffects.GLOWING,200),owner);
    }
    public static void cloud(net.minecraft.server.level.ServerLevel level,Entity owner,SpellPayload payload,
                             double power,double range,net.minecraft.world.phys.Vec3 point) {
        cloud(level,owner,payload,power,range,point,false);
    }
    public static void cloud(net.minecraft.server.level.ServerLevel level,Entity owner,SpellPayload payload,
                             double power,double range,net.minecraft.world.phys.Vec3 point,boolean excludeOwner) {
        for(var effect:payload.effects())if(effect.darkness()) {
            var mist=com.mcmagic.omnira.registry.ModEntityTypes.SHADOW_MIST.get().create(level);
            if(mist==null)continue;
            mist.configureSpell(owner,effect,power,range,enabled(payload));
            mist.excludeOwner(excludeOwner);
            mist.setPos(point);level.addFreshEntity(mist);
        }
    }
}
