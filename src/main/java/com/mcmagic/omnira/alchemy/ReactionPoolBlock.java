package com.mcmagic.omnira.alchemy;

import com.mcmagic.omnira.registry.*;
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
import net.neoforged.neoforge.capabilities.Capabilities;

public class ReactionPoolBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty PART=IntegerProperty.create("part",0,17);
    public ReactionPoolBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(PART,4));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(ReactionPoolBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,PART);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos){return controller(level,pos);}
    public static BlockPos offset(Direction facing,int part){return com.mcmagic.omnira.forging.ForgeLayout.offset(facing,part);}
    public static BlockPos master(BlockPos pos,BlockState state){return pos.subtract(offset(state.getValue(FACING),state.getValue(PART)));}
    public static Direction outward(Direction facing,int part){
        return part==3?facing.getClockWise():part==5?facing.getCounterClockWise():facing;
    }
    public static ReactionPoolBlockEntity controller(Level level,BlockPos pos){
        var state=level.getBlockState(pos);if(!state.is(ModBlocks.REACTION_POOL.get()))return null;
        var center=master(pos,state);if(!level.hasChunkAt(center))return null;
        return level.getBlockEntity(center) instanceof ReactionPoolBlockEntity pool?pool:null;
    }
    public static boolean complete(Level level,BlockPos center,Direction facing){
        for(int i=0;i<18;i++){
            var at=center.offset(offset(facing,i));if(!level.hasChunkAt(at))return false;
            var state=level.getBlockState(at);
            if(!state.is(ModBlocks.REACTION_POOL.get())||state.getValue(PART)!=i||state.getValue(FACING)!=facing)return false;
        }
        return true;
    }
    public static boolean place(Level level,BlockPos center,Direction facing){
        var snapshots=new java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(int i=0;i<18;i++)snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,center.offset(offset(facing,i))));
        for(int i=0;i<18;i++)level.setBlock(center.offset(offset(facing,i)),ModBlocks.REACTION_POOL.get().defaultBlockState().setValue(FACING,facing).setValue(PART,i),18);
        if(complete(level,center,facing)&&controller(level,center)!=null){
            for(var snapshot:snapshots)level.updateNeighborsAt(snapshot.getPos(),ModBlocks.REACTION_POOL.get());return true;
        }
        for(var snapshot:snapshots)snapshot.restore(18);return false;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        int part=state.getValue(PART);
        return net.neoforged.fml.ModList.get().isLoaded("create")&&(part==3||part==5)?
                com.mcmagic.omnira.compat.CreateReactionPool.entity(pos,state):new ReactionPoolBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        if(type!=ModBlockEntityTypes.REACTION_POOL.get())return null;
        return (l,p,s,be)->{if(be instanceof ReactionPoolBlockEntity pool)ReactionPoolBlockEntity.tick(l,p,s,pool);
            else if(!l.isClientSide&&net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.CreateReactionPool.tick(be);};
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return ReactionPoolGeometry.shape(state.getValue(FACING),state.getValue(PART));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack,BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        var pool=controller(level,pos);if(pool==null)return ItemInteractionResult.FAIL;
        if(isPanelHit(state,pos,hit))return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(stack.is(ModItems.RESONANCE_CORE.get())&&hit.getDirection()==Direction.UP){
            var top=pos.atY(pool.getBlockPos().getY()+1);
            var at=new net.minecraft.world.phys.Vec3(hit.getLocation().x,top.getY()+1,hit.getLocation().z);
            var placement=new net.minecraft.world.item.context.UseOnContext(player,hand,new BlockHitResult(at,Direction.UP,top,false));
            return stack.useOn(placement).consumesAction()?ItemInteractionResult.SUCCESS:ItemInteractionResult.FAIL;
        }
        if(net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(stack).isPresent()){
            if(!level.isClientSide)net.neoforged.neoforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,pool.manualFluid);
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!isPanelHit(state,pos,hit))return InteractionResult.PASS;
        var pool=controller(level,pos);if(pool==null)return InteractionResult.FAIL;
        if(!level.isClientSide&&!player.isSpectator())player.openMenu(pool,pool.getBlockPos());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    public static boolean isPanelHit(BlockState state,BlockPos pos,BlockHitResult hit){
        var center=master(pos,state);
        return ReactionPoolGeometry.panelHit(state.getValue(FACING),hit.getLocation().subtract(center.getX(),center.getY(),center.getZ()));
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative())destroy(level,pos,state);
        return super.playerWillDestroy(level,pos,state,player);
    }
    private static void destroy(Level level,BlockPos pos,BlockState state){
        var center=master(pos,state);var pool=controller(level,pos);if(pool!=null)pool.dropContents();
        for(int i=0;i<18;i++){var at=center.offset(offset(state.getValue(FACING),i));
            var s=level.getBlockState(at);if(s.is(ModBlocks.REACTION_POOL.get())&&s.getValue(PART)==i){level.removeBlockEntity(at);level.setBlock(at,Blocks.AIR.defaultBlockState(),18);}}
    }
    @Override public BlockPos bottleOrigin(BlockPos pos,BlockState state){return master(pos,state);}
    @Override public java.util.Set<BlockPos> bottleParts(Level level,BlockPos pos,BlockState state){
        var center=master(pos,state);if(!complete(level,center,state.getValue(FACING)))return java.util.Set.of();
        var parts=new java.util.HashSet<BlockPos>();for(int i=0;i<18;i++)parts.add(center.offset(offset(state.getValue(FACING),i)));return parts;
    }
    public static net.neoforged.neoforge.items.IItemHandler itemPort(BlockEntity entity,Direction side){
        var pool=controller(entity.getLevel(),entity.getBlockPos());if(pool==null||!pool.complete())return null;
        int part=entity.getBlockState().getValue(PART);var facing=entity.getBlockState().getValue(FACING);
        if(part>=9&&side==Direction.UP)return pool.nodeItems;
        if(part==6&&side==outward(facing,part))return pool.itemOutput;
        return pool.itemInput;
    }
    public static net.neoforged.neoforge.fluids.capability.IFluidHandler fluidPort(BlockEntity entity,Direction side){
        var pool=controller(entity.getLevel(),entity.getBlockPos());if(pool==null||!pool.complete())return null;
        int part=entity.getBlockState().getValue(PART);var facing=entity.getBlockState().getValue(FACING);
        if(part>=9&&side==Direction.UP)return pool.nodeFluids;
        if(part==8&&side==outward(facing,part))return pool.fluidOutput;
        return pool.fluidInput;
    }
    public static net.neoforged.neoforge.energy.IEnergyStorage energyPort(BlockEntity entity,Direction side){
        int part=entity.getBlockState().getValue(PART);if(part!=3&&part!=5)return null;
        if(side!=outward(entity.getBlockState().getValue(FACING),part))return null;
        var pool=controller(entity.getLevel(),entity.getBlockPos());return pool!=null&&pool.complete()?pool.energyInput:null;
    }
    public static final class PlacementItem extends BlockItem {
        public PlacementItem(Properties properties){super(ModBlocks.REACTION_POOL.get(),properties);}
        @Override public InteractionResult place(BlockPlaceContext context){
            var player=context.getPlayer();var level=context.getLevel();if(player==null||!player.isCreative()||!context.canPlace())return InteractionResult.FAIL;
            var center=context.getClickedPos();var facing=context.getHorizontalDirection();
            for(int i=0;i<18;i++){var at=center.offset(offset(facing,i));
                if(!level.hasChunkAt(at)||level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at)||!level.mayInteract(player,at)
                        ||!player.mayUseItemAt(at,context.getClickedFace(),context.getItemInHand())||!level.getBlockState(at).canBeReplaced()||level.getBlockEntity(at)!=null)return InteractionResult.FAIL;
                var state=getBlock().defaultBlockState().setValue(FACING,facing).setValue(PART,i);
                if(!level.isUnobstructed(state,at,CollisionContext.empty()))return InteractionResult.FAIL;
            }
            if(level.isClientSide)return InteractionResult.SUCCESS;
            return ReactionPoolBlock.place(level,center,facing)?InteractionResult.CONSUME:InteractionResult.FAIL;
        }
    }
}
