package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.forging.*;
import com.mcmagic.omnira.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class SpacetimeStorm {
    public static final double RADIUS=10;
    private SpacetimeStorm(){}
    public static boolean intersects(Vec3 center,AABB box){
        double x=Math.clamp(center.x,box.minX,box.maxX),y=Math.clamp(center.y,box.minY,box.maxY),z=Math.clamp(center.z,box.minZ,box.maxZ);
        return center.distanceToSqr(new Vec3(x,y,z))<=RADIUS*RADIUS;
    }
    public static void affect(ServerLevel level,Entity entity){
        if(entity instanceof LivingEntity living && !(entity instanceof ArmorStand)){
            // Deliberately outside hurt(): armor, absorption, shields and damage events cannot reduce it.
            if(!living.isAlive())return;
            if(living.getHealth()>40){living.setHealth(living.getHealth()-40);return;}
            if(living instanceof net.minecraft.world.entity.player.Player player){
                // trigger() iterates a pre-death snapshot, so this survives its creating storm only.
                TemporalAmber.sealDeath(player);
            }else{
                living.setHealth(0);living.discard();
            }
        }else entity.discard();
    }
    public static void trigger(ServerLevel level,Vec3 center){
        var area=new AABB(center,center).inflate(RADIUS);
        var targets=level.getEntities((Entity)null,area,e->intersects(center,e.getBoundingBox()));
        // Damage occupants before deleting their vehicle; never delete living passengers with it.
        targets.stream().filter(e->e instanceof LivingEntity && !(e instanceof ArmorStand)).forEach(e->affect(level,e));
        targets.stream().filter(e->!(e instanceof LivingEntity) || e instanceof ArmorStand).forEach(e->{
            if(net.neoforged.fml.ModList.get().isLoaded("create") && com.mcmagic.omnira.compat.CreateStorm.erase(e,center))return;
            affect(level,e);
        });
        if(net.neoforged.fml.ModList.get().isLoaded("sable"))com.mcmagic.omnira.compat.SableStorm.erase(level,center);
        var masters=new java.util.HashSet<BlockPos>();
        for(var pos:BlockPos.betweenClosed(BlockPos.containing(center).offset(-10,-10,-10),BlockPos.containing(center).offset(10,10,10))){
            if(!level.hasChunkAt(pos) || !intersects(center,new AABB(pos)))continue;
            var state=level.getBlockState(pos);
            if(state.is(ModBlocks.ADVANCED_FORGE.get()))masters.add(ForgeLayout.master(pos,state));
        }
        for(var pos:masters){
            var state=level.getBlockState(pos);if(!state.is(ModBlocks.ADVANCED_FORGE.get()))continue;
            var facing=state.getValue(AdvancedForgeBlock.FACING);
            for(int i=0;i<18;i++)level.removeBlockEntity(pos.offset(ForgeLayout.offset(facing,i)));
            for(int i=0;i<18;i++){
                var part=pos.offset(ForgeLayout.offset(facing,i));
                if(level.getBlockState(part).is(ModBlocks.ADVANCED_FORGE.get()))level.setBlock(part,Blocks.AIR.defaultBlockState(),18);
            }
        }
        for(int i=0;i<240;i++){
            double y=1-2*(i+.5)/240,angle=i*2.399963229728653,r=Math.sqrt(1-y*y)*RADIUS;
            var color=i%2==0?new org.joml.Vector3f(.12F,.08F,.2F):new org.joml.Vector3f(.85F,.85F,1);
            level.sendParticles(new DustParticleOptions(color,2),center.x+r*Math.cos(angle),center.y+y*RADIUS,center.z+r*Math.sin(angle),1,.08,.08,.08,0);
        }
        level.playSound(null,center.x,center.y,center.z,net.minecraft.sounds.SoundEvents.END_PORTAL_SPAWN,net.minecraft.sounds.SoundSource.BLOCKS,1.5F,.65F);
    }
    public static void disableDamagedForge(ServerLevel level,BlockPos part){
        var state=level.getBlockState(part);
        if(state.is(ModBlocks.ADVANCED_FORGE.get()) && level.getBlockEntity(ForgeLayout.master(part,state)) instanceof AdvancedForgeBlockEntity forge)forge.markStormDamaged();
    }
}
