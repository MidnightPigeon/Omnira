package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Other rituals retain their own lifetime; genesis owns only an irreversible offering counter. */
public final class RitualEnergyCoreBlockEntity extends BlockEntity {
    public RitualEnergyCoreBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.RITUAL_ENERGY_CORE.get(),pos,state);}
    private int genesisFluid,absorption;
    private float visualFluid,previousVisualFluid;
    private float visualAbsorption,previousVisualAbsorption;
    public float visualFluid(float partial){return previousVisualFluid+(visualFluid-previousVisualFluid)*partial;}
    public float visualAbsorption(float partial){return previousVisualAbsorption+(visualAbsorption-previousVisualAbsorption)*partial;}
    public void tickVisual(){
        previousVisualFluid=visualFluid;visualFluid+=Math.clamp(genesisFluid-visualFluid,-1000F,500F);
        previousVisualAbsorption=visualAbsorption;visualAbsorption+=Math.clamp(absorption()-visualAbsorption,-40F,1F);
    }
    public int genesisFluid(){return genesisFluid;}
    public int absorption(){return Math.max(0,absorption-com.mcmagic.omnira.world.dimension.GenesisRitual.FORMATION_TICKS);}
    public final net.neoforged.neoforge.fluids.capability.IFluidHandler genesisOffering=new net.neoforged.neoforge.fluids.capability.IFluidHandler(){
        @Override public int getTanks(){return 1;}
        @Override public net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank){return net.neoforged.neoforge.fluids.FluidStack.EMPTY;}
        @Override public int getTankCapacity(int tank){return tank==0?com.mcmagic.omnira.world.dimension.GenesisRitual.REQUIRED_FLUID:0;}
        @Override public boolean isFluidValid(int tank,net.neoforged.neoforge.fluids.FluidStack fluid){
            return tank==0 && fluid.is(com.mcmagic.omnira.alchemy.AlchemyMaterials.REFINED.source.get()) && ready();
        }
        @Override public int fill(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){
            if(action.execute())validateGenesis();
            if(!isFluidValid(0,fluid))return 0;
            int accepted=Math.min(fluid.getAmount(),com.mcmagic.omnira.world.dimension.GenesisRitual.REQUIRED_FLUID-genesisFluid);
            if(action.execute() && accepted>0){genesisFluid+=accepted;sync();}
            return accepted;
        }
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(int max,FluidAction action){return net.neoforged.neoforge.fluids.FluidStack.EMPTY;}
        @Override public net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack fluid,FluidAction action){return net.neoforged.neoforge.fluids.FluidStack.EMPTY;}
    };
    private boolean ready(){
        if(level==null || isRemoved() || level.isClientSide)return false;
        return genesisFluid<com.mcmagic.omnira.world.dimension.GenesisRitual.REQUIRED_FLUID
                && com.mcmagic.omnira.world.dimension.GenesisRitual.pedestals(level,worldPosition).size()==8;
    }
    public void validateGenesis(){
        if(level!=null && !level.isClientSide && genesisFluid>0
                && com.mcmagic.omnira.world.dimension.GenesisRitual.loaded(level,worldPosition)
                && com.mcmagic.omnira.world.dimension.GenesisRitual.pedestals(level,worldPosition).size()!=8){
            genesisFluid=0;absorption=0;sync();
        }
    }
    public void tickGenesis(){
        if(level==null || level.isClientSide || genesisFluid==0)return;
        validateGenesis();
        if(genesisFluid<com.mcmagic.omnira.world.dimension.GenesisRitual.REQUIRED_FLUID
                || !com.mcmagic.omnira.world.dimension.GenesisRitual.loaded(level,worldPosition))return;
        if(++absorption<com.mcmagic.omnira.world.dimension.GenesisRitual.ABSORPTION_TICKS+com.mcmagic.omnira.world.dimension.GenesisRitual.FORMATION_TICKS){
            if(absorption%5==0)sync();else setChanged();return;
        }
        var offerings=com.mcmagic.omnira.world.dimension.GenesisRitual.pedestals(level,worldPosition);
        if(offerings.size()!=8)return;
        // Clear state before pedestal callbacks, so completion cannot be mistaken for interruption.
        genesisFluid=0;absorption=0;
        for(var pedestal:offerings){pedestal.removeItem(0,1);pedestal.setChanged();}
        net.minecraft.world.level.block.Block.popResource(level,worldPosition.above(),
                new net.minecraft.world.item.ItemStack(com.mcmagic.omnira.paradox.ParadoxContent.GENESIS.get()));
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.BLOCKS,1,.6F);
        sync();
    }
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){
        super.saveAdditional(tag,lookup);tag.putInt("GenesisOffering",genesisFluid);tag.putInt("GenesisAbsorption",absorption);
    }
    @Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider lookup){
        super.loadAdditional(tag,lookup);genesisFluid=Math.clamp(tag.getInt("GenesisOffering"),0,10000);
        absorption=genesisFluid==10000?Math.clamp(tag.getInt("GenesisAbsorption"),0,59):0;
        if(genesisFluid==0){visualFluid=previousVisualFluid=0;visualAbsorption=previousVisualAbsorption=0;}
    }
    @Override public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
