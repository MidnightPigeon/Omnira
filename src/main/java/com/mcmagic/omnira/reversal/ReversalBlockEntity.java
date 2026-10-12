package com.mcmagic.omnira.reversal;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class ReversalBlockEntity extends com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity implements net.minecraft.world.WorldlyContainer {
    public static final int CAPACITY=64,DURATION=100;
    private int energy,progress;
    private ItemStack started=ItemStack.EMPTY;
    public ReversalBlockEntity(BlockPos p,BlockState s){super(s.is(ReversalContent.INFUSER.get())?ReversalContent.INFUSER_ENTITY.get():ReversalContent.ENTITY.get(),p,s,3);}
    public boolean infusion(){return getBlockState().is(ReversalContent.INFUSER.get());}
    public int duration(){return infusion()?200:DURATION;}
    public net.minecraft.world.item.crafting.RecipeType<ReversalRecipe> recipeType(){return infusion()?ReversalContent.INFUSION_TYPE.get():ReversalContent.TYPE.get();}
    public int fuelValue(ItemStack stack){return infusion()?spaceFuel(stack):fuel(stack);}
    public static int spaceFuel(ItemStack stack){return ReversalFuels.value(stack,true);}
    public int energy(){return energy;} public int progress(){return progress;}
    @Override public int getContainerSize(){return 3;}
    @Override protected net.minecraft.network.chat.Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv){return new ReversalMenu(id,inv,this);}
    public static int fuel(ItemStack s){return ReversalFuels.value(s,false);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==2?fuelValue(stack)>0:slot==0&&level!=null&&level.getRecipeManager().getAllRecipesFor(recipeType()).stream().anyMatch(r->r.value().ingredient().test(stack));}
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{1}:side==Direction.UP?new int[]{0}:new int[]{2};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==1;}
    public void tick(){
        var fuel=getItem(2);int value=fuelValue(fuel);
        if(value>0&&energy+value<=CAPACITY){energy+=value;fuel.shrink(1);setChanged();}
        var input=getItem(0);
        if(input.isEmpty()||energy<=0){
            if(progress>0){progress=0;started=input.copy();setChanged();}
            return;
        }
        var recipe=level.getRecipeManager().getRecipeFor(recipeType(),new net.minecraft.world.item.crafting.SingleRecipeInput(input),level).map(net.minecraft.world.item.crafting.RecipeHolder::value).orElse(null);
        // Random outputs are rolled only on completion; require room for every possibility.
        boolean ready=energy>0&&recipe!=null&&recipe.outputs(input).stream().allMatch(this::fits);
        if(!ready||!ItemStack.isSameItemSameComponents(started,input)){boolean wasWorking=progress>0;progress=0;started=input.copy();if(wasWorking)setChanged();}
        if(!ready)return;
        if(++progress>=duration()){
            var choices=recipe.outputs(input);var result=choices.get(level.random.nextInt(choices.size())).copy();
            input.shrink(recipe.count());if(getItem(1).isEmpty())setItem(1,result);else getItem(1).grow(result.getCount());
            energy--;progress=0;started=input.copy();
        }
        // Save actual progress every tick; send the world render snapshot only twice a second.
        level.blockEntityChanged(worldPosition);
        if(progress==0||progress==1||progress%10==0)setChanged();
    }
    private boolean fits(ItemStack s){var out=getItem(1);return out.isEmpty()||(ItemStack.isSameItemSameComponents(out,s)&&out.getCount()+s.getCount()<=out.getMaxStackSize());}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putInt("Energy",energy);n.putInt("Progress",progress);if(!started.isEmpty())n.put("Started",started.save(r));}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);energy=Math.clamp(n.getInt("Energy"),0,CAPACITY);progress=Math.clamp(n.getInt("Progress"),0,duration()-1);started=ItemStack.parseOptional(r,n.getCompound("Started"));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var n=super.getUpdateTag(r);n.putInt("Energy",energy);n.putInt("Progress",progress);return n;}
}
