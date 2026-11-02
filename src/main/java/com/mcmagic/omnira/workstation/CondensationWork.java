package com.mcmagic.omnira.workstation;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.mana.*;
import com.mcmagic.omnira.recipe.CondensationRecipe;
import com.mcmagic.omnira.registry.ModRecipes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;

/** One server-owned job per table. Costs and durations match the former menu. */
public final class CondensationWork {
    private final SimpleCondensationTableBlockEntity table;
    private ServerPlayer owner;
    private ItemStack input;
    private long started;
    public CondensationWork(SimpleCondensationTableBlockEntity table){this.table=table;}
    public boolean advanced(){return table instanceof AdvancedCondensationTableBlockEntity;}
    public int duration(){return advanced()?200:100;}
    public CondensationRecipe recipe(){
        return table.getLevel().getRecipeManager().getAllRecipesFor(ModRecipes.CONDENSATION_TYPE.get()).stream()
                .map(h->h.value()).filter(r->r.advanced()==advanced() && (!advanced()||r.matches(new SingleRecipeInput(table.getItem(0)),table.getLevel())))
                .findFirst().orElse(null);
    }
    private boolean fits(CondensationRecipe r){
        if(r==null)return false;
        var stack=table.getItem(0);
        return advanced()?r.matches(new SingleRecipeInput(stack),table.getLevel()):
                (stack.isEmpty()||ItemStack.isSameItemSameComponents(stack,r.result()))
                &&stack.getCount()+r.result().getCount()<=Math.min(table.getMaxStackSize(),r.result().getMaxStackSize());
    }
    public boolean active(){return owner!=null;}
    public boolean ownedBy(ServerPlayer p){return owner==p;}
    public void cancel(){owner=null;input=null;}
    public boolean canStart(ServerPlayer p){var r=recipe();return fits(r)&&ManaCosts.canSpend(p,r.manaCost());}
    public boolean start(ServerPlayer p){return start(p,p.level().getGameTime());}
    public boolean start(ServerPlayer p,long pressedAt){
        if(active()||!canStart(p))return false;
        owner=p;input=table.getItem(0).copy();started=Math.min(p.level().getGameTime(),pressedAt);return true;
    }
    public boolean tick(){return advance(false);}
    public void worldRotation(){advance(true);}
    private boolean advance(boolean complete){
        if(!active())return false;
        var r=recipe();
        if(table.isRemoved()||!owner.isAlive()||owner.level()!=table.getLevel()||!table.stillValid(owner)
                ||!ItemStack.matches(input,table.getItem(0))||!fits(r)||!ManaCosts.canSpend(owner,r.manaCost())){cancel();return false;}
        if(!complete&&owner.level().getGameTime()-started<duration())return true;
        var player=owner;cancel();
        if(!complete&&!ManaEvents.trySpend(player,r.manaCost()))return false;
        if(advanced()||table.getItem(0).isEmpty())table.setItem(0,r.assemble(new SingleRecipeInput(table.getItem(0)),player.registryAccess()));
        else table.getItem(0).grow(r.result().getCount());
        table.setChanged();table.getLevel().playSound(null,table.getBlockPos(),net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,net.minecraft.sounds.SoundSource.BLOCKS,.7F,.8F);
        return true;
    }
}
