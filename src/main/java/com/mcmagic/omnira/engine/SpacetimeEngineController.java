package com.mcmagic.omnira.engine;

import com.mcmagic.omnira.block.entity.ManaEngineState;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class SpacetimeEngineController extends BlockEntity implements MenuProvider {
    public static final int BASE_LIMIT=2048,MAX_RPM=128;
    public static boolean hasKinetics(){return net.neoforged.fml.ModList.get().isLoaded("create");}
    private final int[] capacity={512,512,512,512},rpm={16,16,16,16};
    private final boolean[] reversed=new boolean[4];
    private boolean loading;
    private int frontSide;
    public final SpacetimeEngineEnergy energy=new SpacetimeEngineEnergy(this);
    public int frontSide(){return frontSide;}
    public int relativeSide(int absoluteSide){return Math.floorMod(absoluteSide-frontSide,4);}
    public void setFront(Direction front){
        for(int i=0;i<4;i++)if(SpacetimeEngineBlock.OUTPUTS[i]==front){frontSide=i;sync();return;}
    }
    public final ItemStackHandler grids=new ItemStackHandler(3){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return ManaEngineState.multiplier(stack)>0;}
        @Override protected void onContentsChanged(int slot){if(!loading){clamp();sync();}}
    };
    public SpacetimeEngineController(BlockPos p,BlockState s){super(SpacetimeEngineContent.CONTROLLER.get(),p,s);}
    public double gridMultiplier(){
        double value=1;
        for(int i=0;i<3;i++)value*=Math.max(1,ManaEngineState.multiplier(grids.getStackInSlot(i)));
        return value;
    }
    public int limit(){return (int)Math.round(BASE_LIMIT*gridMultiplier());}
    private void clamp(){for(int i=0;i<4;i++){capacity[i]=Math.clamp(capacity[i],0,limit());rpm[i]=Math.clamp(rpm[i],0,MAX_RPM);}}
    public int capacity(int side){return side>=0&&side<4?Math.min(capacity[side],limit()):0;}
    public int rpm(int side){return side>=0&&side<4?rpm[side]:0;}
    public boolean reversed(int side){return side>=0&&side<4&&reversed[side];}
    public boolean configure(int side,int su,int speed,boolean reverse){
        if(side<0||side>=4||su<0||su>limit()||speed<0||speed>MAX_RPM)return false;
        capacity[side]=su;rpm[side]=speed;reversed[side]=reverse;sync();return true;
    }
    public boolean complete(){
        if(level==null||getBlockState().getValue(SpacetimeEngineBlock.PART)!=9||isRemoved())return false;
        var root=worldPosition.below();
        for(int i=0;i<10;i++){var p=root.offset(SpacetimeEngineBlock.offset(i));if(!level.hasChunkAt(p))return false;
            var state=level.getBlockState(p);if(!state.is(SpacetimeEngineContent.BLOCK.get())||state.getValue(SpacetimeEngineBlock.PART)!=i)return false;}
        return true;
    }
    public int generatedSpeed(int side){
        if(side<0||side>=4||!complete()||com.mcmagic.omnira.time.StilledTime.stopped(level,worldPosition))return 0;
        return (hasKinetics()?(capacity(side)>0?rpm(side):0):16)*(reversed(side)?-1:1);
    }
    public boolean configureRotation(int side,boolean reverse){
        if(side<0||side>=4)return false;
        reversed[side]=reverse;sync();return true;
    }
    private void sync(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void dropUpgrades(){
        if(level==null||level.isClientSide)return;
        for(int i=0;i<3;i++){net.minecraft.world.level.block.Block.popResource(level,worldPosition,grids.getStackInSlot(i));grids.setStackInSlot(i,ItemStack.EMPTY);}
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putIntArray("Capacity",capacity);tag.putIntArray("Rpm",rpm);
        tag.putInt("FrontSide",frontSide);
        energy.save(tag);
        for(int i=0;i<4;i++)tag.putBoolean("Reverse"+i,reversed[i]);tag.put("Grids",grids.serializeNBT(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);loading=true;grids.deserializeNBT(registries,tag.getCompound("Grids"));loading=false;
        frontSide=Math.floorMod(tag.getInt("FrontSide"),4);
        var c=tag.getIntArray("Capacity");var r=tag.getIntArray("Rpm");
        for(int i=0;i<4;i++){capacity[i]=c.length==4?c[i]:512;rpm[i]=r.length==4?r[i]:16;reversed[i]=tag.getBoolean("Reverse"+i);}clamp();energy.load(tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public Component getDisplayName(){return Component.translatable("block.omnira.spacetime_engine");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new SpacetimeEngineMenu(id,inv,this);}
}
