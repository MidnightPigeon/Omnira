package com.mcmagic.omnira.space;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class SpaceWarpBlock extends BaseEntityBlock {
    public static final MapCodec<SpaceWarpBlock> CODEC=simpleCodec(SpaceWarpBlock::new);
    public SpaceWarpBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new SpaceWarpBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide?null:(world,pos,current,entity)->{if(entity instanceof SpaceWarpBlockEntity point)point.tick();};
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(2,2,2,14,14,14);}
    @Override public void attack(BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player){
        if(level instanceof net.minecraft.server.level.ServerLevel server&&player.getMainHandItem().getItem() instanceof com.mcmagic.omnira.item.RitualSwordItem)
            com.mcmagic.omnira.spacetime.TimeWarpPointBlock.harvest(server,pos,player);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this)&&level instanceof net.minecraft.server.level.ServerLevel server)SpaceWarpIndex.get(server).remove(pos);
        super.onRemove(state,level,pos,next,moving);
    }
    public static void particles(Level level,BlockPos pos,net.minecraft.util.RandomSource random,boolean remnant){
        double time=level.getGameTime()*.15;
        for(int i=0;i<(remnant?2:8);i++){
            double y=random.nextDouble()*2-1,a=time+i*2.399963,r=remnant?.17:i<3?.35:5;
            float shade=i%4==0?.9F:i%3==0?.12F:.45F;
            var dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(shade,shade*.95F,Math.min(1,shade+.25F)),.65F);
            level.addParticle(dust,pos.getX()+.5+Math.cos(a)*Math.sqrt(1-y*y)*r,pos.getY()+.5+y*r,pos.getZ()+.5+Math.sin(a)*Math.sqrt(1-y*y)*r,0,.003,0);
        }
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,net.minecraft.util.RandomSource r){particles(l,p,r,false);}
}
