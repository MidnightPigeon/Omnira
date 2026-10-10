package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.registry.ModBlockEntityTypes;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class LiquidCrystalBallBlockEntity extends CrystalBallBlockEntity {
    public static final int BASE_CAPACITY=24000;
    private boolean fluidTreasure;
    public void markFluidTreasure(){fluidTreasure=true;setChanged();}
    @Override public boolean hasPendingLoot(){return fluidTreasure || super.hasPendingLoot();}
    public static java.util.List<net.minecraft.world.level.material.Fluid> treasureFluids(){
        return com.mcmagic.omnira.config.OmniraLootConfig.treasureFluids();
    }
    @Override public void unpackLootTable(net.minecraft.world.entity.player.Player player){
        if(fluidTreasure && player!=null && !player.isSpectator() && level!=null && !level.isClientSide){
            var choices=treasureFluids();
            if(!choices.isEmpty()){
                var rolled=new FluidStack(choices.get(level.random.nextInt(choices.size())),tank.getCapacity());
                if(net.neoforged.fml.ModList.get().isLoaded("create"))
                    rolled=com.mcmagic.omnira.compat.TreasurePotionFluid.prepare(rolled,level.random);
                if(!rolled.isEmpty()){
                    fluidTreasure=false;tank.setFluid(rolled);setChanged();
                }
            }
        }
        super.unpackLootTable(player);
    }
    private long budgetTime=Long.MIN_VALUE;
    private int budget,portCredit;
    private ItemStack portSnapshot=ItemStack.EMPTY;
    private boolean portDone,portInput,portInternal;
    private int lastPortTick=Integer.MIN_VALUE;
    public final FluidTank localTank=new FluidTank(BASE_CAPACITY) {
        @Override public boolean isFluidValid(FluidStack stack){return isEmpty() || FluidStack.isSameFluidSameComponents(stack,getFluid());}
        @Override public int fill(FluidStack stack,FluidAction action){capacity=getCapacity();return super.fill(stack,action);}
        @Override public int getCapacity(){return BASE_CAPACITY*multiplier();}
        @Override public int getTankCapacity(int index){return getCapacity();}
        @Override protected void onContentsChanged(){setChanged();}
    };
    private FluidTank backingTank(){return ((LiquidCrystalBallBlockEntity)storage()).localTank;}
    public final FluidTank tank=new FluidTank(BASE_CAPACITY){
        @Override public FluidStack getFluid(){return backingTank().getFluid();}
        @Override public int getFluidAmount(){return backingTank().getFluidAmount();}
        @Override public boolean isEmpty(){return backingTank().isEmpty();}
        @Override public int getCapacity(){return backingTank().getCapacity();}
        @Override public int getTankCapacity(int index){return getCapacity();}
        @Override public FluidStack getFluidInTank(int index){return getFluid();}
        @Override public boolean isFluidValid(FluidStack stack){return backingTank().isFluidValid(stack);}
        @Override public boolean isFluidValid(int index,FluidStack stack){return isFluidValid(stack);}
        @Override public void setFluid(FluidStack stack){backingTank().setFluid(stack);setChanged();}
        @Override public int fill(FluidStack stack,FluidAction action){int amount=backingTank().fill(stack,action);if(action.execute()&&amount>0)setChanged();return amount;}
        @Override public FluidStack drain(int amount,FluidAction action){var result=backingTank().drain(amount,action);if(action.execute()&&!result.isEmpty())setChanged();return result;}
        @Override public FluidStack drain(FluidStack stack,FluidAction action){return FluidStack.isSameFluidSameComponents(stack,getFluid())?drain(stack.getAmount(),action):FluidStack.EMPTY;}
        @Override public CompoundTag writeToNBT(HolderLookup.Provider registries,CompoundTag tag){return backingTank().writeToNBT(registries,tag);}
        @Override public FluidTank readFromNBT(HolderLookup.Provider registries,CompoundTag tag){localTank.readFromNBT(registries,tag);return this;}
    };
    public final IFluidHandler fluidHandler=new IFluidHandler() {
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int index){return tank.getFluid().copy();}
        public int getTankCapacity(int index){return tank.getCapacity();}
        public boolean isFluidValid(int index,FluidStack stack){return tank.isFluidValid(index,stack);}
        public int fill(FluidStack stack,FluidAction action) {
            if(hasPendingLoot())return 0;
            refreshBudget();int amount=tank.fill(stack.copyWithAmount(Math.min(stack.getAmount(),budget)),action);
            if(action.execute())budget-=amount;return amount;
        }
        public FluidStack drain(FluidStack stack,FluidAction action) {
            if(!FluidStack.isSameFluidSameComponents(stack,tank.getFluid()))return FluidStack.EMPTY;
            return drain(stack.getAmount(),action);
        }
        public FluidStack drain(int amount,FluidAction action) {
            if(hasPendingLoot())return FluidStack.EMPTY;
            refreshBudget();int requested=Math.max(0,Math.min(amount,budget));
            var result=infinite()?tank.getFluid().copyWithAmount(requested):tank.drain(requested,action);
            if(action.execute())budget-=result.getAmount();return result;
        }
    };
    public LiquidCrystalBallBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntityTypes.LIQUID_CRYSTAL_BALL.get(),pos,state);}
    @Override public int getContainerSize(){return 1;}
    @Override public int getMaxStackSize(){return 1;}
    @Override public int getMaxStackSize(ItemStack stack){return 1;}
    private static boolean potionContainer(ItemStack stack){
        return net.neoforged.fml.ModList.get().isLoaded("create") && com.mcmagic.omnira.compat.OrbPotionContainers.accepts(stack);
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0 && (stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM)!=null || potionContainer(stack));}
    private FluidActionResult transferPort(ItemStack stack,boolean execute){
        if(potionContainer(stack))return com.mcmagic.omnira.compat.OrbPotionContainers.transfer(stack,tank,portInput,execute);
        return portInput?FluidUtil.tryEmptyContainer(stack,tank,1000,null,execute):FluidUtil.tryFillContainer(stack,tank,1000,null,execute);
    }
    private void resetPort() {
        if(portInternal)return;
        portSnapshot=getItem(0).copy();portCredit=0;portDone=false;portInput=false;
        var handler=portSnapshot.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM);
        if(handler!=null)for(int i=0;i<handler.getTanks();i++)if(!handler.getFluidInTank(i).isEmpty())portInput=true;
        if(potionContainer(portSnapshot))portInput=com.mcmagic.omnira.compat.OrbPotionContainers.filled(portSnapshot);
    }
    @Override public void setItem(int slot,ItemStack stack){super.setItem(slot,stack);if(slot==0)resetPort();}
    @Override public ItemStack removeItem(int slot,int count){var stack=super.removeItem(slot,count);if(slot==0)resetPort();return stack;}
    @Override public ItemStack removeItemNoUpdate(int slot){var stack=super.removeItemNoUpdate(slot);if(slot==0)resetPort();return stack;}
    @Override public void onLoad(){super.onLoad();if(level!=null)budgetTime=level.getGameTime();}
    public boolean infinite() {
        if(level==null || tank.isEmpty())return false;
        var fluid=tank.getFluid().getFluid();
        return fluid.canConvertToSource(fluid.defaultFluidState(),level,worldPosition);
    }
    private void refreshBudget() {
        if(level==null)return;long now=level.getGameTime();
        if(budgetTime==Long.MIN_VALUE)budgetTime=now;
        int speed=upgrades.speedMultiplier();
        budget=(int)Math.min(500*speed,budget+Math.max(0,Math.min(20,now-budgetTime))*25*speed);budgetTime=now;
    }
    public int reserveRemoteTransfer(int requested){refreshBudget();int amount=Math.min(Math.max(0,requested),budget);budget-=amount;return amount;}
    @Override public boolean fitsMultiplier(int multiplier){return tank.getFluidAmount()<=BASE_CAPACITY*multiplier;}
    @Override public void serverTick() {
        super.serverTick();refreshBudget();
        ((LiquidCrystalBallBlockEntity)storage()).tickPort();
    }
    private void tickPort(){
        int tick=level.getServer().getTickCount();if(lastPortTick==tick)return;lastPortTick=tick;refreshBudget();
        var stack=getItem(0);
        if(!ItemStack.matches(stack,portSnapshot)) {
            resetPort();
        }
        if(stack.isEmpty() || portDone || !canPlaceItem(0,stack))return;
        int unit=potionContainer(stack)?250:1000;
        int reserve=Math.min(25*upgrades.speedMultiplier(),Math.min(budget,unit-portCredit));budget-=reserve;portCredit+=reserve;
        if(portCredit<unit)return;
        var result=transferPort(stack,true);
        if(result.isSuccess()) {
            portInternal=true;
            try {setItem(0,result.getResult());} finally {portInternal=false;}
            portSnapshot=getItem(0).copy();portCredit=0;
            var next=transferPort(portSnapshot,false);
            portDone=!next.isSuccess();
        }
    }
    @Override protected Component getDefaultName(){return Component.translatable(resonant()?"block.omnira.resonant_liquid_crystal_ball":"block.omnira.liquid_crystal_ball");}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putBoolean("FluidTreasure",fluidTreasure);tag.put("Fluid",localTank.writeToNBT(registries,new CompoundTag()));tag.putBoolean("PortDone",portDone);tag.putBoolean("PortInput",portInput);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){fluidTreasure=tag.getBoolean("FluidTreasure");super.loadAdditional(tag,registries);tank.readFromNBT(registries,tag.getCompound("Fluid"));portSnapshot=getItem(0).copy();portDone=tag.getBoolean("PortDone");portInput=tag.getBoolean("PortInput");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=super.getUpdateTag(registries);tag.putBoolean("FluidTreasure",fluidTreasure);tag.put("Fluid",tank.writeToNBT(registries,new CompoundTag()));return tag;}
    @Override protected void shattered(net.minecraft.server.level.ServerLevel server) {
        var fluid=tank.getFluid();if(fluid.isEmpty())return;
        int sources=fluid.getAmount()/1000;
        var state=fluid.getFluid().defaultFluidState().createLegacyBlock();
        if(state.isAir())return;
        // Never overwrite a solid block, a container, or an occupied source.
        for(int radius=0;radius<=8 && sources>0;radius++)for(int x=-radius;x<=radius && sources>0;x++)for(int z=-radius;z<=radius && sources>0;z++) {
            if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            var pos=worldPosition.offset(x,0,z);
            if(server.hasChunkAt(pos) && server.getBlockState(pos).isAir()) {server.setBlockAndUpdate(pos,state);sources--;}
        }
    }
}
