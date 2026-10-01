package com.mcmagic.omnira.aggregation;

import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mcmagic.omnira.item.SpellCoreItem;
import com.mcmagic.omnira.menu.CrystalProcessingTableMenu;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;

public final class AggregationRingBlockEntity extends CrystalProcessingTableBlockEntity {
    private int progress;
    private long buttonPressTick=Long.MIN_VALUE;
    public void pressButton(Player player){buttonPressTick=level.getGameTime();start(player);setChanged();}
    public double buttonDepth(double time){return AggregationLayout.buttonDepth(time-buttonPressTick);}
    private long progressSyncTick;
    private java.util.UUID author;
    private NonNullList<ItemStack> snapshot=NonNullList.withSize(AggregationLayout.SLOTS,ItemStack.EMPTY);
    public AggregationRingBlockEntity(BlockPos pos,BlockState state){super(AggregationContent.ENTITY.get(),pos,state,AggregationLayout.SLOTS);}
    @Override public int getContainerSize(){return AggregationLayout.SLOTS;}
    @Override protected Component getDefaultName(){return Component.translatable("block.omnira.advanced_crystal_aggregation_ring");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new AggregationRingMenu(id,inventory,this);}
    public int progress(){return progress;}
    public double renderProgress(double time){
        return progress==0?0:Math.min(1,(progress+Math.min(4,Math.max(0,time-progressSyncTick)))/AggregationLayout.DURATION);
    }
    public boolean powered(){return getItem(AggregationLayout.CORE).getItem() instanceof SpellCoreItem;}
    public static boolean accepts(int slot,ItemStack stack){
        if(slot==0)return CrystalProcessingTableMenu.isTargetMicrocore(stack);
        if(slot==1)return CrystalProcessingTableMenu.isPrimaryMicrocore(stack);
        if(slot<5)return CrystalProcessingTableMenu.isElement(stack);
        if(slot<8)return true;
        return slot==AggregationLayout.SUBSTRATE?stack.is(ModItems.INFUSED_SPIRITUAL_CRYSTAL.get()):
                slot==AggregationLayout.INK?stack.is(ModItems.SPELL_INK.get()):
                slot==AggregationLayout.CORE&&stack.getItem() instanceof SpellCoreItem;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return progress==0&&accepts(slot,stack)&&com.mcmagic.omnira.spell.SpellPattern.compatibleSlots(this,slot,stack);}
    public int manaCost(){int count=0;for(int i=0;i<8;i++)if(!getItem(i).isEmpty())count++;return count*20;}
    public boolean recipeReady(){
        if(!powered()||!getItem(AggregationLayout.OUTPUT).isEmpty()||getItem(0).isEmpty()
                ||getItem(AggregationLayout.SUBSTRATE).isEmpty()||getItem(AggregationLayout.INK).isEmpty())return false;
        for(int i=0;i<=AggregationLayout.CORE;i++)if(!getItem(i).isEmpty()&&!accepts(i,getItem(i)))return false;
        return com.mcmagic.omnira.spell.SpellPattern.fromMicrocores(getItem(0),getItem(1)).isPresent();
    }
    public boolean start(Player player){
        if(!(player instanceof ServerPlayer server)||progress>0||!recipeReady()||!AggregationStructure.intact(level,worldPosition,getBlockState()))return false;
        if(!com.mcmagic.omnira.mana.ManaCosts.canSpend(server,manaCost())){
            player.displayClientMessage(Component.translatable("message.omnira.aggregation.mana",manaCost()),true);return false;
        }
        author=player.getUUID();progress=1;
        for(int i=0;i<getContainerSize();i++)snapshot.set(i,getItem(i).copy());
        setChanged();return true;
    }
    public void tick(){
        if(progress==0)return;
        if(!recipeReady()){cancel();return;}
        for(int i=0;i<getContainerSize();i++)if(!ItemStack.matches(snapshot.get(i),getItem(i))){cancel();return;}
        if(progress++<AggregationLayout.DURATION){if(progress%4==0)setChanged();return;}
        Player player=author==null?null:level.getPlayerByUUID(author);
        if(player==null&&author!=null)player=level.getServer().getPlayerList().getPlayer(author);
        if(player instanceof ServerPlayer server&&player.isAlive()
                &&com.mcmagic.omnira.mana.ManaEvents.trySpend(server,manaCost())){
            var result=CrystalProcessingTableMenu.previewCrystal(this,5,8,2,5,AggregationContent.CRYSTAL.get());
            for(int i=0;i<8;i++)removeItem(i,1);
            removeItem(AggregationLayout.SUBSTRATE,1);
            var ink=getItem(AggregationLayout.INK);
            if(ink.getDamageValue()+1>=ink.getMaxDamage())ink.shrink(1);else ink.setDamageValue(ink.getDamageValue()+1);
            setItem(AggregationLayout.OUTPUT,result);
        }
        cancel();
    }
    private void cancel(){progress=0;author=null;snapshot=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);setChanged();}
    public void interactSlot(Player player,InteractionHand hand,int slot){
        if(progress>0||slot==AggregationLayout.OUTPUT)return;
        var held=player.getItemInHand(hand);var stored=getItem(slot);
        if(held.isEmpty()){
            if(!stored.isEmpty()){player.setItemInHand(hand,stored.copy());setItem(slot,ItemStack.EMPTY);}
        }else if(canPlaceItem(slot,held)&&stored.isEmpty())setItem(slot,held.split(1));
        setChanged();
    }
    public boolean collect(Player player){
        var output=getItem(AggregationLayout.OUTPUT);if(output.isEmpty())return false;
        var result=output.copy();setItem(AggregationLayout.OUTPUT,ItemStack.EMPTY);
        if(!player.addItem(result))player.drop(result,false);setChanged();return true;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=super.getUpdateTag(registries);tag.putInt("Progress",progress);tag.putLong("ButtonPress",buttonPressTick);return tag;}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putInt("Progress",progress);if(author!=null)tag.putUUID("Author",author);
        var recipe=new CompoundTag();ContainerHelper.saveAllItems(recipe,snapshot,registries);tag.put("Snapshot",recipe);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);progress=Math.clamp(tag.getInt("Progress"),0,AggregationLayout.DURATION);
        buttonPressTick=tag.contains("ButtonPress")?tag.getLong("ButtonPress"):Long.MIN_VALUE;
        if(level!=null&&level.isClientSide)progressSyncTick=level.getGameTime();
        author=tag.hasUUID("Author")?tag.getUUID("Author"):null;
        snapshot=NonNullList.withSize(getContainerSize(),ItemStack.EMPTY);ContainerHelper.loadAllItems(tag.getCompound("Snapshot"),snapshot,registries);
    }
}
