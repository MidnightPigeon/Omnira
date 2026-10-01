package com.mcmagic.omnira.reversal;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ReversalBlockEntity extends com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity implements net.minecraft.world.WorldlyContainer {
    public static final int CAPACITY=64,DURATION=100;
    private int energy,progress;
    private ItemStack started=ItemStack.EMPTY;
    public ReversalBlockEntity(BlockPos p,BlockState s){super(ReversalContent.ENTITY.get(),p,s,3);}
    public int energy(){return energy;} public int progress(){return progress;}
    @Override public int getContainerSize(){return 3;}
    @Override protected net.minecraft.network.chat.Component getDefaultName(){return net.minecraft.network.chat.Component.translatable("block.omnira.spacetime_reverser");}
    @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv){return new ReversalMenu(id,inv,this);}
    public static int fuel(ItemStack s){return switch(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString()){
        case "omnira:spatial_crystal_shard"->1;
        case "omnira:time_warp_point","omnira:reborn_rottenleaf_lily","omnira:peaceful_memory_cube","omnira:corrupted_memory_cube"->4;
        case "omnira:timeflow_eel","omnira:chronal_blindfish"->2;
        case "omnira:time_microcore","omnira:space_microcore"->8;
        default->0;};}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==2?fuel(stack)>0:slot==0&&level!=null&&level.getRecipeManager().getAllRecipesFor(ReversalContent.TYPE.get()).stream().anyMatch(r->r.value().ingredient().test(stack));}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{1}:side==Direction.UP?new int[]{0}:new int[]{2};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==1;}
    public void tick(){
        var fuel=getItem(2);int value=fuel(fuel);
        if(value>0&&energy+value<=CAPACITY){energy+=value;fuel.shrink(1);setChanged();}
        var input=getItem(0);var recipe=level.getRecipeManager().getRecipeFor(ReversalContent.TYPE.get(),new net.minecraft.world.item.crafting.SingleRecipeInput(input),level).map(net.minecraft.world.item.crafting.RecipeHolder::value).orElse(null);
        // Random outputs are rolled only on completion; require room for every possibility.
        boolean ready=energy>0&&recipe!=null&&recipe.outputs(input).stream().allMatch(this::fits);
        if(!ready||!ItemStack.isSameItemSameComponents(started,input)){boolean wasWorking=progress>0;progress=0;started=input.copy();if(wasWorking)setChanged();}
        if(!ready)return;
        if(++progress>=DURATION){
            var choices=recipe.outputs(input);var result=choices.get(level.random.nextInt(choices.size())).copy();
            input.shrink(recipe.count());if(getItem(1).isEmpty())setItem(1,result);else getItem(1).grow(result.getCount());
            energy--;progress=0;started=input.copy();
        }
        setChanged();
    }
    private boolean fits(ItemStack s){var out=getItem(1);return out.isEmpty()||(ItemStack.isSameItemSameComponents(out,s)&&out.getCount()+s.getCount()<=out.getMaxStackSize());}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putInt("Energy",energy);n.putInt("Progress",progress);if(!started.isEmpty())n.put("Started",started.save(r));}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);energy=Math.clamp(n.getInt("Energy"),0,64);progress=Math.clamp(n.getInt("Progress"),0,99);started=ItemStack.parseOptional(r,n.getCompound("Started"));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var n=super.getUpdateTag(r);n.putInt("Energy",energy);n.putInt("Progress",progress);return n;}
}
