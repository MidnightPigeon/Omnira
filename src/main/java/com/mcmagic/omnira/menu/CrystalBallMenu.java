package com.mcmagic.omnira.menu;

import com.mcmagic.omnira.block.entity.*;
import com.mcmagic.omnira.item.OrbUpgradeItem;
import com.mcmagic.omnira.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CrystalBallMenu extends AbstractContainerMenu {
    public final CrystalBallBlockEntity ball;
    private final Player viewer;
    private FluidStack lastFluid=FluidStack.EMPTY;
    public final int storageSlots,upgradeStart,playerStart,frequencyStart;
    public CrystalBallMenu(int id,Inventory inventory,RegistryFriendlyByteBuf data) {
        this(id,inventory,clientView(inventory,data));
    }
    private static CrystalBallBlockEntity clientView(Inventory inventory,RegistryFriendlyByteBuf data){
        var level=inventory.player.level();
        var pos=data.readBlockPos();
        var source=(CrystalBallBlockEntity)level.getBlockEntity(pos);
        if(!data.isReadable() || !data.readBoolean())return source;
        // Personal menu slots must never receive public block updates (including PendingLoot).
        CrystalBallBlockEntity view=source instanceof LiquidCrystalBallBlockEntity
                ?new LiquidCrystalBallBlockEntity(pos,source.getBlockState()):new CrystalBallBlockEntity(pos,source.getBlockState());
        view.virtualStorage=true;
        view.lootrPersonalView=true;
        view.setLevel(level);
        if(view instanceof LiquidCrystalBallBlockEntity liquid)liquid.tank.setFluid(FluidStack.OPTIONAL_STREAM_CODEC.decode(data));
        return view;
    }
    public CrystalBallMenu(int id,Inventory inventory,CrystalBallBlockEntity ball) {
        super(ModMenuTypes.CRYSTAL_BALL.get(),id);this.ball=ball;viewer=inventory.player;ball.startOpen(inventory.player);
        storageSlots=liquid()||singleOrb()?1:12;upgradeStart=storageSlots;frequencyStart=upgradeStart+(voidOrb()?2:3);playerStart=frequencyStart+(ball.resonant()?2:0);
        if(singleOrb())addSlot(new Slot(ball,0,80,55){
            @Override public int getMaxStackSize(ItemStack stack){return ball.getMaxStackSize(stack);}
            @Override public boolean mayPlace(ItemStack stack){return ball.canPlaceItem(0,stack);}
        });
        else if(liquid())addSlot(new Slot(ball,0,-26,55) {
            @Override public int getMaxStackSize(){return 1;}
            @Override public int getMaxStackSize(ItemStack stack){return 1;}
            @Override public boolean mayPlace(ItemStack stack){return ball.canPlaceItem(0,stack);}
        });
        else {
            int slot=0;
            for(int row=0;row<4;row++) {
                int columns=row==0 || row==3?2:4;
                for(int col=0;col<columns;col++)addSlot(new Slot(ball,slot++,88-columns*9+col*18,29+row*18) {
                    @Override public int getMaxStackSize(ItemStack stack){return ball.getMaxStackSize(stack);}
                    @Override public boolean mayPlace(ItemStack stack){return ball.canPlaceItem(getContainerSlot(),stack);}
                });
            }
        }
        for(int i=0;i<(voidOrb()?2:3);i++) {
            final int slot=i;
            addSlot(new SlotItemHandler(ball.upgrades,slot,voidOrb()?66+i*28:53+i*27,119) {
                @Override public boolean isActive(){return !com.mcmagic.omnira.compat.LootrOrbs.appearance(ball);}
                @Override public boolean mayPlace(ItemStack stack){return isActive()&&super.mayPlace(stack);}
                @Override public boolean mayPickup(Player player){return isActive()&&ball.upgrades.mayRemove(slot);}
            });
        }
        if(ball.resonant())for(int i=0;i<2;i++){
            final int frequency=i;
            addSlot(new Slot(new SimpleContainer(2){
                @Override public ItemStack getItem(int index){return ball.frequency[index];}
                @Override public void setItem(int index,ItemStack stack){ball.frequency[index]=stack.copyWithCount(1);}
            },i,184,43+i*27){
                @Override public boolean mayPickup(Player player){return false;}
                @Override public boolean mayPlace(ItemStack stack){return false;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,160+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,218));
    }
    public boolean liquid(){return ball instanceof LiquidCrystalBallBlockEntity;}
    public boolean voidOrb(){return ball instanceof VoidCrystalBallBlockEntity;}
    public boolean starOrb(){return ball instanceof com.mcmagic.omnira.space.sea.StarClusterOrbEntity;}
    public boolean singleOrb(){return voidOrb()||starOrb();}
    @Override public void clicked(int index,int button,ClickType type,Player player) {
        com.mcmagic.omnira.progression.BranchStorage.manually(()->clickStorage(index,button,type,player));
    }
    private void clickStorage(int index,int button,ClickType type,Player player) {
        if(UpgradeMenuFeedback.rejectClick(this,index,button,type,player,upgradeStart,frequencyStart))return;
        if(ball.resonant()&&index>=frequencyStart&&index<frequencyStart+2){
            if(type==ClickType.PICKUP&&(button==0||button==1)&&!player.level().isClientSide)ball.setFrequency(index-frequencyStart,getCarried());return;
        }
        if(LargeStackMenuClicks.handle(this,index,button,type,player,0,storageSlots))return;
        super.clicked(index,button,type,player);
    }
    @Override public boolean stillValid(Player player){return ball.lootrPersonalView?ball.personalValid.getAsBoolean() && player.level().getBlockState(ball.getBlockPos()).is(ball.getBlockState().getBlock()) && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(ball.getBlockPos()))<=64:ball.stillValid(player);}
    public void writeInitialPersonalFluid(RegistryFriendlyByteBuf buffer,LiquidCrystalBallBlockEntity liquid){
        lastFluid=liquid.tank.getFluid().copy();
        FluidStack.OPTIONAL_STREAM_CODEC.encode(buffer,lastFluid);
    }
    @Override public void broadcastChanges(){if(ball.lootrPersonalView && viewer instanceof net.minecraft.server.level.ServerPlayer player){
        if(ball instanceof LiquidCrystalBallBlockEntity liquid){
            liquid.tickContainerPort();
            var fluid=liquid.tank.getFluid();
            if(!FluidStack.matches(lastFluid,fluid)){
                lastFluid=fluid.copy();
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,new com.mcmagic.omnira.network.LootrMenuFluidPayload(containerId,lastFluid));
            }
        }
        ball.setChanged();
    }
    super.broadcastChanges();}
    @Override public ItemStack quickMoveStack(Player player,int index) {
        var result=new ItemStack[]{ItemStack.EMPTY};
        com.mcmagic.omnira.progression.BranchStorage.manually(()->result[0]=moveStorage(player,index));return result[0];
    }
    private ItemStack moveStorage(Player player,int index) {
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index<playerStart) {
            var previous=stack.copy();
            boolean moved=false;
            while(!stack.isEmpty()) {
                int before=stack.getCount();
                if(!moveItemStackTo(stack,playerStart,slots.size(),true) || stack.getCount()==before)break;
                moved=true;
            }
            if(!moved)return ItemStack.EMPTY;
            if(index<storageSlots)com.mcmagic.omnira.progression.BranchStorage.items(ball,index,previous,stack);
        } else if(stack.getItem() instanceof OrbUpgradeItem) {
            if(!moveItemStackTo(stack,upgradeStart,frequencyStart,false)&&(!singleOrb()||!moveItemStackTo(stack,0,1,false))){
                UpgradeMenuFeedback.incompatible(this,stack,player,upgradeStart,frequencyStart);return ItemStack.EMPTY;
            }
        } else if(liquid()) {
            if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        } else {
            for(int i=0;i<storageSlots && !stack.isEmpty();i++) {var remainder=ball.itemHandler.insertItem(i,stack,false);stack.setCount(remainder.getCount());}
            if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(player,stack);return original;
    }
    @Override public void removed(Player player){super.removed(player);ball.stopOpen(player);if(ball.lootrPersonalView)ball.personalClose.accept(player);}
}
