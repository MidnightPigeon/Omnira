package com.mcmagic.omnira.space.sea;

import com.mojang.serialization.MapCodec;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class VoidFissureBlock extends Block {
    public static final IntegerProperty FLUID_LEVEL=IntegerProperty.create("fluid_level",0,16);
    private static final VoxelShape SHAPE=Block.box(3,1,3,13,15,13);
    private static final Map<Entity,Map<BlockPos,Long>> CONTACTS=new WeakHashMap<>();
    private static final Map<ServerLevel,Map<BlockPos,Set<UUID>>> PENDING=new WeakHashMap<>();
    public VoidFissureBlock(){super(Properties.of().strength(-1,3600000).noCollission().noOcclusion().noLootTable().lightLevel(s->7));registerDefaultState(stateDefinition.any().setValue(FLUID_LEVEL,16));}
    @Override protected MapCodec<? extends Block> codec(){return simpleCodec(p->new VoidFissureBlock());}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FLUID_LEVEL);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected FluidState getFluidState(BlockState s){return restored(s).getFluidState();}
    public static BlockState restored(BlockState s){int n=s.getValue(FLUID_LEVEL);return n==16?Blocks.AIR.defaultBlockState():VoidSeaContent.LIQUID.get().defaultBlockState().setValue(LiquidBlock.LEVEL,n);}
    public static BlockState at(BlockState old){return VoidSeaContent.FISSURE.get().defaultBlockState().setValue(FLUID_LEVEL,old.hasProperty(LiquidBlock.LEVEL)?old.getValue(LiquidBlock.LEVEL):16);}
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity e){
        if(!(l instanceof ServerLevel server)||e.isSpectator()||!e.isAlive()||!touches(p,e))return;
        long now=l.getGameTime();boolean added=false;
        for(Entity member:e.getSelfAndPassengers().toList()){
            if(member.isSpectator()||!member.isAlive())continue;
            var contacts=CONTACTS.computeIfAbsent(member,k->new HashMap<>());
            contacts.entrySet().removeIf(v->now-v.getValue()>20);
            Long previous=contacts.put(p.immutable(),now);
            if(previous!=null&&now-previous<=1)continue;
            PENDING.computeIfAbsent(server,k->new HashMap<>()).computeIfAbsent(p.immutable(),k->new HashSet<>()).add(member.getUUID());
            added=true;
        }
        if(added)server.scheduleTick(p,this,1);
    }
    public static boolean touches(BlockPos p,Entity e){return SHAPE.bounds().move(p).intersects(e.getBoundingBox());}
    @Override protected void tick(BlockState s,ServerLevel level,BlockPos p,RandomSource random){
        var pending=PENDING.get(level);var ids=pending==null?null:pending.remove(p);
        if(ids==null)return;
        boolean triggered=false;
        // Resolve every contact before moving, including independently vulnerable riders.
        for(UUID id:ids){Entity e=level.getEntity(id);if(e==null||!e.isAlive()||!touches(p,e)&&!touches(p,e.getRootVehicle()))continue;triggered=true;affect(e);}
        if(triggered)relocate(level,p,random);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(state.getBlock()!=next.getBlock()&&level instanceof ServerLevel server){var pending=PENDING.get(server);if(pending!=null)pending.remove(pos);}
        super.onRemove(state,level,pos,next,moving);
    }
    public static void affect(Entity e){
        if(SpatialImmunity.fissure(e))return;
        if(e instanceof LivingEntity living){
            for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET,EquipmentSlot.MAINHAND,EquipmentSlot.OFFHAND}){
                var stack=living.getItemBySlot(slot);if(!stack.isDamageableItem())continue;
                int remaining=stack.getMaxDamage()-stack.getDamageValue();if(remaining<=0)continue;
                int damage=(remaining+1)/2;
                if(damage>=remaining){living.onEquippedItemBroken(stack.getItem(),slot);living.setItemSlot(slot,net.minecraft.world.item.ItemStack.EMPTY);}
                else stack.setDamageValue(stack.getDamageValue()+damage);
            }
            VoidSeaEffects.trueDamage(living,(float)Math.ceil(living.getHealth()/2),living.damageSources().genericKill());
        }else e.hurt(e.damageSources().genericKill(),(float)Math.ceil(1));
    }
    public static boolean destination(ServerLevel level,BlockPos p){
        if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.getBiome(p).is(VoidSeaContent.BIOME))return false;
        BlockState state=level.getBlockState(p);
        if(!(state.isAir()||state.is(VoidSeaContent.LIQUID.get())))return false;
        if(!VoidSeaContent.fluid(state.getFluidState())){
            boolean surface=false;
            for(int d=1;d<=2;d++){var below=level.getBlockState(p.below(d));if(VoidSeaContent.fluid(below.getFluidState())){surface=true;break;}if(!below.isAir())break;}
            if(!surface)return false;
        }
        return level.getEntities((Entity)null,new AABB(p).inflate(.15),Entity::isAlive).isEmpty();
    }
    public static boolean relocate(ServerLevel level,BlockPos old,RandomSource random){
        BlockState source=level.getBlockState(old);if(!source.is(VoidSeaContent.FISSURE.get()))return false;
        for(int i=0;i<64;i++){
            BlockPos next=old.offset(random.nextInt(65)-32,random.nextInt(33)-16,random.nextInt(65)-32);
            if(next.distSqr(old)<9||!destination(level,next))continue;
            BlockState previous=level.getBlockState(next);
            if(!level.setBlock(next,at(previous),3))continue;
            if(!level.setBlock(old,restored(source),3)){level.setBlock(next,previous,3);return false;}
            level.sendParticles(ParticleTypes.PORTAL,old.getX()+.5,old.getY()+.5,old.getZ()+.5,12,.3,.5,.3,.1);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL,next.getX()+.5,next.getY()+.5,next.getZ()+.5,12,.3,.5,.3,.02);
            return true;
        }
        return false;
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r){
        if(r.nextInt(3)==0)l.addParticle(ParticleTypes.REVERSE_PORTAL,p.getX()+.5+(r.nextDouble()-.5)*.6,p.getY()+r.nextDouble(),p.getZ()+.5+(r.nextDouble()-.5)*.6,0,.015,0);
    }
}
