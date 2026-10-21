package com.mcmagic.omnira.pulse;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.shapes.*;

public final class PulseWireBlock extends BaseEntityBlock {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    private static final VoxelShape[] OUTLINES=outlines();
    public PulseWireBlock(Properties p){super(p);registerDefaultState(defaultBlockState().setValue(POWERED,false));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(PulseWireBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(POWERED);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        int mask=0;for(var face:Direction.values())if(PulseVisualConnections.wireConnects(l,p,face))mask|=1<<face.ordinal();
        return OUTLINES[mask];
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    private static VoxelShape[] outlines(){
        var shapes=new VoxelShape[64];
        for(int mask=0;mask<shapes.length;mask++){
            var shape=box(7,7,7,9,9,9);
            for(var face:Direction.values())if((mask&(1<<face.ordinal()))!=0)
                shape=Shapes.or(shape,box(face.getStepX()<0?0:6.5,face.getStepY()<0?0:6.5,face.getStepZ()<0?0:6.5,
                        face.getStepX()>0?16:9.5,face.getStepY()>0?16:9.5,face.getStepZ()>0?16:9.5));
            shapes[mask]=shape.optimize();
        }
        return shapes;
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new PulseWireEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:(level,pos,state,be)->{if(be instanceof PulseWireEntity wire)wire.tick();};}
    public static InteractionResult place(UseOnContext c){
        var level=c.getLevel();var pos=c.getClickedPos().relative(c.getClickedFace());var player=c.getPlayer();
        if(player==null||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(player,pos)
                ||!player.mayUseItemAt(pos,c.getClickedFace(),c.getItemInHand())||!level.getBlockState(pos).canBeReplaced()||level.getBlockEntity(pos)!=null)return InteractionResult.PASS;
        for(var axis:Direction.Axis.values()){
            int length=1;for(var d:Direction.values())if(d.getAxis()==axis)for(int i=1;i<=16;i++){var at=pos.relative(d,i);if(!level.hasChunkAt(at)||!level.getBlockState(at).is(PulseContent.WIRE.get()))break;length++;}
            if(length>16)return InteractionResult.FAIL;
        }
        if(!level.isClientSide&&level.setBlockAndUpdate(pos,PulseContent.WIRE.get().defaultBlockState())){c.getItemInHand().consume(1,player);PulseNetworks.changed(level,pos);}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
