package com.mcmagic.omnira.guidance;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class GuidanceOrbBlock extends BaseEntityBlock {
    public static final IntegerProperty STAGE=IntegerProperty.create("stage",0,4);
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public GuidanceOrbBlock(){
        super(Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_LIGHT_BLUE).strength(1.5F).noOcclusion());
        registerDefaultState(stateDefinition.any().setValue(STAGE,0).setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return MapCodec.unit(this);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(STAGE,FACING);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        return l.getBlockEntity(p) instanceof GuidanceOrbEntity orb&&orb.buttonsVisible()
                ?Shapes.or(Shapes.block(),box(4,16,4,12,20,12)):box(1,0,1,15,15,15);
    }
    @Override protected VoxelShape getCollisionShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return box(2,0,2,14,13,14);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new GuidanceOrbEntity(p,s);}
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        interact(l,p,player,stack,interactionFace(p,hit));return ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        interact(l,p,player,ItemStack.EMPTY,interactionFace(p,hit));return InteractionResult.sidedSuccess(l.isClientSide);
    }
    public static Direction interactionFace(BlockPos p,BlockHitResult hit){
        return hit.getLocation().y>=p.getY()+1?Direction.UP:hit.getDirection();
    }
    private void interact(Level l,BlockPos p,Player player,ItemStack stack,Direction face){
        if(!l.isClientSide&&!player.isSpectator()&&l.getBlockEntity(p) instanceof GuidanceOrbEntity orb){
            String target=orb.interact(stack,face);
            if(!target.isEmpty()&&player instanceof net.minecraft.server.level.ServerPlayer server)
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server,new OpenGuidancePayload(target));
        }
    }
}
