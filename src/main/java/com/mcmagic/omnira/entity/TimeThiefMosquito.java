package com.mcmagic.omnira.entity;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** A single feeding, then dissolution; equipment wear is settled once on the server. */
public final class TimeThiefMosquito extends Monster {
    public static final int FEED_TICKS=100, FADE_TICKS=40;
    private static final EntityDataAccessor<Integer> HOST=SynchedEntityData.defineId(TimeThiefMosquito.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FADE=SynchedEntityData.defineId(TimeThiefMosquito.class,EntityDataSerializers.INT);
    private UUID host;
    private int feeding;
    private boolean spent;
    public TimeThiefMosquito(EntityType<? extends TimeThiefMosquito> type,Level level){
        super(type,level);moveControl=new FlyingMoveControl(this,20,true);setNoGravity(true);xpReward=0;
    }
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,8)
            .add(Attributes.FOLLOW_RANGE,16).add(Attributes.FLYING_SPEED,.32).add(Attributes.MOVEMENT_SPEED,.25);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HOST,-1);b.define(FADE,0);}
    @Override protected PathNavigation createNavigation(Level level){var nav=new FlyingPathNavigation(this,level);nav.setCanFloat(true);return nav;}
    @Override protected void registerGoals(){
        targetSelector.addGoal(1,new NearestAttackableTargetGoal<>(this,Player.class,true));
        goalSelector.addGoal(1,new Goal(){
            {setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
            private int repathTicks;
            @Override public void start(){repathTicks=0;}
            @Override public boolean requiresUpdateEveryTick(){return true;}
            public boolean canUse(){return host==null&&!spent&&getTarget()!=null;}
            public void tick(){var target=getTarget();if(target==null)return;getLookControl().setLookAt(target,30,30);
                if(distanceToSqr(target)<2.25&&hasLineOfSight(target)&&target instanceof Player p)attach(p);
                else if(--repathTicks<=0){navigation.moveTo(target.getX(),target.getY()+1,target.getZ(),1);repathTicks=10;}
            }
            public void stop(){navigation.stop();}
        });
        goalSelector.addGoal(3,new WaterAvoidingRandomFlyingGoal(this,.7){public boolean canUse(){return host==null&&!spent&&super.canUse();}});
        goalSelector.addGoal(4,new RandomLookAroundGoal(this));
    }
    public boolean attach(Player player){
        if(level().isClientSide||spent||host!=null||!player.isAlive()||player.isCreative()||player.isSpectator())return false;
        host=player.getUUID();feeding=0;entityData.set(HOST,player.getId());navigation.stop();setDeltaMovement(Vec3.ZERO);return true;
    }
    public boolean attached(){return entityData.get(HOST)>=0;}
    public float opacity(float partial){return Math.max(0,1-(entityData.get(FADE)+(entityData.get(FADE)>0?partial:0))/FADE_TICKS);}
    @Override public void tick(){
        super.tick();setNoGravity(true);fallDistance=0;
        if(level().isClientSide||!isAlive())return;
        if(spent){navigation.stop();setTarget(null);setDeltaMovement(0,.015,0);int fade=entityData.get(FADE)+1;entityData.set(FADE,fade);if(fade>=FADE_TICKS)discard();return;}
        if(host==null)return;
        var player=((ServerLevel)level()).getPlayerByUUID(host);
        if(player==null||!player.isAlive()||player.isCreative()||player.isSpectator()){host=null;feeding=0;entityData.set(HOST,-1);setTarget(null);return;}
        entityData.set(HOST,player.getId());navigation.stop();setDeltaMovement(Vec3.ZERO);
        // Keep the actual attackable entity in front of the upper torso, including while crouching.
        float facing=player.yBodyRot;
        double angle=Math.toRadians(facing);
        double reach=(1.875*player.getScale()+7.05)/16;
        setPos(player.getX()-Math.sin(angle)*reach,Math.max(player.getY()+player.getBbHeight()*.2,player.getEyeY()-.85*player.getScale()),player.getZ()+Math.cos(angle)*reach);
        setYRot(facing+180);
        yBodyRot=yHeadRot=getYRot();
        if(++feeding>=FEED_TICKS){erodeEquipment(player);spent=true;host=null;entityData.set(HOST,-1);entityData.set(FADE,1);setTarget(null);}
    }
    public static void erodeEquipment(Player player){
        var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
        for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND))
            erode(player.getItemBySlot(slot),seen,()->player.onEquippedItemBroken(player.getItemBySlot(slot).getItem(),slot));
        for(int i=0;i<9;i++)erode(player.getInventory().getItem(i),seen,()->player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.ITEM_BREAK,net.minecraft.sounds.SoundSource.PLAYERS,.8F,1));
        player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
    private static void erode(ItemStack stack,Set<ItemStack> seen,Runnable broken){
        if(stack.isEmpty()||!stack.isDamageableItem()||!seen.add(stack))return;
        int max=stack.getMaxDamage(),loss=max/10+(max%10==0?0:1);
        if((long)stack.getDamageValue()+loss>=max){broken.run();stack.shrink(1);}else stack.setDamageValue(stack.getDamageValue()+loss);
    }
    @Override public void travel(Vec3 input){if(isEffectiveAi()){moveRelative(getSpeed(),input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.91));}else super.travel(input);}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override protected boolean shouldDropLoot(){return !spent&&super.shouldDropLoot();}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(host!=null)tag.putUUID("FeedingHost",host);tag.putInt("FeedingTicks",feeding);tag.putBoolean("Spent",spent);tag.putInt("FadeTicks",entityData.get(FADE));}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);host=tag.hasUUID("FeedingHost")?tag.getUUID("FeedingHost"):null;feeding=Math.clamp(tag.getInt("FeedingTicks"),0,FEED_TICKS);spent=tag.getBoolean("Spent");entityData.set(FADE,Math.clamp(tag.getInt("FadeTicks"),0,FADE_TICKS));}
}
