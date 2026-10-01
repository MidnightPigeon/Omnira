package com.mcmagic.omnira.entity;

import com.mcmagic.omnira.registry.DreamContent;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public final class LightSpirit extends PathfinderMob {
    private Player approaching;
    private int approachTicks;
    private int wanderPause;
    public LightSpirit(EntityType<? extends LightSpirit> type, Level level) {
        super(type, level);
        moveControl = new SpiritFlightControl(this);
        setNoGravity(true);
        noPhysics=true;
        setCanPickUpLoot(false);
    }

    @Override protected PathNavigation createNavigation(Level level) {
        var navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        navigation.setCanPassDoors(true);
        return navigation;
    }

    @Override public boolean canPickUpLoot() { return false; }
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        var health=getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if(health!=null && health.getBaseValue()==20) {
            health.setBaseValue(8);setHealth(Math.min(getHealth(),getMaxHealth()));
        }
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 400 == 0) {
            approaching = level().getNearestPlayer(this, 20);
            if (approaching != null && approaching.isAlive() && !approaching.isSpectator()) approachTicks = 100;
            else approaching = null;
        }
        if (approaching != null) {
            if (!approaching.isAlive() || approaching.isSpectator() || distanceToSqr(approaching) > 400 || approachTicks <= 0) {
                approaching = null;
                approachTicks = 0;
                stopFlight();
            } else {
                approachTicks--;
                getLookControl().setLookAt(approaching, 30, 30);
                if (distanceToSqr(approaching) < 4) stopFlight();
                else if (approachTicks % 10 == 9) moveControl.setWantedPosition(approaching.getX(), approaching.getEyeY(), approaching.getZ(), 1.2);
                return;
            }
        }
        if (!moveControl.hasWanted() && --wanderPause<=0) {
            Vec3 look = getViewVector(0);
            // Match RandomStroll.fly used by the allay: 10 horizontal, 7 vertical, -2 bias.
            Vec3 target = AirAndWaterRandomPos.getPos(this,10,7,-2,look.x,look.z,(float)Math.PI/2);
            if (target != null) moveControl.setWantedPosition(target.x, target.y, target.z, 1);
            wanderPause=target==null?10:30+random.nextInt(31);
        }
    }

    private void stopFlight() {
        navigation.stop();moveControl.setWantedPosition(getX(),getY(),getZ(),0);setDeltaMovement(Vec3.ZERO);
    }
    @Override public boolean isInWall() {return false;}
    @Override public boolean isPushable() {return false;}
    @Override public void push(Entity entity) {}

    @Override public void travel(Vec3 direction) {
        // Same flying acceleration and drag as the allay, without its item-collection brain.
        if (isControlledByLocalInstance()) {
            moveRelative(isInWater() || isInLava() ? .02F : getSpeed(), direction);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(isInWater() ? .8 : isInLava() ? .5 : .91));
        }
        calculateEntityAnimation(false);
    }

    @Override protected void checkFallDamage(double y, boolean onGround,
            net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {}

    public static boolean canSpawn(EntityType<LightSpirit> type, ServerLevelAccessor level,
                                   MobSpawnType reason, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(ModDimensions.DREAM_REALM)
                && level.getBiome(pos).is(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira", "dawn_crystal_fields"))
                && hasSpawnSurface(level, pos);
    }

    public static boolean hasSpawnSurface(net.minecraft.world.level.LevelReader level, BlockPos pos) {
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        // Natural spawning samples random heights; flying spirits need not start exactly on the floor.
        for (int depth=1; depth<=8; depth++) {
            var ground=level.getBlockState(pos.below(depth));
            if (!ground.isAir()) return ground.is(DreamContent.LIGHT_CONDENSATE.get())
                    || ground.is(DreamContent.LIGHT_SOURCE_CRYSTAL.get())
                    || ground.is(DreamContent.LIGHT_CRYSTAL_CORE.get());
        }
        return false;
    }

    @Override public void aiStep() {
        super.aiStep();
        if (level().isClientSide && isAlive() && tickCount % 4 == 0) {
            double angle = tickCount * .12 + random.nextFloat();
            level().addParticle(ParticleTypes.END_ROD, getX() + Math.cos(angle) * .38,
                    getY() + .3 + random.nextDouble() * .3, getZ() + Math.sin(angle) * .38, 0, .008, 0);
        }
    }

    @Override public boolean removeWhenFarAway(double distance) {
        return !hasCustomName() && !isLeashed();
    }
}
