package com.mcmagic.omnira.entity.spacetime;

import java.util.EnumSet;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import com.mcmagic.omnira.combat.OmniraDamage;
import com.mcmagic.omnira.weaver.WeaverCombat;

public final class RiftMantis extends Monster {
    public static final double STRIKE_DISTANCE=24,RETREAT_DISTANCE=16;
    // Horizontal offsets, bottom to top, in the plane facing the incoming strike.
    private static final double[] CRACK_PROFILE={-.20,-.10,.02,.14,.02,-.12,.02,.16,.27};
    private static final EntityDataAccessor<Integer> WINDUP=SynchedEntityData.defineId(RiftMantis.class,EntityDataSerializers.INT);
    private Vec3 strikePoint,attackOrigin,retreatPoint;
    private int cooldown;
    public RiftMantis(EntityType<? extends RiftMantis> type,Level level){super(type,level);}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.FOLLOW_RANGE,32).add(Attributes.ATTACK_DAMAGE,15);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(WINDUP,0);}
    public int windup(){return entityData.get(WINDUP);}
    @Override protected void registerGoals(){
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            public boolean canUse(){return getTarget()!=null&&getTarget().isAlive();}
            public boolean requiresUpdateEveryTick(){return true;}
            public void tick(){combatTick();}
            public void stop(){navigation.stop();entityData.set(WINDUP,0);strikePoint=null;}
        });
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(4,new RandomLookAroundGoal(this));
    }
    public boolean beginStrike(LivingEntity target){
        if(level().isClientSide||windup()>0||cooldown>0||!hasLineOfSight(target))return false;
        double distance=distanceTo(target);if(distance<23||distance>25.5)return false;
        var destination=position().add(target.position().subtract(position()).normalize().scale(STRIKE_DISTANCE));
        if(!SpatialSteps.safe(this,destination))return false;
        retreatPoint=destination.add(position().subtract(destination).normalize().scale(RETREAT_DISTANCE));
        if(!SpatialSteps.safe(this,retreatPoint))return false;
        strikePoint=destination;attackOrigin=position();entityData.set(WINDUP,20);navigation.stop();return true;
    }
    private void combatTick(){
        var target=getTarget();if(target==null)return;
        if(distanceToSqr(target)>32*32){setTarget(null);return;}
        getLookControl().setLookAt(target,30,30);
        if(cooldown>0)cooldown--;
        if(windup()>0){
            navigation.stop();setDeltaMovement(getDeltaMovement().multiply(0,1,0));
            if(windup()%2==0 && level() instanceof ServerLevel level)showStrikeCrack(level);
            int next=windup()-1;entityData.set(WINDUP,next);
            if(next==0){
                if(position().distanceToSqr(attackOrigin)<.04&&SpatialSteps.safe(this,retreatPoint)&&SpatialSteps.teleport(this,strikePoint,STRIKE_DISTANCE)){
                    if(target.isAlive()&&getBoundingBox().inflate(.75).intersects(target.getBoundingBox()))
                        OmniraDamage.trueDamage(target,15,WeaverCombat.source(target,OmniraDamage.TRUE_DAMAGE,this,this,false,false));
                    SpatialSteps.teleport(this,retreatPoint,RETREAT_DISTANCE);
                }
                cooldown=40;strikePoint=null;
            }return;
        }
        if(beginStrike(target))return;
        if(tickCount%10!=0)return;
        if(distanceToSqr(target)<23*23){
            Vec3 away=position().subtract(target.position()).normalize();var p=target.position().add(away.scale(24));navigation.moveTo(p.x,p.y,p.z,1);
        }else navigation.moveTo(target,1);
    }
    private void showStrikeCrack(ServerLevel level){
        Vec3 approach=strikePoint.subtract(attackOrigin).normalize();
        Vec3 center=strikePoint.subtract(approach.scale(2.4));
        Vec3 across=new Vec3(-approach.z,0,approach.x);
        across=across.lengthSqr()<1.0e-6?new Vec3(1,0,0):across.normalize();
        for(int i=0;i<CRACK_PROFILE.length;i++){
            double offset=CRACK_PROFILE[i];
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    center.x+across.x*offset,center.y+.62+i*.18,center.z+across.z*offset,1,0,0,0,0);
        }
    }
    @Override public boolean doHurtTarget(Entity target){return false;}
}
