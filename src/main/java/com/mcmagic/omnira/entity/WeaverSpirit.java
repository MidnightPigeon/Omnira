package com.mcmagic.omnira.entity;

import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.network.syncher.*;
import net.minecraft.nbt.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import com.mcmagic.omnira.weaver.WeaverCombat;
import com.mcmagic.omnira.weaver.WeaverDeparture;

/** Reload-only neutral boss; combat state is persistent and independent of the client animation clock. */
public final class WeaverSpirit extends PathfinderMob implements Enemy {
    private static final EntityDataAccessor<Boolean> ACTIVE=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PHASING=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.BOOLEAN);
    private Vec3 escapePoint;
    private int escapeRetry;
    private int nextBlast=4;
    private static final EntityDataAccessor<Boolean> RED=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> TIERS=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> MOTION_START=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> MOTION_FROZEN=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CLOUD_NEAR=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> EXIT=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> EXIT_START=SynchedEntityData.defineId(WeaverSpirit.class,EntityDataSerializers.LONG);
    private int exitTicks,returned;
    private CompoundTag pendingDeathLoot=new CompoundTag();
    private net.minecraft.world.item.ItemStack offeredCloud=net.minecraft.world.item.ItemStack.EMPTY;
    private final ServerBossEvent bossEvent=new ServerBossEvent(getDisplayName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> opponents=new LinkedHashSet<>();
    private boolean reloaded;
    private int attackClock=60,normals,normalSinceRay,phaseClock=-1;
    private final MoveControl idleControl,flightControl;
    public WeaverSpirit(EntityType<? extends WeaverSpirit> type,Level level){
        super(type,level);setPersistenceRequired();
        idleControl=new MoveControl(this);flightControl=new SpiritFlightControl(this);moveControl=idleControl;
    }
    public static AttributeSupplier.Builder attributes(){
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,1024).add(Attributes.MOVEMENT_SPEED,.25)
                .add(Attributes.FLYING_SPEED,.35).add(Attributes.FOLLOW_RANGE,48).add(Attributes.ATTACK_DAMAGE,0);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(ACTIVE,false);builder.define(PHASING,false);builder.define(RED,false);builder.define(TIERS,0);builder.define(MOTION_START,-1L);builder.define(MOTION_FROZEN,0F);builder.define(CLOUD_NEAR,false);builder.define(EXIT,0);builder.define(EXIT_START,0L);}
    public boolean active(){return entityData.get(ACTIVE);}
    public boolean phasing(){return entityData.get(PHASING);}
    private void phasing(boolean value){entityData.set(PHASING,value);noPhysics=value;}
    public boolean redCore(){return entityData.get(RED);}
    public int strength(){return entityData.get(TIERS);}
    public int normalAttacks(){return normals;}
    public boolean reloading(){return phaseClock>=0;}
    public boolean cloudNear(){return entityData.get(CLOUD_NEAR);}
    public int departure(){return entityData.get(EXIT);}
    public float departureAge(float partial){return departure()==0?0:Math.max(0,level().getGameTime()-entityData.get(EXIT_START))+partial;}
    public float edgeClock(float partial){return (level().getGameTime()%1000000)+partial;}
    public static boolean carriesCloud(net.minecraft.world.entity.player.Player player){
        for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(com.mcmagic.omnira.myriad.MiniatureWorldContent.PROBABILITY_CLOUD.get()))return true;
        return false;
    }
    @Override protected net.minecraft.world.InteractionResult mobInteract(net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand){
        var stack=player.getItemInHand(hand);
        if(departure()!=0||!isAlive()||active()||phasing()||!opponents().isEmpty()||player.isSpectator()||distanceToSqr(player)>25
                ||!stack.is(com.mcmagic.omnira.myriad.MiniatureWorldContent.PROBABILITY_CLOUD.get())||!carriesCloud(player))return super.mobInteract(player,hand);
        if(!level().isClientSide){offeredCloud=stack.copyWithCount(1);stack.shrink(1);beginDeparture(WeaverDeparture.DISSOLVE);}
        return net.minecraft.world.InteractionResult.sidedSuccess(level().isClientSide);
    }
    private void beginDeparture(int mode){
        if(departure()!=0)return;
        active(false);entityData.set(RED,false);entityData.set(CLOUD_NEAR,mode==WeaverDeparture.DISSOLVE);
        entityData.set(EXIT,mode);entityData.set(EXIT_START,level().getGameTime());exitTicks=0;returned=0;
        if(mode==WeaverDeparture.DEFEATED)bossEvent.setProgress(0);
        opponents.clear();setTarget(null);navigation.stop();setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);
    }
    @Override public void tick(){
        noPhysics=phasing();
        super.tick();
        if(departure()==WeaverDeparture.DISSOLVE){
            setDeltaMovement(Vec3.ZERO);
            if(!level().isClientSide){
                exitTicks++;
                if(exitTicks>=WeaverDeparture.CLOUD_TICK&&(returned&1)==0){
                    returned|=1;spawnAtLocation(offeredCloud.isEmpty()?new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.myriad.MiniatureWorldContent.PROBABILITY_CLOUD.get()):offeredCloud.copy());
                    offeredCloud=net.minecraft.world.item.ItemStack.EMPTY;
                }
                for(int i=0;i<4;i++)if(exitTicks>=WeaverDeparture.FIRST_SEED_TICK+i*WeaverDeparture.SEED_INTERVAL&&(returned&(2<<i))==0){
                    returned|=2<<i;spawnAtLocation(random.nextBoolean()?com.mcmagic.omnira.myriad.WorldSeedContent.ACTIVE.get():com.mcmagic.omnira.myriad.WorldSeedContent.DORMANT.get());
                }
                if(exitTicks>=WeaverDeparture.DISSOLVE_TICKS){finishDepartureRewards();bossEvent.removeAllPlayers();discard();}
            }
        }else if(!level().isClientSide&&departure()==0&&isAlive()&&tickCount%10==0){
            boolean near=!active()&&!phasing()&&opponents().isEmpty()&&level().getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,getBoundingBox().inflate(5),
                    p->p.isAlive()&&!p.isSpectator()&&distanceToSqr(p)<=25&&carriesCloud(p)).size()>0;
            entityData.set(CLOUD_NEAR,near);
        }
    }
    @Override public void die(DamageSource source){
        super.die(source);
        if(!level().isClientSide&&isDeadOrDying()&&departure()==0)beginDeparture(WeaverDeparture.DEFEATED);
    }
    @Override protected int getBaseExperienceReward(){return WeaverDeparture.EXPERIENCE;}
    @Override protected boolean isAlwaysExperienceDropper(){return true;}
    @Override protected void dropAllDeathLoot(net.minecraft.server.level.ServerLevel server,DamageSource source){
        // Keep vanilla kill/death events immediate, but run the loot pipeline only after the finale.
        if(!pendingDeathLoot.isEmpty()||(returned&WeaverDeparture.REWARD_RETURNED)!=0)return;
        pendingDeathLoot.putString("Type",source.typeHolder().unwrapKey().orElseThrow().location().toString());
        if(source.getEntity()!=null)pendingDeathLoot.putUUID("Attacker",source.getEntity().getUUID());
        if(source.getDirectEntity()!=null)pendingDeathLoot.putUUID("Direct",source.getDirectEntity().getUUID());
        if(lastHurtByPlayer!=null)pendingDeathLoot.putUUID("Player",lastHurtByPlayer.getUUID());
        pendingDeathLoot.putInt("RecentPlayer",lastHurtByPlayerTime);
    }
    private void finishDepartureRewards(){
        if((returned&WeaverDeparture.REWARD_RETURNED)!=0||!(level() instanceof net.minecraft.server.level.ServerLevel server))return;
        returned|=WeaverDeparture.REWARD_RETURNED;
        if(departure()==WeaverDeparture.DISSOLVE){
            spawnAtLocation(com.mcmagic.omnira.registry.ModItems.FOUNDER_APPROVAL.get());
            dropExperience(null);
        }else if(!pendingDeathLoot.isEmpty()){
            // Run vanilla loot and experience at the final rendered core, not the grounded entity origin.
            setPos(position().add(0,1.5+WeaverDeparture.deathRise(WeaverDeparture.DEATH_TICKS),0));
            var registry=registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
            var type=net.minecraft.resources.ResourceLocation.tryParse(pendingDeathLoot.getString("Type"));
            var holder=type!=null&&registry.containsKey(type)?registry.getHolderOrThrow(ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,type)):damageSources().generic().typeHolder();
            var attacker=pendingDeathLoot.hasUUID("Attacker")?server.getEntity(pendingDeathLoot.getUUID("Attacker")):null;
            var direct=pendingDeathLoot.hasUUID("Direct")?server.getEntity(pendingDeathLoot.getUUID("Direct")):null;
            lastHurtByPlayer=pendingDeathLoot.hasUUID("Player")?server.getPlayerByUUID(pendingDeathLoot.getUUID("Player")):null;
            lastHurtByPlayerTime=pendingDeathLoot.getInt("RecentPlayer");
            super.dropAllDeathLoot(server,new DamageSource(holder,direct,attacker));
            pendingDeathLoot=new CompoundTag();
        }
    }
    @Override protected void tickDeath(){
        deathTime++;
        if(level().isClientSide)return;
        if(departure()==0)beginDeparture(WeaverDeparture.DEFEATED);
        exitTicks++;
        if(level() instanceof net.minecraft.server.level.ServerLevel server&&exitTicks>=nextBlast){
            nextBlast=exitTicks+4+random.nextInt(7);
            var center=position().add((random.nextDouble()-.5)*1.8,1.5+WeaverDeparture.deathRise(exitTicks)+(random.nextDouble()-.5)*1.8,(random.nextDouble()-.5)*1.8);
            WeaverCombat.visual(server,3,center,center,WeaverDeparture.BLAST_MIN_RADIUS+random.nextFloat()*(WeaverDeparture.BLAST_MAX_RADIUS-WeaverDeparture.BLAST_MIN_RADIUS));
            if(exitTicks%3==0)server.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.HOSTILE,.45F,.8F+random.nextFloat()*.4F);
        }
        if(exitTicks>=WeaverDeparture.DEATH_TICKS){finishDepartureRewards();bossEvent.removeAllPlayers();remove(RemovalReason.KILLED);}
    }
    public float motionClock(float partial){
        long start=entityData.get(MOTION_START);
        return entityData.get(MOTION_FROZEN)+(active()&&start>=0?Math.max(0,level().getGameTime()-start)+partial:0);
    }
    private void active(boolean value){
        if(active()==value)return;
        if(value){phasing(true);escapePoint=null;escapeRetry=0;entityData.set(MOTION_START,level().getGameTime());entityData.set(CLOUD_NEAR,false);}
        else{if(isAlive()&&departure()==0){setHealth(getMaxHealth());bossEvent.setProgress(1);}entityData.set(MOTION_FROZEN,motionClock(0));entityData.set(MOTION_START,-1L);}
        entityData.set(ACTIVE,value);
    }
    @Override protected void registerGoals(){}
    @Override protected double getDefaultGravity(){
        double gravity=super.getDefaultGravity();
        return !active()&&getDeltaMovement().y<=0?Math.min(gravity,.01):gravity;
    }
    @Override protected PathNavigation createNavigation(Level level){var nav=new FlyingPathNavigation(this,level);nav.setCanFloat(true);nav.setCanOpenDoors(false);return nav;}
    public List<LivingEntity> opponents(){
        if(!(level() instanceof net.minecraft.server.level.ServerLevel level))return List.of();
        var result=new ArrayList<LivingEntity>();
        opponents.removeIf(id->{
            if(level.getEntity(id) instanceof LivingEntity target&&target.isAlive()&&!target.isSpectator()
                    &&(!(target instanceof net.minecraft.world.entity.player.Player p)||!p.isCreative())&&distanceToSqr(target)<=48*48){result.add(target);return false;}
            return true;
        });
        return result;
    }
    public void attacked(DamageSource source){
        if(!isAlive()||departure()!=0)return;
        var attacker=source.getEntity();
        if(attacker instanceof TamableAnimal tame&&tame.getOwner()!=null)attacker=tame.getOwner();
        if(attacker instanceof LivingEntity living&&attacker!=this)opponents.add(living.getUUID());
        if(opponents().isEmpty())return;
        active(true);
        if(!WeaverCombat.reacting())WeaverCombat.bend(this,random.nextBoolean());
        if(!reloaded&&getHealth()<=512){reloaded=true;phaseClock=0;}
    }
    @Override protected void customServerAiStep(){
        if(departure()!=0)return;
        super.customServerAiStep();bossEvent.setName(getDisplayName());bossEvent.setProgress(getHealth()/getMaxHealth());
        var targets=opponents();boolean engaged=!targets.isEmpty();
        active(engaged);setNoGravity(engaged||phasing());moveControl=engaged?flightControl:idleControl;
        if(!engaged){
            setTarget(null);navigation.stop();moveControl.setWantedPosition(getX(),getY(),getZ(),0);
            // Do not carry flight steering into gravity or terrain-exit movement.
            setXxa(0);setYya(0);setZza(0);setSpeed(0);
            setDeltaMovement(getDeltaMovement().multiply(0,1,0));attackClock=60;
            reloaded=false;phaseClock=-1;entityData.set(RED,false);
            if(phasing()){
                if(com.mcmagic.omnira.weaver.WeaverPhasing.clear(this,position())){phasing(false);setNoGravity(false);escapePoint=null;}
                else{
                    if(--escapeRetry<=0){escapePoint=com.mcmagic.omnira.weaver.WeaverPhasing.exit(this);escapeRetry=20;}
                    if(escapePoint!=null&&!com.mcmagic.omnira.weaver.WeaverPhasing.clear(this,escapePoint))escapePoint=null;
                    setDeltaMovement(escapePoint==null?Vec3.ZERO:escapePoint.subtract(position()).normalize().scale(.25));
                }
            }
            return;
        }
        if(!targets.isEmpty()){
            var nearest=targets.stream().min(Comparator.comparingDouble(this::distanceToSqr)).orElseThrow();
            setTarget(nearest);getLookControl().setLookAt(nearest,20,20);
            if(tickCount%10==0)hoverNear(nearest);
        }else{setTarget(null);navigation.stop();setDeltaMovement(getDeltaMovement().scale(.5));}
        if(phaseClock>=0){
            if(phaseClock==0)WeaverCombat.bend(this,false);
            if(phaseClock==10)WeaverCombat.bend(this,true);
            if(phaseClock>=20&&phaseClock<=50&&phaseClock%10==0){for(var target:targets)WeaverCombat.lockedRay(this,target);heal(128);}
            if(++phaseClock>50){phaseClock=-1;entityData.set(RED,true);attackClock=60;}
        }else if(!targets.isEmpty()&&--attackClock<=0){attackRound(targets);attackClock=60;}
    }
    private void hoverNear(LivingEntity target){
        var delta=position().subtract(target.position()).multiply(1,0,1);
        double angle=delta.lengthSqr()<.01?0:Math.atan2(delta.z,delta.x);
        var goal=target.position().add(Math.cos(angle)*7,2,Math.sin(angle)*7);
        navigation.stop();moveControl.setWantedPosition(goal.x,goal.y,goal.z,1);
    }
    public void attackRound(List<LivingEntity> targets){
        if(normalSinceRay>=5){for(var target:targets)WeaverCombat.lockedRay(this,target);normalSinceRay=0;return;}
        for(var target:targets)WeaverCombat.bolt(this,target,getEyePosition(),target.getBoundingBox().getCenter().subtract(getEyePosition()));
        normalSinceRay++;normals++;
        if(normals%10==0)entityData.set(TIERS,Math.min(3,strength()+1));
    }
    @Override public void startSeenByPlayer(ServerPlayer player){super.startSeenByPlayer(player);bossEvent.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayer player){super.stopSeenByPlayer(player);bossEvent.removePlayer(player);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public boolean isInvulnerableTo(DamageSource source){return departure()!=0||source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)
            ||source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||source.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)||super.isInvulnerableTo(source);}
    @Override public boolean doHurtTarget(Entity target){return false;}
    @Override protected ResourceKey<LootTable> getDefaultLootTable(){return ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,net.minecraft.resources.ResourceLocation.parse("omnira:entities/weaver_spirit"));}
    @Override public void addAdditionalSaveData(CompoundTag tag){
        super.addAdditionalSaveData(tag);tag.putBoolean("WeaverReloaded",reloaded);tag.putBoolean("WeaverRed",redCore());tag.putInt("WeaverStrength",strength());
        tag.putBoolean("WeaverPhasing",phasing());tag.putBoolean("WeaverEngaged",active());
        tag.putInt("WeaverExit",departure());tag.putInt("WeaverExitTicks",exitTicks);tag.putInt("WeaverReturned",returned);
        if(!pendingDeathLoot.isEmpty())tag.put("WeaverPendingDeathLoot",pendingDeathLoot.copy());
        if(!offeredCloud.isEmpty())tag.put("WeaverCloud",offeredCloud.save(level().registryAccess()));
        tag.putInt("WeaverAttackClock",attackClock);tag.putInt("WeaverNormals",normals);tag.putInt("WeaverSinceRay",normalSinceRay);tag.putInt("WeaverPhaseClock",phaseClock);
        var list=new ListTag();opponents.forEach(id->list.add(StringTag.valueOf(id.toString())));tag.put("WeaverOpponents",list);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);reloaded=tag.getBoolean("WeaverReloaded");entityData.set(RED,tag.getBoolean("WeaverRed"));entityData.set(TIERS,Math.clamp(tag.getInt("WeaverStrength"),0,3));
        attackClock=tag.contains("WeaverAttackClock")?Math.clamp(tag.getInt("WeaverAttackClock"),1,60):60;
        normals=Math.max(0,tag.getInt("WeaverNormals"));normalSinceRay=Math.clamp(tag.getInt("WeaverSinceRay"),0,5);
        phaseClock=tag.contains("WeaverPhaseClock")?Math.clamp(tag.getInt("WeaverPhaseClock"),-1,50):-1;
        opponents.clear();for(var raw:tag.getList("WeaverOpponents",8))try{opponents.add(UUID.fromString(raw.getAsString()));}catch(IllegalArgumentException ignored){}
        entityData.set(ACTIVE,false);entityData.set(MOTION_START,-1L);entityData.set(MOTION_FROZEN,0F);
        phasing(tag.getBoolean("WeaverPhasing"));escapePoint=null;escapeRetry=0;
        entityData.set(ACTIVE,tag.getBoolean("WeaverEngaged"));
        if(active())entityData.set(MOTION_START,level().getGameTime());
        int exit=Math.clamp(tag.getInt("WeaverExit"),0,2);exitTicks=Math.clamp(tag.getInt("WeaverExitTicks"),0,WeaverDeparture.DEATH_TICKS);returned=tag.getInt("WeaverReturned")&63;
        pendingDeathLoot=tag.getCompound("WeaverPendingDeathLoot").copy();
        offeredCloud=net.minecraft.world.item.ItemStack.parseOptional(level().registryAccess(),tag.getCompound("WeaverCloud"));
        entityData.set(EXIT,exit);entityData.set(EXIT_START,level().getGameTime()-exitTicks);entityData.set(CLOUD_NEAR,exit==WeaverDeparture.DISSOLVE);
        nextBlast=exitTicks+4;
        if(exit!=0){opponents.clear();setNoAi(true);setNoGravity(true);setDeltaMovement(Vec3.ZERO);}
    }
}
