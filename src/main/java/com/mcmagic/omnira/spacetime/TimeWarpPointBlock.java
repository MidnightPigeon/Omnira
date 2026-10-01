package com.mcmagic.omnira.spacetime;

import com.mcmagic.omnira.item.RitualSwordItem;
import com.mcmagic.omnira.registry.ModBlocks;
import com.mcmagic.omnira.registry.ModItems;
import com.mcmagic.omnira.registry.ModParticles;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.world.phys.Vec3;

public final class TimeWarpPointBlock extends BaseEntityBlock {
    public static final MapCodec<TimeWarpPointBlock> CODEC=simpleCodec(TimeWarpPointBlock::new);
    public static final int RADIUS=5;
    private static final VoxelShape SHAPE=Block.box(2,2,2,14,14,14);
    public TimeWarpPointBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new TimeWarpPointBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:(world,pos,current,entity)->{
            if(entity instanceof TimeWarpPointBlockEntity warp)warp.serverTick();
        };
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public void attack(BlockState state,Level level,BlockPos pos,Player player){
        if(level instanceof ServerLevel server && player.getMainHandItem().getItem() instanceof RitualSwordItem)harvest(server,pos,player);
    }
    public static boolean harvest(ServerLevel level,BlockPos pos,Player player){
        if(!level.getBlockState(pos).is(ModBlocks.TIME_WARP_POINT.get()) || !player.mayBuild()
                || !level.mayInteract(player,pos))return false;
        if(!level.setBlock(pos,ModBlocks.TEMPORAL_MOTE.get().defaultBlockState(),Block.UPDATE_ALL))return false;
        Block.popResource(level,pos,ModItems.TIME_WARP_POINT.get().getDefaultInstance());
        return true;
    }
    public static boolean harvestNearby(ServerLevel level,BlockPos center,Player player){
        for(var pos:BlockPos.betweenClosed(center.offset(-1,-1,-1),center.offset(1,1,1)))
            if(level.getBlockState(pos).is(ModBlocks.TIME_WARP_POINT.get()) && harvest(level,pos.immutable(),player))return true;
        return false;
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,RandomSource random){
        Vec3 center=Vec3.atCenterOf(pos);
        double time=level.getGameTime()*.16;
        for(int i=0;i<3;i++){
            double phase=time+i*Math.PI*2/3;
            level.addParticle(ModParticles.TIME_WARP_SPARK.get(),center.x+Math.cos(phase)*.36,
                    center.y+Math.sin(phase*1.3)*.31,center.z+Math.sin(phase)*.36,0,.003,0);
        }
        for(int i=0;i<5;i++){
            double y=random.nextDouble()*2-1,angle=random.nextDouble()*Math.PI*2;
            double horizontal=Math.sqrt(1-y*y),radius=RADIUS*(.94+random.nextDouble()*.06);
            double dx=Math.cos(angle)*horizontal,dz=Math.sin(angle)*horizontal;
            level.addParticle(ModParticles.TIME_WARP_SPARK.get(),center.x+dx*radius,center.y+y*radius,
                    center.z+dz*radius,dx*.004,y*.004,dz*.004);
        }
    }
}
