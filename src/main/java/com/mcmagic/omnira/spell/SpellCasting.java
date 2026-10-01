package com.mcmagic.omnira.spell;

import com.mcmagic.omnira.spell.entity.SpellEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SpellCasting {
    public static final int LIFETIME = 400;
    public static final double PROJECTILE_SPEED = 0.30;
    public static final double BURST_RADIUS = 3;
    private static final double IMPACT_BURST_RADIUS_BONUS = .2;
    public static final int[][] GROUND_OFFSETS = {{0,-2},{1,-1},{2,0},{1,1},{0,2},{-1,1},{-2,0},{-1,-1}};

    private SpellCasting() {}

    public record GroundPoint(BlockPos block, Vec3 surface) {}
    public record Modifiers(double speed, double range) {
        public static final Modifiers NONE = new Modifiers(1,1);
        public Modifiers {
            speed=Math.clamp(speed,1,11);
            range=Math.clamp(range,1,3);
        }
    }

    public static boolean validTarget(Entity entity) {
        var living=livingTarget(entity);
        return living!=null && living.isAlive() && !living.isSpectator() && entity.isPickable();
    }

    public static LivingEntity livingTarget(Entity entity) {
        if(entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part) entity=part.getParent();
        return entity instanceof LivingEntity living?living:null;
    }

    public static List<GroundPoint> groundPoints(ServerLevel level, Vec3 feet) {
        return groundPoints(level, feet, 2);
    }

    public static List<GroundPoint> groundPoints(ServerLevel level, Vec3 feet, int outerRadius) {
        List<GroundPoint> result = new ArrayList<>();
        BlockPos origin = BlockPos.containing(feet);
        var offsets = new ArrayList<int[]>(List.of(GROUND_OFFSETS));
        // Keep the original eight points; additional Manhattan rings use the same winding.
        for (int radius=3; radius<=Math.clamp(outerRadius,2,6); radius++) {
            for (int quarter=0; quarter<4; quarter++) for (int step=0; step<radius; step++) {
                int x=step,z=step-radius;
                for (int turn=0; turn<quarter; turn++) {int previous=x;x=-z;z=previous;}
                offsets.add(new int[]{x,z});
            }
        }
        for (int[] offset : offsets) {
            BlockPos start = origin.offset(offset[0], 0, offset[1]);
            if (!level.hasChunkAt(start)) continue;
            boolean down = level.getBlockState(start).getCollisionShape(level, start).isEmpty();
            // The top face of the block one below the boundary can be exactly twelve blocks away.
            for (int step = 0; step <= (down ? 13 : 12); step++) {
                BlockPos pos = start.offset(0, down ? -step : step, 0);
                if (level.isOutsideBuildHeight(pos)) break;
                VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
                if (shape.isEmpty()) continue;
                // Sample the center of the column, so slabs and stairs use their real surface.
                BlockHitResult top = shape.clip(new Vec3(pos.getX() + .5, pos.getY() + 1.01, pos.getZ() + .5),
                        new Vec3(pos.getX() + .5, pos.getY() - .01, pos.getZ() + .5), pos);
                if (top == null) continue;
                Vec3 surface = top.getLocation();
                AABB clearance = new AABB(surface.x - .05, surface.y + .01, surface.z - .05,
                        surface.x + .05, surface.y + .1, surface.z + .05);
                if (Math.abs(surface.y - feet.y) <= 12 && level.noCollision(clearance)) {
                    result.add(new GroundPoint(pos.immutable(), surface));
                    break;
                }
            }
        }
        return result;
    }

    public static HitResult target(Player caster,double range,boolean entities) {return target(caster,range,entities,false);}
    public static HitResult target(Player caster, double range, boolean entities,boolean outline) {
        Vec3 start = caster.getEyePosition();
        Vec3 end = start.add(caster.getLookAngle().scale(range));
        BlockHitResult block = caster.level().clip(new ClipContext(start, end, outline?ClipContext.Block.OUTLINE:ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (entities) {
            EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, start, block.getLocation(),
                    caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                    e -> e != caster && validTarget(e), start.distanceToSqr(block.getLocation()));
            if (hit != null) return hit;
        }
        return block;
    }

    public static boolean cast(Player caster, SpellPattern pattern) {
        return cast(caster,pattern,SpellPayload.EMPTY,1);
    }

    public static boolean cast(Player caster, SpellPattern pattern,SpellPayload payload,double power) {
        return cast(caster,pattern,payload,power,Modifiers.NONE);
    }

    public static boolean cast(Player caster,SpellPattern pattern,SpellPayload payload,double power,Modifiers modifiers) {
        return cast(caster,pattern,payload,power,1,1,false,false,modifiers);
    }

    public static boolean castArquebus(Player caster,SpellPattern pattern,SpellPayload payload,double power,boolean explosive) {
        if(pattern==null || pattern.shapeKeyword()!=SpellShapeKeyword.PROJECTILE
                || pattern.targetKeyword()!=SpellTargetKeyword.TARGET && pattern.targetKeyword()!=SpellTargetKeyword.AIM) return false;
        return cast(caster,pattern,payload,power,4,2,explosive,false,Modifiers.NONE);
    }

    public static boolean castArquebus(Player caster,SpellPattern pattern,SpellPayload payload,double power,
                                      com.mcmagic.omnira.item.ArquebusPlugin plugin) {
        if(pattern==null || pattern.shapeKeyword()!=SpellShapeKeyword.PROJECTILE
                || pattern.targetKeyword()!=SpellTargetKeyword.TARGET && pattern.targetKeyword()!=SpellTargetKeyword.AIM) return false;
        return cast(caster,pattern,payload,power,plugin.selectionMultiplier,plugin.speedMultiplier,
                plugin==com.mcmagic.omnira.item.ArquebusPlugin.ANCESTOR_LAUNCHER,
                plugin==com.mcmagic.omnira.item.ArquebusPlugin.KINGS_NEW_CLOTHES,Modifiers.NONE);
    }

    private static HitResult ghostTarget(Player caster,double range,boolean entities) {
        Vec3 start=caster.getEyePosition(),end=start.add(caster.getLookAngle().scale(range));
        if(entities) {
            var hit=ProjectileUtil.getEntityHitResult(caster,start,end,
                    caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
                    e->e!=caster && validTarget(e),range*range);
            if(hit!=null)return hit;
        }
        return caster.level().clip(new ClipContext(start,end,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,caster));
    }

    private static boolean cast(Player caster,SpellPattern pattern,SpellPayload payload,double power,
                                double selectionBonus,double speedBonus,boolean explosive,boolean ghost,Modifiers modifiers) {
        if (pattern==null || !(caster.level() instanceof ServerLevel level) || !caster.isAlive() || caster.isSpectator()) return false;
        if(pattern.composite())return com.mcmagic.omnira.spell.entity.CompositeSpellMotion.cast(caster,pattern,payload,
                com.mcmagic.omnira.fate.FateEffects.spellPower(caster,power),modifiers);
        SpellShapeKeyword shape = pattern.shapeKeyword();
        SpellTargetKeyword target = pattern.targetKeyword();
        if (shape == null) return castDirect(caster,level,target,payload,power,modifiers);
        double range = modifiers.range();
        List<GroundPoint> ground = target == SpellTargetKeyword.GROUND
                ? groundPoints(level, caster.position(), (int)Math.round(2*range)) : List.of();
        if (target == SpellTargetKeyword.GROUND && ground.isEmpty()) return false;
        double selectionScale = range > 1 && (target == SpellTargetKeyword.TARGET || target == SpellTargetKeyword.AIM) ? 2 : 1;
        double selectionRange = (target == SpellTargetKeyword.TARGET
                ? SpellTargetingRule.TARGET_MAX_RANGE : SpellTargetingRule.AIM_MAX_RANGE) * selectionScale * selectionBonus;
        HitResult hit = ghost?ghostTarget(caster,selectionRange,target==SpellTargetKeyword.TARGET):target(caster, selectionRange, target == SpellTargetKeyword.TARGET,
                payload.effects().stream().anyMatch(e->e.operation().equals("dissociation")));
        if (target == SpellTargetKeyword.TARGET && hit.getType() == HitResult.Type.MISS) return false;
        if(shape==SpellShapeKeyword.PROJECTILE && target==SpellTargetKeyword.SELF && caster.isPassenger())return false;
        Entity attached = target == SpellTargetKeyword.SELF ? caster : hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        Vec3 center = attached != null ? attached.getBoundingBox().getCenter() : hit.getLocation();
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK && attached == null) {
            center = center.add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.02));
        }
        if(shape!=SpellShapeKeyword.PROJECTILE && shape!=SpellShapeKeyword.ORBIT && shape!=SpellShapeKeyword.BURST && attached==null) {
            if(target==SpellTargetKeyword.GROUND) {
                for(var point:ground) for(var effect:payload.effects())
                    UtilitySpellEffects.applyBlock(effect,caster,point.block(),Direction.UP,point.block().above());
            } else if(hit instanceof BlockHitResult block && hit.getType()==HitResult.Type.BLOCK) {
                for(var effect:payload.effects()) UtilitySpellEffects.applyBlock(effect,caster,block.getBlockPos(),
                        block.getDirection(),block.getBlockPos().relative(block.getDirection()));
            } else {
                for(var effect:payload.effects()) UtilitySpellEffects.applyBlock(effect,caster,BlockPos.containing(center),Direction.UP,BlockPos.containing(center));
            }
        }
        power=com.mcmagic.omnira.fate.FateEffects.spellPower(caster,power);
        switch (shape) {
            case BARRIER -> {
                if (attached != null && target != SpellTargetKeyword.AIM && target != SpellTargetKeyword.GROUND) {
                    spawn(level, caster, power, payload, SpellEntity.Kind.WARD, center,modifiers).follow(attached);
                } else if (target == SpellTargetKeyword.GROUND) {
                    for (GroundPoint point : ground) spawn(level, caster, power, payload, SpellEntity.Kind.BARRIER, point.surface(),modifiers);
                } else if (target == SpellTargetKeyword.TARGET && hit instanceof BlockHitResult block) {
                    int count = 0;
                    for (Direction face : Direction.values()) {
                        BlockPos outside = block.getBlockPos().relative(face);
                        if (!level.hasChunkAt(outside) || !level.getBlockState(outside).getCollisionShape(level, outside).isEmpty()) continue;
                        Vec3 faceCenter = Vec3.atCenterOf(block.getBlockPos()).add(Vec3.atLowerCornerOf(face.getNormal()).scale(.56));
                        spawn(level, caster, power, payload, SpellEntity.Kind.FACE_BARRIER, faceCenter,modifiers).face(face);
                        count++;
                    }
                    if (count == 0) return false;
                } else {
                    spawn(level, caster, power, payload, SpellEntity.Kind.BARRIER, center,modifiers);
                }
            }
            case ORBIT -> {
                BlockPos origin = caster.blockPosition();
                int outerRing = ground.stream().mapToInt(p -> Math.abs(p.block().getX()-origin.getX())
                        + Math.abs(p.block().getZ()-origin.getZ())).max().orElse(0);
                List<Vec3> path = ground.stream().filter(p -> Math.abs(p.block().getX()-origin.getX())
                        + Math.abs(p.block().getZ()-origin.getZ())==outerRing)
                        .map(p -> p.surface().add(0, .65, 0)).toList();
                if (target == SpellTargetKeyword.TARGET && hit instanceof BlockHitResult block) center = Vec3.atCenterOf(block.getBlockPos());
                for (int i = 0; i < Math.round(4*range); i++) {
                    SpellEntity orb = spawn(level, caster, power, payload, SpellEntity.Kind.ORB, center,modifiers);
                    orb.orbit(center, i, path);
                    if (target == SpellTargetKeyword.SELF || target == SpellTargetKeyword.TARGET) orb.follow(attached);
                }
            }
            case BURST -> {
                if(target==SpellTargetKeyword.SELF && caster.getVehicle() instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb)orb.selfBurst();
                if (target == SpellTargetKeyword.GROUND) {
                    for (GroundPoint point : ground) burst(level, caster, power, payload, point.surface().add(0, .1, 0), range,null,null);
                } else burst(level, caster, power, payload, center,range,null,null);
            }
            case PROJECTILE -> {
                if (target == SpellTargetKeyword.SELF) {
                    if (caster.isPassenger()) return false;
                    SpellEntity flight = spawn(level, caster, power, payload, SpellEntity.Kind.SELF_FLIGHT, caster.position(),modifiers);
                    if (!caster.startRiding(flight, true)) { flight.discard(); return false; }
                    flight.setDeltaMovement(caster.getLookAngle().scale(PROJECTILE_SPEED*flight.speedMultiplier()));
                } else if (target == SpellTargetKeyword.GROUND) {
                    for (GroundPoint point : ground) {
                        SpellEntity block = spawn(level, caster, power, payload, SpellEntity.Kind.FLYING_BLOCK, point.surface().add(0, .02, 0),modifiers);
                        block.blockAppearance(level.getBlockState(point.block()));
                        block.setDeltaMovement(0, .76*block.speedMultiplier(), 0);
                    }
                } else {
                    // Start at the eye, not beyond a nearby wall.
                    SpellEntity projectile = spawn(level, caster, power, payload, SpellEntity.Kind.PROJECTILE, caster.getEyePosition(),modifiers);
                    projectile.speedMultiplier(projectile.speedMultiplier()*speedBonus);
                    projectile.explosiveImpact(explosive);
                    projectile.ghost(ghost,hit instanceof BlockHitResult b && hit.getType()==HitResult.Type.BLOCK?b.getBlockPos():null);
                    projectile.setDeltaMovement(caster.getLookAngle().scale(PROJECTILE_SPEED*projectile.speedMultiplier()));
                    if (target == SpellTargetKeyword.TARGET) {
                        projectile.home(attached, center);
                        if(attached==null && hit instanceof BlockHitResult blockHit) projectile.homeBlock(blockHit);
                    }
                }
            }
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, .6F, 1.1F);
        return true;
    }

    private static boolean castDirect(Player caster,ServerLevel level,SpellTargetKeyword target,
                                      SpellPayload payload,double power,Modifiers modifiers) {
        double blockRange=caster.blockInteractionRange(),entityRange=caster.entityInteractionRange();
        List<GroundPoint> ground=target==SpellTargetKeyword.GROUND
                ?groundPoints(level,caster.position(),(int)Math.round(2*modifiers.range())):List.of();
        if(target==SpellTargetKeyword.GROUND && ground.isEmpty())return false;
        HitResult hit=target==SpellTargetKeyword.TARGET?target(caster,Math.max(blockRange,entityRange),true,
                payload.effects().stream().anyMatch(e->e.operation().equals("dissociation"))):null;
        if(target==SpellTargetKeyword.TARGET) {
            if(hit.getType()==HitResult.Type.MISS || hit.getLocation().distanceToSqr(caster.getEyePosition())>
                    Math.pow(hit instanceof EntityHitResult?entityRange:blockRange,2))return false;
        }
        if(target==SpellTargetKeyword.AIM) {
            var candidates=directRayTargets(caster,level,payload,blockRange,entityRange);
            if(candidates.isEmpty())return false;
            hit=candidates.get(level.random.nextInt(candidates.size()));
        }
        power=com.mcmagic.omnira.fate.FateEffects.spellPower(caster,power);
        SpellEntity effect=SpellEntity.spawn(level,caster,SpellEntity.Kind.BURST,caster.position());
        effect.configure(power,payload);
        effect.rangeMultiplier(modifiers.range());
        effect.suppressCloud();
        if(target==SpellTargetKeyword.GROUND) {
            var touched=new java.util.HashSet<java.util.UUID>();
            for(GroundPoint point:ground) {
                applyDirectBlock(caster,payload,power,modifiers.range(),point.block(),Direction.UP,point.block().above());
                for(var living:level.getEntitiesOfClass(LivingEntity.class,new AABB(point.surface().add(-.5,0,-.5),
                        point.surface().add(.5,2,.5)),SpellCasting::validTarget))
                    if(touched.add(living.getUUID()))effect.applyEffects(living);
            }
        } else if(target==SpellTargetKeyword.SELF) {
            HolyMagic.cloud(level,caster,payload,power,modifiers.range(),caster.position());
            effect.applyEffects(caster);
        } else if(hit instanceof EntityHitResult entityHit) {
            HolyMagic.cloud(level,caster,payload,power,modifiers.range(),entityHit.getLocation());
            effect.applyEffects(entityHit.getEntity());
        } else if(hit instanceof BlockHitResult block) applyDirectBlock(caster,payload,power,modifiers.range(),block.getBlockPos(),
                block.getDirection(),block.getBlockPos().relative(block.getDirection()));
        effect.discard();
        level.playSound(null,caster.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.6F,1.1F);
        return true;
    }

    private static void applyDirectBlock(Player caster,SpellPayload payload,double power,double range,
                                         BlockPos block,Direction face,BlockPos placement) {
        for(var effect:payload.effects())UtilitySpellEffects.applyBlock(effect,caster,block,face,placement);
        if(payload.effects().stream().anyMatch(SpellEffect::darkness))
            HolyMagic.cloud((ServerLevel)caster.level(),caster,payload,power,range,Vec3.atCenterOf(block));
    }

    private static List<HitResult> directRayTargets(Player caster,ServerLevel level,SpellPayload payload,
                                                     double blockRange,double entityRange) {
        Vec3 start=caster.getEyePosition(),direction=caster.getLookAngle();
        var hits=new ArrayList<HitResult>();
        boolean affectsBlocks=payload.effects().isEmpty() || payload.effects().stream().anyMatch(e->e.utility() || e.darkness());
        boolean affectsLiving=payload.effects().isEmpty() || payload.damage()>0
                || payload.effects().stream().anyMatch(e->!e.utility());
        if(affectsBlocks) {
            var visited=new java.util.HashSet<BlockPos>();
            for(double distance=0;distance<=blockRange;distance+=.125) {
                Vec3 point=start.add(direction.scale(distance));
                BlockPos pos=BlockPos.containing(point);
                if(!visited.add(pos) || !level.hasChunkAt(pos))continue;
                if(!level.getBlockState(pos).isAir())hits.add(new BlockHitResult(point,
                        Direction.getNearest(direction.x,direction.y,direction.z).getOpposite(),pos,false));
            }
        }
        if(affectsLiving) {
            Vec3 end=start.add(direction.scale(entityRange));
            for(Entity entity:level.getEntities(caster,new AABB(start,end).inflate(1),
                    e->e!=caster && validTarget(e))) {
                var crossing=entity.getBoundingBox().inflate(.2).clip(start,end);
                crossing.ifPresent(point->hits.add(new EntityHitResult(entity,point)));
            }
        }
        return hits;
    }

    public static void burst(ServerLevel level, Entity caster, Vec3 center, boolean excludeCaster) {
        burst(level,caster,1,SpellPayload.EMPTY,center,excludeCaster);
    }

    private static SpellEntity spawn(ServerLevel level,Entity caster,double power,SpellPayload payload,SpellEntity.Kind kind,Vec3 pos,Modifiers modifiers) {
        SpellEntity spell=SpellEntity.spawn(level,caster,kind,pos);
        spell.configure(power,payload);
        spell.speedMultiplier(modifiers.speed());
        spell.rangeMultiplier(modifiers.range());
        return spell;
    }

    public static void burst(ServerLevel level,Entity caster,double power,SpellPayload payload,Vec3 center,boolean excludeCaster) {
        burst(level,caster,power,payload,center,1,null,null);
    }

    public static void impactBurst(ServerLevel level,Entity caster,double power,SpellPayload payload,Vec3 center,
                                   java.util.UUID directTarget,BlockPos directBlock) {
        burst(level,caster,power,payload,center,.5,directTarget,directBlock,false);
    }

    private static void burst(ServerLevel level,Entity caster,double power,SpellPayload payload,Vec3 center,
                              double radiusScale,java.util.UUID directTarget,BlockPos directBlock) {
        burst(level,caster,power,payload,center,radiusScale,directTarget,directBlock,true);
    }
    private static void burst(ServerLevel level,Entity caster,double power,SpellPayload payload,Vec3 center,
                              double radiusScale,java.util.UUID directTarget,BlockPos directBlock,boolean authoredBurst) {
        SpellEntity burst=SpellEntity.spawn(level,caster,SpellEntity.Kind.BURST,center);
        burst.configure(power,payload);
        burst.rangeMultiplier(radiusScale);
        if(authoredBurst)burst.triggerCloud(center);else burst.suppressCloud();
        double radius=BURST_RADIUS*radiusScale+(authoredBurst?0:IMPACT_BURST_RADIUS_BONUS);
        for(var orb:level.getEntitiesOfClass(com.mcmagic.omnira.vehicle.CruiseOrbEntity.class,new AABB(center,center).inflate(radius))){
            var box=orb.getBoundingBox();
            double dx=Math.max(box.minX-center.x,Math.max(0,center.x-box.maxX));
            double dy=Math.max(box.minY-center.y,Math.max(0,center.y-box.maxY));
            double dz=Math.max(box.minZ-center.z,Math.max(0,center.z-box.maxZ));
            if(dx*dx+dy*dy+dz*dz<=radius*radius)orb.impact();
        }
        burst.burstRadius(radius);
        if(authoredBurst)level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, .7F, 1.3F);
        var affectedEntities=new java.util.HashSet<java.util.UUID>();
        for (Entity entity : level.getEntities(caster, new AABB(center, center).inflate(radius),
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof SpellEntity))) {
            if(entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon) continue;
            if(entity.getBoundingBox().getCenter().distanceToSqr(center)>=radius*radius) continue;
            var living=livingTarget(entity);
            if(living!=null && living.getUUID().equals(directTarget)) continue;
            if(living!=null && !affectedEntities.add(living.getUUID())) continue;
            pushBurst(level, entity, center, radius, burst);
        }
        // The legacy flag no longer exempts self bursts. Caster and bystanders obey the same sphere.
        if (validTarget(caster) && !caster.getUUID().equals(directTarget)) pushBurst(level, caster, center, radius, burst);
        // Snapshot the sphere so earlier edits cannot expand or change later target selection.
        List<BlockPos> affected=new ArrayList<>();
        if(payload.effects().stream().anyMatch(SpellEffect::utility)) {
            for(BlockPos pos:BlockPos.betweenClosed(BlockPos.containing(center.subtract(radius,radius,radius)),
                    BlockPos.containing(center.add(radius,radius,radius)))) {
                double dx=pos.getX()+.5-center.x,dy=pos.getY()+.5-center.y,dz=pos.getZ()+.5-center.z;
                if(dx*dx+dy*dy+dz*dz<=radius*radius && level.hasChunkAt(pos)
                        && !level.isOutsideBuildHeight(pos) && !pos.equals(directBlock)) affected.add(pos.immutable());
            }
            UtilitySpellEffects.applyBurstBlocks(payload.effects(),caster,affected);
        }
    }

    private static void pushBurst(ServerLevel level, Entity entity, Vec3 center,double radius,SpellEntity burst) {
        if(entity instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity)return;
        if(com.mcmagic.omnira.vehicle.CruiseOrbProtection.cabin(entity)!=null){burst.applyEffects(entity);return;}
        Vec3 difference = entity.getBoundingBox().getCenter().subtract(center);
        double distance = difference.length();
        if (distance >= radius) return;
        Vec3 direction = distance < .001 ? new Vec3(0, 1, 0) : difference.normalize();
        entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale((1 - distance / radius) * .9)));
        entity.hurtMarked = true;
        burst.applyEffects(entity);
    }
}
