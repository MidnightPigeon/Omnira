package com.mcmagic.omnira.spacetime;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class SpacetimeRiftBlock extends Block {
    public static final MapCodec<SpacetimeRiftBlock> CODEC=simpleCodec(SpacetimeRiftBlock::new);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty TIME=BooleanProperty.create("time");
    public SpacetimeRiftBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(HALF,DoubleBlockHalf.LOWER).setValue(TIME,true));}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,HALF,TIME);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return state.getValue(FACING).getAxis()==Direction.Axis.X?box(6,0,0,10,16,16):box(0,0,6,16,16,10);}
    @Override protected void entityInside(BlockState state,Level world,BlockPos pos,Entity entity){
        if(!(world instanceof ServerLevel level) || level.dimension()!=com.mcmagic.omnira.world.dimension.ModDimensions.SPACETIME_CORRIDOR)return;
        var root=entity.getRootVehicle();var data=root.getPersistentData();
        if(!com.mcmagic.omnira.world.dimension.DreamTransitRules.allowed(root) || data.getLong("OmniraRiftCooldown")>level.getGameTime())return;
        if(Math.abs(pos.getX())!=2 || pos.getY()<CorridorLayout.FLOOR+1 || pos.getY()>CorridorLayout.FLOOR+3)return;
        Vec3 landing=null;
        var members=root.getSelfAndPassengers().toList();
        for(int attempt=0;attempt<16;attempt++){
            boolean time=level.random.nextBoolean();
            int x=level.random.nextInt(CorridorLayout.RIFT_RANGE*2+1)-CorridorLayout.RIFT_RANGE;
            int z=level.random.nextInt(CorridorLayout.RIFT_RANGE*2+1)-CorridorLayout.RIFT_RANGE;
            int y=time?CorridorLayout.TIME_ENTRANCE.getY():CorridorLayout.SPACE_ENTRANCE.getY();
            var target=new BlockPos(x,y,z);
            if(!level.getWorldBorder().isWithinBounds(target))continue;
            level.getChunkAt(target);
            if(!level.getBlockState(target.below()).isFaceSturdy(level,target.below(),Direction.UP))continue;
            var point=Vec3.atBottomCenterOf(target);
            boolean clear=true;
            for(var member:members){
                var box=member.getBoundingBox().move(point.subtract(root.position()));
                for(int cx=net.minecraft.util.Mth.floor(box.minX)>>4;cx<=net.minecraft.util.Mth.floor(box.maxX)>>4;cx++)
                    for(int cz=net.minecraft.util.Mth.floor(box.minZ)>>4;cz<=net.minecraft.util.Mth.floor(box.maxZ)>>4;cz++)level.getChunk(cx,cz);
                if(!level.getWorldBorder().isWithinBounds(box) || !level.noCollision(box)){clear=false;break;}
            }
            if(clear){landing=point;break;}
        }
        if(landing==null)return;
        for(var member:root.getSelfAndPassengers().toList())member.getPersistentData().putLong("OmniraRiftCooldown",level.getGameTime()+40);
        root.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(level,landing,Vec3.ZERO,root.getYRot(),root.getXRot(),e->e.fallDistance=0));
    }
}
