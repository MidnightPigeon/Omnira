package com.mcmagic.omnira.space;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.util.*;

public final class FoldGateBlockEntity extends BlockEntity {
    public UUID identity=UUID.randomUUID(),peerIdentity;
    public GlobalPos peer;
    public boolean white;
    public long selectedUntil;
    private final Map<UUID,Integer> touching=new HashMap<>();
    public FoldGateBlockEntity(BlockPos p,BlockState s){super(ModBlockEntityTypes.FOLD_GATE.get(),p,s);}
    public void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static FoldGateBlockEntity resolve(net.minecraft.server.MinecraftServer server,GlobalPos p){
        var l=server.getLevel(p.dimension());if(l==null)return null;l.getChunkAt(p.pos());
        return l.getBlockEntity(p.pos()) instanceof FoldGateBlockEntity g?g:null;
    }
    public static boolean pair(FoldGateBlockEntity a,FoldGateBlockEntity b){
        if(a==b||a.peer!=null||b.peer!=null||!FoldGateBlock.stable(a.getBlockState())||!FoldGateBlock.stable(b.getBlockState()))return false;
        a.peer=GlobalPos.of(b.level.dimension(),b.worldPosition);b.peer=GlobalPos.of(a.level.dimension(),a.worldPosition);
        a.peerIdentity=b.identity;b.peerIdentity=a.identity;a.white=true;b.white=false;a.sync();b.sync();return true;
    }
    public void unlink(){
        if(!(level instanceof ServerLevel l)||peer==null)return;
        var old=peer;var id=peerIdentity;peer=null;peerIdentity=null;sync();
        var other=resolve(l.getServer(),old);
        if(other!=null&&other.identity.equals(id)&&identity.equals(other.peerIdentity)){
            other.peer=null;other.peerIdentity=null;other.sync();
        }
    }
    public void tick(){
        if(level==null||!FoldGateBlock.active(getBlockState()))return;
        if(level.isClientSide){
            if(level.getGameTime()%3!=0)return;
            double t=level.getGameTime()*.13;var d=getBlockState().getValue(FoldGateBlock.FACING);
            boolean selected=selectedUntil>level.getGameTime();float shade=white||selected?.97F:.06F;
            var color=peer!=null||selected?new org.joml.Vector3f(shade,shade,shade):new org.joml.Vector3f(.35F,.6F,.95F);
            var dust=new DustParticleOptions(color,.65F);
            for(int i=0;i<4;i++){
                double a=t+i*Math.PI/2,u=Math.cos(a)*.47,v=Math.sin(a)*.88;
                level.addParticle(dust,worldPosition.getX()+.5+(d.getAxis()==Direction.Axis.Z?u:0),worldPosition.getY()+.95+v,worldPosition.getZ()+.5+(d.getAxis()==Direction.Axis.X?u:0),0,0,0);
            }
            return;
        }
        var l=(ServerLevel)level;boolean stable=FoldGateBlock.stable(getBlockState());
        if(stable&&peer==null){touching.clear();return;}
        var axis=getBlockState().getValue(FoldGateBlock.FACING).getAxis();
        var area=axis==Direction.Axis.Z?new AABB(worldPosition).inflate(0,0,-.32).expandTowards(0,1,0):new AABB(worldPosition).inflate(-.32,0,0).expandTowards(0,1,0);
        var seen=new HashSet<UUID>();
        for(var e:l.getEntities((net.minecraft.world.entity.Entity)null,area,e->e.isAlive()&&!e.isSpectator())){
            if(!stable&&!(e instanceof net.minecraft.server.level.ServerPlayer))continue;
            var root=e.getRootVehicle();
            if(!seen.add(root.getUUID())||!com.mcmagic.omnira.world.dimension.DreamTransitRules.allowed(root)||root.getPersistentData().getLong("OmniraFoldGateCooldown")>l.getServer().overworld().getGameTime())continue;
            int ticks=touching.merge(root.getUUID(),1,Integer::sum);
            if(ticks>=20){touching.remove(root.getUUID());FoldGateTransit.transfer(this,root,stable);}
        }
        touching.keySet().retainAll(seen);
    }
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putUUID("Identity",identity);t.putBoolean("White",white);if(peer!=null){t.putString("PeerDimension",peer.dimension().location().toString());t.putLong("PeerPos",peer.pos().asLong());if(peerIdentity!=null)t.putUUID("PeerIdentity",peerIdentity);}}
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);selectedUntil=t.getLong("SelectedUntil");if(t.hasUUID("Identity"))identity=t.getUUID("Identity");white=t.getBoolean("White");peer=null;peerIdentity=null;var id=ResourceLocation.tryParse(t.getString("PeerDimension"));if(id!=null&&t.hasUUID("PeerIdentity")){peer=GlobalPos.of(ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,id),BlockPos.of(t.getLong("PeerPos")));peerIdentity=t.getUUID("PeerIdentity");}}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=saveWithoutMetadata(r);t.putLong("SelectedUntil",selectedUntil);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
