package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.util.PhaseAge;
import com.mcmagic.omnira.util.GameTimeCooldown;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.network.TransitViewPayload;
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
    private BlockPos paired;
    private final PhaseAge collapse=new PhaseAge("CollapseAge","CollapseAt",COLLAPSE_TICKS);
    public static final int OPEN_TICKS=40;
    private final PhaseAge openingPhase=new PhaseAge("OpeningAge","OpenedAt",OPEN_TICKS);
    private final java.util.Map<java.util.UUID,Integer> entering=new java.util.HashMap<>();
    public static final int TRANSIT_TICKS=TransitViewPayload.PORTAL_TICKS;
    public DreamPortalBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.DREAM_PORTAL.get(),pos,state);}
    public void initialize(boolean exit) {this.exit=exit;collapse.stop();openingPhase.start();sync();}
    public static BlockPos mappedReturn(ServerLevel source,BlockPos origin){
        var world=source.getServer().overworld();
        for(int r=0;r<=6;r++)for(int dx=-r;dx<=r;dx++)for(int dz=-r;dz<=r;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r)continue;
            int x=origin.getX()+dx,z=origin.getZ()+dz;
            var column=new BlockPos(x,world.getMinBuildHeight(),z);
            if(!world.getWorldBorder().isWithinBounds(column))continue;
            world.getChunkAt(column);
            int y=world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            var pos=new BlockPos(x,y,z);
            if(!world.isOutsideBuildHeight(pos.above()) && world.getBlockState(pos.below()).isFaceSturdy(world,pos.below(),Direction.UP)
                    && world.isEmptyBlock(pos) && world.isEmptyBlock(pos.above()))return pos;
        }
        return null;
    }
    public void pairWith(BlockPos target){paired=target.immutable();sync();}
    public float scale(float partial) {
        if(level==null) return 1;
        float opening=!openingPhase.active()?1:Math.clamp((openingPhase.ticks(level)+partial)/OPEN_TICKS,0,1);
        opening=opening*opening*(3-2*opening);
        return opening*(!collapse.active()?1:Math.clamp(1-(collapse.ticks(level)+partial)/COLLAPSE_TICKS,0,1));
    }
    private void sync() {
        setChanged();
        if(level!=null && !level.isClientSide) level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public void tick() {
        if(level==null) return;
        boolean changed=openingPhase.advanceAnimated(level,worldPosition);
        changed |= collapse.advanceAnimated(level,worldPosition);
        if(changed && !level.isClientSide)setChanged();
        long time=level.getGameTime();
        if(!level.isClientSide) {
            if(exit && paired!=null && !collapse.active() && time%10==0){
                var other=((ServerLevel)level).getServer().getLevel(ModDimensions.DREAM_REALM);
                if(other!=null && other.hasChunkAt(paired) && (!(other.getBlockEntity(paired) instanceof DreamPortalBlockEntity portal)
                        || !worldPosition.equals(portal.paired) || !DreamRitual.anchorsIntact(other,paired))){collapse.start();sync();}
            }
            if(!exit && !collapse.active() && time%10==0 && !DreamRitual.anchorsIntact(level,worldPosition)) {
                collapse.start();sync();
            }
            if(collapse.active() && collapse.ticks(level)>=COLLAPSE_TICKS) level.removeBlock(worldPosition,false);
            if(!collapse.active() && (!openingPhase.active() || openingPhase.ticks(level)>=OPEN_TICKS)){
                var source=(ServerLevel)level;
                var area=new AABB(worldPosition).inflate(.15,.5,.15);
                var touching=new java.util.HashSet<java.util.UUID>();
                for(var entity:source.getEntities((Entity)null,area,e->e.isAlive() && !e.isSpectator())){
                    var root=entity.getRootVehicle();
                    if(!touching.add(root.getUUID()))continue;
                    enter(root);
                    if(!entering.containsKey(root.getUUID()))continue;
                    int ticks=entering.merge(root.getUUID(),1,Integer::sum);
                    TransitViewPayload.charging(root, TransitViewPayload.Style.DREAM, ticks);
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
        if(exit || collapse.active()) return;
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
        if(level==null || collapse.active() || openingPhase.active() && openingPhase.ticks(level)<OPEN_TICKS)return;
        if(level instanceof ServerLevel && root.isAlive() && !root.isSpectator() && !root.isRemoved()
                && DreamTransitRules.allowed(root)
                && !GameTimeCooldown.active(root, "OmniraDreamCooldown", level.getGameTime(), 100)){
            if(entering.putIfAbsent(root.getUUID(),0)==null && root instanceof com.mcmagic.omnira.vehicle.CruiseOrbEntity orb){
                orb.stop();orb.setDeltaMovement(Vec3.ZERO);
            }
        }
    }
    private Entity transfer(Entity player) {
        if(!(level instanceof ServerLevel source) || collapse.active() || (openingPhase.active() && openingPhase.ticks(level)<OPEN_TICKS) || player.isSpectator()
                || player.isPassenger() || !player.isAlive() || !DreamTransitRules.allowed(player)) return null;
        if(!exit && !DreamRitual.anchorsIntact(source,worldPosition)) return null;
        var data=player.getPersistentData();
        long now=source.getGameTime();
        if(GameTimeCooldown.active(player, "OmniraDreamCooldown", now, 100)) return null;
        GameTimeCooldown.start(player, "OmniraDreamCooldown", now, 60);
        ServerLevel destination;
        Vec3 landing;
        if(paired!=null){
            destination=source.dimension()==ModDimensions.DREAM_REALM?source.getServer().overworld():source.getServer().getLevel(ModDimensions.DREAM_REALM);
            landing=null;
            if(destination!=null){
                destination.getChunkAt(paired);
                if(destination.getBlockEntity(paired) instanceof DreamPortalBlockEntity portal && worldPosition.equals(portal.paired)
                        && !portal.collapse.active())landing=findLanding(destination,paired,player);
            }
        }else if(source.dimension()!=ModDimensions.DREAM_REALM) {
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
            return null;
        }
        var group=player.getSelfAndPassengers().toList();
        for(var member:group)if(!member.canChangeDimensions(source,destination)
                || !net.neoforged.neoforge.common.CommonHooks.onTravelToDimension(member,destination.dimension()))return null;
        for(var member:group){
            var memberData=member.getPersistentData();
            GameTimeCooldown.start(member, "OmniraDreamCooldown", destination.getGameTime(), 100);
            if(source.dimension()!=ModDimensions.DREAM_REALM){
                memberData.putString("OmniraDreamOriginDimension",source.dimension().location().toString());
                memberData.putLong("OmniraDreamOrigin",worldPosition.asLong());
            }
        }
        return com.mcmagic.omnira.space.anchor.StabilityField.portalTravel(player,new DimensionTransition(destination,landing,Vec3.ZERO,player.getYRot(),player.getXRot(),entity->{
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
    public static Vec3 findLanding(ServerLevel world,BlockPos origin,Entity player) {
        for(int r=1;r<=6;r++) for(int dx=-r;dx<=r;dx++) for(int dz=-r;dz<=r;dz++) {
            if(Math.max(Math.abs(dx),Math.abs(dz))!=r) continue;
            for(int dy=3;dy>=-4;dy--) {
                BlockPos feet=origin.offset(dx,dy,dz);
                if(!world.getWorldBorder().isWithinBounds(feet) || world.isOutsideBuildHeight(feet.above())) continue;
                world.getChunkAt(feet);
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
        super.saveAdditional(tag,registries);tag.putBoolean("DreamExit",exit);collapse.save(tag);openingPhase.save(tag);
        if(paired!=null)tag.putLong("Paired",paired.asLong());
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);exit=tag.getBoolean("DreamExit");collapse.load(tag);openingPhase.load(tag);
        paired=tag.contains("Paired")?BlockPos.of(tag.getLong("Paired")):null;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag=new CompoundTag();saveAdditional(tag,registries);return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {return ClientboundBlockEntityDataPacket.create(this);}
}
