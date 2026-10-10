package com.mcmagic.omnira.engine;

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

public class SpacetimeEngineBlock extends BaseEntityBlock implements com.mcmagic.omnira.item.bottle.BottleMultiblock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,9);
    public static final Direction[] OUTPUTS={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    public SpacetimeEngineBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(PART,9));}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return simpleCodec(SpacetimeEngineBlock::new);}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(PART);}
    @Override protected RenderShape getRenderShape(BlockState s){return RenderShape.ENTITYBLOCK_ANIMATED;}
    public static BlockPos offset(int part){return part==9?new BlockPos(0,1,0):new BlockPos(part%3-1,0,part/3-1);}
    public static int side(int part){return switch(part){case 1->0;case 5->1;case 7->2;case 3->3;default->-1;};}
    public static BlockPos origin(BlockPos p,BlockState s){return p.subtract(offset(s.getValue(PART)));}
    public static SpacetimeEngineController controller(Level l,BlockPos p){
        if(l==null||!l.hasChunkAt(p))return null;var s=l.getBlockState(p);
        if(!s.is(SpacetimeEngineContent.BLOCK.get()))return null;
        var at=origin(p,s).above();
        return l.hasChunkAt(at)&&l.getBlockEntity(at) instanceof SpacetimeEngineController be?be:null;
    }
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){
        return side(s.getValue(PART))>=0&&net.neoforged.fml.ModList.get().isLoaded("create")
                ?com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics.entity(p,s):new SpacetimeEngineController(p,s);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){
        if(s.getValue(PART)==9)return (level,pos,state,be)->{if(be instanceof SpacetimeEngineController engine)engine.energy.tick();};
        return side(s.getValue(PART))>=0&&net.neoforged.fml.ModList.get().isLoaded("create")
                ?(level,pos,state,be)->com.mcmagic.omnira.compat.create.SpacetimeEngineKinetics.tick(be):null;
    }
    public static net.neoforged.neoforge.energy.IEnergyStorage energyPort(BlockEntity entity,Direction face){
        int side=side(entity.getBlockState().getValue(PART));
        if(side<0||face!=OUTPUTS[side])return null;
        var engine=controller(entity.getLevel(),entity.getBlockPos());
        return engine==null?null:engine.energy.port(side);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(s.getValue(PART)==9)return box(2,0,2,14,14,14);
        if(side(s.getValue(PART))>=0)return Shapes.or(box(1,0,1,15,13,15),side(s.getValue(PART))%2==0?box(6,6,0,10,10,16):box(0,6,6,16,10,10));
        return box(1,0,1,15,s.getValue(PART)==4?16:10,15);
    }
    @Override protected InteractionResult useWithoutItem(BlockState s,Level l,BlockPos p,Player player,BlockHitResult hit){
        if(s.getValue(PART)!=9)return InteractionResult.PASS;
        var be=controller(l,p);if(be==null||!be.complete())return InteractionResult.FAIL;
        if(!l.isClientSide)player.openMenu(be,p);return InteractionResult.sidedSuccess(l.isClientSide);
    }
    @Override protected MenuProvider getMenuProvider(BlockState s,Level l,BlockPos p){return s.getValue(PART)==9?controller(l,p):null;}
    @Override public BlockState playerWillDestroy(Level l,BlockPos p,BlockState s,Player player){
        if(!l.isClientSide&&player.isCreative()){
            var be=controller(l,p);if(be!=null)be.dropUpgrades();
            var root=origin(p,s);
            for(int i=0;i<10;i++){var at=root.offset(offset(i));if(l.getBlockState(at).is(this)){l.removeBlockEntity(at);l.setBlock(at,Blocks.AIR.defaultBlockState(),18);}}
            for(int i=0;i<10;i++)l.updateNeighborsAt(root.offset(offset(i)),Blocks.AIR);
        }
        return super.playerWillDestroy(l,p,s,player);
    }
    @Override public BlockPos bottleOrigin(BlockPos p,BlockState s){return origin(p,s);}
    @Override public java.util.Set<BlockPos> bottleParts(Level l,BlockPos p,BlockState s){
        var be=controller(l,p);if(be==null||!be.complete())return java.util.Set.of();
        var result=new java.util.LinkedHashSet<BlockPos>();var root=origin(p,s);
        for(int i=0;i<10;i++)result.add(root.offset(offset(i)));return result;
    }
    public static final class PlacementItem extends BlockItem {
        public PlacementItem(Properties p){super(SpacetimeEngineContent.BLOCK.get(),p);}
        @Override public InteractionResult place(BlockPlaceContext c){
            var player=c.getPlayer();if(player==null||!player.isCreative()||!c.canPlace())return InteractionResult.FAIL;
            var root=c.getClickedPos();var l=c.getLevel();
            for(int i=0;i<10;i++){var p=root.offset(offset(i));if(!SpacetimeEngineStructure.allowed(l,p,player,c.getItemInHand())
                    ||!l.getBlockState(p).canBeReplaced()||l.getBlockEntity(p)!=null
                    ||!l.isUnobstructed(getBlock().defaultBlockState().setValue(PART,i),p,CollisionContext.empty()))return InteractionResult.FAIL;}
            if(l.isClientSide)return InteractionResult.SUCCESS;
            if(!SpacetimeEngineStructure.place(l,root))return InteractionResult.FAIL;
            controller(l,root.above()).setFront(player.getDirection().getOpposite());return InteractionResult.CONSUME;
        }
    }
}
