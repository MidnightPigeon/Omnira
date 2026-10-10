package com.mcmagic.omnira.engine;

import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class SpacetimeEngineStructure {
    public static Block required(int part){
        if(part==9)return ModBlocks.SPACETIME_SPELL_CORE.get();
        if(part==4)return com.mcmagic.omnira.alchemy.AlchemyMaterials.ENHANCED_CORE.get();
        return SpacetimeEngineBlock.side(part)>=0?ModBlocks.MANA_ENGINE.get():ModBlocks.INFUSED_CRYSTAL_CASING.get();
    }
    public static boolean allowed(Level l,BlockPos p,Player player,ItemStack stack){
        return l.hasChunkAt(p)&&!l.isOutsideBuildHeight(p)&&l.getWorldBorder().isWithinBounds(p)
                &&l.mayInteract(player,p)&&player.mayUseItemAt(p,Direction.UP,stack);
    }
    @net.neoforged.bus.api.SubscribeEvent public static void activate(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e){
        if(e.getEntity().isSpectator()||e.getEntity().isShiftKeyDown()||!e.getItemStack().is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())
                ||!e.getLevel().getBlockState(e.getPos()).is(ModBlocks.SPACETIME_SPELL_CORE.get()))return;
        boolean ok=assemble(e.getLevel(),e.getPos(),e.getEntity(),e.getItemStack());
        e.setCanceled(true);e.setCancellationResult(ok?InteractionResult.sidedSuccess(e.getLevel().isClientSide):InteractionResult.FAIL);
    }
    public static boolean assemble(Level l,BlockPos top,Player player,ItemStack crystal){
        if(player.isSpectator()||!crystal.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))return false;
        var root=top.below();
        for(int i=0;i<10;i++){
            var p=root.offset(SpacetimeEngineBlock.offset(i));
            if(!allowed(l,p,player,crystal)||!l.getBlockState(p).is(required(i)))return false;
            if(l.getBlockEntity(p) instanceof com.mcmagic.omnira.block.entity.ManaEngineAccess engine&&!engine.engineState().inventory.isEmpty())return false;
        }
        if(l.isClientSide)return true;
        if(!place(l,root))return false;
        SpacetimeEngineBlock.controller(l,root.above()).setFront(player.getDirection().getOpposite());
        crystal.shrink(1);return true;
    }
    public static boolean place(Level l,BlockPos root){
        var snapshots=new java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(int i=0;i<10;i++)snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(l.dimension(),l,root.offset(SpacetimeEngineBlock.offset(i))));
        for(int i=0;i<10;i++)if(!l.setBlock(root.offset(SpacetimeEngineBlock.offset(i)),SpacetimeEngineContent.BLOCK.get().defaultBlockState().setValue(SpacetimeEngineBlock.PART,i),18)){
            snapshots.forEach(s->s.restore(18));return false;
        }
        var be=SpacetimeEngineBlock.controller(l,root.above());
        if(be==null||!be.complete()){snapshots.forEach(s->s.restore(18));return false;}
        for(int i=0;i<10;i++)l.updateNeighborsAt(root.offset(SpacetimeEngineBlock.offset(i)),SpacetimeEngineContent.BLOCK.get());
        return true;
    }
    private SpacetimeEngineStructure(){}
}
