package com.mcmagic.omnira.entity;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;

public final class ShadowGhost extends Monster {
    private int chargeTicks, recoveryTicks;
    private boolean hitThisCharge;
    private Vec3 chargeDirection = Vec3.ZERO;
    public ShadowGhost(EntityType<? extends ShadowGhost> type, Level level) {
        super(type, level);
        moveControl = new SpiritFlightControl(this);
        setNoGravity(true);
        noPhysics=true;
        xpReward = 5;
    }
    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,3)
                .add(Attributes.ARMOR,0).add(Attributes.MOVEMENT_SPEED,.23).add(Attributes.FLYING_SPEED,.1)
                .add(Attributes.FOLLOW_RANGE,16);
    }
    @Override protected void registerGoals() {
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,10,false,false,this::withinView) {
            {targetConditions.ignoreLineOfSight();}
        });
    }
    public boolean withinView(LivingEntity target) {
        Vec3 direction=target.getEyePosition().subtract(getEyePosition());
        return direction.lengthSqr()<.01 || getViewVector(1).dot(direction.normalize())>=.5;
    }
    @Override public boolean isInWall() {return false;}
    @Override protected PathNavigation createNavigation(Level level) {
        var nav=new FlyingPathNavigation(this,level);
        nav.setCanOpenDoors(false);nav.setCanFloat(true);nav.setCanPassDoors(true);
        return nav;
    }
    public static boolean canSpawn(EntityType<ShadowGhost> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        var floor=level.getBlockState(pos.below());
        return level.getDifficulty()!=Difficulty.PEACEFUL && level.getLevel().dimension().equals(ModDimensions.DREAM_REALM)
                && level.getBiome(pos).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","shadow_woodland"))
                && (floor.is(DreamContent.SHADOW_ROCK.get()) || floor.is(DreamContent.MOSSY_SHADOW_ROCK.get()))
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        var target=getTarget();
        if(target!=null && (!target.isAlive() || distanceToSqr(target)>256
                || target instanceof Player p && (p.isCreative() || p.isSpectator()))) {
            setTarget(null);target=null;chargeTicks=0;recoveryTicks=40;navigation.stop();
            moveControl.setWantedPosition(getX(),getY(),getZ(),0);
        }
        if(chargeTicks>0) {
            chargeTicks--;
            navigation.stop();
            setDeltaMovement(chargeDirection.scale(.36));
            if(chargeTicks==0) {
                chargeTicks=0;recoveryTicks=40;setDeltaMovement(getDeltaMovement().scale(.3));
            }
            return;
        }
        if(recoveryTicks>0) {
            if(recoveryTicks--==40 || recoveryTicks==20) wander();
            return;
        }
        if(target!=null) {
            getLookControl().setLookAt(target,30,30);
            if(distanceToSqr(target)<=64) {
                chargeDirection=target.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter()).normalize();
                chargeTicks=Math.max(12,(int)(distanceTo(target)/.36)+9);
                hitThisCharge=false;navigation.stop();moveControl.setWantedPosition(getX(),getY(),getZ(),0);
            } else if(tickCount%10==0) moveControl.setWantedPosition(target.getX(),target.getY()+.15,target.getZ(),1.1);
        } else if(tickCount%60==0) wander();
    }
    @Override public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        if (!super.checkSpawnRules(level, reason)) return false;
        if (reason != MobSpawnType.NATURAL) return true;
        // Weight cannot control abundance when this is the biome's only monster.
        return random.nextInt(8)==0 && level().getEntitiesOfClass(ShadowGhost.class,
                getBoundingBox().inflate(48), ghost -> ghost.isAlive()).size()<4;
    }
    @Override public int getMaxSpawnClusterSize() {return 1;}
    private void wander() {
        for(int attempt=0;attempt<12;attempt++) {
            int x=getBlockX()+random.nextInt(13)-6,z=getBlockZ()+random.nextInt(13)-6;
            Vec3 pos=groundHoverPoint(x,z);
            if(pos!=null) {moveControl.setWantedPosition(pos.x,pos.y,pos.z,.8);return;}
        }
        Vec3 below=groundHoverPoint(getBlockX(),getBlockZ());
        if(below!=null) moveControl.setWantedPosition(below.x,below.y,below.z,.8);
    }
    private Vec3 groundHoverPoint(int x,int z) {
        for(int y=Math.min(getBlockY()+4,level().getMaxBuildHeight()-3);y>=Math.max(level().getMinBuildHeight(),getBlockY()-48);y--) {
            BlockPos pos=new BlockPos(x,y,z);
            if(!level().hasChunkAt(pos)) return null;
            var shape=level().getBlockState(pos).getCollisionShape(level(),pos);
            if(!shape.isEmpty() && level().getBlockState(pos.above()).isAir() && level().getBlockState(pos.above(2)).isAir())
                return new Vec3(x+.5,y+shape.max(net.minecraft.core.Direction.Axis.Y)+.2,z+.5);
        }
        return null;
    }
    @Override public void tick() {
        Vec3 before=getBoundingBox().getCenter();
        boolean charging=chargeTicks>0;
        super.tick();
        var target=getTarget();
        if(!level().isClientSide && (charging || chargeTicks>0) && !hitThisCharge && target!=null && target.isAlive()) {
            // Swept volume avoids missing a target between ticks; one hit per complete pass.
            AABB volume=target.getBoundingBox().inflate(getBbWidth()/2,getBbHeight()/2,getBbWidth()/2);
            if(volume.contains(before) || volume.clip(before,getBoundingBox().getCenter()).isPresent()) {
                hitThisCharge=true;doHurtTarget(target);
            }
        }
    }
    @Override public boolean isPushable() {return false;}
    @Override public void push(Entity entity) {}
    @Override public boolean canPickUpLoot() {return false;}
    @Override public void travel(Vec3 direction) {
        if(isControlledByLocalInstance()) {
            if(chargeTicks==0) moveRelative(getSpeed(),direction);
            move(MoverType.SELF,getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(.91));
        }
        calculateEntityAnimation(false);
    }
    @Override protected void checkFallDamage(double y,boolean grounded,net.minecraft.world.level.block.state.BlockState state,BlockPos pos) {}
}
