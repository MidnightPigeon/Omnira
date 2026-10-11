package com.mcmagic.omnira.space.phase;

import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class PhaseControlBlock extends Block {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty INVERTED=BooleanProperty.create("inverted");
    public PhaseControlBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(INVERTED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,INVERTED);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var face=c.getClickedFace();if(!face.getAxis().isHorizontal())return null;
        return defaultBlockState().setValue(FACING,face);
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){
        PhaseNetworks.invalidateControls(level);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this))PhaseNetworks.invalidateControls(level);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public boolean canSurvive(BlockState s,LevelReader level,BlockPos p){return level.getBlockState(p.relative(s.getValue(FACING).getOpposite())).is(PhaseContent.BLOCK.get());}
    @Override public BlockState updateShape(BlockState s,Direction face,BlockState next,LevelAccessor level,BlockPos p,BlockPos other){
        return face==s.getValue(FACING).getOpposite()&&!canSurvive(s,level,p)?Blocks.AIR.defaultBlockState():s;
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return switch(s.getValue(FACING)){
        case NORTH->box(2,2,14,14,14,16);case SOUTH->box(2,2,0,14,14,2);
        case WEST->box(14,2,2,16,14,14);default->box(0,2,2,2,14,14);
    };}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos p,Player player,BlockHitResult hit){
        if(player.isSpectator()||!level.mayInteract(player,p))return InteractionResult.FAIL;
        if(!level.isClientSide){
            var changed=s.cycle(INVERTED);level.setBlock(p,changed,3);
            var group=PhaseNetworks.component((ServerLevel)level,p.relative(s.getValue(FACING).getOpposite()));
            if(group.complete())PhaseNetworks.control((ServerLevel)level,group);
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(changed.getValue(INVERTED)?"message.omnira.phase_inverted":"message.omnira.phase_solid"),true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public void animateTick(BlockState s,Level level,BlockPos p,net.minecraft.util.RandomSource random){PhaseParticles.around(level,p,random);}
}
