package com.mcmagic.omnira.alchemy;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

public final class RoughAlchemyCauldronBlock extends BaseEntityBlock {
    public static final MapCodec<RoughAlchemyCauldronBlock> CODEC=simpleCodec(RoughAlchemyCauldronBlock::new);
    private static final VoxelShape SHAPE=shape();
    public RoughAlchemyCauldronBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return SHAPE;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new RoughAlchemyCauldronBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return createTickerHelper(type,com.mcmagic.omnira.registry.ModBlockEntityTypes.ROUGH_ALCHEMY_CAULDRON.get(),RoughAlchemyCauldronBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(level.getBlockEntity(pos) instanceof RoughAlchemyCauldronBlockEntity vessel){
            if(level.isClientSide&&net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent())return ItemInteractionResult.SUCCESS;
            if(!level.isClientSide&&net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,vessel.tank))return ItemInteractionResult.SUCCESS;
            if(net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent())return ItemInteractionResult.FAIL;
            if(!stack.isEmpty()){
                if(!level.isClientSide){var rest=vessel.insert(stack);stack.shrink(stack.getCount()-rest.getCount());}
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide&&level.getBlockEntity(pos) instanceof RoughAlchemyCauldronBlockEntity vessel){
            for(int i=3;i>=0;i--)if(!vessel.items.getStackInSlot(i).isEmpty()){
                var stack=vessel.items.extractItem(i,64,false);
                if(!player.getInventory().add(stack))player.drop(stack,false);break;
            }
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        BlockState original=null;net.minecraft.nbt.CompoundTag data=null;
        if(!state.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof RoughAlchemyCauldronBlockEntity vessel)
            for(int i=0;i<4;i++)net.minecraft.world.Containers.dropItemStack(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,vessel.items.getStackInSlot(i).copy());
        if(!state.is(next.getBlock())&&next.isAir()&&!level.isClientSide
                &&level.getBlockEntity(pos) instanceof RoughAlchemyCauldronBlockEntity vessel&&vessel.reaction()){
            original=vessel.campfire();data=vessel.campfireData();
        }
        super.onRemove(state,level,pos,next,moving);
        if(original!=null){
            level.setBlock(pos,original,3);
            var restored=level.getBlockEntity(pos);
            if(restored!=null&&!data.isEmpty())restored.loadWithComponents(data,level.registryAccess());
        }
    }
    private static VoxelShape shape(){
        VoxelShape result=Shapes.empty();
        for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=0;y<15;y++){
            double dx=Math.abs(x-7.5),dz=Math.abs(z-7.5);
            boolean foot=y<3&&(x>=3&&x<=4||x>=11&&x<=12)&&(z>=3&&z<=4||z>=11&&z<=12);
            double outer=y<7?5.5:y<13?6.5:7.5;
            double inner=outer-1.5;
            boolean outside=dx<=outer&&dz<=outer&&dx+dz<=outer*1.5;
            boolean hollow=dx<inner&&dz<inner&&dx+dz<inner*1.5;
            boolean bowl=y>=3&&outside&&(y<5||!hollow);
            boolean handle=y>=10&&y<13&&(x==0||x==15)&&z>=6&&z<=9;
            if(foot||bowl||handle)result=Shapes.or(result,box(x,y,z,x+1,y+1,z+1));
        }
        return result.optimize();
    }
}
