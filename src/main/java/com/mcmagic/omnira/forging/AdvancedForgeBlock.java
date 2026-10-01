package com.mcmagic.omnira.forging;

import com.mcmagic.omnira.registry.*;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class AdvancedForgeBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public BlockPos bottleOrigin(BlockPos pos,BlockState state){return ForgeLayout.master(pos,state);}
    public java.util.Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state){return ForgeLayout.formedParts(level,pos,state);}
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,17);
    public static final MapCodec<AdvancedForgeBlock> CODEC=simpleCodec(AdvancedForgeBlock::new);
    public AdvancedForgeBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.SOUTH).setValue(PART,4));}
    protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
    public BlockEntity newBlockEntity(BlockPos p,BlockState s){return s.getValue(PART)==4?new AdvancedForgeBlockEntity(p,s):null;}
    protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        var result=super.playerWillDestroy(level,pos,state,player);
        ForgeLayout.destroyCreative(level,pos,state,player);
        return result;
    }
    protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){
        int part=s.getValue(PART);
        if(part==4)return box(0,0,0,16,2,16);
        if(part==13)return box(1,0,1,15,2,15);
        if(part==1){
            var panel=switch(s.getValue(FACING)){
                case SOUTH -> box(3,5,11,13,15,14);
                case NORTH -> box(3,5,2,13,15,5);
                case EAST -> box(11,5,3,14,15,13);
                default -> box(2,5,3,5,15,13);
            };
            return Shapes.or(box(0,0,0,16,2,16),panel);
        }
        if(part<9)return box(0,0,0,16,2,16);
        if(part==10)return Shapes.empty();
        return box(3,0,3,13,10,13);
    }
    private AdvancedForgeBlockEntity controller(Level level,BlockPos pos,BlockState state){return level.getBlockEntity(ForgeLayout.master(pos,state)) instanceof AdvancedForgeBlockEntity f?f:null;}
    protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return interact(state,level,pos,player,hand,hit)?ItemInteractionResult.sidedSuccess(level.isClientSide):ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        return interact(state,level,pos,player,InteractionHand.MAIN_HAND,hit)?InteractionResult.sidedSuccess(level.isClientSide):InteractionResult.PASS;
    }
    private boolean interact(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var forge=controller(level,pos,state);if(forge==null || player.isSpectator())return false;
        int part=state.getValue(PART),x=part%3-1,z=(part%9)/3-1;
        for(int i=0;part>=9 && i<3;i++)if(ForgeLayout.NODES[i][0]==x && ForgeLayout.NODES[i][1]==z){
            if(!level.isClientSide)forge.node(player,hand,6+i);return true;
        }
        if(part==1 && hit.getLocation().y-pos.getY()>=5.0/16){
            if(!level.isClientSide)forge.start(player);
            return true;
        }
        if(part!=4 && part!=13)return false;
        if(!level.isClientSide){
            if(!forge.collect(player))player.openMenu(forge,forge.getBlockPos());
        }
        return true;
    }
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){
        return level.isClientSide?null:createTickerHelper(type,ModBlockEntityTypes.ADVANCED_FORGE.get(),(l,p,b,f)->f.tick());
    }
}
