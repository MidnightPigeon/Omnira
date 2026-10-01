package com.mcmagic.omnira.ancient;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Shared ownership and clocks; flight and riding remain species-specific. */
public abstract class AncientCompanion extends TamableAnimal {
    private int recoveryTicks;

    protected AncientCompanion(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    @Override protected void registerGoals() {
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        targetSelector.addGoal(3, new HurtByTargetGoal(this));
    }

    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && isAlive() && ++recoveryTicks >= 1200) {
            recoveryTicks = 0;
            heal(getMaxHealth() / 2);
        }
    }

    @Override public boolean canAttack(LivingEntity target) {
        return !isOrderedToSit() && super.canAttack(target)
                && !isAlliedTo(target);
    }

    @Override public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target == owner || target instanceof AncientCompanion pet && pet.getOwnerUUID() != null
                && pet.getOwnerUUID().equals(getOwnerUUID())) return false;
        if (target instanceof TamableAnimal pet && pet.isTame()) return false;
        return !(target instanceof Player other && owner instanceof Player player && !player.canHarmPlayer(other));
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isOwnedBy(player)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                setOrderedToSit(!isOrderedToSit());
                setTarget(null);
                getNavigation().stop();
                setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        isOrderedToSit()?"message.omnira.companion.stay":"message.omnira.companion.follow"),true);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return ownerInteract(player, hand);
    }

    protected InteractionResult ownerInteract(Player player, InteractionHand hand) { return InteractionResult.PASS; }
    @Override public boolean isFood(ItemStack stack) { return false; }
    @Override public boolean canMate(Animal other) { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("RecoveryTicks", recoveryTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        recoveryTicks = Math.clamp(tag.getInt("RecoveryTicks"), 0, 1199);
    }
}
