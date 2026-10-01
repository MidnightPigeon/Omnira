package com.mcmagic.omnira.vehicle;

import com.mcmagic.omnira.item.SpellCoreItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Server-authoritative hovercraft; no Create or physics-mod dependency. */
public final class CruiseOrbEntity extends Entity {
    private static final EntityDataAccessor<Boolean> STARTED=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> OVERLOAD=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> AXES=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VORTEX=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> IMPACTS=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> BUTTON=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> CORE=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<CompoundTag> STORAGE=SynchedEntityData.defineId(CruiseOrbEntity.class,EntityDataSerializers.COMPOUND_TAG);
    public final CruiseOrbStorage storage=new CruiseOrbStorage(this);
    private int keys,previousKeys,repairTicks,impactCooldown;
    private long lastInput,lastAction=-100;
    private boolean storageDirty;
    private final java.util.Set<Integer> shellContacts=new java.util.HashSet<>();
    private Vec3 latched=Vec3.ZERO;
    private int interpolation;
    private Vec3 target=Vec3.ZERO;
    private float targetYaw;
    private int buttonTicks;
    private float riderTurn;
    private int lastRiderTurnTick=-1;
    private CruiseVortex.Active vortex;

    public CruiseOrbEntity(EntityType<? extends CruiseOrbEntity> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){
        b.define(STARTED,false);b.define(OVERLOAD,false);b.define(AXES,13);b.define(VORTEX,0);
        b.define(IMPACTS,0);b.define(BUTTON,0);b.define(CORE,ItemStack.EMPTY);b.define(STORAGE,new CompoundTag());
    }
    public boolean started(){return entityData.get(STARTED);}
    public boolean overloaded(){return entityData.get(OVERLOAD);}
    public ItemStack core(){return entityData.get(CORE);}
    public void setCore(ItemStack stack){if(stack.isEmpty() || stack.getItem() instanceof SpellCoreItem)entityData.set(CORE,stack.copyWithCount(stack.isEmpty()?0:1));}
    public int vortexTicks(){return entityData.get(VORTEX);}
    public int crackStage(){return (entityData.get(IMPACTS)+2)/3;}
    public boolean hasCracks(){return entityData.get(IMPACTS)>0;}
    public boolean buttonPressed(int button){return entityData.get(BUTTON)==button+1;}
    public Vec3 motionAxes(){int a=entityData.get(AXES);return new Vec3(a%3-1,a/3%3-1,a/9-1);}
    private void axes(Vec3 v){entityData.set(AXES,((int)v.x+1)+3*((int)v.y+1)+9*((int)v.z+1));}
    public void beginVortex(ServerPlayer player){vortex=new CruiseVortex.Active(player);entityData.set(VORTEX,CruiseVortex.DURATION);}
    public void syncStorage(){if(!level().isClientSide)storageDirty=true;}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(key==STORAGE && level().isClientSide && storage!=null)storage.load(entityData.get(STORAGE));
    }
    public double speed(){return (core().getItem() instanceof SpellCoreItem?core().is(ModItems.TEST_SPELL_CORE.get())?2:3:1)*storage.speedMultiplier();}
    public float turnSpeed(){return 3;}
    public static Vec3 pressedAxes(int keys){return new Vec3(((keys&8)!=0?1:0)-((keys&4)!=0?1:0),
            ((keys&16)!=0?1:0)-((keys&32)!=0?1:0),((keys&1)!=0?1:0)-((keys&2)!=0?1:0));}
    public static double latchAxis(double old,double pressed){return pressed==0?old:old==-pressed?0:pressed;}
    public void input(ServerPlayer player,int mask){
        if(getFirstPassenger()!=player || mask<0 || mask>63)return;
        lastInput=level().getGameTime();keys=player.containerMenu instanceof CruiseOrbMenu?0:mask;
    }
    public void stop(){entityData.set(STARTED,false);entityData.set(OVERLOAD,false);keys=previousKeys=0;latched=Vec3.ZERO;axes(Vec3.ZERO);}
    /** Both ends ray-test the same physical panel; a packet cannot toggle it from behind. */
    public int pointedButton(Player player){
        Vec3 eye=player.getEyePosition().subtract(position()).yRot(getYRot()*Mth.DEG_TO_RAD);
        Vec3 ray=player.getLookAngle().yRot(getYRot()*Mth.DEG_TO_RAD);
        var inverse=CruiseOrbPanel.transform().invert();
        var localEye=inverse.transformPosition(eye.toVector3f());
        var localRay=inverse.transformDirection(ray.toVector3f());
        eye=new Vec3(localEye);ray=new Vec3(localRay);
        if(Math.abs(ray.z)<1e-6)return -1;
        double t=(-.9-eye.z)/ray.z;
        if(t<0 || t>3)return -1;
        Vec3 hit=eye.add(ray.scale(t));
        if(Math.abs(hit.y-1.8)>CruiseOrbPanel.BUTTON_HEIGHT)return -1;
        return Math.abs(hit.x+CruiseOrbPanel.BUTTON_X)<CruiseOrbPanel.BUTTON_WIDTH?0:Math.abs(hit.x-CruiseOrbPanel.BUTTON_X)<CruiseOrbPanel.BUTTON_WIDTH?1:-1;
    }
    public void action(ServerPlayer player,int action){
        if(getFirstPassenger()!=player || !player.isAlive() || isRemoved())return;
        long now=level().getGameTime();if(now-lastAction<4)return;lastAction=now;
        if(action==3){
            if(player.containerMenu==player.inventoryMenu)player.openMenu(new SimpleMenuProvider((id,inv,p)->new CruiseOrbMenu(id,inv,this),getName()),b->b.writeInt(getId()));
            return;
        }
        if(player.containerMenu!=player.inventoryMenu || pointedButton(player)!=action-1)return;
        entityData.set(BUTTON,action);buttonTicks=4;
        if(started() || getDeltaMovement().lengthSqr()>.000001){stop();return;}
        entityData.set(STARTED,true);entityData.set(OVERLOAD,action==2);
        latched=Vec3.ZERO;previousKeys=0;
    }
    @Override public InteractionResult interact(Player player,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        var held=player.getMainHandItem();
        if(player.isShiftKeyDown() && held.is(ModItems.GOLDEN_TOILET.get()) && !storage.hasGoldenToilet() && !isVehicle() && !started()){
            if(!level().isClientSide && storage.installGoldenToilet(held))held.consume(1,player);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(player.isShiftKeyDown() && held.isEmpty() && !core().isEmpty() && !isVehicle()){
            if(!level().isClientSide){player.setItemInHand(hand,core().copy());entityData.set(CORE,ItemStack.EMPTY);}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(!player.isShiftKeyDown() && held.getItem() instanceof SpellCoreItem && core().isEmpty() && !isVehicle()){
            if(!level().isClientSide){entityData.set(CORE,held.copyWithCount(1));held.consume(1,player);}
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if(!player.isShiftKeyDown() && !started() && !isVehicle() && getDeltaMovement().lengthSqr()<.000001){
            if(!level().isClientSide)player.startRiding(this);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return InteractionResult.PASS;
    }
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public boolean isPushable(){return false;}
    @Override public boolean canBeCollidedWith(){return !isRemoved();}
    @Override public boolean canCollideWith(Entity other){return !isPassengerOfSameVehicle(other) && (other.isPushable() || other.canBeCollidedWith());}
    @Override protected boolean canAddPassenger(Entity passenger){return passenger instanceof Player && !isVehicle() && !started();}
    @Override public boolean isControlledByLocalInstance(){return false;}
    @Override public boolean dismountsUnderwater(){return false;}
    @Override public boolean canBeRiddenUnderFluidType(net.neoforged.neoforge.fluids.FluidType type,Entity rider){return true;}
    @Override protected void positionRider(Entity passenger,MoveFunction move){
        if(hasPassenger(passenger)){
            var offset=new Vec3(0,CruiseOrbPanel.RIDER_Y,.18).yRot(-getYRot()*Mth.DEG_TO_RAD);
            move.accept(passenger,getX()+offset.x,getY()+offset.y,getZ()+offset.z);
            if(lastRiderTurnTick!=tickCount){
                lastRiderTurnTick=tickCount;
                passenger.setYRot(passenger.getYRot()+riderTurn);
                passenger.setYHeadRot(passenger.getYHeadRot()+riderTurn);
            }
            passenger.fallDistance=0;
        }
    }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){
        for(int dy=0;dy<=2;dy++)for(int i=0;i<8;i++){
            double a=i*Math.PI/4;Vec3 p=position().add(Math.cos(a)*2,dy,Math.sin(a)*2);
            if(level().getBlockCollisions(passenger,passenger.getDimensions(Pose.STANDING).makeBoundingBox(p)
                    .minmax(passenger.getDimensions(Pose.STANDING).makeBoundingBox(passenger.position()))).iterator().hasNext())continue;
            if(level().noCollision(passenger,passenger.getDimensions(Pose.STANDING).makeBoundingBox(p)))return p;
        }
        // Never teleport through a wall or into a ceiling when no exit is clear.
        return passenger.position();
    }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps){target=new Vec3(x,y,z);targetYaw=yaw;interpolation=Math.max(1,steps);}
    @Override public void tick(){
        super.tick();setNoGravity(true);fallDistance=0;riderTurn=0;
        if(level().isClientSide){
            if(interpolation>0){setPos(position().lerp(target,1.0/interpolation));float previous=getYRot();setYRot(Mth.rotLerp(1F/interpolation,previous,targetYaw));riderTurn=Mth.wrapDegrees(getYRot()-previous);interpolation--;}
            return;
        }
        if(!(getFirstPassenger() instanceof ServerPlayer player) || !player.isAlive())stop();
        if(level().getGameTime()-lastInput>30)keys=0;
        if(impactCooldown>0)impactCooldown--;
        if(buttonTicks>0 && --buttonTicks==0)entityData.set(BUTTON,0);
        if(entityData.get(IMPACTS)>0 && ++repairTicks>=200){repairTicks=0;entityData.set(IMPACTS,entityData.get(IMPACTS)-1);}
        if(vortexTicks()>0){CruiseVortex.tick(this,vortex);entityData.set(VORTEX,vortexTicks()-1);if(vortexTicks()==0)vortex=null;}
        storage.tick();
        if(storageDirty){storageDirty=false;entityData.set(STORAGE,storage.save());}
        if(com.mcmagic.omnira.time.FleetingTime.bonus(this))return;
        Vec3 motion=Vec3.ZERO;
        if(started() && getFirstPassenger() instanceof ServerPlayer player){
            if(overloaded()){
                Vec3 press=pressedAxes(keys&~previousKeys);
                latched=new Vec3(latchAxis(latched.x,press.x),latchAxis(latched.y,press.y),latchAxis(latched.z,press.z));
                if(core().isEmpty()){
                    // One mana per tick is 20/s, without free bursts from toggling before a full second.
                    var mana=player.getData(ModAttachments.MANA);
                    if(mana.canSpend(1))player.setData(ModAttachments.MANA,mana.spend(1));else stop();
                }
            }
            if(started()){
                motion=overloaded()?latched:pressedAxes(keys);
                if(!overloaded()){
                    riderTurn=(float)motion.x*turnSpeed();
                    setYRot(getYRot()+riderTurn);
                    motion=new Vec3(0,motion.y,motion.z);
                }
            }
        }
        previousKeys=keys;axes(motion);
        Vec3 velocity=started()?new Vec3(motion.x,motion.y,-motion.z).normalize().scale(speed()/20).yRot(-getYRot()*Mth.DEG_TO_RAD):getDeltaMovement().scale(.82);
        if(velocity.lengthSqr()<.000001)velocity=Vec3.ZERO;
        var next=getBoundingBox().move(velocity);
        if(next.minY<level().getMinBuildHeight() || next.maxY>level().getMaxBuildHeight() || !level().getWorldBorder().isWithinBounds(next)
                || !level().hasChunksAt(BlockPos.containing(next.minX,next.minY,next.minZ),BlockPos.containing(next.maxX,next.maxY,next.maxZ)))velocity=Vec3.ZERO;
        setDeltaMovement(velocity);move(MoverType.SELF,velocity);
        checkEntityImpacts();
    }
    private void checkEntityImpacts(){
        if(isRemoved() || storage.stabilized()){shellContacts.clear();return;}
        var shell=getBoundingBox();
        var touching=level().getEntities(this,shell.inflate(.1,.25,.1),
                other->other instanceof LivingEntity && other.isAlive() && !isPassengerOfSameVehicle(other));
        var current=new java.util.HashSet<Integer>();
        for(var other:touching){
            var feet=other.getBoundingBox().minY;
            if(feet<shell.maxY-.2 || feet>shell.maxY+.25
                    || other.distanceToSqr(getX(),other.getY(),getZ())>1.2*1.2)continue;
            current.add(other.getId());
            if(!shellContacts.contains(other.getId()) && (other.getDeltaMovement().y<-.04
                    || other.yo-other.getY()>.04))impact();
        }
        shellContacts.clear();shellContacts.addAll(current);
    }
    public void impact(){
        damageShell(true);
    }
    private void damageShell(boolean collision){
        if(level().isClientSide || isRemoved() || collision && storage.stabilized() || impactCooldown>0)return;
        impactCooldown=10;repairTicks=0;int count=entityData.get(IMPACTS)+1;
        if(count>=9){destroyOrb(collision);return;}entityData.set(IMPACTS,count);
        playSound(net.minecraft.sounds.SoundEvents.GLASS_HIT,.8F,.7F);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(isInvulnerableTo(source) || isRemoved() || level().isClientSide)return false;
        if(source.getEntity() instanceof Player player && hasPassenger(player))return false;
        if(!isVehicle() && source.getEntity() instanceof Player
                && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)){destroyOrb();return true;}
        if(CruiseOrbProtection.environmental(source))return false;
        impact();
        return true;
    }
    @Override public boolean fireImmune(){return true;}
    public void selfBurst(){if(!storage.stabilized())destroyOrb(true);}
    public void destroyOrb(){
        destroyOrb(false);
    }
    private void destroyOrb(boolean shattered){
        if(isRemoved() || level().isClientSide)return;
        stop();ejectPassengers();
        if(storage.stabilized()){
            var stack=new ItemStack(ModItems.CRUISE_ORB.get());var data=storage.save();
            if(!core().isEmpty())data.put("Core",core().save(registryAccess()));
            stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data));spawnAtLocation(stack);
        } else {
            for(int i=0;i<storage.items.getSlots();i++)dropStack(storage.items.getStackInSlot(i));
            for(int i=0;i<3;i++)dropStack(storage.upgrades.getStackInSlot(i));
            dropStack(storage.port.getStackInSlot(0));dropStack(storage.trash.getStackInSlot(0));dropStack(core());
            dropStack(storage.installedSeat());
            spawnAtLocation(shattered?new ItemStack(Items.GLASS_PANE,3):new ItemStack(ModItems.CRUISE_ORB.get()));
            if(shattered){
                spawnAtLocation(new ItemStack(ModItems.CRYSTAL_BALL.get()));
                spawnAtLocation(new ItemStack(ModItems.LIQUID_CRYSTAL_BALL.get()));
            }
            releaseFluid();
        }
        playSound(net.minecraft.sounds.SoundEvents.GLASS_BREAK,1,1);discard();
    }
    private void dropStack(ItemStack stack){var rest=stack.copy();while(!rest.isEmpty())spawnAtLocation(rest.split(Math.min(rest.getCount(),rest.getMaxStackSize())));}
    private void releaseFluid(){
        var fluid=storage.tank.getFluid();if(fluid.isEmpty())return;
        var state=fluid.getFluid().defaultFluidState().createLegacyBlock();if(state.isAir())return;
        int sources=fluid.getAmount()/1000;
        for(int r=0;r<=6 && sources>0;r++)for(int x=-r;x<=r && sources>0;x++)for(int z=-r;z<=r && sources>0;z++){
            if(Math.max(Math.abs(x),Math.abs(z))!=r)continue;
            var p=blockPosition().offset(x,0,z);
            if(level().hasChunkAt(p) && level().getBlockState(p).isAir()){level().setBlockAndUpdate(p,state);sources--;}
        }
    }
    public void restoreItem(ItemStack item){var tag=item.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();storage.load(tag);entityData.set(CORE,ItemStack.parseOptional(registryAccess(),tag.getCompound("Core")));syncStorage();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){tag.put("Storage",storage.save());if(!core().isEmpty())tag.put("Core",core().save(registryAccess()));tag.putInt("Impacts",entityData.get(IMPACTS));}
    @Override protected void readAdditionalSaveData(CompoundTag tag){storage.load(tag.getCompound("Storage"));entityData.set(CORE,ItemStack.parseOptional(registryAccess(),tag.getCompound("Core")));entityData.set(IMPACTS,Math.clamp(tag.getInt("Impacts"),0,8));stop();syncStorage();}
}
