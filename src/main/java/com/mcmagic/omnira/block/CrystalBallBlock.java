package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public final class CrystalBallBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty MOUNT=net.minecraft.world.level.block.state.properties.DirectionProperty.create("mount");
    public static final MapCodec<CrystalBallBlock> CODEC=simpleCodec(CrystalBallBlock::new);
    private static final VoxelShape SHAPE=Shapes.or(box(2,0,2,14,3,14),box(2.5,4,2.5,13.5,15.5,13.5));
    private static final java.util.Map<net.minecraft.core.Direction,VoxelShape> SHAPES=new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    static{for(var face:net.minecraft.core.Direction.values())SHAPES.put(face,OrbMount.shape(face,SHAPE));}
    public CrystalBallBlock(Properties properties) {super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH).setValue(MOUNT,net.minecraft.core.Direction.UP));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,MOUNT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite()).setValue(MOUNT,context.getClickedFace());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING))).setValue(MOUNT,rotation.rotate(state.getValue(MOUNT)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.setValue(FACING,mirror.mirror(state.getValue(FACING))).setValue(MOUNT,mirror.mirror(state.getValue(MOUNT)));}
    @Override public MapCodec<CrystalBallBlock> codec() {return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        if(state.is(com.mcmagic.omnira.space.sea.StellarCraftContent.ORB.get()))return new com.mcmagic.omnira.space.sea.StarClusterOrbEntity(pos,state);
        if(state.is(com.mcmagic.omnira.storage.CrystalStorageContent.VOID_ORB.get()))return new com.mcmagic.omnira.block.entity.VoidCrystalBallBlockEntity(pos,state);
        return state.is(com.mcmagic.omnira.registry.ModBlocks.LIQUID_CRYSTAL_BALL.get())||state.is(com.mcmagic.omnira.storage.CrystalStorageContent.FLUID_ORB.get())?new com.mcmagic.omnira.block.entity.LiquidCrystalBallBlockEntity(pos,state):new CrystalBallBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide?null:(l,p,s,be)->{if(be instanceof CrystalBallBlockEntity ball)ball.serverTick();};
    }
    @Override protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder) {
        var drops=super.getDrops(state,builder);
        var be=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(be instanceof CrystalBallBlockEntity networkBall)networkBall.detachNetwork();
        if(be instanceof CrystalBallBlockEntity ball && ball.stabilized())for(var stack:drops)if(stack.is(asItem()))
            stack.set(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA,net.minecraft.world.item.component.CustomData.of(ball.saveWithFullMetadata(builder.getLevel().registryAccess())));
        return drops;
    }
    @Override protected RenderShape getRenderShape(BlockState state) {return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){
        if(!(level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball)||!ball.resonant()||ball.networkStatus==0)return;
        int color=switch(ball.networkStatus){case 1->0x80E9A3;case 2->0x69B8FF;default->0xF07285;};
        var dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),.65F);
        for(int i=0;i<4;i++){double angle=(level.getGameTime()+i*17)*.13;
            var p=OrbMount.point(state.getValue(MOUNT),new net.minecraft.world.phys.Vec3(.5+Math.cos(angle)*.38,.18+Math.sin(angle*1.7)*.1,.5+Math.sin(angle)*.38));
            level.addParticle(dust,pos.getX()+p.x,pos.getY()+p.y,pos.getZ()+p.z,0,.01,0);
        }
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {return SHAPES.get(state.getValue(MOUNT));}
    @Override public void fallOn(Level level,BlockState state,BlockPos pos,net.minecraft.world.entity.Entity entity,float distance) {
        if(!level.isClientSide && distance>.2F && entity instanceof net.minecraft.world.entity.LivingEntity living
                && living.isAlive() && !living.isSpectator() && level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball)
            ball.landedOn(living);
        super.fallOn(level,state,pos,entity,distance);
    }
    @Override protected void tick(BlockState state,net.minecraft.server.level.ServerLevel level,BlockPos pos,net.minecraft.util.RandomSource random) {
        if(level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball) ball.repairCracks();
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball) player.openMenu(ball,pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving) {
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CrystalBallBlockEntity networkBall)networkBall.detachNetwork();
        if(!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof CrystalBallBlockEntity ball && !ball.stabilized() && ball.dropsOnRemoval()) {
            if(!level.isClientSide)ball.unpackLootTable(null);
            Containers.dropContentsOnDestroy(state,next,level,pos);
            for(int i=0;i<3;i++)Block.popResource(level,pos,ball.upgrades.getStackInSlot(i));
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) {return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
}
