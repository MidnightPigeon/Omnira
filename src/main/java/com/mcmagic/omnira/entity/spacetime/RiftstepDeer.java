package com.mcmagic.omnira.entity.spacetime;

import net.minecraft.world.entity.*;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.mcmagic.omnira.combat.OmniraDamage;
import com.mcmagic.omnira.weaver.WeaverCombat;

public final class RiftstepDeer extends PathfinderMob {
    private static final EntityDataAccessor<Boolean> ANGRY=SynchedEntityData.defineId(RiftstepDeer.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RAMMING=SynchedEntityData.defineId(RiftstepDeer.class,EntityDataSerializers.BOOLEAN);
    private float headLowering,previousHeadLowering;
    private int stepCooldown,angerTicks;
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ANGRY,false);b.define(RAMMING,false);}
    public boolean angry(){return entityData.get(ANGRY);}
    public boolean ramming(){return entityData.get(RAMMING);}
    public float headLowering(float partial){return net.minecraft.util.Mth.lerp(partial,previousHeadLowering,headLowering);}
    private final class RamGoal extends MeleeAttackGoal {
        private int ramTicks;
        private boolean struck;
        RamGoal(){super(RiftstepDeer.this,1.4,true);}
        @Override protected void checkAndPerformAttack(LivingEntity target){
            if(ramTicks==0&&isTimeToAttack()&&distanceToSqr(target)<=9&&hasLineOfSight(target)){
                ramTicks=16;struck=false;entityData.set(RAMMING,true);resetAttackCooldown();
            }
            if(ramTicks==0)return;
            getNavigation().stop();
            getLookControl().setLookAt(target,30,30);
            if(ramTicks>10){setDeltaMovement(getDeltaMovement().multiply(0,1,0));}
            else if(!struck){
                Vec3 direction=target.position().subtract(position()).multiply(1,0,1).normalize();
                setDeltaMovement(direction.scale(.65).add(0,getDeltaMovement().y,0));
                if(isWithinMeleeAttackRange(target)&&hasLineOfSight(target)){
                    doHurtTarget(target);struck=true;
                }
            }
            if(--ramTicks==0)entityData.set(RAMMING,false);
        }
        @Override public void stop(){super.stop();ramTicks=0;entityData.set(RAMMING,false);}
    }
    private java.util.UUID angerTarget;
    public RiftstepDeer(EntityType<? extends RiftstepDeer> type,Level level){super(type,level);getNavigation().setCanFloat(true);setPathfindingMalus(net.minecraft.world.level.pathfinder.PathType.WATER,16);}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,24).add(Attributes.MOVEMENT_SPEED,.4).add(Attributes.STEP_HEIGHT,.6).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.FOLLOW_RANGE,32);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(1,new RamGoal());
        goalSelector.addGoal(3,new WaterAvoidingRandomStrollGoal(this,.35));goalSelector.addGoal(4,new RandomLookAroundGoal(this));
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        boolean hit=super.hurt(source,amount);
        if(hit&&!level().isClientSide&&isAlive()&&source.getEntity() instanceof LivingEntity living&&living!=this){angerTicks=1200;entityData.set(ANGRY,true);angerTarget=living.getUUID();setTarget(living);}
        return hit;
    }
    public int angerTicks(){return angerTicks;}
    @Override public boolean isInWater(){return super.isInWater()||isInFluidType((type,height)->type.canSwim(this));}
    @Override public boolean doHurtTarget(Entity target){
        if(!(target instanceof LivingEntity living))return false;
        boolean hit=OmniraDamage.trueDamage(living,10,WeaverCombat.source(living,OmniraDamage.TRUE_DAMAGE,this,this,false,false));
        if(hit){living.knockback(1.2,getX()-living.getX(),getZ()-living.getZ());living.hurtMarked=true;}
        return hit;
    }
    private void clearAnger(){angerTicks=0;entityData.set(ANGRY,false);entityData.set(RAMMING,false);angerTarget=null;setTarget(null);setLastHurtByMob(null);getNavigation().stop();}
    private boolean pursue(LivingEntity target){
        double distance=distanceTo(target);
        if(distance<3&&hasLineOfSight(target)&&!horizontalCollision)return false;
        double angle=Math.atan2(target.getZ()-getZ(),target.getX()-getX());
        // Upper and level platforms first, then descents; short-range approach stays available.
        double minimum=distance>=8?8:Math.max(1,distance-2);
        double longest=Math.min(16,Math.max(minimum,distance-1.5));
        for(int dy:new int[]{1,2,3,4,0,-1,-2,-3,-4})for(double reach:new double[]{longest,(longest+minimum)/2,minimum})
            for(double turn:new double[]{0,.2,-.2,.4,-.4,.7,-.7}){
                Vec3 p=new Vec3(getX()+Math.cos(angle+turn)*reach,Math.floor(getY())+dy,getZ()+Math.sin(angle+turn)*reach);
                double step=position().distanceToSqr(p);
                if(step<minimum*minimum||step>256||p.distanceToSqr(target.position())>=distance*distance)continue;
                if(SpatialSteps.teleport(this,p,16)){stepCooldown=20;return true;}
            }
        return false;
    }
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.addAdditionalSaveData(t);t.putInt("AngerTicks",angerTicks);if(angerTarget!=null)t.putUUID("AngerTarget",angerTarget);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){super.readAdditionalSaveData(t);angerTicks=Math.clamp(t.getInt("AngerTicks"),0,1200);entityData.set(ANGRY,angerTicks>0);angerTarget=t.hasUUID("AngerTarget")?t.getUUID("AngerTarget"):null;}
    public boolean requiresHeightStep(net.minecraft.core.BlockPos next){
        return Math.abs(net.minecraft.world.level.pathfinder.WalkNodeEvaluator.getFloorLevel(level(),next)-getY())>=.99;
    }
    @Override public void jumpFromGround(){if(isInWater()||isInLava())super.jumpFromGround();}
    @Override public void travel(Vec3 input){
        var path=getNavigation().getPath();
        if(!level().isClientSide&&onGround()&&!isInWater()&&path!=null&&!path.isDone()&&requiresHeightStep(path.getNextNodePos())){
            input=Vec3.ZERO;setDeltaMovement(getDeltaMovement().multiply(0,1,0));
        }
        super.travel(input);
    }
    @Override public void tick(){
        super.tick();
        previousHeadLowering=headLowering;
        headLowering=net.minecraft.util.Mth.clamp(headLowering+(ramming()?.17F:-.12F),0,1);
        if(level().isClientSide||!isAlive())return;
        if(angerTicks>0){
            if(getTarget()==null&&angerTarget!=null&&level() instanceof net.minecraft.server.level.ServerLevel server&&server.getEntity(angerTarget) instanceof LivingEntity living)setTarget(living);
            var target=getTarget();
            if(--angerTicks==0||target==null||!target.isAlive()||target.level()!=level()||distanceToSqr(target)>32*32)clearAnger();
        }
        if(isInWater())setDeltaMovement(getDeltaMovement().add(0,.04,0));
        if(stepCooldown>0){stepCooldown--;return;}
        // An ongoing ram never suppresses pursuit around cover or a distant fleeing target.
        var chase=getTarget();
        if(ramming()&&chase!=null&&distanceToSqr(chase)<64&&hasLineOfSight(chase)&&!horizontalCollision)return;
        if(tickCount%10!=0)return;
        if(getTarget()!=null&&pursue(getTarget()))return;
        var path=getNavigation().getPath();boolean heightChange=path!=null&&!path.isDone()&&requiresHeightStep(path.getNextNodePos());
        if(heightChange){
            Vec3 destination=SpatialSteps.platform(this,16);
            if(destination!=null&&SpatialSteps.teleport(this,destination,16))stepCooldown=30;
            else getNavigation().stop();
        }
    }
}
