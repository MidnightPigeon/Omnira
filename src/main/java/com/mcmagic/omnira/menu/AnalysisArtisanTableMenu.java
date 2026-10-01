package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.AnalysisArtisanTableBlockEntity;
import com.mcmagic.omnira.mana.ManaEvents;
import com.mcmagic.omnira.recipe.AnalysisRecipe;
import com.mcmagic.omnira.registry.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class AnalysisArtisanTableMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;
    private final Player owner;
    private final TimedWork work = new TimedWork(40);

    public AnalysisArtisanTableMenu(int id, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(id, inventory, new SimpleContainer(3), ContainerLevelAccess.create(inventory.player.level(), data.readBlockPos()));
    }
    public AnalysisArtisanTableMenu(int id, Inventory inventory, AnalysisArtisanTableBlockEntity table) {
        this(id, inventory, table, ContainerLevelAccess.create(table.getLevel(), table.getBlockPos()));
    }
    private AnalysisArtisanTableMenu(int id, Inventory inventory, Container container, ContainerLevelAccess access) {
        super(ModMenuTypes.ANALYSIS_ARTISAN_TABLE.get(), id);
        checkContainerSize(container, 3);
        this.container = container;
        this.access = access;
        this.owner = inventory.player;
        addDataSlot(work.progress);
        container.startOpen(owner);
        addSlot(new Slot(container, 0, 80, 48));
        addSlot(new OutputSlot(container, 1, 56, 96));
        addSlot(new OutputSlot(container, 2, 104, 96));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 158 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 216));
    }
    @Override public boolean stillValid(Player player) { return container.stillValid(player) && stillValid(access, player, ModBlocks.ANALYSIS_ARTISAN_TABLE.get()); }
    public AnalysisRecipe recipe() {
        return owner.level().getRecipeManager().getRecipeFor(ModRecipes.ANALYSIS_TYPE.get(), new SingleRecipeInput(container.getItem(0)), owner.level())
                .map(holder -> holder.value()).orElse(null);
    }
    private boolean room(int slot, ItemStack output) {
        if (output.isEmpty()) return true;
        ItemStack existing = container.getItem(slot);
        int limit = Math.min(container.getMaxStackSize(), output.getMaxStackSize());
        return (existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, output)) && existing.getCount() + output.getCount() <= limit;
    }
    private boolean fits(AnalysisRecipe recipe) {
        // Reserve room for every possible roll; never reroll until an output happens to fit.
        return room(1, recipe.result()) && recipe.secondaryOutput().maximumOutputs().stream().allMatch(output->room(2,output));
    }
    public double manaCost() {return com.mcmagic.omnira.mana.ManaCosts.cost(owner,AnalysisRecipe.MANA_COST);}
    public boolean canAnalyze() {
        AnalysisRecipe recipe = recipe();
        return recipe != null && fits(recipe) && com.mcmagic.omnira.mana.ManaCosts.canSpend(owner,AnalysisRecipe.MANA_COST);
    }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || player != owner || player.level().isClientSide || !stillValid(player) || !canAnalyze()) return false;
        return work.start(player,container);
    }
    public boolean isWorking() { return work.active(); }
    public float workProgress() { return work.fraction(); }
    @Override public void broadcastChanges() {
        work.tick(owner,container,stillValid(owner) && canAnalyze(),() -> finishAnalysis(owner,0));
        super.broadcastChanges();
    }
    private boolean finishAnalysis(Player player, int id) {
        if (id != 0 || player != owner || !(player instanceof ServerPlayer serverPlayer) || !stillValid(player)) return false;
        AnalysisRecipe recipe = recipe();
        if (recipe == null || !fits(recipe)) return false;
        ItemStack input = container.getItem(0);
        ItemStack remainder = recipe.inputDamage()>0?ItemStack.EMPTY:input.getCraftingRemainingItem();
        // Keep remainders in the emptied input slot. Never discard buckets or debit mana before capacity checks.
        if (!remainder.isEmpty() && (input.getCount() != recipe.count() || recipe.count() != 1)) return false;
        if (!ManaEvents.trySpend(serverPlayer, AnalysisRecipe.MANA_COST)) return false;
        if(recipe.inputDamage()>0) {
            // Damage the existing tool in the input slot; it is never an output or a remainder.
            input.setDamageValue(input.getDamageValue()+recipe.inputDamage());
            if(input.getDamageValue()>=input.getMaxDamage()) container.setItem(0,ItemStack.EMPTY);
        } else {
            container.removeItem(0, recipe.count());
            if (!remainder.isEmpty()) container.setItem(0, remainder);
        }
        addOutput(1, recipe.result().copy());
        var byproduct=recipe.secondaryOutput().roll(player.getRandom());
        if(!byproduct.isEmpty()) addOutput(2,byproduct);
        container.setChanged();
        broadcastChanges();
        access.execute((level, pos) -> {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, .8F, 1.3F);
            if (level instanceof net.minecraft.server.level.ServerLevel server)
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, pos.getX()+.5, pos.getY()+.8, pos.getZ()+.5, 12, .2, .05, .2, .1);
        });
        return true;
    }
    private void addOutput(int slot, ItemStack output) {
        if (container.getItem(slot).isEmpty()) container.setItem(slot, output);
        else container.getItem(slot).grow(output.getCount());
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), original = stack.copy();
        if (index < 3 ? !moveItemStackTo(stack,3,39,true) : !moveItemStackTo(stack,0,1,false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack);
        return original;
    }
    @Override public void removed(Player player) { super.removed(player); container.stopOpen(player); }
}
