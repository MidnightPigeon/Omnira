package com.mcmagic.omnira.space.sea;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Swimming and flight share collision-aware steering; neither species acquires nearby players. */
public abstract class StarSeaCreature extends TamableAnimal {
    private Vec3 destination;
    private int wanderTicks,attackTicks;
    private boolean attracted;
    protected StarSeaCreature(EntityType<? extends TamableAnimal> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void registerGoals(){targetSelector.addGoal(1,new HurtByTargetGoal(this));}
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type){return false;}
    @Override public boolean isInWater(){return super.isInWater()||isInFluidType((type,height)->type.canSwim(this));}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override public boolean isFood(ItemStack stack){return false;}
    @Override public boolean canMate(Animal other){return false;}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){return null;}
    @Override public boolean canAttack(LivingEntity target){return super.canAttack(target)&&!isAlliedTo(target)&&!(target instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator()));}
    @Override protected void customServerAiStep(){
        super.customServerAiStep();setNoGravity(true);
        if(attackTicks>0)attackTicks--;
        LivingEntity target=getTarget();
        if(target!=null&&(!target.isAlive()||!canAttack(target)||distanceToSqr(target)>48*48)){setTarget(null);target=null;}
        if(isVehicle()){if(target!=null)combat(target);return;}
        Vec3 lure=target==null&&!isOrderedToSit()?attraction():null;
        if(attracted&&lure==null){destination=null;wanderTicks=0;}
        attracted=lure!=null;
        if(target!=null){destination=target.position().add(0,target.getBbHeight()*.35,0);combat(target);}
        else if(isOrderedToSit()){setDeltaMovement(getDeltaMovement().scale(.6));return;}
        else if(lure!=null)destination=lure;
        else if(isTame()&&getOwner()!=null&&distanceToSqr(getOwner())>36)destination=getOwner().position().add(0,1,0);
        else if(--wanderTicks<=0||destination==null||position().distanceToSqr(destination)<1||horizontalCollision||verticalCollision){
            wanderTicks=40+random.nextInt(60);destination=chooseDestination();
        }
        if(destination!=null){
            Vec3 delta=destination.subtract(position());
            double speed=target!=null?.19:.1;
            Vec3 desired=delta.lengthSqr()<1?delta.scale(speed):delta.normalize().scale(speed);
            Vec3 motion=getDeltaMovement().scale(.8).add(desired.scale(.2));
            if(!safePath(motion)){motion=Vec3.ZERO;destination=null;wanderTicks=0;}
            setDeltaMovement(motion);
            if(delta.horizontalDistanceSqr()>.02){float yaw=(float)(Math.atan2(-delta.x,delta.z)*180/Math.PI);setYRot(net.minecraft.util.Mth.rotLerp(.12F,getYRot(),yaw));yBodyRot=yHeadRot=getYRot();}
        }
    }
    protected void combat(LivingEntity target){}
    protected Vec3 attraction(){return null;}
    protected boolean attackReady(){return attackTicks==0;}
    protected void attackCooldown(int ticks){attackTicks=ticks;}
    private Vec3 chooseDestination(){
        boolean safeWater=isInWater()&&attractingWater(blockPosition());
        if(!safeWater){
            Vec3 water=nearbyWater();if(water!=null)return water;
            if(isInFluidType(com.mcmagic.omnira.mire.MireContent.TYPE.get())){
                for(int dy=1;dy<=32&&getBlockY()+dy<level().getMaxBuildHeight()-2;dy++){
                    BlockPos exit=blockPosition().above(dy);
                    if(!level().getFluidState(exit).isEmpty())continue;
                    Vec3 point=Vec3.atBottomCenterOf(exit).add(0,.25,0);
                    if(safePath(point.subtract(position())))return point;
                }
            }
        }
        Vec3 best=position();double score=-Double.MAX_VALUE;
        for(int i=0;i<32;i++){
            BlockPos p=blockPosition().offset(random.nextInt(25)-12,random.nextInt(17)-8,random.nextInt(25)-12);
            if(!level().hasChunkAt(p)||!level().getWorldBorder().isWithinBounds(p))continue;
            if(!safeWater){
                BlockPos ground=p.atY(getBlockY());
                while(ground.getY()>level().getMinBuildHeight()+1&&level().getBlockState(ground).getCollisionShape(level(),ground).isEmpty())ground=ground.below();
                if(level().getBlockState(ground).getCollisionShape(level(),ground).isEmpty())continue;
                p=ground.above(2+random.nextInt(5));
            }
            Vec3 point=Vec3.atCenterOf(p),delta=point.subtract(position());var box=getBoundingBox().move(delta);
            if(box.minY<level().getMinBuildHeight()+1||box.maxY>=level().getMaxBuildHeight()||!level().noCollision(this,box))continue;
            // Reject blocked straight paths so a nearer reachable pool wins over water behind a wall.
            if(!safePath(delta))continue;
            var fluid=level().getFluidState(p);
            if(!fluid.isEmpty()&&!attractingWater(p)||safeWater&&!attractingWater(p))continue;
            double value=(attractingWater(p)?100:0)-delta.lengthSqr()*.06+random.nextDouble()*4;
            if(value>score){score=value;best=point;}
        }
        return best;
    }
    private Vec3 nearbyWater(){
        Vec3 best=null;double distance=257;BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(int x=-16;x<=16;x++)for(int z=-16;z<=16;z++)for(int y=-16;y<=16;y++){
            double d=x*x+y*y+z*z;if(d>256||d>=distance)continue;
            p.set(getBlockX()+x,getBlockY()+y,getBlockZ()+z);if(!level().hasChunkAt(p))continue;
            if(!attractingWater(p))continue;
            Vec3 point=Vec3.atCenterOf(p),delta=point.subtract(position());
            if(safePath(delta)){best=point;distance=d;}
        }
        return best;
    }
    public boolean preferredFluid(net.minecraft.world.level.material.FluidState state){return !state.isEmpty()&&!com.mcmagic.omnira.mire.MireContent.fluid(state)&&state.getFluidType().canSwim(this);}
    public boolean attractingWater(BlockPos center){
        var fluid=level().getFluidState(center);if(!preferredFluid(fluid))return false;
        if(!level().hasChunksAt(center.offset(-1,0,-1),center.offset(1,1,1)))return false;
        for(BlockPos p:BlockPos.betweenClosed(center.offset(-1,0,-1),center.offset(1,1,1))){
            var sample=level().getFluidState(p);
            if(!preferredFluid(sample)||sample.getFluidType()!=fluid.getFluidType()||sample.getAmount()<8)return false;
        }
        return true;
    }
    protected boolean safePath(Vec3 delta){
        boolean escaped=!isInFluidType(com.mcmagic.omnira.mire.MireContent.TYPE.get());
        int steps=Math.max(1,(int)Math.ceil(delta.length()*2));
        for(int step=1;step<=steps;step++){
            var box=getBoundingBox().move(delta.scale((double)step/steps));
            if(!level().hasChunksAt(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))||!level().noCollision(this,box))return false;
            boolean mire=BlockPos.betweenClosedStream(box).anyMatch(p->com.mcmagic.omnira.mire.MireContent.fluid(level().getFluidState(p)));
            if(mire&&escaped)return false;if(!mire)escaped=true;
        }
        return true;
    }
    @Override public void travel(Vec3 input){
        setNoGravity(true);
        if(isControlledByLocalInstance()){
            moveRelative(isVehicle()?.32F:.025F,input);
            move(MoverType.SELF,getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(isVehicle()?.82:.94));
        }
        calculateEntityAnimation(false);
    }
    @Override public void tick(){super.tick();setNoGravity(true);fallDistance=0;}
}
