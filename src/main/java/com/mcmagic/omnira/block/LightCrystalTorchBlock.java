package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Anchor and tip occupy two cells extending away from any sturdy supporting face. */
public final class LightCrystalTorchBlock extends Block {
    public static final EnumProperty<DoubleBlockHalf> HALF=BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final DirectionProperty MOUNT=BlockStateProperties.FACING;
    public static final MapCodec<LightCrystalTorchBlock> CODEC=simpleCodec(LightCrystalTorchBlock::new);
    private static final VoxelShape[][] SHAPES=new VoxelShape[6][2];
    static {
        var anchor=Shapes.or(box(4,0,4,12,2,12),box(7,2,7,9,16,9));
        var tip=box(5.5,0,5.5,10.5,9.6,10.5);
        for(var direction:Direction.values()){
            SHAPES[direction.ordinal()][0]=OrbMount.shape(direction,anchor);
            SHAPES[direction.ordinal()][1]=OrbMount.shape(direction,tip);
        }
    }
    public LightCrystalTorchBlock(Properties properties) {
        super(properties);registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER).setValue(MOUNT,Direction.UP));
    }
    @Override public MapCodec<LightCrystalTorchBlock> codec() {return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(HALF,MOUNT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var pos=context.getClickedPos();var level=context.getLevel();var mount=context.getClickedFace();var tip=pos.relative(mount);
        var state=defaultBlockState().setValue(MOUNT,mount);
        return level.isInWorldBounds(tip)&&level.getWorldBorder().isWithinBounds(tip)
                &&level.getBlockState(tip).canBeReplaced(context)&&state.canSurvive(level,pos)?state:null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        level.setBlock(pos.relative(state.getValue(MOUNT)),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }
    private boolean matching(BlockState state,BlockState other){
        return other.is(this)&&other.getValue(MOUNT)==state.getValue(MOUNT)&&other.getValue(HALF)!=state.getValue(HALF);
    }
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){
        var support=pos.relative(state.getValue(MOUNT).getOpposite());var below=level.getBlockState(support);
        return state.getValue(HALF)==DoubleBlockHalf.UPPER?matching(state,below):below.isFaceSturdy(level,support,state.getValue(MOUNT));
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbour,LevelAccessor level,BlockPos pos,BlockPos neighbourPos){
        var mount=state.getValue(MOUNT);
        var pair=state.getValue(HALF)==DoubleBlockHalf.LOWER?mount:mount.getOpposite();
        if(direction==pair&&!matching(state,neighbour)||direction==mount.getOpposite()&&!state.canSurvive(level,pos))return Blocks.AIR.defaultBlockState();
        return state;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&state.getValue(HALF)==DoubleBlockHalf.UPPER){
            var anchor=pos.relative(state.getValue(MOUNT).getOpposite());
            if(matching(state,level.getBlockState(anchor)))level.setBlock(anchor,Blocks.AIR.defaultBlockState(),35);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(MOUNT,rotation.rotate(state.getValue(MOUNT)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return rotate(state,mirror.getRotation(state.getValue(MOUNT)));}
    @Override protected long getSeed(BlockState state,BlockPos pos){
        var anchor=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.relative(state.getValue(MOUNT).getOpposite()):pos;
        return Mth.getSeed(anchor);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return SHAPES[state.getValue(MOUNT).ordinal()][state.getValue(HALF)==DoubleBlockHalf.UPPER?1:0];
    }
}
