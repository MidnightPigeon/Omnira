package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.recipe.AssemblyRecipe;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.mana.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

public final class ArcaneAssemblyTableBlockEntity extends CrystalProcessingTableBlockEntity implements AssemblyAccess {
    public static final int CENTER=6,BASE_COST=50,STRIKE_COOLDOWN=10;
    private net.minecraft.world.level.block.entity.BlockEntity host;
    private boolean mechanicalPowered;
    @Override public ArcaneAssemblyTableBlockEntity assembly() {return this;}
    public void setHost(net.minecraft.world.level.block.entity.BlockEntity host) {this.host=host;}
    public boolean mechanicalPowered() {return mechanicalPowered;}
    public void setMechanicalPowered(boolean powered) {
        if(mechanicalPowered!=powered) {mechanicalPowered=powered;setChanged();}
    }
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        if(host==null) return super.stillValid(player);
        return level!=null && level.getBlockEntity(worldPosition)==host && !host.isRemoved()
                && player.distanceToSqr(worldPosition.getCenter())<=64;
    }
    public void writeWork(CompoundTag tag,HolderLookup.Provider registries) {saveAdditional(tag,registries);}
    public void readWork(CompoundTag tag,HolderLookup.Provider registries) {loadAdditional(tag,registries);}
    public boolean automaticStrike() {
        if(level==null || level.isClientSide || !mechanicalPowered || cooldown()>0) return false;
        var recipe=work();
        return recipe!=null && completeStrike(recipe);
    }
    private int progress;
    private boolean finished;
    private String activeRecipe="";
    private long lastStrike=-20;
    public ArcaneAssemblyTableBlockEntity(BlockPos pos,BlockState state) {super(ModBlockEntityTypes.ARCANE_ASSEMBLY_TABLE.get(),pos,state,7);}
    @Override public int getContainerSize() {return 7;}
    @Override public int getMaxStackSize() {return 1;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack) {return !finished;}
    @Override public void setChanged() {
        if(level!=null && !level.isClientSide && progress>0 && !finished && work()==null) invalidateWork();
        super.setChanged();
        if(host!=null) host.setChanged();
    }
    public int cooldown() {
        return level==null?0:(int)Math.clamp(STRIKE_COOLDOWN-(level.getGameTime()-lastStrike),0,STRIKE_COOLDOWN);
    }
    public long lastStrike() {return lastStrike;}
    public int progress() {return progress;}
    public boolean finished() {return finished;}
    public RecipeHolder<AssemblyRecipe> recipe() {
        return level==null || finished?null:level.getRecipeManager()
                .getRecipeFor(ModRecipes.ASSEMBLY_TYPE.get(),new AssemblyRecipe.Input(this),level).orElse(null);
    }
    private com.mcmagic.omnira.recipe.AssemblyWork work() {
        return level==null || finished?null:com.mcmagic.omnira.recipe.AssemblyWork.find(level,this);
    }
    public static boolean directEngine(net.minecraft.world.level.Level level,BlockPos pos,BlockState state) {
        var back=state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        var adjacent=level.getBlockEntity(pos.relative(back));
        return adjacent instanceof ManaEngineAccess engine && engine.engineState().running()
                && adjacent.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==back.getOpposite();
    }
    public void tickDirectDrive() {
        if(level==null || level.isClientSide) return;
        setMechanicalPowered(directEngine(level,worldPosition,getBlockState()));
        if(mechanicalPowered) automaticStrike();
    }
    private void invalidateWork() {progress=0;activeRecipe="";}
    @Override public void setItem(int slot,ItemStack stack) {
        if(!ItemStack.matches(getItem(slot),stack)) invalidateWork();
        super.setItem(slot,stack);
        if(finished && getItems().stream().allMatch(ItemStack::isEmpty)) finished=false;
        setChanged();
    }
    @Override public ItemStack removeItem(int slot,int count) {
        var removed=super.removeItem(slot,count);
        if(!removed.isEmpty()) invalidateWork();
        if(finished && getItems().stream().allMatch(ItemStack::isEmpty)) finished=false;
        setChanged();return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        var result=super.removeItemNoUpdate(slot);
        if(!result.isEmpty()) invalidateWork();
        if(finished && getItems().stream().allMatch(ItemStack::isEmpty)) finished=false;
        setChanged();return result;
    }
    @Override public void clearContent() {invalidateWork();finished=false;super.clearContent();}
    public boolean strike(ServerPlayer player) {
        if(level==null || !stillValid(player) || player.isSpectator() || !player.isAlive() || cooldown()>0) return false;
        var recipe=work();
        if(recipe==null) return false;
        if(mechanicalPowered || !ManaEvents.trySpend(player,BASE_COST)) return false;
        return completeStrike(recipe);
    }
    private boolean completeStrike(com.mcmagic.omnira.recipe.AssemblyWork recipe) {
        String id=recipe.id();
        if(!activeRecipe.equals(id)) progress=0;
        activeRecipe=id;lastStrike=level.getGameTime();
        progress=Math.min(100,progress+(level.random.nextBoolean()?5:10));
        if(progress==100) {
            var results=recipe.finish().get();
            // All seven slots are limited to one item; replace the entire recipe atomically.
            for(int i=0;i<7;i++) getItems().set(i,ItemStack.EMPTY);
            int output=0;
            for(var result:results) if(!result.isEmpty()) {
                if(output<7) getItems().set(output==0?CENTER:output-1,result.copy());
                else net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,result.copy());
                output++;
            }
            finished=output>0;
            if(!finished) invalidateWork();
        }
        setChanged();
        level.playSound(null,worldPosition,finished?net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME:
                net.minecraft.sounds.SoundEvents.ANVIL_USE,net.minecraft.sounds.SoundSource.BLOCKS,.45F,finished?1.2F:1.65F);
        if(level instanceof net.minecraft.server.level.ServerLevel server)
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    worldPosition.getX()+.5,worldPosition.getY()+.85,worldPosition.getZ()+.5,finished?10:3,.12,.06,.12,.01);
        return true;
    }
    public void collect(net.minecraft.world.entity.player.Player player) {
        if(!finished) return;
        for(int i=0;i<7;i++) {
            var output=removeItem(i,Integer.MAX_VALUE);
            if(!output.isEmpty() && !player.getInventory().add(output)) player.drop(output,false);
        }
    }
    @Override protected Component getDefaultName() {return Component.translatable("container.omnira.arcane_assembly_table");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory) {
        return new com.mcmagic.omnira.menu.ArcaneAssemblyTableMenu(id,inventory,this);
    }
    private void saveWork(CompoundTag tag) {tag.putBoolean("AssemblyPowered",mechanicalPowered);tag.putLong("AssemblyLastStrike",lastStrike);tag.putInt("AssemblyProgress",progress);tag.putBoolean("AssemblyFinished",finished);tag.putString("AssemblyRecipe",activeRecipe);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries) {super.saveAdditional(tag,registries);saveWork(tag);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {var tag=super.getUpdateTag(registries);saveWork(tag);return tag;}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries) {
        super.loadAdditional(tag,registries);mechanicalPowered=host!=null && tag.getBoolean("AssemblyPowered");lastStrike=tag.contains("AssemblyLastStrike")?tag.getLong("AssemblyLastStrike"):-20;progress=Math.clamp(tag.getInt("AssemblyProgress"),0,100);
        finished=tag.getBoolean("AssemblyFinished") && getItems().stream().anyMatch(s->!s.isEmpty());activeRecipe=tag.getString("AssemblyRecipe");
    }
}
