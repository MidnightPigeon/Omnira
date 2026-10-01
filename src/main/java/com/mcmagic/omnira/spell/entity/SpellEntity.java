package com.mcmagic.omnira.spell.entity;

import com.mcmagic.omnira.registry.ModEntityTypes;
import com.mcmagic.omnira.spell.SpellCasting;
import com.mcmagic.omnira.spell.SpellPayload;
import com.mcmagic.omnira.spell.SpellEffect;
import com.mcmagic.omnira.spell.SpellDamageSource;
import com.mcmagic.omnira.spell.UtilitySpellEffects;
import com.mcmagic.omnira.entity.DreamMirror;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpellEntity extends Entity {
    public enum Kind { BARRIER, FACE_BARRIER, WARD, ORB, PROJECTILE, FLYING_BLOCK, SELF_FLIGHT, BURST, REVERSE_FLOW, SPATIAL_BLADE }
    private CompositeSpellMotion compositeMotion;
    private static final EntityDataAccessor<Integer> COMPOSITE_DELAY=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COMPOSITE_STEP=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.INT);
    public int compositeDelay(){return entityData.get(COMPOSITE_DELAY);}
    public float compositeAge(float partialTick){return Math.max(0,entityData.get(COMPOSITE_STEP)-1)+partialTick;}
    public void compositeStep(int step){entityData.set(COMPOSITE_STEP,step);}
    public void composite(CompositeSpellMotion motion,int duration,int delay){compositeMotion=motion;entityData.set(DURATION,duration);entityData.set(COMPOSITE_DELAY,delay);}

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHARGES = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HITS = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> BORN = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> TEMPORAL_AGE = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<BlockState> BLOCK = SynchedEntityData.defineId(SpellEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Integer> DURATION=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> POWER=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RANGE=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BURST_RADIUS=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> KNOWLEDGE_BLOOD=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.BOOLEAN);
    public void burstRadius(double radius){entityData.set(BURST_RADIUS,(float)Math.max(0,radius));}
    public float burstRadius(){return entityData.get(BURST_RADIUS)<0?3*rangeMultiplier():entityData.get(BURST_RADIUS);}
    private float damage;
    private boolean knowledgeShock;
    private boolean knowledgeLeech;
    private SpellPayload payload=SpellPayload.EMPTY;
    private boolean explosiveImpact;
    private boolean darkTriggered;
    public boolean holy(){return com.mcmagic.omnira.spell.HolyMagic.enabled(payload);}
    public void triggerCloud(Vec3 point) {
        if(darkTriggered || !(level() instanceof ServerLevel server))return;
        darkTriggered=true;
        com.mcmagic.omnira.spell.HolyMagic.cloud(server,ownerId==null?null:server.getEntity(ownerId),payload,power(),rangeMultiplier(),point,kind()==Kind.REVERSE_FLOW);
    }
    public void suppressCloud(){darkTriggered=true;}
    private static final EntityDataAccessor<Boolean> GHOST=SynchedEntityData.defineId(SpellEntity.class,EntityDataSerializers.BOOLEAN);
    private net.minecraft.core.BlockPos ghostTargetBlock;
    public boolean ghost() {return entityData.get(GHOST);}
    public void ghost(boolean value,net.minecraft.core.BlockPos target) {
        entityData.set(GHOST,value);ghostTargetBlock=target==null?null:target.immutable();
    }
    private UUID directTarget;
    private net.minecraft.core.BlockPos directBlock;
    public void explosiveImpact(boolean enabled) {explosiveImpact=enabled;}
    public boolean explosiveImpact() {return explosiveImpact;}
    private List<SpellEffect> effects = List.of();
    private UUID ownerId;
    private UUID followedId;
    private Vec3 anchor = Vec3.ZERO;
    private Vec3 homePoint;
    private net.minecraft.world.phys.BlockHitResult homeBlock;
    private List<Vec3> orbitPath = List.of();
    private int orbitIndex;
    private long lastHit = Long.MIN_VALUE;
    private Vec3 lerpPosition;
    private int lerpSteps;
    private UUID flightRider;
    private double flightStartY;
    private boolean flightAwarded;
    private UUID lastMirror;
    private int reflectionGrace;

    public boolean reflectFrom(DreamMirror mirror) {
        return reflectFrom(mirror,true);
    }
    public boolean reflectFrom(DreamMirror mirror,boolean returnDamage) {
        if(!(level() instanceof ServerLevel) || kind()!=Kind.PROJECTILE && kind()!=Kind.SELF_FLIGHT && kind()!=Kind.FLYING_BLOCK && kind()!=Kind.ORB) return false;
        if(reflectionGrace>0 && mirror.getUUID().equals(lastMirror)) return false;
        Entity owner=ownerId==null?null:((ServerLevel)level()).getEntity(ownerId);
        Vec3 reversed=getDeltaMovement().scale(-1);
        if(kind()==Kind.ORB&&reversed.lengthSqr()<1.0E-8)
            reversed=orbitPosition(age(0)).subtract(orbitPosition(age(0)+1));
        if(reversed.lengthSqr()<1.0E-8) return false;
        lastMirror=mirror.getUUID();reflectionGrace=8;
        if(returnDamage) {
            triggerCloud(mirror.position());
            if(holy())com.mcmagic.omnira.spell.HolyMagic.touch(owner,mirror);
            var source=level().damageSources().mobProjectile(this,owner instanceof LivingEntity living?living:null);
            for(var effect:effects) {
                // Reflection rejects physical damage, not the rest of the carried spell.
                // Apply before changing owner so locks, potion effects and hostility credit the caster.
                if(effect.utility()) UtilitySpellEffects.applyLiving(effect,this,owner,mirror,power(),false);
                else if(effect.physicalDamage(power())==0) effect.apply(this,owner,mirror,power());
                if(effect.utility() && !(owner instanceof ServerPlayer)) continue;
                mirror.returnPhysicalDamage(owner,source,effect.physicalDamage(power()*(holy()?1.5:1)));
            }
        }
        if(kind()!=Kind.SELF_FLIGHT) {
            boolean homing=homePoint!=null;
            ownerId=mirror.getUUID();followedId=homing && owner!=null?owner.getUUID():null;
            homePoint=homing && owner!=null?owner.getBoundingBox().getCenter():null;homeBlock=null;ghostTargetBlock=null;
            if(kind()==Kind.ORB) {
                entityData.set(KIND,Kind.PROJECTILE.ordinal());
                followedId=null;homePoint=null;orbitPath=List.of();
            }
        }
        setDeltaMovement(reversed);hasImpulse=true;hurtMarked=true;
        return true;
    }

    private boolean interceptMirror(Vec3 movement) {
        Vec3 from=getBoundingBox().getCenter(),to=from.add(movement);
        var wall=ghost()?ghostBlockHit(from,to):level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        if(ghost()) {
            var first=ProjectileUtil.getEntityHitResult(level(),this,from,wall.getLocation(),
                    getBoundingBox().expandTowards(movement).inflate(.3),this::canContact,.18F);
            return first!=null && first.getEntity() instanceof DreamMirror mirror && reflectFrom(mirror);
        }
        for(var mirror:level().getEntitiesOfClass(DreamMirror.class,
                getBoundingBox().expandTowards(movement).inflate(.1),e->e.isAlive() && !ownedBy(e) && !(kind()==Kind.ORB&&follows(e)))) {
            if(reflectionGrace>0 && mirror.getUUID().equals(lastMirror)) continue;
            var box=mirror.getBoundingBox().inflate(getBbWidth()/2,getBbHeight()/2,.18);
            var hit=box.clip(from,to);
            if((box.contains(from) || hit.isPresent()) && (wall.getType()==HitResult.Type.MISS
                    || hit.isPresent() && from.distanceToSqr(hit.get())<from.distanceToSqr(wall.getLocation())))
                return reflectFrom(mirror);
        }
        return false;
    }

    public SpellEntity(EntityType<? extends SpellEntity> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    public static SpellEntity spawn(ServerLevel level, Entity owner, Kind kind, Vec3 position) {
        SpellEntity spell = new SpellEntity(ModEntityTypes.SPELL.get(), level);
        spell.ownerId = owner.getUUID();
        spell.entityData.set(KIND, kind.ordinal());
        spell.entityData.set(BORN, level.getGameTime());
        spell.entityData.set(CHARGES, kind == Kind.WARD ? 4 : 1);
        spell.setPos(position);
        level.addFreshEntity(spell);
        return spell;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(GHOST,false);
        builder.define(COMPOSITE_DELAY,0);
        builder.define(COMPOSITE_STEP,0);
        builder.define(KNOWLEDGE_BLOOD,false);
        builder.define(BURST_RADIUS,-1F);
        builder.define(DURATION,400);
        builder.define(POWER,1F);
        builder.define(SPEED,1F);
        builder.define(RANGE,1F);
        builder.define(KIND, 0);
        builder.define(FACE, Direction.UP.get3DDataValue());
        builder.define(CHARGES, 1);
        builder.define(HITS, 0);
        builder.define(COLOR, 0xFFFFFF);
        builder.define(BORN, 0L);
        builder.define(TEMPORAL_AGE,0);
        builder.define(BLOCK, Blocks.STONE.defaultBlockState());
    }

    public void configure(double power,float baseDamage) {
        entityData.set(POWER,(float)power);
        entityData.set(DURATION,kind()==Kind.SELF_FLIGHT?400:Math.max(1,(int)Math.round((kind()==Kind.BURST?12:400)*power)));
        damage=(float)(baseDamage*power);
    }
    public void knowledgeShock(boolean blood,boolean leech) {
        knowledgeShock=true;knowledgeLeech=leech;entityData.set(KNOWLEDGE_BLOOD,blood);
    }
    public boolean knowledgeBlood(){return entityData.get(KNOWLEDGE_BLOOD);}
    public void configure(double power, SpellPayload payload) {
        this.payload=payload;
        configure(power,payload.damage());
        effects=payload.effects();
        if (kind() == Kind.SELF_FLIGHT) entityData.set(DURATION, flightDuration(payload));
        for(var effect:effects) if(effect.utility()) {setSpellColor(effect.operation().equals("construction")?0xA0D8F0:0xC39FE5);break;}
    }
    private static int flightDuration(SpellPayload payload) {
        int delays = (int) payload.keywords().stream().filter("delay"::equals).count();
        boolean unlimited = payload.keywords().contains("infusion");
        // Older crystals may have composed effects but no authored keywords.
        for (SpellEffect effect : payload.effects()) {
            int effectDelays = effect.utility() ? effect.delay()
                    : effect.duration() >= 800 ? 1 + (effect.duration() - 800) / 1200 : 0;
            delays = Math.max(delays, effectDelays);
            unlimited |= effect.infused() || effect.infusion() > 0;
        }
        return unlimited ? -1 : (int) Math.min(Integer.MAX_VALUE, 400L * (1L + delays));
    }
    public int duration() {return kind()==Kind.BURST && entityData.get(DURATION)==400?12:entityData.get(DURATION);}
    public float power() {return entityData.get(POWER);}
    public float speedMultiplier() {return entityData.get(SPEED);}
    public float rangeMultiplier() {return entityData.get(RANGE);}
    public void rangeMultiplier(double multiplier) {entityData.set(RANGE,(float)Math.clamp(multiplier,.1,3));}
    public void speedMultiplier(double multiplier) {entityData.set(SPEED,(float)Math.clamp(multiplier,1,11));}
    private double projectileSpeed() {return SpellCasting.PROJECTILE_SPEED*speedMultiplier();}
    public float opacity(float partialTick) {
        if(duration()<0)return 1;
        float fade=Math.clamp((duration()-age(partialTick))/(kind()==Kind.SELF_FLIGHT?60F:15F),0,1);
        return kind()==Kind.SELF_FLIGHT?fade*fade*(3-2*fade):fade;
    }
    public void applyEffects(Entity target) {
        if(!level().isClientSide&&kind()==Kind.SPATIAL_BLADE&&target instanceof SpellEntity)
            triggerCloud(target.position());
        if(target instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb){
            if(!level().isClientSide && ownerId!=null && ((ServerLevel)level()).getEntity(ownerId) instanceof Player player
                    && kind()!=Kind.BURST && !orb.isVehicle())orb.hurt(level().damageSources().playerAttack(player),1);
            else orb.impact();
            return;
        }
        var living=SpellCasting.livingTarget(target);
        if(level().isClientSide || living==null || !living.isAlive()) return;
        if(explosiveImpact) directTarget=living.getUUID();
        Entity owner=ownerId==null?null:((ServerLevel)level()).getEntity(ownerId);
        var cabin=com.mcmagic.omnira.vehicle.CruiseOrbProtection.cabin(target);
        if(cabin!=null && owner!=target){cabin.impact();return;}
        triggerCloud(target.position());
        if(holy())com.mcmagic.omnira.spell.HolyMagic.touch(owner,living);
        float before=living.getHealth();
        if(damage>0) target.hurt(SpellDamageSource.physical(living,this,owner,"spell"),damage*(holy()?1.5F:1));
        if(knowledgeShock && knowledgeBlood() && living.isAlive())
            living.hurt(SpellDamageSource.of(living,net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,this,owner,"spell"),2);
        if(knowledgeLeech && owner instanceof LivingEntity caster)
            caster.heal(Math.max(0,before-living.getHealth())*.5F);
        for(var effect:effects) effect.apply(this,owner,target,power());
    }
    public void applyBlockEffects(net.minecraft.world.phys.BlockHitResult hit) {
        if(!(level() instanceof ServerLevel server)) return;
        if(explosiveImpact) directBlock=hit.getBlockPos().immutable();
        triggerCloud(hit.getLocation());
        Entity owner=ownerId==null?null:server.getEntity(ownerId);
        for(var effect:effects) UtilitySpellEffects.applyBlock(effect,owner,hit.getBlockPos(),
                hit.getDirection(),hit.getBlockPos().relative(hit.getDirection()));
    }
    public void applyWardEffects(LivingEntity bearer,Entity attacker) {
        if(level().isClientSide) return;
        darkTriggered=false;triggerCloud(bearer.position());
        Entity owner=ownerId==null?null:((ServerLevel)level()).getEntity(ownerId);
        if(holy()) {
            com.mcmagic.omnira.spell.HolyMagic.touch(owner,bearer);
            if(attacker instanceof LivingEntity living)com.mcmagic.omnira.spell.HolyMagic.touch(owner,living);
        }
        if(damage>0 && attacker instanceof LivingEntity living)
            living.hurt(SpellDamageSource.physical(living,this,owner,"spell"),damage*(holy()?1.5F:1));
        for(var effect:effects) {
            Entity target=effect.healing()?bearer:attacker;
            if(target instanceof LivingEntity living && living.isAlive()) effect.apply(this,owner,living,power());
        }
    }
    public Kind kind() { return Kind.values()[entityData.get(KIND)]; }
    public Direction face() { return Direction.from3DDataValue(entityData.get(FACE)); }
    public int charges() { return entityData.get(CHARGES); }
    public int barrierHits() { return entityData.get(HITS); }
    public int spellColor() { return entityData.get(COLOR); }
    public void setSpellColor(int rgb) { entityData.set(COLOR, rgb & 0xFFFFFF); }
    public float age(float partialTick) { return level().getGameTime() - entityData.get(BORN) + entityData.get(TEMPORAL_AGE) + partialTick; }
    public BlockState blockAppearance() { return entityData.get(BLOCK); }
    public void blockAppearance(BlockState state) { entityData.set(BLOCK, state); }
    public void face(Direction direction) { entityData.set(FACE, direction.get3DDataValue()); setBoundingBox(makeBoundingBox()); }
    public boolean isPhysicalBarrier() { return kind() == Kind.BARRIER || kind() == Kind.FACE_BARRIER; }
    public boolean follows(Entity entity) { return entity.getUUID().equals(followedId); }
    public boolean ownedBy(Entity entity) { return entity.getUUID().equals(ownerId); }
    public void follow(Entity entity) {
        // Wards and orbit centers attach to the creature, homing retains the exact hitbox.
        if(kind()!=Kind.PROJECTILE && entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part) entity=part.getParent();
        followedId = entity == null ? null : entity.getUUID();
    }
    public void home(Entity entity, Vec3 point) { follow(entity); homePoint = point; }
    public void homeBlock(net.minecraft.world.phys.BlockHitResult hit) {homeBlock=hit;}

    public void orbit(Vec3 center, int index, List<Vec3> path) {
        anchor = center;
        orbitIndex = index;
        orbitPath = List.copyOf(path);
        setPos(orbitPosition(0));
    }

    private Vec3 orbitPosition(float age) {
        age-=entityData.get(TEMPORAL_AGE);
        double phase = age * speedMultiplier() / 100.0 + orbitIndex / (double)Math.round(4*rangeMultiplier());
        if (!orbitPath.isEmpty()) {
            double position = (phase % 1) * orbitPath.size();
            int index = (int) position;
            return orbitPath.get(index).lerp(orbitPath.get((index + 1) % orbitPath.size()), position - index);
        }
        double angle = phase * Math.PI * 2;
        double radius = 1.2 * rangeMultiplier();
        return anchor.add(Math.cos(angle) * radius, Math.sin(angle * 2) * .12, Math.sin(angle) * radius);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (lerpSteps > 0) {
                setPos(position().lerp(lerpPosition, 1.0 / lerpSteps--));
            }
            return;
        }
        if(com.mcmagic.omnira.time.FleetingTime.bonus(this)){
            if(compositeMotion!=null)return;
            entityData.set(TEMPORAL_AGE,entityData.get(TEMPORAL_AGE)+1);
            if(duration()>0&&age(0)>=duration())discard();
            if(reflectionGrace>0)reflectionGrace--;
            return;
        }
        if(compositeMotion!=null){compositeMotion.tick(this);return;}
        if (duration()>0 && age(0) >= duration()) { discard(); return; }
        ServerLevel level = (ServerLevel) level();
        if(reflectionGrace>0) reflectionGrace--;
        Entity followed = resolveFollowed(level);
        if (followedId != null && (followed == null || !followed.isAlive() || followed.isSpectator())) { discard(); return; }
        if(kind()==Kind.PROJECTILE || kind()==Kind.SELF_FLIGHT || kind()==Kind.FLYING_BLOCK) {
            if(kind()==Kind.PROJECTILE && homePoint!=null) {
                Vec3 destination=followed==null?homePoint:followed.getBoundingBox().getCenter();
                setDeltaMovement(destination.subtract(position()).normalize().scale(projectileSpeed()));
            }
            if(interceptMirror(getDeltaMovement())) return;
        }
        switch (kind()) {
            case BARRIER, FACE_BARRIER -> {
                if(charges()>0) {
                    var contacts=level.getEntities(this,getBoundingBox().inflate(.06),e->SpellCasting.validTarget(e)&&!e.getUUID().equals(ownerId));
                    if(!contacts.isEmpty()) {trigger(false);applyEffects(contacts.getFirst());}
                }
            }
            case WARD -> {
                if (followed == null) discard();
                else setPos(followed.getBoundingBox().getCenter());
            }
            case ORB -> {
                if (followed != null) anchor = followed.getBoundingBox().getCenter();
                Vec3 next = orbitPosition(age(0));
                setDeltaMovement(next.subtract(position()));
                if(interceptMirror(getDeltaMovement()))return;
                if (contactAlong(position(), next)) { trigger(true); return; }
                setPos(next);
            }
            case PROJECTILE -> {
                if (homePoint != null) {
                    Vec3 destination = followed == null ? homePoint : followed.getBoundingBox().getCenter();
                    if (position().distanceToSqr(destination) <= projectileSpeed() * projectileSpeed()) {
                        if(contactAlong(position(),destination)) {trigger(true);return;}
                        setPos(destination);
                        if(followed!=null) applyEffects(followed);else if(homeBlock!=null) applyBlockEffects(homeBlock);
                        trigger(true); return;
                    }
                    setDeltaMovement(destination.subtract(position()).normalize().scale(projectileSpeed()));
                }
                Vec3 next = position().add(getDeltaMovement());
                if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(next))) { discard(); return; }
                if (contactAlong(position(), next)) { trigger(true); return; }
                setPos(next);
            }
            case FLYING_BLOCK, SELF_FLIGHT -> tickFlight(level);
            case BURST -> { }
        }
    }

    private void tickFlight(ServerLevel level) {
        boolean self = kind() == Kind.SELF_FLIGHT;
        if (self && getPassengers().isEmpty()) { discard(); return; }
        Vec3 movement = getDeltaMovement();
        if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(position().add(movement)))) { discard(); return; }
        AABB previousBox = getBoundingBox();
        Vec3 previous=position();
        if(dissociates() && contactAlong(previous.add(0,.1,0),previous.add(movement).add(0,.1,0))) {trigger(true);return;}
        move(MoverType.SELF, movement);
        for (Entity passenger : getPassengers()) {
            passenger.fallDistance = 0;
            positionRider(passenger);
            if(self && !flightAwarded && passenger.getUUID().equals(flightRider)
                    && passenger.getY()-flightStartY>50 && passenger instanceof ServerPlayer player) {
                var advancement=level.getServer().getAdvancements().get(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("omnira","personal_spaceflight"));
                if(advancement!=null) {
                    player.getAdvancements().award(advancement,"ascend");
                    flightAwarded=true;
                }
            }
        }
        Entity contact = flightContact(previousBox, position().subtract(previous));
        if (horizontalCollision || verticalCollision || contact != null) {
            if (contact != null) applyEffects(contact);
            if(horizontalCollision || verticalCollision) {
                var block=level.clip(new ClipContext(previous.add(0,.1,0),previous.add(movement).add(0,.1,0),
                        ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
                if(block.getType()==HitResult.Type.BLOCK) applyBlockEffects(block);
                else {
                    var pos=movement.y<0?blockPosition().below():net.minecraft.core.BlockPos.containing(position().add(movement.normalize().scale(.55)));
                    if(!level.getBlockState(pos).isAir()) applyBlockEffects(new net.minecraft.world.phys.BlockHitResult(position(),movement.y<0?Direction.UP:Direction.DOWN,pos,false));
                }
            }
            trigger(true);
            return;
        }
        // Speed up the whole ballistic motion, rather than increasing the launch height.
        if (!self) setDeltaMovement(movement.add(0, -.03*speedMultiplier()*speedMultiplier(), 0));
    }

    private Entity resolveFollowed(ServerLevel level) {
        if (followedId == null) return null;
        Entity entity = level.getEntity(followedId);
        if (entity != null) return entity;
        for (var part : level.getPartEntities()) {
            if (part.getUUID().equals(followedId)) return part;
        }
        return null;
    }

    private Entity flightContact(AABB startBox, Vec3 movement) {
        // Broad-phase boxes include off-path corners. Test the swept volume along the actual,
        // collision-clipped displacement so walls stop hits on entities behind them.
        Vec3 from = startBox.getCenter();
        Vec3 to = from.add(movement);
        Entity nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (Entity entity : level().getEntities(this, startBox.expandTowards(movement), this::canContact)) {
            AABB target = entity.getBoundingBox().inflate(startBox.getXsize() / 2,
                    startBox.getYsize() / 2, startBox.getZsize() / 2);
            var hit = target.clip(from, to);
            if (!target.contains(from) && hit.isEmpty()) continue;
            double distance = target.contains(from) ? 0 : from.distanceToSqr(hit.get());
            if (distance < nearestDistance) {
                nearest = entity;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private boolean canContact(Entity entity) {
        if(entity instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb)
            return orb.getPassengers().stream().noneMatch(p->p.getUUID().equals(ownerId));
        // Homing projectiles must hit their selected entity; orbiting balls must not hit their center.
        var living=SpellCasting.livingTarget(entity);
        return !entity.getUUID().equals(ownerId)
                && !(reflectionGrace>0 && entity.getUUID().equals(lastMirror))
                && !(kind() == Kind.ORB && entity.getUUID().equals(followedId))
                && !(kind() == Kind.ORB && living!=null && living.getUUID().equals(followedId))
                && (SpellCasting.validTarget(entity) || ghost() && entity.isAlive() && entity.isPickable() && !entity.isSpectator()
                    || entity instanceof SpellEntity spell && spell.isPhysicalBarrier());
    }

    private net.minecraft.world.phys.BlockHitResult ghostBlockHit(Vec3 from,Vec3 to) {
        if(ghostTargetBlock!=null && level().hasChunkAt(ghostTargetBlock)) {
            var hit=level().getBlockState(ghostTargetBlock).getShape(level(),ghostTargetBlock).clip(from,to,ghostTargetBlock);
            if(hit!=null)return hit;
        }
        return net.minecraft.world.phys.BlockHitResult.miss(to,Direction.getNearest(to.x-from.x,to.y-from.y,to.z-from.z),net.minecraft.core.BlockPos.containing(to));
    }

    private boolean dissociates() {return effects.stream().anyMatch(e->e.operation().equals("dissociation"));}
    private boolean contactAlong(Vec3 from, Vec3 to) {
        var contacts=level().getEntities(this,new AABB(from,from).inflate(.18),this::canContact);
        if(!contacts.isEmpty()) {applyEffects(contacts.getFirst());return true;}
        HitResult block = ghost()?ghostBlockHit(from,to):level().clip(new ClipContext(from, to, dissociates()?ClipContext.Block.OUTLINE:ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        var entity = ProjectileUtil.getEntityHitResult(level(), this, from, block.getLocation(),
                getBoundingBox().expandTowards(to.subtract(from)).inflate(.3), this::canContact, .18F);
        if (entity != null) { setPos(entity.getLocation()); applyEffects(entity.getEntity()); return true; }
        if (block instanceof net.minecraft.world.phys.BlockHitResult blockHit && block.getType() != HitResult.Type.MISS) { setPos(block.getLocation()); applyBlockEffects(blockHit); return true; }
        return false;
    }

    public void trigger(boolean consume) {
        if (level().isClientSide || charges() <= 0) return;
        if(consume)triggerCloud(position());else darkTriggered=false;
        entityData.set(CHARGES, charges() - 1);
        if(consume && explosiveImpact && kind()==Kind.PROJECTILE) {
            explosiveImpact=false;
            var server=(ServerLevel)level();
            Entity owner=ownerId==null?null:server.getEntity(ownerId);
            SpellCasting.impactBurst(server,owner==null?this:owner,power(),payload,position(),directTarget,directBlock);
        }
        ((ServerLevel) level()).sendParticles(ParticleTypes.END_ROD, getX(), getY() + .15, getZ(), 10, .18, .18, .18, .025);
        if (consume || kind() == Kind.WARD && charges() == 0) discard();
    }

    @Override public boolean isPickable() { return isPhysicalBarrier() && !isRemoved(); }
    @Override public boolean canBeCollidedWith() { return isPhysicalBarrier() && !isRemoved(); }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isPushedByFluid() {return false;}
    @Override public boolean isPushedByFluid(net.neoforged.neoforge.fluids.FluidType type) {return false;}
    @Override public void onAboveBubbleCol(boolean downwards) {}
    @Override public void onInsideBubbleColumn(boolean downwards) {}
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || !isPhysicalBarrier() || isRemoved() || amount <= 0
                || !(source.getEntity() instanceof LivingEntity attacker) || !attacker.isAlive() || attacker.isSpectator()) return false;
        long now = level().getGameTime();
        if (lastHit != Long.MIN_VALUE && now - lastHit < 5) return false;
        lastHit = now;
        entityData.set(HITS, barrierHits() + 1);
        ((ServerLevel)level()).sendParticles(ParticleTypes.END_ROD, getX(), getY()+.5, getZ(), 6, .3, .4, .3, .02);
        if (barrierHits() >= 8) discard();
        return true;
    }
    @Override public boolean shouldRiderSit() { return false; }
    @Override protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        if(kind()==Kind.SELF_FLIGHT) {
            flightRider=passenger.getUUID();flightStartY=passenger.getY();flightAwarded=false;
        }
    }
    @Override protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if(!level().isClientSide&&kind()==Kind.SELF_FLIGHT&&passenger instanceof ServerPlayer player&&player.isAlive()) {
            player.fallDistance=0;
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,40,0));
        }
        if(passenger.getUUID().equals(flightRider)) {flightRider=null;flightAwarded=false;}
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger) { return position(); }
    @Override protected void positionRider(Entity passenger, Entity.MoveFunction callback) { callback.accept(passenger, getX(), getY(), getZ()); }
    @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger) { return position(); }

    @Override
    protected AABB makeBoundingBox() {
        if (entityData == null) return super.makeBoundingBox();
        return switch (kind()) {
            case BARRIER -> new AABB(getX() - .48, getY(), getZ() - .48, getX() + .48, getY() + 2, getZ() + .48);
            case FACE_BARRIER -> {
                double x = face().getAxis() == Direction.Axis.X ? .06 : .5;
                double y = face().getAxis() == Direction.Axis.Y ? .06 : .5;
                double z = face().getAxis() == Direction.Axis.Z ? .06 : .5;
                yield new AABB(getX() - x, getY() - y, getZ() - z, getX() + x, getY() + y, getZ() + z);
            }
            case FLYING_BLOCK -> new AABB(getX() - .49, getY(), getZ() - .49, getX() + .49, getY() + .98, getZ() + .49);
            case SELF_FLIGHT -> new AABB(getX() - .3, getY(), getZ() - .3, getX() + .3, getY() + 1.8, getZ() + .3);
            default -> new AABB(getX() - .18, getY() - .18, getZ() - .18, getX() + .18, getY() + .18, getZ() + .18);
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (KIND.equals(accessor) || FACE.equals(accessor)) setBoundingBox(makeBoundingBox());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps) {
        lerpPosition = new Vec3(x, y, z);
        lerpSteps = Math.max(1, steps);
    }

    // Temporary spell entities are deliberately not saved with chunks.
    @Override protected void readAdditionalSaveData(CompoundTag tag) {}
    @Override protected void addAdditionalSaveData(CompoundTag tag) {}
}
