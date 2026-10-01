package com.mcmagic.omnira.time;

import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mcmagic.omnira.reversal.ReversalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class SundialBlockEntity extends CrystalProcessingTableBlockEntity {
    public static final int CAPACITY=128,COST=32,DURATION=2560,CHARGE_INTERVAL=80;
    private int energy,progress,selection;
    private ItemStack started=ItemStack.EMPTY;
    private ItemStack target=ItemStack.EMPTY;

    public SundialBlockEntity(BlockPos pos,BlockState state){super(SundialContent.ENTITY.get(),pos,state,3);}
    @Override public int getContainerSize(){return 3;}
    @Override protected Component getDefaultName(){return Component.translatable("block.omnira.split_time_sundial");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new SundialMenu(id,inventory,this);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==2?ReversalBlockEntity.fuel(stack)>0:slot==0&&!SundialTimeline.choices(stack).isEmpty();}
    public int energy(){return energy;}
    public int progress(){return progress;}
    public int selection(){return selection;}
    public ItemStack selectedOutput(){
        var choices=SundialTimeline.choices(getItem(0));
        return choices.isEmpty()?ItemStack.EMPTY:choices.get(Math.floorMod(selection,choices.size()));
    }
    public void cycle(int step){
        var choices=SundialTimeline.choices(getItem(0));
        if(choices.size()<2||Math.floorMod(step,choices.size())==0)return;
        selection=Math.floorMod(selection+step,choices.size());
        progress=0;started=ItemStack.EMPTY;target=ItemStack.EMPTY;
        start();setChanged();
    }
    public boolean start(){
        if(progress>0||energy<COST||getItem(0).isEmpty())return false;
        ItemStack chosen=selectedOutput();
        if(chosen.isEmpty()||!fits(chosen))return false;
        started=getItem(0).copyWithCount(1);
        target=chosen.copyWithCount(1);
        progress=1;setChanged();return true;
    }
    public void tick(){
        var fuel=getItem(2);int value=ReversalBlockEntity.fuel(fuel);
        if(value>0&&energy+value<=CAPACITY){energy+=value;fuel.shrink(1);setChanged();}
        if(progress==0){start();return;}
        var input=getItem(0);
        if(input.isEmpty()||input.getCount()<1||!ItemStack.isSameItemSameComponents(started,input)
                ||target.isEmpty()||SundialTimeline.choices(input).stream().noneMatch(choice->ItemStack.isSameItemSameComponents(choice,target))){
            progress=0;started=ItemStack.EMPTY;target=ItemStack.EMPTY;setChanged();return;
        }
        if(energy<=0||!fits(target))return;
        if(level instanceof net.minecraft.server.level.ServerLevel server&&progress%10==0){
            double phase=progress*.19;
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                    worldPosition.getX()+.5+Math.cos(phase)*.38,worldPosition.getY()+.55,
                    worldPosition.getZ()+.5+Math.sin(phase)*.38,2,.03,.04,.03,.02);
        }
        progress++;
        if(progress%CHARGE_INTERVAL==0)energy--;
        if(progress>=DURATION){
            input.shrink(1);
            if(getItem(1).isEmpty())setItem(1,target.copy());else getItem(1).grow(1);
            progress=0;started=ItemStack.EMPTY;target=ItemStack.EMPTY;
        }
        if(progress==0||progress%10==0)setChanged();
    }
    private boolean fits(ItemStack item){
        var output=getItem(1);
        return output.isEmpty()||ItemStack.isSameItemSameComponents(output,item)&&output.getCount()<output.getMaxStackSize();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){
        var tag=super.getUpdateTag(registries);tag.putInt("Energy",energy);tag.putInt("Progress",progress);
        tag.putInt("Selection",selection);return tag;
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putInt("Energy",energy);tag.putInt("Progress",progress);
        tag.putInt("Selection",selection);
        if(!started.isEmpty())tag.put("Started",started.save(registries));
        if(!target.isEmpty())tag.put("TargetStack",target.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);energy=Math.clamp(tag.getInt("Energy"),0,CAPACITY);
        progress=Math.clamp(tag.getInt("Progress"),0,DURATION-1);selection=Math.max(0,tag.getInt("Selection"));
        started=ItemStack.parseOptional(registries,tag.getCompound("Started"));
        target=ItemStack.parseOptional(registries,tag.getCompound("TargetStack"));
        if(progress>0&&(started.isEmpty()||target.isEmpty()))progress=0;
    }
}
