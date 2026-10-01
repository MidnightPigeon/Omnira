package com.mcmagic.omnira.entity;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import com.mcmagic.omnira.spell.entity.SpellEntity;
import com.mcmagic.omnira.world.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.*;
import java.util.UUID;

public final class DreamMirror extends PathfinderMob {
    public static final int OFFERING_REFLECTION_RANGE = 32;
    public static final TagKey<DamageType> PHYSICAL=TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("omnira","mirror_physical"));
    private static final ResourceLocation COPIED=ResourceLocation.fromNamespaceAndPath("omnira","mirror_copy");
    public static final ResourceKey<LootTable> TRADE_LOOT=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.fromNamespaceAndPath("omnira","gameplay/dream_mirror_trade"));
    private static final TagKey<DamageType> MAGIC=TagKey.create(Registries.DAMAGE_TYPE,ResourceLocation.fromNamespaceAndPath("c","is_magic"));
    private static final ThreadLocal<Boolean> REFLECTING=ThreadLocal.withInitial(()->false);
    private UUID angryAt;
    private boolean followingOffering;
    private int shotTicks=100,copyTicks=600;

    public DreamMirror(EntityType<? extends DreamMirror> type,Level level) {
        super(type,level);moveControl=new SpiritFlightControl(this);setNoGravity(true);noPhysics=true;xpReward=5;
    }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,30).add(Attributes.ARMOR,0)
                .add(Attributes.ARMOR_TOUGHNESS,0).add(Attributes.MOVEMENT_SPEED,.22)
                .add(Attributes.FLYING_SPEED,.1).add(Attributes.FOLLOW_RANGE,32).add(ModAttributes.SPELL_POWER,1);
    }
    @Override protected PathNavigation createNavigation(Level level) {return new FlyingPathNavigation(this,level);}
    public Vec3 front() {return Vec3.directionFromRotation(0,getYRot());}
    public boolean faces(Entity entity) {
        return front().dot(entity.getBoundingBox().getCenter().subtract(getBoundingBox().getCenter()).normalize())>.5;
    }
    public void provoke(Entity attacker) {
        if(attacker instanceof Player player && !player.isCreative() && !player.isSpectator()) {
            if(!player.getUUID().equals(angryAt)) {shotTicks=100;copyTicks=600;}
            angryAt=player.getUUID();setTarget(player);
        }
    }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(level().isClientSide || amount<=0 || source.is(DamageTypes.THORNS) || isInvulnerableTo(source)) return false;
        updateCopies();
        if(source.is(PHYSICAL)) {
            if(source.getDirectEntity() instanceof Projectile projectile) {
                com.mcmagic.omnira.event.MirrorProjectileEvents.bounce(projectile,this,projectile.position(),source,amount);
                return false;
            }
            returnPhysicalDamage(source.getEntity(),source,amount);
            if(source.getDirectEntity() instanceof SpellEntity spell) spell.reflectFrom(this,false);
            return false;
        }
        float before=getHealth()+getAbsorptionAmount();
        boolean hurt=super.hurt(source,amount);
        if(hurt && getHealth()+getAbsorptionAmount()<before
                && (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(MAGIC)))
            provoke(source.getEntity());
        return hurt;
    }
    public void returnPhysicalDamage(Entity attacker,DamageSource source,float amount) {
        if(level().isClientSide || amount<=0 || source.is(DamageTypes.THORNS) || REFLECTING.get() || !(attacker instanceof LivingEntity living) || attacker==this) return;
        REFLECTING.set(true);
        try {living.hurt(new SpellDamageSource(source.typeHolder(),this,this,"reflection"),amount);}
        finally {REFLECTING.set(false);}
    }
    @Override public InteractionResult mobInteract(Player player,InteractionHand hand) {
        ItemStack offering=player.getItemInHand(hand);
        if(!isAlive() || player.isSpectator() || !offering.is(ModItems.DREAM_CRYSTAL_SHARD.get()))
            return super.mobInteract(player,hand);
        if(level() instanceof ServerLevel server) {
            var params=new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN,position())
                    .withParameter(LootContextParams.THIS_ENTITY,this).create(LootContextParamSets.GIFT);
            var rewards=server.getServer().reloadableRegistries().getLootTable(TRADE_LOOT).getRandomItems(params);
            if(rewards.isEmpty()) return InteractionResult.FAIL;
            offering.shrink(1);
            for(var reward:rewards) player.getInventory().placeItemBackInInventory(reward,false);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+.9,getZ(),5,.2,.3,.2,.01);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void approachOffering() {
        var player=level().getNearestPlayer(getX(),getY(),getZ(),OFFERING_REFLECTION_RANGE,
                e->e instanceof Player p && p.isAlive() && !p.isSpectator() && (p.getMainHandItem().is(ModItems.DREAM_CRYSTAL_SHARD.get())
                        || p.getOffhandItem().is(ModItems.DREAM_CRYSTAL_SHARD.get())));
        if(player==null) {
            if(followingOffering) moveControl.setWantedPosition(getX(),getY(),getZ(),0);
            followingOffering=false;
            if(tickCount%60==0) wander();return;
        }
        followingOffering=true;
        if(distanceToSqr(player)>4) moveControl.setWantedPosition(player.getX(),player.getY()+.3,player.getZ(),.8);
        else moveControl.setWantedPosition(getX(),getY(),getZ(),0);
        var delta=player.position().subtract(position());
        setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
        setYBodyRot(getYRot());setYHeadRot(getYRot());
    }
    @Override protected float getDamageAfterMagicAbsorb(DamageSource source,float amount) {
        float result=super.getDamageAfterMagicAbsorb(source,amount);
        if(getTarget() instanceof Player player && faces(player) && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ENCHANTMENTS)
                && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_EFFECTS) && level() instanceof ServerLevel server) {
            result=CombatRules.getDamageAfterMagicAbsorb(result,EnchantmentHelper.getDamageProtection(server,player,source));
            damageContainers.peek().setReduction(net.neoforged.neoforge.common.damagesource.DamageContainer.Reduction.ENCHANTMENTS,
                    damageContainers.peek().getNewDamage()-result);
        }
        return result;
    }
    private void copyAttribute(net.minecraft.core.Holder<Attribute> type,double value) {
        var attribute=getAttribute(type);attribute.removeModifier(COPIED);
        double delta=value-attribute.getValue();
        if(delta!=0) attribute.addTransientModifier(new AttributeModifier(COPIED,delta,AttributeModifier.Operation.ADD_VALUE));
    }
    public void updateCopies() {
        var player=getTarget() instanceof Player p && faces(p)?p:null;
        copyAttribute(Attributes.ARMOR,player==null?0:player.getAttributeValue(Attributes.ARMOR));
        copyAttribute(Attributes.ARMOR_TOUGHNESS,player==null?0:player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        copyAttribute(ModAttributes.SPELL_POWER,player==null?1:player.getAttributeValue(ModAttributes.SPELL_POWER));
    }
    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if(getTarget()==null && angryAt!=null) setTarget(level().getPlayerByUUID(angryAt));
        var target=getTarget();
        if(target!=null && (!target.isAlive() || distanceToSqr(target)>32*32 || target.isSpectator()
                || target instanceof Player p && p.isCreative())) {setTarget(null);angryAt=null;target=null;}
        updateCopies();
        if(target==null) {if(tickCount%10==0) approachOffering();return;}
        if(tickCount%10==0) {
            Vec3 away=position().subtract(target.position()).multiply(1,0,1).normalize();
            if(away.lengthSqr()<.01) away=front().scale(-1);
            Vec3 hover=target.position().add(away.scale(Math.sqrt(96))).add(0,2,0);
            moveControl.setWantedPosition(hover.x,hover.y,hover.z,1);
        }
        if(--copyTicks<=0) {
            copyTicks=600;
            if(faces(target)) for(var effect:java.util.List.copyOf(target.getActiveEffects())) {
                removeEffect(effect.getEffect());addEffect(new MobEffectInstance(effect),target);
            }
        }
        if(--shotTicks<=0) {
            shotTicks=100;
            if(hasLineOfSight(target)) {
                var spell=SpellEntity.spawn((ServerLevel)level(),this,SpellEntity.Kind.PROJECTILE,getEyePosition().add(front().scale(.7)));
                spell.configure(getAttributeValue(ModAttributes.SPELL_POWER),new SpellPayload(0,0,java.util.List.of(SpellEffect.compose(false,0,1))));
                spell.home(target,target.getBoundingBox().getCenter());
                spell.setDeltaMovement(target.getEyePosition().subtract(spell.position()).normalize().scale(SpellCasting.PROJECTILE_SPEED));
            }
        }
    }
    private void wander() {
        for(int attempt=0;attempt<12;attempt++) {
            int x=getBlockX()+random.nextInt(13)-6,z=getBlockZ()+random.nextInt(13)-6;
            for(int y=Math.min(getBlockY()+4,level().getMaxBuildHeight()-3);y>=Math.max(getBlockY()-32,level().getMinBuildHeight());y--) {
                var pos=new BlockPos(x,y,z);if(!level().hasChunkAt(pos)) break;
                var shape=level().getBlockState(pos).getCollisionShape(level(),pos);
                if(!shape.isEmpty() && level().getBlockState(pos.above()).isAir() && level().getBlockState(pos.above(2)).isAir()) {
                    moveControl.setWantedPosition(x+.5,y+shape.max(net.minecraft.core.Direction.Axis.Y)+.15,z+.5,.8);return;
                }
            }
        }
    }
    @Override public void tick() {
        super.tick();
        if(!level().isClientSide && getTarget()!=null) {
            var delta=getTarget().position().subtract(position());
            float yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
            setYRot(yaw);setYBodyRot(yaw);setYHeadRot(yaw);
        }
    }
    @Override public void travel(Vec3 direction) {
        if(isControlledByLocalInstance()) {moveRelative(getSpeed(),direction);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.91));}
        calculateEntityAnimation(false);
    }
    @Override public boolean isInWall() {return false;}
    @Override public boolean isPushable() {return false;}
    @Override public void push(Entity entity) {}
    @Override public boolean canPickUpLoot() {return false;}
    @Override protected void checkFallDamage(double y,boolean grounded,net.minecraft.world.level.block.state.BlockState state,BlockPos pos) {}
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);if(angryAt!=null) tag.putUUID("MirrorTarget",angryAt);
        tag.putInt("MirrorShot",shotTicks);tag.putInt("MirrorCopy",copyTicks);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);angryAt=tag.hasUUID("MirrorTarget")?tag.getUUID("MirrorTarget"):null;
        shotTicks=tag.contains("MirrorShot")?Math.clamp(tag.getInt("MirrorShot"),1,100):100;
        copyTicks=tag.contains("MirrorCopy")?Math.clamp(tag.getInt("MirrorCopy"),1,600):600;
    }
    public static boolean canSpawn(EntityType<DreamMirror> type,ServerLevelAccessor level,MobSpawnType reason,BlockPos pos,RandomSource random) {
        return level.getLevel().dimension().equals(ModDimensions.DREAM_REALM)
                && level.getBiome(pos).is(ResourceLocation.fromNamespaceAndPath("omnira","mirror_dream_border"))
                && hasSpawnSurface(level,pos);
    }
    public static boolean hasSpawnSurface(net.minecraft.world.level.LevelReader level,BlockPos pos) {
        if(!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) return false;
        for(int depth=1;depth<=6;depth++) {
            var state=level.getBlockState(pos.below(depth));
            if(state.is(DreamContent.MIRROR_ROCK.get()) || state.is(DreamContent.DREAM_CRYSTAL_BEDROCK.get())) return true;
            if(!state.isAir() && !(state.getBlock() instanceof net.minecraft.world.level.block.AmethystClusterBlock)) return false;
        }
        return false;
    }
    @Override public int getMaxSpawnClusterSize() {return 1;}
    @Override public boolean checkSpawnRules(LevelAccessor level,MobSpawnType reason) {
        return reason!=MobSpawnType.NATURAL || level().getEntitiesOfClass(DreamMirror.class,getBoundingBox().inflate(32)).size()<2;
    }
}
