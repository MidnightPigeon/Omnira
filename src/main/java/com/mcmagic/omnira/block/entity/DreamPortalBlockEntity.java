package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.world.dimension.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.DimensionTransition;

public final class DreamPortalBlockEntity extends BlockEntity {
    public static final int COLLAPSE_TICKS=60;
    private boolean exit;
    private long collapseAt=-1;
    public static final int OPEN_TICKS=40;
    private long openedAt=-1;
    private final java.util.Map<java.util.UUID,Integer> entering=new java.util.HashMap<>();
    public static final int TRANSIT_TICKS=24;
    public DreamPortalBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.DREAM_PORTAL.get(),pos,state);}
    public void initialize(boolean exit) {this.exit=exit;collapseAt=-1;openedAt=level==null?-1:level.getGameTime();sync();}
    public float scale(float partial) {
        if(level==null) return 1;
        float opening=openedAt<0?1:Math.clamp((level.getGameTime()+partial-openedAt)/OPEN_TICKS,0,1);
        opening=opening*opening*(3-2*opening);
        return opening*(collapseAt<0?1:Math.clamp(1-(level.getGameTime()+partial-collapseAt)/COLLAPSE_TICKS,0,1));
    }
    private void sync() {
        setChanged();
        if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public void tick() {
        if(level==null) return;
        long time=level.getGameTime();
        if(!level.isClientSide) {
            if(!exit && collapseAt<0 && time%10==0 && !DreamRitual.anchorsIntact(level,worldPosition)) {
                collapseAt=time;sync();
            }
            if(collapseAt>=0 && time-collapseAt>=COLLAPSE_TICKS) level.removeBlock(worldPosition,false);
            if(collapseAt<0 && (openedAt<0 || time-openedAt>=OPEN_TICKS)){
                var source=(ServerLevel)level;
                var area=new AABB(worldPosition).inflate(.15,.5,.15);
                var touching=new java.util.HashSet<java.util.UUID>();
                for(var entity:source.getEntities((Entity)null,area,e->e.isAlive() && !e.isSpectator())){
                    var root=entity.getRootVehicle();
                    if(!touching.add(root.getUUID()))continue;
                    enter(root);
                    if(!entering.containsKey(root.getUUID()))continue;
                    int ticks=entering.merge(root.getUUID(),1,Integer::sum);
                    if(ticks%4==0)TransitionParticles.ring(source,root.position(),Math.max(.6,root.getBbWidth()*.6)*(1-ticks/(double)TRANSIT_TICKS),root.getBbHeight(),ticks/(double)TRANSIT_TICKS);
                    if(ticks>=TRANSIT_TICKS){entering.remove(root.getUUID());transfer(root);}
                }
                entering.keySet().retainAll(touching);
            }else entering.clear();
            return;
        }
        if(time%8!=0) return;
        var dust=new DustParticleOptions(new Vector3f(.74F,.86F,1F),.55F);
        float size=scale(0);
        level.addParticle(dust,worldPosition.getX()+.5+(level.random.nextDouble()-.5)*size,
                worldPosition.getY()+.5+level.random.nextDouble()*size,worldPosition.getZ()+.5,0,.015,0);
        if(exit || collapseAt>=0) return;
        // Two moving points per tether, refreshed only every eight ticks.
        for(int i=0;i<DreamRitual.ANCHORS.length;i++) {
            int[] offset=DreamRitual.ANCHORS[i];
            Vec3 from=Vec3.atCenterOf(worldPosition).add(offset[0],-.85,offset[1]);
            Vec3 to=Vec3.atCenterOf(worldPosition);
            for(int p=0;p<2;p++) {
                double t=((time/8+i)%8)/16.0+p*.5;
                Vec3 point=from.lerp(to,t);
                level.addParticle(dust,point.x,point.y,point.z,0,0,0);
            }
        }
    }
    public void enter(Entity entity) {
        var root=entity.getRootVehicle();
        if(level==null || collapseAt>=0 || openedAt>=0 && level.getGameTime()-openedAt<OPEN_TICKS)return;
        if(level instanceof ServerLevel && root.isAlive() && !root.isSpectator() && !root.isRemoved()
                && DreamTransitRules.allowed(root)
                && root.getPersistentData().getLong("OmniraDreamCooldown")<=level.getGameTime()){
            if(entering.putIfAbsent(root.getUUID(),0)==null && root instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb){
                orb.stop();orb.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
    private void transfer(Entity player) {
        if(!(level instanceof ServerLevel source) || collapseAt>=0 || (openedAt>=0 && level.getGameTime()-openedAt<OPEN_TICKS) || player.isSpectator()
                || player.isPassenger() || !player.isAlive() || !DreamTransitRules.allowed(player)) return;
        if(!exit && !DreamRitual.anchorsIntact(source,worldPosition)) return;
        var data=player.getPersistentData();
        long now=source.getGameTime();
        if(data.getLong("OmniraDreamCooldown")>now) return;
        data.putLong("OmniraDreamCooldown",now+60);
        ServerLevel destination;
        Vec3 landing;
        if(source.dimension()!=ModDimensions.DREAM_REALM) {
            destination=source.getServer().getLevel(ModDimensions.DREAM_REALM);
            landing=destination==null?null:prepareHub(destination,player);
        } else {
            var id=ResourceLocation.tryParse(data.getString("OmniraDreamOriginDimension"));
            destination=id==null?source.getServer().overworld():source.getServer().getLevel(
                    ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id));
            if(destination==null || destination.dimension()==ModDimensions.DREAM_REALM) destination=source.getServer().overworld();
            BlockPos origin=data.contains("OmniraDreamOrigin")?BlockPos.of(data.getLong("OmniraDreamOrigin")):destination.getSharedSpawnPos();
            landing=findLanding(destination,origin,player);
            if(landing==null) {
                destination=source.getServer().overworld();
                BlockPos spawn=destination.getSharedSpawnPos();
                int y=destination.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.getX(),spawn.getZ());
                landing=findLanding(destination,new BlockPos(spawn.getX(),y,spawn.getZ()),player);
            }
        }
        if(destination==null || landing==null) {
            for(var member:player.getSelfAndPassengers().toList())if(member instanceof ServerPlayer passenger)
                passenger.displayClientMessage(Component.translatable("message.omnira.dream.arrival_blocked"),true);
            return;
        }
        var group=player.getSelfAndPassengers().toList();
        for(var member:group)if(!member.canChangeDimensions(source,destination)
                || !net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(member,destination.dimension()))return;
        for(var member:group){
            var memberData=member.getPersistentData();
            memberData.putLong("OmniraDreamCooldown",destination.getGameTime()+100);
            if(source.dimension()!=ModDimensions.DREAM_REALM){
                memberData.putString("OmniraDreamOriginDimension",source.dimension().location().toString());
                memberData.putLong("OmniraDreamOrigin",worldPosition.asLong());
            }
        }
        player.changeDimension(new DimensionTransition(destination,landing,Vec3.ZERO,player.getYRot(),player.getXRot(),entity->{
            entity.fallDistance=0;
            entity.placePortalTicket(entity.blockPosition());
            TransitionParticles.arrive(entity);
        }));
    }
    private static Vec3 prepareHub(ServerLevel destination,Entity player) {
        int surface=destination.getChunkSource().getGenerator().getBaseHeight(0,0,
                net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,destination,
                destination.getChunkSource().randomState());
        BlockPos hub=new BlockPos(0,Math.clamp(surface+1,4,destination.getMaxBuildHeight()-4),0);
        destination.getChunkAt(hub);
        if(!destination.getBlockState(hub).isAir() && !destination.getBlockState(hub).is(ModBlocks.DREAM_PORTAL.get())) return null;
        for(int x=-3;x<=3;x++) for(int z=-3;z<=3;z++) {
            BlockPos floor=hub.offset(x,-2,z);
            if(destination.getBlockState(floor).isAir())
                destination.setBlockAndUpdate(floor,ModBlocks.PERMANENT_VOID_CRYSTAL.get().defaultBlockState());
        }
        if(!destination.getBlockState(hub).is(ModBlocks.DREAM_PORTAL.get())) {
            destination.setBlockAndUpdate(hub,ModBlocks.DREAM_PORTAL.get().defaultBlockState());
            if(destination.getBlockEntity(hub) instanceof DreamPortalBlockEntity portal) portal.initialize(true);
        }
        return findLanding(destination,hub,player);
    }
    private static Vec3 findLanding(ServerLevel world,BlockPos origin,Entity player) {
        for(int r=1;r<=6;r++) for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r) continue;
            for(int dy=3;dy>=-4;dy--) {
                BlockPos feet=origin.offset(dx,dy,dz);
                if(!world.getWorldBorder().isWithinBounds(feet) || world.isOutsideBuildHeight(feet.above())) continue;
                if(!world.getBlockState(feet.below()).isFaceSturdy(world,feet.below(),Direction.UP)
                        || !world.getBlockState(feet).isAir() || !world.getBlockState(feet.above()).isAir()) continue;
                Vec3 point=Vec3.atBottomCenterOf(feet);
                AABB box=player.getDimensions(player.getPose()).makeBoundingBox(point);
                boolean clear=world.noCollision(box) && world.getWorldBorder().isWithinBounds(box);
                for(var passenger:player.getIndirectPassengers()){
                    var passengerBox=passenger.getBoundingBox().move(point.subtract(player.position()));
                    clear &= world.noCollision(passengerBox) && world.getWorldBorder().isWithinBounds(passengerBox)
                            && passengerBox.maxY<world.getMaxBuildHeight();
                }
                if(clear && box.maxY<world.getMaxBuildHeight()) return point;
            }
        }
        return null;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.saveAdditional(tag,registries);tag.putBoolean("DreamExit",exit);tag.putLong("CollapseAt",collapseAt);tag.putLong("OpenedAt",openedAt);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);exit=tag.getBoolean("DreamExit");collapseAt=tag.contains("CollapseAt")?tag.getLong("CollapseAt"):-1;openedAt=tag.contains("OpenedAt")?tag.getLong("OpenedAt"):-1;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag=new CompoundTag();saveAdditional(tag,registries);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}
