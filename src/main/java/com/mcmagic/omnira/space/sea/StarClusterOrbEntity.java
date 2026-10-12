package com.mcmagic.omnira.space.sea;

import com.mcmagic.omnira.block.entity.CrystalBallBlockEntity;
import com.mcmagic.omnira.space.anchor.*;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

public final class StarClusterOrbEntity extends CrystalBallBlockEntity {
    public static final int DURATION=200;
    public static final TagKey<Item> FISH=TagKey.create(net.minecraft.core.registries.Registries.ITEM,net.minecraft.resources.ResourceLocation.parse("omnira:starlight_fish"));
    private final SpatialActivation activation=new SpatialActivation(this,32);
    private int progress;
    private ItemStack processing=ItemStack.EMPTY;
    public StarClusterOrbEntity(BlockPos pos,BlockState state){super(pos,state);}
    @Override public int getContainerSize(){return 1;}
    public int progress(){return progress;}
    public ItemStack result(ItemStack input){return com.mcmagic.omnira.recipe.StarlightConversionRecipe.output(level,input);}
    @Override public int getMaxStackSize(ItemStack stack){
        var output=result(stack);return output.isEmpty()?stack.getMaxStackSize():Math.min(stack.getMaxStackSize(),output.getMaxStackSize());
    }
    @Override public ItemStack displayItem(int slot){return slot==0?super.displayItem(0):ItemStack.EMPTY;}
    @Override protected Component getDefaultName(){return Component.translatable("block.omnira.wandering_star_cluster_orb");}
    @Override public void onLoad(){super.onLoad();updateField();}
    private void updateField(){if(level instanceof ServerLevel server)StabilityField.updateStellar(server,worldPosition,stabilized());}
    @Override public void setChanged(){super.setChanged();updateField();}
    @Override public void serverTick(){
        if(!(level instanceof ServerLevel server))return;
        if(stabilized())activation.tick(server);else activation.clear();
        var input=getItem(0);var output=result(input);
        if(output.isEmpty()||output.getCount()>output.getMaxStackSize()){
            if(progress!=0){progress=0;processing=ItemStack.EMPTY;setChanged();}return;
        }
        if(!ItemStack.matches(processing,input)){processing=input.copy();progress=0;}
        if(!com.mcmagic.omnira.recipe.RecipeControl.allows(level,upgrades,java.util.List.of(output),net.neoforged.neoforge.fluids.FluidStack.EMPTY))return;
        if(++progress>=DURATION){progress=0;processing=ItemStack.EMPTY;setItem(0,output);
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,worldPosition.getX()+.5,worldPosition.getY()+.625,worldPosition.getZ()+.5,16,.2,.2,.2,.02);
        }else if(progress%10==0)setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.saveAdditional(tag,provider);tag.putInt("StarProgress",progress);
        if(!processing.isEmpty())tag.put("StarInput",processing.save(provider));activation.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
        super.loadAdditional(tag,provider);progress=Math.clamp(tag.getInt("StarProgress"),0,DURATION-1);
        processing=ItemStack.parseOptional(provider,tag.getCompound("StarInput"));activation.load(tag,provider);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){var tag=super.getUpdateTag(provider);tag.putInt("StarProgress",progress);return tag;}
}
