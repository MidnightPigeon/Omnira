package com.mcmagic.omnira.spell.entity;

import com.mcmagic.omnira.registry.ModEffects;
import com.mcmagic.omnira.spell.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.*;
import java.util.*;

/** Swept contact volumes: once per contraction or blade, with no air placements. */
public final class CompositeSpellMotion {
    public static final int CONTRACTION_TICKS=20;
    public static final double BLADE_DISTANCE=128, BLADE_SPEED=1, BLADE_HALF_WIDTH=1, BLADE_HALF_HEIGHT=.125;
    private final Player owner;
    private final SpellPayload payload;
    private final Set<UUID> touched=new HashSet<>();
    private final Set<BlockPos> blocks=new HashSet<>();
    private final Vec3 direction;
    private double traveled;
    private int phase=-1,step;

    private CompositeSpellMotion(Player owner,SpellPayload payload,Vec3 direction){this.owner=owner;this.payload=payload;this.direction=direction;}
    public static boolean cast(Player caster,SpellPattern pattern,SpellPayload payload,double power,SpellCasting.Modifiers modifiers){
        var level=(ServerLevel)caster.level();
        if(pattern.targetElement()==ElementType.TIME){
            var spell=create(level,caster,payload,power,modifiers,SpellEntity.Kind.REVERSE_FLOW,0);
            spell.burstRadius(SpellCasting.BURST_RADIUS*1.5*modifiers.range());
            spell.composite(new CompositeSpellMotion(caster,payload,Vec3.ZERO),CONTRACTION_TICKS+1,0);
            caster.addEffect(new MobEffectInstance(ModEffects.TEMPORAL_DISLOCATION,200,enhanced(payload)?1:0));
        }else{
            int delay=0;
            var spell=create(level,caster,payload,power,modifiers,SpellEntity.Kind.SPATIAL_BLADE,delay);
            spell.setDeltaMovement(caster.getLookAngle().scale(BLADE_SPEED*modifiers.speed()));
            spell.setYRot(caster.getYRot());spell.setXRot(caster.getXRot());
            spell.composite(new CompositeSpellMotion(caster,payload,caster.getLookAngle()),
                    delay+(int)Math.ceil(BLADE_DISTANCE/(BLADE_SPEED*modifiers.speed()))+2,delay);
        }
        return true;
    }
    private static SpellEntity create(ServerLevel level,Player caster,SpellPayload payload,double power,SpellCasting.Modifiers modifiers,SpellEntity.Kind kind,int delay){
        var spell=SpellEntity.spawn(level,caster,kind,caster.getEyePosition());spell.configure(power,payload);
        spell.rangeMultiplier(modifiers.range());spell.speedMultiplier(modifiers.speed());
        spell.setSpellColor(kind==SpellEntity.Kind.REVERSE_FLOW?0xA6EDBA:0x94ACF5);
        return spell;
    }
    public static boolean enhanced(SpellPayload payload){return payload.keywords().contains("enhancement")||payload.effects().stream().anyMatch(e->e.enhancement()>0);}
    public static boolean infused(SpellPayload payload){return payload.keywords().contains("infusion")||payload.effects().stream().anyMatch(e->e.infused()||e.infusion()>0);}
    public static int percentDamage(LivingEntity target,boolean infusion){
        boolean boss=com.mcmagic.omnira.spell.ConstructionPrison.boss(target);
        return Math.max(2,(int)Math.ceil(target.getHealth()*(boss?(infusion?.1:.05):(infusion?.5:.2))));
    }
    public static boolean shellTouches(AABB box,Vec3 center,double inner,double outer){
        double x=Math.clamp(center.x,box.minX,box.maxX),y=Math.clamp(center.y,box.minY,box.maxY),z=Math.clamp(center.z,box.minZ,box.maxZ);
        double farX=Math.max(Math.abs(box.minX-center.x),Math.abs(box.maxX-center.x));
        double farY=Math.max(Math.abs(box.minY-center.y),Math.abs(box.maxY-center.y));
        double farZ=Math.max(Math.abs(box.minZ-center.z),Math.abs(box.maxZ-center.z));
        return center.distanceToSqr(new Vec3(x,y,z))<=outer*outer && farX*farX+farY*farY+farZ*farZ>=inner*inner;
    }
    public void tick(SpellEntity spell){
        if(!owner.isAlive()||owner.level()!=spell.level()){spell.discard();return;}
        if(spell.kind()==SpellEntity.Kind.REVERSE_FLOW)reverse(spell);else blade(spell);
        spell.compositeStep(step);
    }
    private void reverse(SpellEntity spell){
        int age=step++,nextPhase=age/CONTRACTION_TICKS;
        if(nextPhase>=1){spell.discard();return;}
        if(nextPhase!=phase){phase=nextPhase;touched.clear();blocks.clear();}
        Vec3 center=owner.getBoundingBox().getCenter();spell.setPos(center);
        double radius=spell.burstRadius(),outer=radius*(1-(age%CONTRACTION_TICKS)/(double)CONTRACTION_TICKS);
        double inner=radius*(1-((age%CONTRACTION_TICKS)+1)/(double)CONTRACTION_TICKS);
        var level=(ServerLevel)spell.level();
        for(var entity:level.getEntities(spell,new AABB(center,center).inflate(radius),e->e!=owner&&!(e instanceof SpellEntity)&&!e.isSpectator())){
            if(shellTouches(entity.getBoundingBox(),center,inner,outer)&&touched.add(entity.getUUID())){
                spell.applyEffects(entity);
                var living=SpellCasting.livingTarget(entity);
                if(living!=null)living.addEffect(new MobEffectInstance(MobEffects.WITHER,100,enhanced(payload)?2:1),owner);
            }
            if(touched.contains(entity.getUUID())){
                entity.setDeltaMovement(entity.getDeltaMovement().scale(.5).add(center.subtract(entity.getBoundingBox().getCenter()).normalize().scale(.35)));
                entity.hasImpulse=true;entity.hurtMarked=true;
            }
        }
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(center.add(-outer,-outer,-outer)),BlockPos.containing(center.add(outer,outer,outer)))){
            if(!level.hasChunkAt(pos)||blocks.contains(pos))continue;
            var state=level.getBlockState(pos);if(state.isAir())continue;
            var shape=state.getShape(level,pos);
            boolean contact=shape.isEmpty()?shellTouches(new AABB(pos),center,inner,outer):
                    shape.toAabbs().stream().anyMatch(box->shellTouches(box.move(pos),center,inner,outer));
            if(contact){
                blocks.add(pos.immutable());
                Direction face=Direction.getNearest(center.x-pos.getX()-.5,center.y-pos.getY()-.5,center.z-pos.getZ()-.5);
                spell.applyBlockEffects(new BlockHitResult(Vec3.atCenterOf(pos),face,pos.immutable(),false));
            }
        }
    }
    private void blade(SpellEntity spell){
        if(step++<=spell.compositeDelay()){
            spell.setPos(owner.getEyePosition().add(direction.scale(.8)));
            return;
        }
        double distance=Math.min(BLADE_DISTANCE-traveled,BLADE_SPEED*spell.speedMultiplier());
        Vec3 from=spell.position(),to=from.add(direction.scale(distance));
        var level=(ServerLevel)spell.level();
        if(!level.hasChunkAt(BlockPos.containing(to))){spell.discard();return;}
        Vec3 right=direction.cross(new Vec3(0,1,0));
        if(right.lengthSqr()<.001)right=new Vec3(1,0,0);else right=right.normalize();
        Vec3 up=right.cross(direction).normalize();
        double width=BLADE_HALF_WIDTH*spell.rangeMultiplier(),height=BLADE_HALF_HEIGHT*spell.rangeMultiplier();
        var candidates=level.getEntities(spell,new AABB(from,to).inflate(width+height),e->!e.isSpectator());
        for(var entity:candidates){
            var living=SpellCasting.livingTarget(entity);UUID id=living==null?entity.getUUID():living.getUUID();
            if(touched.contains(id)||!bladeTouches(entity.getBoundingBox(),from,to,right,up,width,height))continue;
            touched.add(id);
            int damage=living==null?0:percentDamage(living,infused(payload));
            if(living!=null&&living.isAlive()&&!(living instanceof Player p&&(p.isCreative()||p.isSpectator()))){
                living.setHealth(Math.max(0,living.getHealth()-damage));
                if(living.getHealth()<=0)living.die(SpellDamageSource.of(living,net.minecraft.world.damagesource.DamageTypes.GENERIC_KILL,spell,owner,"spell"));
            }
            if(living!=null&&living.isAlive()){
                int protection=living.invulnerableTime;
                living.invulnerableTime=0;
                try{spell.applyEffects(entity);}finally{living.invulnerableTime=Math.max(protection,living.invulnerableTime);}
            }else if(living==null)spell.applyEffects(entity);
            if(entity instanceof SpellEntity other){
                other.hurt(new net.minecraft.world.damagesource.DamageSource(level.damageSources().magic().typeHolder(),spell,owner),1);
            }
        }
        spell.setPos(to);traveled+=distance;
        if(traveled>=BLADE_DISTANCE)spell.discard();
    }
    /** A blade is a swept thin rectangle, not the axis-aligned query box. */
    public static boolean bladeTouches(AABB box,Vec3 from,Vec3 to,Vec3 right,Vec3 up,double width,double height){
        Vec3 half=new Vec3((box.maxX-box.minX)/2,(box.maxY-box.minY)/2,(box.maxZ-box.minZ)/2);
        Vec3 delta=box.getCenter().subtract(from.lerp(to,.5)),forward=to.subtract(from).normalize();
        double length=from.distanceTo(to)/2;
        Vec3[] world={new Vec3(1,0,0),new Vec3(0,1,0),new Vec3(0,0,1)},local={right,up,forward};
        var axes=new ArrayList<Vec3>(List.of(world));axes.addAll(List.of(local));
        for(var a:world)for(var b:local)axes.add(a.cross(b));
        for(var axis:axes){
            if(axis.lengthSqr()<1e-12)continue;
            double boxRadius=Math.abs(axis.x)*half.x+Math.abs(axis.y)*half.y+Math.abs(axis.z)*half.z;
            double bladeRadius=Math.abs(axis.dot(right))*width+Math.abs(axis.dot(up))*height+Math.abs(axis.dot(forward))*length;
            if(Math.abs(delta.dot(axis))>boxRadius+bladeRadius+1e-9)return false;
        }
        return true;
    }
}
