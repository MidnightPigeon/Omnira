package com.mcmagic.omnira.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Static, orientable crystal masonry; no ticking block entity. */
public final class CrystalColumnBlock extends RotatedPillarBlock {
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty REVERSED=net.minecraft.world.level.block.state.properties.BooleanProperty.create("reversed");
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty MANUAL=net.minecraft.world.level.block.state.properties.BooleanProperty.create("manual");
    public static final MapCodec<CrystalColumnBlock> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
            propertiesCodec(),Codec.intRange(0,2).fieldOf("part").forGetter(b->b.part)).apply(i,CrystalColumnBlock::new));
    private final int part;
    private final VoxelShape[] shapes=new VoxelShape[6];
    public CrystalColumnBlock(Properties p,int part) {
        super(p);this.part=part;registerDefaultState(defaultBlockState().setValue(REVERSED,false).setValue(MANUAL,false));
        for(var direction:Direction.values()) {
            var axis=direction.getAxis();
            VoxelShape shape=Shapes.empty();
            // Collision approximates the eight-sided exterior without a full-cube hitbox.
            for(int y=0;y<16;y++) {
                double r=part==1 && y<3 || part==2 && y>=13?7:part!=0 && (part==1?y<5:y>=11)?6:5;
                {
                    double yy=direction.getAxisDirection()==Direction.AxisDirection.NEGATIVE?15-y:y;
                    double[] a={8-r,yy,8-r},b={8+r,yy+1,8+r};
                    if(axis==Direction.Axis.X){double t=a[0];a[0]=a[1];a[1]=t;t=b[0];b[0]=b[1];b[1]=t;}
                    if(axis==Direction.Axis.Z){double t=a[2];a[2]=a[1];a[1]=t;t=b[2];b[2]=b[1];b[1]=t;}
                    shape=Shapes.or(shape,box(a[0],a[1],a[2],b[0],b[1],b[2]));
                }
            }
            shapes[direction.ordinal()]=shape.optimize();
        }
    }
    @Override public MapCodec<? extends RotatedPillarBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder) {
        super.createBlockStateDefinition(builder);builder.add(REVERSED,MANUAL);
    }
    public static Direction tipDirection(BlockState s) {return Direction.fromAxisAndDirection(s.getValue(AXIS),s.getValue(REVERSED)?Direction.AxisDirection.NEGATIVE:Direction.AxisDirection.POSITIVE);}
    private static BlockState oriented(BlockState state,Direction tip) {return state.setValue(AXIS,tip.getAxis()).setValue(REVERSED,tip.getAxisDirection()==Direction.AxisDirection.NEGATIVE);}
    private static boolean shaftAt(BlockGetter level,BlockPos pos,Direction direction) {
        var other=level.getBlockState(pos.relative(direction));
        return other.getBlock() instanceof CrystalColumnBlock c && c.part==0 && other.getValue(AXIS)==direction.getAxis();
    }
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        if(part==0)return super.getStateForPlacement(context);
        var state=com.mcmagic.omnira.registry.DreamContent.CRYSTAL_COLUMN_BASE.get().defaultBlockState();
        var support=context.getClickedFace().getOpposite();
        if(shaftAt(context.getLevel(),context.getClickedPos(),support))return oriented(state,support);
        for(var direction:context.getNearestLookingDirections())
            if(shaftAt(context.getLevel(),context.getClickedPos(),direction))return oriented(state,direction);
        return oriented(state,context.getClickedFace());
    }
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos neighborPos) {
        if(part==1 && !state.getValue(MANUAL) && !shaftAt(level,pos,tipDirection(state)) && shaftAt(level,pos,direction))return oriented(state,direction);
        return super.updateShape(state,direction,neighbor,level,pos,neighborPos);
    }
    @Override protected BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation) {return oriented(state,rotation.rotate(tipDirection(state)));}
    @Override protected BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror) {return oriented(state,mirror.mirror(tipDirection(state)));}
    public static BlockState wrenchTurn(BlockState state) {
        Direction[] cycle={Direction.UP,Direction.NORTH,Direction.EAST,Direction.DOWN,Direction.SOUTH,Direction.WEST};
        var tip=tipDirection(state);var next=tip.getOpposite();
        for(int i=0;i<cycle.length;i++)if(cycle[i]==tip){next=cycle[(i+1)%cycle.length];break;}
        return oriented(state,next).setValue(MANUAL,true);
    }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,
            net.minecraft.world.level.Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit) {
        var wrench=net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("create","wrench"));
        if(!stack.is(wrench) && !net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(wrench.location()))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(!player.mayBuild() || !level.mayInteract(player,pos))return net.minecraft.world.ItemInteractionResult.FAIL;
        if(!level.isClientSide) {
            if(part==2)state=com.mcmagic.omnira.registry.DreamContent.CRYSTAL_COLUMN_BASE.get().defaultBlockState().setValue(AXIS,state.getValue(AXIS)).setValue(REVERSED,!state.getValue(REVERSED));
            level.setBlockAndUpdate(pos,wrenchTurn(state));
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_HIT,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1.2F);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return shapes[tipDirection(s).ordinal()];}
    @Override protected boolean skipRendering(BlockState s,BlockState adjacent,Direction direction) {
        return adjacent.getBlock() instanceof CrystalColumnBlock && adjacent.getValue(AXIS)==s.getValue(AXIS)
                && direction.getAxis()==s.getValue(AXIS) || super.skipRendering(s,adjacent,direction);
    }
}
