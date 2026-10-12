package com.mcmagic.omnira.workstation;

import com.mcmagic.omnira.block.entity.CrystalProcessingTableBlockEntity;
import com.mcmagic.omnira.registry.ModAttachments;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class CrystalWorkstationEntity extends CrystalProcessingTableBlockEntity implements WorkstationAccess,net.minecraft.world.WorldlyContainer {
    public BlockEntity host;
    public int durability=16,progress,duration,burn,burnTotal,energy,energyRemainder;
    public double repairMana;
    public float rpm,experience;
    public boolean electrical,mechanical;
    public String rename;
    public final java.util.Set<String> removedEnchantments=new java.util.HashSet<>();
    private ItemStack previous0=ItemStack.EMPTY,previous1=ItemStack.EMPTY;
    private WorkstationRecipes.Result cached;
    private int ticks;
    public CrystalWorkstationEntity(BlockPos pos,BlockState s){super(CrystalWorkstations.ENTITY.get(),pos,s,3);}
    @Override public CrystalWorkstationEntity workstation(){return this;}
    public CrystalWorkstations.Kind kind(){return CrystalWorkstations.Kind.values()[CrystalWorkstations.index(getBlockState())%3];}
    public boolean infused(){return CrystalWorkstations.index(getBlockState())>=3;}
    public boolean powered(){return mechanical||electrical;}
    @Override public int getContainerSize(){return 3;}
    @Override protected Component getDefaultName(){return getBlockState().getBlock().getName();}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inventory){return new CrystalWorkstationMenu(id,inventory,this);}
    @Override public boolean stillValid(Player p){return p.level()==level&&p.canInteractWithBlock(worldPosition,4)&&CrystalWorkstations.work(level.getBlockEntity(worldPosition))==this;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        if(slot<0||slot>1)return false;
        if(kind()==CrystalWorkstations.Kind.FURNACE)return slot==0||fuelTime(stack)>0;
        return kind()!=CrystalWorkstations.Kind.GRINDSTONE||stack.isDamageableItem()||net.minecraft.world.item.enchantment.EnchantmentHelper.hasAnyEnchantments(stack)||stack.canGrindstoneRepair();
    }
    @Override public int[] getSlotsForFace(Direction face){return face==Direction.DOWN?new int[]{2}:kind()==CrystalWorkstations.Kind.FURNACE?face==Direction.UP?new int[]{0}:new int[]{1}:new int[]{0,1};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction face){return slot!=2&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction face){return slot==2;}
    private final com.mcmagic.omnira.energy.EnergyInputLimit inputLimit=new com.mcmagic.omnira.energy.EnergyInputLimit(256);
    public final net.neoforged.neoforge.energy.IEnergyStorage energyInput=new net.neoforged.neoforge.energy.IEnergyStorage(){
        public int receiveEnergy(int amount,boolean simulate){
            if(level==null||level.isClientSide)return 0;
            int n=inputLimit.receive(level.getGameTime(),amount,5120-energy,simulate);
            if(!simulate&&n>0){energy+=n;level.blockEntityChanged(worldPosition);}return n;
        }
        public int extractEnergy(int amount,boolean simulate){return 0;}
        public int getEnergyStored(){return energy;}
        public int getMaxEnergyStored(){return 5120;}
        public boolean canReceive(){return true;}
        public boolean canExtract(){return false;}
    };
    public net.neoforged.neoforge.energy.IEnergyStorage energyPort(Direction side){
        return side!=null&&((CrystalWorkstationBlock)getBlockState().getBlock()).shaft(getBlockState(),side)?energyInput:null;
    }
    public void tickDirect(){
        boolean drive=false;
        for(Direction face:Direction.Plane.HORIZONTAL)if(((CrystalWorkstationBlock)getBlockState().getBlock()).shaft(getBlockState(),face))
            drive|=com.mcmagic.omnira.space.SpacetimePortState.driveAt(level,worldPosition.relative(face),face.getOpposite())>=1;
        tick(drive,drive?32:0);
    }
    public void tick(boolean rotating,float speed){
        if(level==null||level.isClientSide)return;
        ticks++;refreshInputs();
        boolean before=electrical,wasMechanical=mechanical;float oldRpm=rpm;
        mechanical=rotating;int cost=(256+energyRemainder)/20;
        electrical=infused()&&kind()!=CrystalWorkstations.Kind.GRINDSTONE&&!mechanical&&energy>=cost;
        if(electrical){energy-=cost;energyRemainder=(256+energyRemainder)%20;for(Direction face:Direction.Plane.HORIZONTAL)if(energyPort(face)!=null)com.mcmagic.omnira.energy.ShaftCurrent.spark(level,worldPosition,face);}
        rpm=mechanical?speed:0;
        if(kind()==CrystalWorkstations.Kind.FURNACE)smelt();
        else if(kind()==CrystalWorkstations.Kind.ANVIL){
            if(!infused()&&durability<16&&ticks%20==0)repairAnvil();
            if(infused()&&powered()&&getItem(2).isEmpty()){
                var result=recipe();duration=Math.max(0,result.levels())*400;
                if(!result.stack().isEmpty()&&duration>0){
                    if(ticks%8==0)WorkstationEffects.processing(this);
                    if(++progress>=duration)finish(null,true);
                }
            }
        }else if(infused()&&ticks%100==0){
            for(int i=0;i<2;i++)if(getItem(i).isDamaged()){getItem(i).setDamageValue(getItem(i).getDamageValue()-1);cached=null;setChanged();WorkstationEffects.repaired(this,i);}
            previous0=getItem(0).copy();previous1=getItem(1).copy();
        }
        if(before!=electrical||wasMechanical!=mechanical||oldRpm!=rpm||ticks%20==0)setChanged();
    }
    private void repairAnvil(){
        double remaining=Math.min(10,200-repairMana);
        for(var player:level.players()){
            if(!player.isAlive()||player.isSpectator()||player.distanceToSqr(Vec3.atCenterOf(worldPosition))>25)continue;
            var mana=player.getData(ModAttachments.MANA);double take=Math.min(remaining,mana.current());
            if(take<=0)continue;
            player.setData(ModAttachments.MANA,mana.spend(take));repairMana+=take;remaining-=take;WorkstationEffects.absorbing(this,player);
            if(remaining<=0)break;
        }
        if(repairMana>=200){durability++;repairMana-=200;}setChanged();
    }
    private void refreshInputs(){
        if(!ItemStack.matches(previous0,getItem(0))||!ItemStack.matches(previous1,getItem(1))){
            if(kind()!=CrystalWorkstations.Kind.FURNACE||!ItemStack.isSameItemSameComponents(previous0,getItem(0)))progress=0;
            previous0=getItem(0).copy();previous1=getItem(1).copy();cached=null;removedEnchantments.clear();
        }
    }
    public void changedSelection(){cached=null;progress=0;setChanged();}
    public WorkstationRecipes.Result recipe(){
        refreshInputs();
        if(cached==null)cached=WorkstationRecipes.calculate(this,net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft((ServerLevel)level));
        return cached;
    }
    public ItemStack preview(){return !getItem(2).isEmpty()?getItem(2):kind()==CrystalWorkstations.Kind.FURNACE?ItemStack.EMPTY:recipe().stack();}
    public boolean canTake(Player p){
        if(!getItem(2).isEmpty())return true;
        if(kind()==CrystalWorkstations.Kind.FURNACE)return false;
        var r=recipe();if(r.stack().isEmpty())return false;
        if(kind()==CrystalWorkstations.Kind.ANVIL)return (infused()||durability>0)&&(p.hasInfiniteMaterials()||p.experienceLevel>=r.levels());
        return !infused()||p.getData(ModAttachments.MANA).canSpend(r.mana());
    }
    public ItemStack take(Player player,int count){
        if(!canTake(player))return ItemStack.EMPTY;
        if(getItem(2).isEmpty())finish(player,false);
        var result=removeItem(2,count);
        if(kind()==CrystalWorkstations.Kind.FURNACE&&experience>0){
            int xp=(int)experience;if(level.random.nextFloat()<experience-xp)xp++;
            net.minecraft.world.entity.ExperienceOrb.award((ServerLevel)level,player.position(),xp);experience=0;
        }
        setChanged();return result;
    }
    private void finish(Player player,boolean automatic){
        var r=recipe();if(r.stack().isEmpty()||!getItem(2).isEmpty())return;
        if(kind()==CrystalWorkstations.Kind.ANVIL){
            if(!automatic&&!player.hasInfiniteMaterials())player.giveExperienceLevels(-r.levels());
            if(!automatic)net.neoforged.neoforge.common.CommonHooks.onAnvilRepair(player,r.stack(),getItem(0),getItem(1));
            if(!infused())durability--;
            getItem(0).setCount(0);if(r.materials()>0)getItem(1).shrink(r.materials());else getItem(1).setCount(0);
            level.levelEvent(1030,worldPosition,0);
            WorkstationEffects.completed(this);
        }else{
            if(infused()){
                var mana=player.getData(ModAttachments.MANA);player.setData(ModAttachments.MANA,mana.spend(r.mana()));
                getItem(0).setCount(0);getItem(1).setCount(0);level.levelEvent(1042,worldPosition,0);
            }else{
                r.grinder().getSlot(2).onTake(player,r.stack().copy());
                setItem(0,r.grinder().getSlot(0).getItem().copy());setItem(1,r.grinder().getSlot(1).getItem().copy());
            }
        }
        setItem(2,r.stack().copy());progress=0;rename=null;cached=null;removedEnchantments.clear();setChanged();
    }
    private void smelt(){
        if(powered()){burn=0;burnTotal=0;}
        else if(burn>0)burn--;
        if(getItem(0).isEmpty()){progress=0;return;}
        var found=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(getItem(0)),level);
        if(found.isEmpty()){progress=0;return;}
        var recipe=found.get().value();var result=recipe.assemble(new SingleRecipeInput(getItem(0)),level.registryAccess());
        duration=Math.max(1,recipe.getCookingTime()/2);
        var output=getItem(2);
        if(result.isEmpty()||!output.isEmpty()&&(!ItemStack.isSameItemSameComponents(output,result)||output.getCount()+result.getCount()>output.getMaxStackSize()))return;
        if(!powered()&&burn==0){
            var fuel=getItem(1);burn=fuelTime(fuel);burnTotal=burn;
            if(burn>0){var remainder=fuel.getCraftingRemainingItem();fuel.shrink(1);if(fuel.isEmpty())setItem(1,remainder);}
        }
        if(!powered()&&burn==0)return;
        if(ticks%8==0)WorkstationEffects.processing(this);
        if(++progress>=duration){
            getItem(0).shrink(1);if(output.isEmpty())setItem(2,result.copy());else output.grow(result.getCount());
            experience+=recipe.getExperience();progress=0;setChanged();
        }
        // Fuel and input counts are bookkeeping, not a changed smelting recipe.
        previous0=getItem(0).copy();previous1=getItem(1).copy();
    }
    private static int fuelTime(ItemStack stack){return stack.is(com.mcmagic.omnira.registry.ModItems.INFUSED_SPIRITUAL_CRYSTAL.get())?1600:stack.getBurnTime(RecipeType.SMELTING);}
    @Override public void setChanged(){super.setChanged();if(host!=null){host.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}}
    public void writeWork(CompoundTag t,HolderLookup.Provider r){saveAdditional(t,r);}
    @Override protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components){
        super.collectImplicitComponents(components);
        if(kind()==CrystalWorkstations.Kind.ANVIL&&!infused()){
            var t=new CompoundTag();t.putInt("CrystalAnvilDurability",durability);t.putDouble("CrystalAnvilRepairMana",repairMana);
            components.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(t));
        }
    }
    @Override protected void applyImplicitComponents(BlockEntity.DataComponentInput components){
        super.applyImplicitComponents(components);
        var data=components.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if(data.contains("CrystalAnvilDurability")){durability=Math.clamp(data.getInt("CrystalAnvilDurability"),0,16);repairMana=Math.clamp(data.getDouble("CrystalAnvilRepairMana"),0,199.999);}
    }
    public void readWork(CompoundTag t,HolderLookup.Provider r){loadAdditional(t,r);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();saveAdditional(t,r);return t;}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){
        super.saveAdditional(t,r);t.putInt("Durability",durability);t.putDouble("RepairMana",repairMana);t.putInt("Progress",progress);t.putInt("Duration",duration);
        t.putInt("Burn",burn);t.putInt("BurnTotal",burnTotal);t.putInt("Energy",energy);t.putInt("EnergyRemainder",energyRemainder);
        t.putFloat("Rpm",rpm);t.putBoolean("Electrical",electrical);t.putBoolean("Mechanical",mechanical);t.putFloat("Experience",experience);if(rename!=null)t.putString("Rename",rename);
        var removed=new ListTag();removedEnchantments.stream().sorted().forEach(id->removed.add(StringTag.valueOf(id)));t.put("RemovedEnchantments",removed);
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        super.loadAdditional(t,r);durability=t.contains("Durability")?Math.clamp(t.getInt("Durability"),0,16):16;repairMana=Math.clamp(t.getDouble("RepairMana"),0,199.999);
        progress=Math.max(0,t.getInt("Progress"));duration=Math.max(0,t.getInt("Duration"));burn=Math.max(0,t.getInt("Burn"));burnTotal=Math.max(0,t.getInt("BurnTotal"));
        energy=Math.clamp(t.getInt("Energy"),0,5120);energyRemainder=Math.clamp(t.getInt("EnergyRemainder"),0,19);rpm=t.getFloat("Rpm");electrical=t.getBoolean("Electrical");mechanical=t.getBoolean("Mechanical");
        experience=Math.max(0,t.getFloat("Experience"));rename=t.contains("Rename")?t.getString("Rename"):null;removedEnchantments.clear();for(var id:t.getList("RemovedEnchantments",Tag.TAG_STRING))removedEnchantments.add(id.getAsString());
        previous0=getItem(0).copy();previous1=getItem(1).copy();cached=null;
    }
}
