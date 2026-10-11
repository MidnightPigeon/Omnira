package com.mcmagic.omnira.space.labyrinth;

import net.minecraft.core.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class VoidPortalBlock extends BaseEntityBlock implements LiquidBlockContainer {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final IntegerProperty FLUID=IntegerProperty.create("fluid",0,3);
    public VoidPortalBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(FLUID,0));}
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(VoidPortalBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,FLUID);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos p,BlockState s){return new VoidPortalEntity(p,s);}
    @Override public boolean onDestroyedByPlayer(BlockState s,Level level,BlockPos p,net.minecraft.world.entity.player.Player player,boolean harvest,net.minecraft.world.level.material.FluidState fluid){return false;}
    @Override public boolean canEntityDestroy(BlockState s,BlockGetter level,BlockPos p,net.minecraft.world.entity.Entity entity){return false;}
    // Keep the wall/floor sealed; the vortex projects into the room and triggers before collision.
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter level,BlockPos p,CollisionContext c){return Shapes.block();}
    @Override protected VoxelShape getOcclusionShape(BlockState s,BlockGetter level,BlockPos p){return Shapes.block();}
    public static net.minecraft.world.phys.AABB bounds(BlockState s,BlockPos p){
        var d=s.getValue(FACING).getOpposite();var center=net.minecraft.world.phys.Vec3.atCenterOf(p).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(d.getNormal()).scale(.55));
        return new net.minecraft.world.phys.AABB(center,center).inflate(d.getAxis()==Direction.Axis.X?.15:.62,d.getAxis()==Direction.Axis.Y?.15:.62,d.getAxis()==Direction.Axis.Z?.15:.62);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.create(bounds(s,BlockPos.ZERO));}
    public static int liquid(net.minecraft.world.level.material.Fluid f){return f.isSame(net.minecraft.world.level.material.Fluids.WATER)?1:f.isSame(com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get())?2:f.isSame(com.mcmagic.omnira.mire.MireContent.SOURCE.get())?3:0;}
    @Override protected net.minecraft.world.level.material.FluidState getFluidState(BlockState s){return switch(s.getValue(FLUID)){case 1->net.minecraft.world.level.material.Fluids.WATER.defaultFluidState();case 2->com.mcmagic.omnira.space.sea.VoidSeaContent.SOURCE.get().defaultFluidState();case 3->com.mcmagic.omnira.mire.MireContent.SOURCE.get().defaultFluidState();default->net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState();};}
    @Override public boolean canPlaceLiquid(net.minecraft.world.entity.player.Player player,BlockGetter level,BlockPos p,BlockState s,net.minecraft.world.level.material.Fluid f){return s.getValue(FLUID)==0&&liquid(f)>0;}
    @Override public boolean placeLiquid(LevelAccessor level,BlockPos p,BlockState s,net.minecraft.world.level.material.FluidState f){if(s.getValue(FLUID)!=0||liquid(f.getType())==0)return false;level.setBlock(p,s.setValue(FLUID,liquid(f.getType())),3);level.scheduleTick(p,f.getType(),f.getType().getTickDelay(level));return true;}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor level,BlockPos p,BlockPos next){
        var fluid=s.getFluidState();if(!fluid.isEmpty())level.scheduleTick(p,fluid.getType(),fluid.getType().getTickDelay(level));return super.updateShape(s,d,neighbor,level,p,next);
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getClickedFace().getOpposite()).setValue(FLUID,liquid(c.getLevel().getFluidState(c.getClickedPos()).getType()));}
    @Override public void animateTick(BlockState s,Level level,BlockPos p,net.minecraft.util.RandomSource random){
        if(random.nextInt(16)!=0)return;
        var viewer=level.getNearestPlayer(p.getX()+.5,p.getY()+.5,p.getZ()+.5,8,false);
        if(viewer==null)return;
        var d=s.getValue(FACING).getOpposite();var center=net.minecraft.world.phys.Vec3.atCenterOf(p).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(d.getNormal()).scale(.55));
        var color=s.is(LabyrinthContent.LONG_RANGE_PORTAL.get())?new org.joml.Vector3f(.48F,.8F,1):new org.joml.Vector3f(.64F,.36F,.85F);
        level.addParticle(new net.minecraft.core.particles.DustParticleOptions(color,.7F),center.x+(random.nextDouble()-.5)*.6,center.y+(random.nextDouble()-.5)*.6,center.z+(random.nextDouble()-.5)*.6,0,0,0);
    }
}
