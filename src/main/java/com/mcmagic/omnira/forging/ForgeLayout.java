package com.mcmagic.omnira.forging;

import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.block.entity.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Authored sample center (3,-60,-23); local -Z is front, +X is right. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class ForgeLayout {
    @net.neoforged.bus.api.SubscribeEvent
    public static void preventMining(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event){
        if(!event.getEntity().isCreative() && event.getLevel().getBlockState(event.getPos()).is(ModBlocks.ADVANCED_FORGE.get()))event.setCanceled(true);
    }
    public static void destroyCreative(Level level,BlockPos clicked,BlockState state,Player player){
        if(level.isClientSide || !player.isCreative())return;
        var center=master(clicked,state);var facing=state.getValue(AdvancedForgeBlock.FACING);
        // Remove the controller first so legacy recovery cannot recreate a deliberately destroyed machine.
        var forge=level.getBlockEntity(center) instanceof AdvancedForgeBlockEntity f?f:null;
        var parts=new java.util.ArrayList<BlockPos>();
        for(int i=0;i<18;i++){
            var pos=center.offset(offset(facing,i));var part=level.getBlockState(pos);
            if(part.is(ModBlocks.ADVANCED_FORGE.get()) && part.getValue(AdvancedForgeBlock.PART)==i
                    && part.getValue(AdvancedForgeBlock.FACING)==facing)parts.add(pos);
        }
        for(var pos:parts)level.removeBlockEntity(pos);
        for(var pos:parts)level.setBlock(pos,Blocks.AIR.defaultBlockState(),18);
        for(var pos:parts)level.updateNeighborsAt(pos,Blocks.AIR);
        if(forge!=null)forge.dropContents();
    }
    /** Recover old incomplete assemblies without replacing occupied cells or the controller. */
    public static boolean restoreMissingParts(Level level,BlockPos center,BlockState controller){
        if(level.isClientSide || !controller.is(ModBlocks.ADVANCED_FORGE.get())
                || controller.getValue(AdvancedForgeBlock.PART)!=4
                || !(level.getBlockEntity(center) instanceof AdvancedForgeBlockEntity))return false;
        var facing=controller.getValue(AdvancedForgeBlock.FACING);
        var missing=new java.util.ArrayList<BlockPos>();
        for(int part=0;part<18;part++){
            var pos=center.offset(offset(facing,part));
            if(!level.hasChunkAt(pos))return false;
            var state=level.getBlockState(pos);
            if(state.isAir())missing.add(pos);
            else if(!state.is(ModBlocks.ADVANCED_FORGE.get()) || state.getValue(AdvancedForgeBlock.PART)!=part
                    || state.getValue(AdvancedForgeBlock.FACING)!=facing)return false;
        }
        for(int part=0;part<18;part++){
            var pos=center.offset(offset(facing,part));
            if(missing.contains(pos))level.setBlock(pos,controller.setValue(AdvancedForgeBlock.PART,part),2);
        }
        for(var pos:missing)level.updateNeighborsAt(pos,ModBlocks.ADVANCED_FORGE.get());
        return true;
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void activate(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event){
        if(!event.getItemStack().is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()) || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get()))return;
        boolean formed=form(event.getLevel(),event.getPos(),event.getEntity(),event.getItemStack());
        event.setCancellationResult(formed?InteractionResult.sidedSuccess(event.getLevel().isClientSide):InteractionResult.FAIL);event.setCanceled(true);
        if(!formed && !event.getLevel().isClientSide)event.getEntity().displayClientMessage(net.minecraft.network.chat.Component.translatable("message.omnira.forge_structure"),true);
    }
    /** Left / middle / right as viewed from the entrance (local +X is screen-left). */
    public static final int[][] NODES={{1,0},{0,1},{-1,0}};
    public static final int[][] TOOLS={{-1,-1},{1,-1},{1,1},{-1,1}};
    public static Block corner(int index){return switch(index){case 0->ModBlocks.ADVANCED_CONDENSATION_TABLE.get();case 1->ModBlocks.ANALYSIS_ARTISAN_TABLE.get();case 2->ModBlocks.CRYSTAL_PROCESSING_TABLE.get();default->ModBlocks.ARCANE_ASSEMBLY_TABLE.get();};}
    public static BlockPos offset(Direction facing,int x,int y,int z){
        var front=facing.getOpposite();var right=front.getClockWise();
        return new BlockPos(right.getStepX()*x-front.getStepX()*z,y,right.getStepZ()*x-front.getStepZ()*z);
    }
    public static BlockPos offset(Direction facing,int part){return offset(facing,part%3-1,part/9,(part%9)/3-1);}
    public static BlockPos master(BlockPos pos,BlockState state){return pos.subtract(offset(state.getValue(AdvancedForgeBlock.FACING),state.getValue(AdvancedForgeBlock.PART)));}
    public static java.util.Set<BlockPos> formedParts(Level level,BlockPos pos,BlockState state){
        var center=master(pos,state);var facing=state.getValue(AdvancedForgeBlock.FACING);
        java.util.Set<BlockPos> parts=new java.util.HashSet<>(18);
        for(int i=0;i<18;i++){
            var at=center.offset(offset(facing,i));
            if(!level.hasChunkAt(at))return java.util.Set.of();
            var s=level.getBlockState(at);
            if(!s.is(ModBlocks.ADVANCED_FORGE.get()) || s.getValue(AdvancedForgeBlock.PART)!=i || s.getValue(AdvancedForgeBlock.FACING)!=facing)return java.util.Set.of();
            parts.add(at);
        }
        return parts;
    }
    public static Block required(int x,int y,int z){
        if(y==0)return x==0 && z==0?ModBlocks.ARCANE_ASSEMBLY_TABLE.get():ModBlocks.CRYSTAL_CASING.get();
        for(int[] node:NODES)if(node[0]==x && node[1]==z)return ModBlocks.MANA_ENGINE.get();
        for(int i=0;i<4;i++)if(TOOLS[i][0]==x && TOOLS[i][1]==z)return corner(i);
        return Blocks.AIR;
    }
    public static boolean form(Level level,BlockPos center,Player player,ItemStack crystal){
        if(!level.getBlockState(center).is(ModBlocks.ARCANE_ASSEMBLY_TABLE.get()) || player.isSpectator()
                || !crystal.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()))return false;
        for(Direction facing:Direction.Plane.HORIZONTAL){
            if(!matches(level,center,player,crystal,facing))continue;
            if(level.isClientSide)return true;
            if(!assemble(level,center,facing))return false;
            crystal.shrink(1);
            if(level.getBlockEntity(center) instanceof AdvancedForgeBlockEntity forge)forge.beginFormation();
            level.playSound(null,center,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,1,.7F);
            return true;
        }
        return false;
    }
    public static boolean assemble(Level level,BlockPos center,Direction facing){
        if(level.isClientSide)return false;
        var snapshots=new java.util.ArrayList<net.neoforged.neoforge.common.util.BlockSnapshot>();
        for(int part=0;part<18;part++)snapshots.add(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(),level,center.offset(offset(facing,part))));
        for(int part=0;part<18;part++)level.setBlock(center.offset(offset(facing,part)),ModBlocks.ADVANCED_FORGE.get().defaultBlockState()
                .setValue(AdvancedForgeBlock.FACING,facing).setValue(AdvancedForgeBlock.PART,part),18);
        for(int part=0;part<18;part++)level.updateNeighborsAt(center.offset(offset(facing,part)),ModBlocks.ADVANCED_FORGE.get());
        var controller=level.getBlockState(center);
        if(controller.is(ModBlocks.ADVANCED_FORGE.get()) && formedParts(level,center,controller).size()==18
                && level.getBlockEntity(center) instanceof AdvancedForgeBlockEntity)return true;
        for(var snapshot:snapshots)snapshot.restore(18);
        return false;
    }
    private static boolean matches(Level level,BlockPos center,Player player,ItemStack crystal,Direction facing){
        for(int part=0;part<18;part++){
            var pos=center.offset(offset(facing,part));
            if(!level.hasChunkAt(pos) || !level.mayInteract(player,pos) || !player.mayUseItemAt(pos,Direction.UP,crystal))return false;
            var state=level.getBlockState(pos);var expected=required(part%3-1,part/9,(part%9)/3-1);
            if(expected==Blocks.AIR?!state.isAir():!state.is(expected))return false;
            var be=level.getBlockEntity(pos);
            if(be instanceof Container c && !c.isEmpty())return false;
            if(be instanceof AssemblyAccess a && !a.assembly().isEmpty())return false;
            if(be instanceof ManaEngineAccess engine && !engine.engineState().inventory.isEmpty())return false;
        }
        return true;
    }
    private ForgeLayout(){}
}
