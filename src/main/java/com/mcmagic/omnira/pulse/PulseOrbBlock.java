package com.mcmagic.omnira.pulse;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;

public class PulseOrbBlock extends BaseEntityBlock {
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty MOUNT=com.mcmagic.omnira.block.CrystalBallBlock.MOUNT;
    public final boolean resonant;
    private static final java.util.Map<net.minecraft.core.Direction,VoxelShape> SHAPES=new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    static{var shape=Shapes.or(box(6,0,6,10,2,10),box(2,0,2,14,2,14),box(3,5,3,13,15,13));for(var face:net.minecraft.core.Direction.values())SHAPES.put(face,com.mcmagic.omnira.block.OrbMount.shape(face,shape));}
    public PulseOrbBlock(boolean resonant){super(Properties.of().strength(.5F).sound(SoundType.GLASS).noOcclusion().lightLevel(s->15));this.resonant=resonant;registerDefaultState(stateDefinition.any().setValue(MOUNT,net.minecraft.core.Direction.UP));}
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(MOUNT);}
    @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context){return defaultBlockState().setValue(MOUNT,context.getClickedFace());}
    @Override protected BlockState rotate(BlockState state,Rotation rotation){return state.setValue(MOUNT,rotation.rotate(state.getValue(MOUNT)));}
    @Override protected BlockState mirror(BlockState state,Mirror mirror){return state.setValue(MOUNT,mirror.mirror(state.getValue(MOUNT)));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(p->new PulseOrbBlock(resonant));}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext c){return SHAPES.get(s.getValue(MOUNT));}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState s){return PulseContent.ENTITY.get().create(pos,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState s,BlockEntityType<T> type){return level.isClientSide?null:(l,p,state,be)->{if(be instanceof com.mcmagic.omnira.pulse.PulseOrbHost host){if(net.neoforged.fml.ModList.get().isLoaded("create"))com.mcmagic.omnira.compat.create.PulseOrbKinetics.tick(be);else host.orb().tick();}};}
    @Override protected InteractionResult useWithoutItem(BlockState s,Level level,BlockPos pos,Player player,BlockHitResult hit){
        var orb=PulseOrbEntity.from(level.getBlockEntity(pos));if(orb!=null){if(!level.isClientSide)player.openMenu(orb,pos);return InteractionResult.sidedSuccess(level.isClientSide);}return InteractionResult.PASS;
    }
    @Override protected void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moving){
        var orb=PulseOrbEntity.from(l.getBlockEntity(p));if(!s.is(next.getBlock())&&orb!=null){if(l instanceof net.minecraft.server.level.ServerLevel server){PulseOrbGroups.get(server).detach(orb);net.minecraft.world.Containers.dropItemStack(l,p.getX(),p.getY(),p.getZ(),orb.upgrade.local());orb.upgrade.local(net.minecraft.world.item.ItemStack.EMPTY);}PulseNetworks.removed(orb);}
        super.onRemove(s,l,p,next,moving);
    }
}
