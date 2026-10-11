package com.mcmagic.omnira.space.anchor;

import java.util.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class StabilityAnchorEntity extends BlockEntity {
    private final SpatialActivation activation=new SpatialActivation(this,64);
    private long lastTick=Long.MIN_VALUE;
    public StabilityAnchorEntity(BlockPos p,BlockState s){super(AnchorContent.ENTITY.get(),p,s);}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server)StabilityField.update(server,worldPosition,getBlockState().is(AnchorContent.ACTIVE.get()));}
    public static BlockState result(BlockState state){
        if(state.is(DreamContent.SPATIAL_CRYSTAL.get()))return DreamContent.EXCITED_SPATIAL_CRYSTAL.get().defaultBlockState();
        if(state.is(DreamContent.TEMPORAL_SILT.get()))return DreamContent.LIVING_TEMPORAL_SILT.get().defaultBlockState();
        if(state.is(TimeNatureContent.SILT.get()))return TimeNatureContent.LIVING_SILT.get().defaultBlockState();
        return null;
    }
    public static void orbit(ServerLevel level,Vec3 center,double radius,long time){
        StabilityVisualPayload.queue(level,center,radius);
    }
    public boolean begin(BlockPos pos){
        return activation.begin(pos);
    }
    public static void tick(Level l,BlockPos p,BlockState s,StabilityAnchorEntity anchor){
        if(!s.is(AnchorContent.ACTIVE.get()))return;
        long now=l.getGameTime();if(anchor.lastTick==now)return;anchor.lastTick=now;
        if(l.isClientSide){
            double a=now*.09;for(int i=0;i<2;i++){double t=a+i*Math.PI;l.addParticle(ParticleTypes.END_ROD,p.getX()+.5+Math.cos(t)*.4,p.getY()+.7+Math.sin(t*2)*.12,p.getZ()+.5+Math.sin(t)*.4,0,.005,0);}
            return;
        }
        anchor.activation.tick((ServerLevel)l);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);activation.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);activation.load(tag,provider);
    }
}
