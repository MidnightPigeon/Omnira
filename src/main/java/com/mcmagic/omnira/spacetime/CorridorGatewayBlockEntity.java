package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.entity.DreamMirror;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import java.util.UUID;

public final class CorridorGatewayBlockEntity extends BlockEntity {
    public static final int TRANSIT_TICKS=24;
    private final java.util.Map<UUID,Integer> entering=new java.util.HashMap<>();
    private long started=-1,opened=-1;
    private float yaw;
    private UUID mirrorId;
    private ItemStack aggregate=ItemStack.EMPTY;
    private CompoundTag mirrorData=new CompoundTag();
    private boolean released;
    private boolean permanent;
    public CorridorGatewayBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.CORRIDOR_GATEWAY.get(),pos,state);}
    public void initialize(Player player,InteractionHand hand,DreamMirror mirror){
        mirrorId=mirror.getUUID();aggregate=player.getItemInHand(hand).split(1);
        permanent=aggregate.is(ModItems.SPACETIME_SPELL_CORE.get());
        mirror.saveWithoutId(mirrorData);
        var delta=player.position().subtract(Vec3.atCenterOf(worldPosition));
        yaw=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
        started=level.getGameTime();mirror.discard();sync();
    }
    public float yaw(){return yaw;}
    public UUID mirrorId(){return mirrorId;}
    public float progress(float partial){return started<0?0:(float)Math.clamp((level.getGameTime()+partial-started)/CorridorRitual.FORM_TICKS,0,1);}
    public boolean open(){return opened>=0;}
    public boolean permanent(){return permanent;}
    private void sync(){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void tick(){
        if(!(level instanceof ServerLevel server) || started<0 || released)return;
        long now=level.getGameTime();
        if(opened>=0){
            if(!permanent && now-opened>=CorridorRitual.OPEN_TICKS){server.removeBlock(worldPosition,false);return;}
            transferTouching(server);
        }else{
            for(var a:DreamRitual.ANCHORS)if(!server.hasChunkAt(worldPosition.offset(a[0],-1,a[1])))return;
            var pedestals=CorridorRitual.pedestals(server,worldPosition.below());
            if(!server.getBlockState(worldPosition.below()).is(ModBlocks.ADVANCED_RITUAL_ENERGY_CORE.get()) || pedestals.size()!=8){
                server.removeBlock(worldPosition,false);return;
            }
            if(now-started>=CorridorRitual.FORM_TICKS){
                aggregate=ItemStack.EMPTY;opened=now;sync();
                server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.PORTAL_TRIGGER,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1.4F);
            }
        }
        if(now%4==0)particles(server,now);
    }
    private void particles(ServerLevel server,long now){
        var color=new DustParticleOptions(new Vector3f(.64F,.5F,1),.7F);
        var center=Vec3.atBottomCenterOf(worldPosition).add(0,.9,0);
        if(!open())for(var a:DreamRitual.ANCHORS){
            double t=(now%40)/40.;var point=center.add(a[0]*(1-t),-.5*(1-t),a[1]*(1-t));
            server.sendParticles(color,point.x,point.y,point.z,1,0,0,0,0);
        }
        if(open()){
            double angle=now*.085;
            for(int i=0;i<8;i++){
                double a=angle+i*Math.PI/4;
                double x=center.x+Math.cos(a)*.48,z=center.z+Math.sin(a)*.48;
                server.sendParticles(color,x,center.y+Math.sin(a*2+angle)*.7,z,1,0,0,0,0);
            }
        }else server.sendParticles(color,center.x,center.y,center.z,2,.35,.65,.08,.005);
    }
    private void transferTouching(ServerLevel source){
        var center=Vec3.atBottomCenterOf(worldPosition).add(0,.9,0);
        var seen=new java.util.HashSet<UUID>();
        var touching=new java.util.HashSet<UUID>();
        for(var entity:source.getEntities((Entity)null,new AABB(worldPosition).inflate(1,.9,1),Entity::isAlive)){
            var root=entity.getRootVehicle();if(!seen.add(root.getUUID()) || !DreamTransitRules.allowed(root))continue;
            var box=root.getBoundingBox();var delta=box.getCenter().subtract(center);
            double radius=Math.max(box.getXsize(),box.getZsize())*.5;
            if(Math.hypot(delta.x,delta.z)>.43+radius
                    || box.maxY<worldPosition.getY()+.1 || box.minY>worldPosition.getY()+1.7)continue;
            touching.add(root.getUUID());
            int ticks=entering.merge(root.getUUID(),1,Integer::sum);
            if(ticks%4==0)transitionRing(source,root,ticks);
            if(ticks>=TRANSIT_TICKS){entering.remove(root.getUUID());transfer(source,root);}
        }
        entering.keySet().retainAll(touching);
    }
    private static void transitionRing(ServerLevel level,Entity root,int ticks){
        double progress=ticks/(double)TRANSIT_TICKS;
        var blue=new DustParticleOptions(new Vector3f(.56F,.84F,1F),.7F);
        var green=new DustParticleOptions(new Vector3f(.64F,1F,.8F),.7F);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6+progress*Math.PI*2,radius=Math.max(.45,root.getBbWidth()*.6)*(1-progress);
            var p=root.position();
            level.sendParticles(i%2==0?blue:green,p.x+Math.cos(angle)*radius,p.y+root.getBbHeight()*progress,
                    p.z+Math.sin(angle)*radius,1,0,0,0,0);
        }
    }
    public static boolean transfer(ServerLevel source,Entity root){
        if(root.isPassenger() || !DreamTransitRules.allowed(root)
                || root.getPersistentData().getLong("OmniraCorridorCooldown")>source.getGameTime())return false;
        var destination=source.getServer().getLevel(ModDimensions.SPACETIME_CORRIDOR);
        if(destination==null || source==destination)return false;
        var members=root.getSelfAndPassengers().toList();
        for(var member:members)if(!member.canChangeDimensions(source,destination)
                || !net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(member,destination.dimension()))return false;
        Vec3 landing=null;
        for(int attempt=0;attempt<12;attempt++){
            int z=destination.random.nextInt(16385)-8192;
            var point=new Vec3(.5,CorridorLayout.FLOOR+1,z+.5);
            var pos=BlockPos.containing(point);
            if(!destination.getWorldBorder().isWithinBounds(pos))continue;
            destination.getChunkAt(pos);
            boolean clear=true;
            for(var member:members){
                var box=member.getBoundingBox().move(point.subtract(root.position()));
                if(box.minX< -1 || box.maxX>2 || box.minY<CorridorLayout.FLOOR+1 || box.maxY>CorridorLayout.FLOOR+4){clear=false;break;}
                for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(box.maxX)>>4;cx++)
                    for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(box.maxZ)>>4;cz++)destination.getChunk(cx,cz);
                clear &= destination.getWorldBorder().isWithinBounds(box) && destination.noCollision(box);
            }
            if(clear && destination.getBlockState(pos.below()).isFaceSturdy(destination,pos.below(),Direction.UP)){landing=point;break;}
        }
        if(landing==null){root.getPersistentData().putLong("OmniraCorridorCooldown",source.getGameTime()+40);return false;}
        for(var member:members)member.getPersistentData().putLong("OmniraCorridorCooldown",destination.getGameTime()+60);
        if(root instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb)orb.stop();
        return root.changeDimension(new DimensionTransition(destination,landing,Vec3.ZERO,root.getYRot(),root.getXRot(),e->{
            e.fallDistance=0;e.placePortalTicket(e.blockPosition());TransitionParticles.arrive(e);
        }))!=null;
    }
    public void release(){
        if(!(level instanceof ServerLevel server) || released || started<0)return;
        released=true;
        if(!aggregate.isEmpty()){
            var dropped=new net.minecraft.world.entity.item.ItemEntity(server,worldPosition.getX()+.5,worldPosition.getY()+.1,worldPosition.getZ()+.5,aggregate);
            dropped.setDeltaMovement(0,.2,0);server.addFreshEntity(dropped);aggregate=ItemStack.EMPTY;
        }
        var mirror=ModEntityTypes.DREAM_MIRROR.get().create(server);
        if(mirror!=null){
            if(!mirrorData.isEmpty())mirror.load(mirrorData);
            mirror.moveTo(worldPosition.getX()+.5,worldPosition.getY(),worldPosition.getZ()+.5,yaw,0);
            mirror.setDeltaMovement(Vec3.ZERO);mirror.setTarget(null);server.addFreshEntity(mirror);
        }
        setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);writeVisual(tag);tag.putBoolean("Released",released);tag.put("Mirror",mirrorData);
        if(!aggregate.isEmpty())tag.put("Aggregate",aggregate.save(registries));
    }
    private void writeVisual(CompoundTag tag){tag.putLong("Started",started);tag.putLong("Opened",opened);tag.putFloat("Yaw",yaw);tag.putBoolean("Permanent",permanent);if(mirrorId!=null)tag.putUUID("MirrorId",mirrorId);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);started=tag.contains("Started")?tag.getLong("Started"):-1;
        opened=tag.contains("Opened")?tag.getLong("Opened"):-1;yaw=tag.getFloat("Yaw");released=tag.getBoolean("Released");permanent=tag.getBoolean("Permanent");
        mirrorId=tag.hasUUID("MirrorId")?tag.getUUID("MirrorId"):null;mirrorData=tag.getCompound("Mirror");
        aggregate=ItemStack.parseOptional(registries,tag.getCompound("Aggregate"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=new CompoundTag();writeVisual(tag);return tag;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
