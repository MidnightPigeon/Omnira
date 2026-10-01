package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.SimpleCondensationTableBlockEntity;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.recipe.CondensationRecipe;
import com.mcmagic.omnira.registry.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class SimpleCondensationTableMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final Player owner;
    private final TimedWork work;
    private final boolean advanced;
    public static SimpleCondensationTableMenu advanced(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        return new SimpleCondensationTableMenu(id,inventory,new SimpleContainer(1),ContainerLevelAccess.create(inventory.player.level(),data.readBlockPos()),true);
    }
    public SimpleCondensationTableMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,new SimpleContainer(1),ContainerLevelAccess.create(inventory.player.level(),data.readBlockPos()));
    }
    public SimpleCondensationTableMenu(int id,Inventory inventory,SimpleCondensationTableBlockEntity table) {
        this(id,inventory,table,ContainerLevelAccess.create(table.getLevel(),table.getBlockPos()),table instanceof com.mcmagic.omnira.block.entity.AdvancedCondensationTableBlockEntity);
    }
    private SimpleCondensationTableMenu(int id,Inventory inventory,Container container,ContainerLevelAccess access) {
        this(id,inventory,container,access,false);
    }
    private SimpleCondensationTableMenu(int id,Inventory inventory,Container container,ContainerLevelAccess access,boolean advanced) {
        super(advanced?ModMenuTypes.ADVANCED_CONDENSATION_TABLE.get():ModMenuTypes.SIMPLE_CONDENSATION_TABLE.get(),id);
        this.advanced=advanced;work=new TimedWork(advanced?200:100);
        this.container=container;this.access=access;owner=inventory.player;
        container.startOpen(owner);addDataSlot(work.progress);
        addSlot(advanced?new Slot(container,0,80,70) {
            @Override public boolean mayPlace(ItemStack stack) {return accepts(owner,stack);}
            @Override public int getMaxStackSize() {return 1;}
        }:new OutputSlot(container,0,80,70));
        for(int row=0;row<3;row++) for(int col=0;col<9;col++)
            addSlot(new Slot(inventory,9+row*9+col,8+col*18,158+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inventory,col,8+col*18,216));
    }
    public CondensationRecipe recipe() {
        return owner.level().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get()).stream()
                .filter(h->h.value().advanced()==advanced && (!advanced || h.value().matches(new SingleRecipeInput(container.getItem(0)),owner.level()))).findFirst()
                .map(h->h.value()).orElse(null);
    }
    private boolean fits(CondensationRecipe recipe) {
        if(recipe==null) return false;
        if(advanced) return container.getItem(0).getCount()==1 && recipe.matches(new SingleRecipeInput(container.getItem(0)),owner.level());
        var output=container.getItem(0);
        return (output.isEmpty() || ItemStack.isSameItemSameComponents(output,recipe.result()))
                && output.getCount()+recipe.result().getCount()<=Math.min(container.getMaxStackSize(),recipe.result().getMaxStackSize());
    }
    public boolean canCondense() {
        var recipe=recipe();
        return fits(recipe) && ManaCosts.canSpend(owner,recipe.manaCost());
    }
    public double manaCost() {var recipe=recipe();return ManaCosts.cost(owner,recipe==null?(advanced?300:0):recipe.manaCost());}
    public boolean isWorking() {return work.active();}
    public float workProgress() {return work.fraction();}
    public static boolean accepts(Player player,ItemStack stack) {return player.level().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get()).stream().anyMatch(h->h.value().advanced() && h.value().ingredient().test(stack));}
    @Override public boolean stillValid(Player player) {return player==owner && container.stillValid(player) && stillValid(access,player,advanced?ModBlocks.ADVANCED_CONDENSATION_TABLE.get():ModBlocks.SIMPLE_CONDENSATION_TABLE.get());}
    @Override public boolean clickMenuButton(Player player,int id) {
        if(id!=0 || player!=owner || !(player instanceof ServerPlayer) || !stillValid(player) || !canCondense()) return false;
        return work.start(player,container);
    }
    @Override public void broadcastChanges() {
        work.tick(owner,container,stillValid(owner) && canCondense(),this::finish);
        super.broadcastChanges();
    }
    private void finish() {
        var recipe=recipe();
        if(!(owner instanceof ServerPlayer server) || !stillValid(owner) || !fits(recipe) || !ManaEvents.trySpend(server,recipe.manaCost())) return;
        if(advanced || container.getItem(0).isEmpty()) container.setItem(0,recipe.assemble(new SingleRecipeInput(container.getItem(0)),owner.registryAccess()));
        else container.getItem(0).grow(recipe.result().getCount());
        container.setChanged();
        access.execute((level,pos)->level.playSound(null,pos,net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,.7F,.8F));
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        if(index<0 || index>=slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        var slot=slots.get(index);var stack=slot.getItem();var original=stack.copy();
        if(index==0) {if(!moveItemStackTo(stack,1,37,true)) return ItemStack.EMPTY;}
        else if(!advanced || !accepts(player,stack) || !moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return original;
    }
    @Override public void removed(Player player) {work.cancel();super.removed(player);container.stopOpen(player);}
}
