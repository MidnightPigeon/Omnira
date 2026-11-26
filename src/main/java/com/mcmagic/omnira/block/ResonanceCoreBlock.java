package com.mcmagic.omnira.block;

import com.mcmagic.omnira.block.entity.ResonanceCoreBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.*;

public final class ResonanceCoreBlock extends BaseEntityBlock {
    public static final IntegerProperty MODE=IntegerProperty.create("mode",0,7);
    public static final MapCodec<ResonanceCoreBlock> CODEC=simpleCodec(ResonanceCoreBlock::new);
    private static final net.minecraft.core.particles.DustParticleOptions[] MOTES=new net.minecraft.core.particles.DustParticleOptions[MODE.getPossibleValues().size()];
    public ResonanceCoreBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(MODE,0));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(MODE);}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return box(5,3,5,11,11,11);}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ResonanceCoreBlockEntity(pos,state);}
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock())&&level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity node&&!level.isClientSide)
            com.mcmagic.omnira.storage.NodeTransport.unpair(node);
        if(!state.is(replacement.getBlock())&&level instanceof net.minecraft.server.level.ServerLevel server
                &&level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core&&core.terminal!=null)
            com.mcmagic.omnira.item.ResonanceLinks.get(server).destroyed(core.identity);
        super.onRemove(state,level,pos,replacement,moving);
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof ResonanceCoreBlockEntity core){
            var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if(data!=null)core.pendingDelivery=com.mcmagic.omnira.storage.FluidDelivery.read(data.copyTag(),level.registryAccess());
            core.owner=placer==null?null:placer.getUUID();core.setChanged();core.refresh();
        }
    }
    @Override protected java.util.List<ItemStack> getDrops(BlockState state,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var drops=super.getDrops(state,builder);
        var be=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(be instanceof ResonanceCoreBlockEntity core && !core.pendingDelivery.isEmpty()){
            for(var stack:drops)if(stack.is(asItem())){
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,stack,
                        tag->com.mcmagic.omnira.storage.FluidDelivery.write(tag,builder.getLevel().registryAccess(),core.pendingDelivery));
                break;
            }
        }
        return drops;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return level.isClientSide?null:(l,p,s,be)->{
            if(be instanceof ResonanceCoreBlockEntity core&&com.mcmagic.omnira.storage.NodeSchedule.due(l.getGameTime(),p,5)){
                if(com.mcmagic.omnira.storage.NodeSchedule.due(l.getGameTime(),p,20))core.refresh();
                com.mcmagic.omnira.storage.NodeTransport.tick(core);
            }
        };
    }
    @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random) {
        int color=switch(state.getValue(MODE)){case 1->0xF5A4B9;case 2->0xA2EDBB;case 3->0x7BBFFF;case 4->0xFFE58A;case 5->0x16151E;case 6,7->0xACB0B5;default->0xC099F1;};
        int mode=state.getValue(MODE);var dust=MOTES[mode];
        if(dust==null)MOTES[mode]=dust=new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F),.65F);
        for(int i=0;i<5;i++) {
            double angle=(level.getGameTime()+i*12)*.14,r=.22+random.nextDouble()*.16;
            level.addParticle(dust,pos.getX()+.5+Math.cos(angle)*r,pos.getY()+.45+Math.sin(angle*1.7)*.3,pos.getZ()+.5+Math.sin(angle)*r,0,.01,0);
        }
    }
}
