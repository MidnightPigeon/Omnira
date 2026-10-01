package com.mcmagic.omnira.forging;

import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mcmagic.omnira.item.SpellCoreItem;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class AdvancedForgeBlockEntity extends CrystalProcessingTableBlockEntity {
    private int elapsed,duration,buttonTicks;
    private boolean consumed;
    private ItemStack pending=ItemStack.EMPTY,output=ItemStack.EMPTY;
    private NonNullList<ItemStack> remainders=NonNullList.withSize(9,ItemStack.EMPTY);
    private NonNullList<ItemStack> reserved=NonNullList.withSize(9,ItemStack.EMPTY);
    private long syncedAt;
    private int formingTicks;
    private boolean checkStructure=true;
    private boolean stormDamaged;
    public void markStormDamaged(){stormDamaged=true;checkStructure=false;setChanged();}
    public void beginFormation(){formingTicks=40;setChanged();}
    public AdvancedForgeBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.ADVANCED_FORGE.get(),pos,state,10);}
    public int getContainerSize(){return 10;}
    @Override public void onLoad(){super.onLoad();syncedAt=level==null?0:level.getGameTime();}
    public int getMaxStackSize(){return 1;}
    public boolean working(){return duration>0;}
    public float progress(float partial){return working()?Math.min(1,(elapsed+partial+(level!=null && level.isClientSide?level.getGameTime()-syncedAt:0))/duration):0;}
    public boolean hot(){return working() && elapsed>=duration/5 && elapsed<duration*9/10;}
    public int buttonTicks(){return buttonTicks;}
    public ItemStack output(){return output;}
    public boolean canPlaceItem(int slot,ItemStack item){return !working() && (slot!=9 || item.getItem() instanceof SpellCoreItem);}
    public static int duration(ItemStack core){return core.getItem() instanceof SpellCoreItem source?Math.max(1,(int)Math.round(1200D*512/Math.max(1,source.engineOutput(core).stress()))):0;}
    public boolean start(Player player){
        if(stormDamaged || level==null || level.isClientSide || !stillValid(player) || player.isSpectator() || working() || !output.isEmpty())return false;
        buttonTicks=4;setChanged();
        int time=duration(getItem(9));if(time==0)return false;
        var input=new AdvancedForgeRecipe.Input(this);
        var recipe=level.getRecipeManager().getRecipeFor(ModRecipes.ADVANCED_FORGE_TYPE.get(),input,level).orElse(null);
        if(recipe==null)return false;
        pending=recipe.value().assemble(input,level.registryAccess());
        remainders=recipe.value().getRemainingItems(input);
        for(int i=0;i<9;i++)reserved.set(i,getItem(i).copy());
        elapsed=0;duration=time;consumed=false;setChanged();return true;
    }
    public void tick(){
        if(stormDamaged)return;
        if(checkStructure && level!=null && !level.isClientSide
                && level.hasChunksAt(worldPosition.offset(-1,0,-1),worldPosition.offset(1,1,1))){
            checkStructure=false;
            ForgeLayout.restoreMissingParts(level,worldPosition,getBlockState());
        }
        if(formingTicks>0){
            formingTicks--;
            if(formingTicks%4==0 && level instanceof net.minecraft.server.level.ServerLevel server)
                com.mcmagic.omnira.world.dimension.TransitionParticles.ring(server,net.minecraft.world.phys.Vec3.atBottomCenterOf(worldPosition),1.5,2,1-formingTicks/40.0);
        }
        if(buttonTicks>0){buttonTicks--;if(buttonTicks==0)setChanged();}
        if(!working())return;
        elapsed++;
        if(!consumed && elapsed>=duration/5){for(int i=0;i<9;i++)getItems().set(i,ItemStack.EMPTY);consumed=true;setChanged();}
        if(hot() && elapsed%20==0){
            for(var player:level.getEntitiesOfClass(Player.class,getRenderBoundingBox())){
                if(player.isSpectator() || player.getAbilities().invulnerable)continue;
                player.hurt(level.damageSources().magic(),2);player.igniteForSeconds(2);
            }
        }
        if(elapsed>=duration){
            output=pending;pending=ItemStack.EMPTY;duration=0;elapsed=0;consumed=false;
            for(int i=0;i<9;i++)getItems().set(i,remainders.get(i));
            remainders=NonNullList.withSize(9,ItemStack.EMPTY);setChanged();
            reserved=NonNullList.withSize(9,ItemStack.EMPTY);
        }else if(elapsed%20==0)setChanged();
    }
    public void node(Player player,InteractionHand hand,int slot){
        if(working() || !stillValid(player))return;
        var held=player.getItemInHand(hand);
        if(held.isEmpty()){
            var item=removeItem(slot,1);if(!player.getInventory().add(item))player.drop(item,false);
        }else if(getItem(slot).isEmpty()){setItem(slot,held.copyWithCount(1));if(!player.getAbilities().instabuild)held.shrink(1);}
    }
    public boolean collect(Player player){
        if(working() || output.isEmpty())return false;
        var item=output;output=ItemStack.EMPTY;if(!player.getInventory().add(item))player.drop(item,false);setChanged();return true;
    }
    public void dropContents(){
        Containers.dropContents(level,worldPosition,this);
        if(!output.isEmpty())Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,output);
        clearContent();output=ItemStack.EMPTY;pending=ItemStack.EMPTY;duration=0;
    }
    protected Component getDefaultName(){return Component.translatable("block.omnira.advanced_assembly_table");}
    protected AbstractContainerMenu createMenu(int id,Inventory inv){return new AdvancedForgeMenu(id,inv,this);}
    private void write(CompoundTag tag,HolderLookup.Provider registries){
        tag.putBoolean("StormDamaged",stormDamaged);
        tag.putInt("NodeOrderVersion",1);
        tag.putInt("Elapsed",elapsed);tag.putInt("Duration",duration);tag.putInt("Button",buttonTicks);tag.putBoolean("Consumed",consumed);
        if(!pending.isEmpty())tag.put("Pending",pending.save(registries));if(!output.isEmpty())tag.put("Output",output.save(registries));
        var remaining=new CompoundTag();ContainerHelper.saveAllItems(remaining,remainders,registries);tag.put("Remainders",remaining);
        var inputs=new CompoundTag();ContainerHelper.saveAllItems(inputs,reserved,registries);tag.put("Reserved",inputs);
    }
    protected void saveAdditional(CompoundTag tag,HolderLookup.Provider r){super.saveAdditional(tag,r);write(tag,r);}
    protected void loadAdditional(CompoundTag tag,HolderLookup.Provider r){
        stormDamaged=tag.getBoolean("StormDamaged");
        super.loadAdditional(tag,r);elapsed=Math.max(0,tag.getInt("Elapsed"));duration=Math.max(0,tag.getInt("Duration"));buttonTicks=tag.getInt("Button");consumed=tag.getBoolean("Consumed");
        pending=ItemStack.parseOptional(r,tag.getCompound("Pending"));output=ItemStack.parseOptional(r,tag.getCompound("Output"));
        remainders=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag.getCompound("Remainders"),remainders,r);
        reserved=NonNullList.withSize(9,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag.getCompound("Reserved"),reserved,r);
        if(tag.getInt("NodeOrderVersion")==0){
            migrateNodes(getItems());migrateNodes(remainders);migrateNodes(reserved);
        }
        syncedAt=level==null?0:level.getGameTime();
    }
    /** Old saves (including bottled and working forges) used middle / right / left. */
    private static void migrateNodes(NonNullList<ItemStack> items){
        var left=items.get(8);items.set(8,items.get(7));items.set(7,items.get(6));items.set(6,left);
    }
    public CompoundTag getUpdateTag(HolderLookup.Provider r){var tag=super.getUpdateTag(r);write(tag,r);return tag;}
    public AABB getRenderBoundingBox(){return new AABB(worldPosition.getX()-1,worldPosition.getY(),worldPosition.getZ()-1,worldPosition.getX()+2,worldPosition.getY()+2,worldPosition.getZ()+2);}
}
