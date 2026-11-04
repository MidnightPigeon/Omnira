package com.mcmagic.omnira.spell.world;

import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

@EventBusSubscriber(modid="omnira")
public final class WorldRotation {
    public static final int RADIUS=32,COOLDOWN=1200;
    private static final List<BlockPos> OFFSETS=offsets();
    private static List<BlockPos> offsets(){
        var list=new ArrayList<BlockPos>();for(int x=-RADIUS;x<=RADIUS;x++)for(int y=-RADIUS;y<=RADIUS;y++)for(int z=-RADIUS;z<=RADIUS;z++)
            if(x*x+y*y+z*z<=RADIUS*RADIUS)list.add(new BlockPos(x,y,z));
        list.sort(Comparator.comparingInt(p->p.getX()*p.getX()+p.getY()*p.getY()+p.getZ()*p.getZ()));return List.copyOf(list);
    }
    private static final List<Wave> WAVES=new ArrayList<>();
    public static void cancel(UUID caster){WAVES.removeIf(wave->wave.caster.equals(caster));}
    private static final int[] COLORS={0xD8BB86,0x65BCFF,0xFF9060,0x8BE5D6,0xFFF293,0xBB8BE8,0xB1E9B8,0x8F99FF};
    private static final class Wave {
        final ServerLevel level;final UUID caster;final Vec3 center;final BlockPos origin;final Set<UUID> touched=new HashSet<>();
        final Set<BlockPos> machines=new HashSet<>();int cursor;double radius;
        Wave(ServerPlayer player){level=player.serverLevel();caster=player.getUUID();center=player.position();origin=BlockPos.containing(center);}
    }
    public static boolean cast(ServerPlayer player){
        var mana=player.getData(ModAttachments.MANA);long now=player.server.overworld().getGameTime();
        if(!player.isAlive()||player.isSpectator())return false;
        long ready=player.getData(ModAttachments.WORLD_ROTATION_READY);
        // A saved deadline cannot be more than one cooldown ahead of this world clock.
        // Rebuilt worlds may retain player attachments from the old timeline.
        if(ready>now && ready-now>COOLDOWN){
            player.setData(ModAttachments.WORLD_ROTATION_READY,0L);ready=0;
        }
        if(now<ready){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.world_rotation.cooldown",(ready-now+19)/20),true);return false;
        }
        if(mana.current()<=0){
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.mana.insufficient"),true);return false;
        }
        player.setData(ModAttachments.WORLD_ROTATION_READY,now+COOLDOWN);
        player.setData(ModAttachments.MANA,mana.withCurrent(0));player.removeAllEffects();player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);player.getFoodData().setSaturation(20);repairCarried(player);
        WAVES.add(new Wave(player));player.serverLevel().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,net.minecraft.sounds.SoundSource.PLAYERS,1,1);
        com.mcmagic.omnira.progression.BranchProgress.record(player,"fleeting_world");
        return true;
    }
    private static void repairCarried(ServerPlayer player){
        var seen=Collections.newSetFromMap(new IdentityHashMap<ItemStack,Boolean>());
        for(int i=0;i<player.getInventory().getContainerSize();i++)repair(player.getInventory().getItem(i),seen,0);
        repair(player.containerMenu.getCarried(),seen,0);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(player).ifPresent(inv->inv.getCurios().values().forEach(handler->{
            for(int i=0;i<handler.getSlots();i++){repair(handler.getStacks().getStackInSlot(i),seen,0);repair(handler.getCosmeticStacks().getStackInSlot(i),seen,0);}
        }));player.getInventory().setChanged();player.containerMenu.broadcastChanges();
    }
    private static void repair(ItemStack stack,Set<ItemStack> seen,int depth){
        if(stack.isEmpty()||!seen.add(stack)||depth>16)return;if(stack.isDamaged())stack.setDamageValue(0);
        var container=stack.get(net.minecraft.core.component.DataComponents.CONTAINER);
        if(container!=null){var items=container.stream().toList();items.forEach(s->repair(s,seen,depth+1));stack.set(net.minecraft.core.component.DataComponents.CONTAINER,net.minecraft.world.item.component.ItemContainerContents.fromItems(items));}
        var bundle=stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
        if(bundle!=null){var items=bundle.itemCopyStream().toList();items.forEach(s->repair(s,seen,depth+1));stack.set(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS,new net.minecraft.world.item.component.BundleContents(items));}
        var handler=stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ITEM);
        if(handler instanceof net.neoforged.neoforge.items.IItemHandlerModifiable mutable)for(int i=0;i<mutable.getSlots();i++){
            var item=mutable.getStackInSlot(i).copy();repair(item,seen,depth+1);mutable.setStackInSlot(i,item);
        }
    }
    public static void affect(LivingEntity entity,ServerPlayer caster){
        if(!entity.isAlive()||entity==caster)return;
        if(WorldEntityKinds.hostileNow(entity)){
            if(WorldEntityKinds.boss(entity)&&entity.getHealth()>entity.getMaxHealth()/2)entity.setHealth(entity.getHealth()-entity.getMaxHealth()/2);
            else entity.discard();return;
        }
        if(entity instanceof Animal animal&&!animal.isBaby()){animal.setAge(0);if(animal.canFallInLove())animal.setInLove(caster);}
        if(entity instanceof Chicken chicken&&!chicken.isBaby()){chicken.spawnAtLocation(Items.EGG);chicken.eggTime=6000+chicken.getRandom().nextInt(6000);}
        if(entity instanceof net.minecraft.world.entity.animal.armadillo.Armadillo armadillo&&!armadillo.isBaby())armadillo.brushOffScute();
        if(entity instanceof net.neoforged.neoforge.common.IShearable shearable){
            var tool=new ItemStack(Items.SHEARS);
            if(shearable.isShearable(caster,tool,entity.level(),entity.blockPosition()))
                for(var drop:shearable.onSheared(caster,tool,entity.level(),entity.blockPosition()))entity.spawnAtLocation(drop);
        }
        WorldRotationHooks.entity(entity,caster);
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        int budget=4096;
        for(var iterator=WAVES.iterator();iterator.hasNext()&&budget>0;){var wave=iterator.next();
            if(wave.level.getServer()!=event.getServer()){continue;}
            int work=Math.min(2048,budget);budget-=work;
            var caster=event.getServer().getPlayerList().getPlayer(wave.caster);
            if(caster==null&&wave.level.getPlayerByUUID(wave.caster) instanceof ServerPlayer local)caster=local;
            for(int i=0;i<work&&wave.cursor<OFFSETS.size();i++){
                var offset=OFFSETS.get(wave.cursor++);wave.radius=Math.sqrt(offset.distSqr(BlockPos.ZERO));var pos=wave.origin.offset(offset);
                if(wave.level.isOutsideBuildHeight(pos)||!wave.level.hasChunkAt(pos)||!wave.level.getWorldBorder().isWithinBounds(pos))continue;
                if(Vec3.atCenterOf(pos).distanceToSqr(wave.center)>RADIUS*RADIUS)continue;
                if(caster!=null&&!wave.level.mayInteract(caster,pos))continue;
                WorldGrowth.mature(wave.level,pos);
                if(caster!=null&&caster.level()==wave.level&&wave.level.getBlockState(pos).is(com.mcmagic.omnira.space.phase.PhaseContent.CLUSTER.get()))
                    com.mcmagic.omnira.space.phase.PhaseGrowth.mature(caster,pos);
                var machine=wave.level.getBlockEntity(pos);if(machine!=null&&wave.machines.add(pos.immutable()))WorldRotationHooks.machine(machine,caster);
            }
            for(var entity:wave.level.getEntitiesOfClass(LivingEntity.class,new AABB(wave.center,wave.center).inflate(wave.radius+1)))
                if(entity.position().distanceToSqr(wave.center)<=wave.radius*wave.radius&&wave.touched.add(entity.getUUID()))affect(entity,caster);
            if(caster!=null)for(var item:wave.level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(wave.center,wave.center).inflate(wave.radius+1)))
                if(item.position().distanceToSqr(wave.center)<=wave.radius*wave.radius&&wave.touched.add(item.getUUID()))collect(item,caster);
            if(wave.cursor%4096==0||wave.cursor==OFFSETS.size())particles(wave);
            if(wave.cursor>=OFFSETS.size())iterator.remove();
        }
    }
    public static void collect(net.minecraft.world.entity.item.ItemEntity entity,ServerPlayer player){
        if(!entity.isAlive()||entity.getItem().isEmpty())return;
        var remaining=entity.getItem().copy();int before=remaining.getCount();player.getInventory().add(remaining);
        int taken=before-remaining.getCount();
        if(taken>0){player.take(entity,taken);player.getInventory().setChanged();player.containerMenu.broadcastChanges();}
        if(remaining.isEmpty())entity.discard();
        else{
            entity.setItem(remaining);
            if(entity.level()==player.level())entity.teleportTo(player.getX(),player.getY(),player.getZ());
            else{
                var moved=entity.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(player.serverLevel(),player.position(),Vec3.ZERO,entity.getYRot(),entity.getXRot(),net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
                if(moved instanceof net.minecraft.world.entity.item.ItemEntity item)entity=item;
            }
            entity.setDeltaMovement(Vec3.ZERO);entity.hurtMarked=true;
        }
    }
    private static void particles(Wave wave){
        for(int i=0;i<48;i++){double y=1-2*(i+.5)/48,angle=i*2.3999632297,r=Math.sqrt(1-y*y);
            int rgb=COLORS[i%COLORS.length];
            wave.level.sendParticles(new DustParticleOptions(new org.joml.Vector3f((rgb>>16&255)/255F,(rgb>>8&255)/255F,(rgb&255)/255F),1.5F),
                    wave.center.x+wave.radius*r*Math.cos(angle),wave.center.y+wave.radius*y,wave.center.z+wave.radius*r*Math.sin(angle),1,0,0,0,0);
        }
    }
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event){WAVES.clear();ReloadEffect.clear();}
    @SubscribeEvent public static void tags(net.neoforged.neoforge.event.TagsUpdatedEvent event){ReloadEffect.clear();}
    private WorldRotation(){}
}
