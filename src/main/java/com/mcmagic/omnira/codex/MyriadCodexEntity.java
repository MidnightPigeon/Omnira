package com.mcmagic.omnira.codex;

import java.util.UUID;
import com.mcmagic.omnira.pulse.PulseNetworks;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class MyriadCodexEntity extends BlockEntity implements MenuProvider,CodexAccess {
    public BlockEntity host;
    public static MyriadCodexEntity from(BlockEntity entity){return entity instanceof CodexAccess access?access.codex():null;}
    @Override public MyriadCodexEntity codex(){return this;}
    @Override public double mechanicalPotential(){return host instanceof CodexAccess access?access.mechanicalPotential():0;}
    public float shaftSpeed(){return host instanceof CodexAccess access?access.shaftSpeed():0;}
    public boolean live(){return level!=null&&from(level.getBlockEntity(worldPosition))==this&&(host==null?!isRemoved():!host.isRemoved());}
    public UUID identity=UUID.randomUUID(),lease=UUID.randomUUID(),holder,job,requester;
    public BlockPos heartPos;
    public int potential,fade,mode,shape,axis=1,phase;
    public boolean hollow;
    public boolean electrical;
    private int outsideTicks;
    public final com.mcmagic.omnira.energy.ContinuousEnergyInput energyInput=new com.mcmagic.omnira.energy.ContinuousEnergyInput(
            8192,16384,()->level==null?0:level.getGameTime(),
            ()->level!=null&&!level.isClientSide&&!PulseNetworks.power(level,worldPosition).connected()&&mechanicalPotential()<8192);
    public final ItemStackHandler materials=new ItemStackHandler(2){
        @Override public int getSlotLimit(int slot){return 1;}
        @Override public boolean isItemValid(int slot,ItemStack stack){return !busy()&&CodexMaterials.allowed(stack);}
        @Override protected void onContentsChanged(int slot){sync();}
    };
    public MyriadCodexEntity(BlockPos p,BlockState s){super(MyriadCodexContent.ENTITY.get(),p,s);}
    public void saveTo(CompoundTag tag,HolderLookup.Provider registries){saveAdditional(tag,registries);}
    public static int diameter(double power){return power<8192?0:7+2*Math.clamp((int)Math.floor((Math.min(16384,power)+1e-6-8192)/2048),0,4);}
    public int diameter(){return diameter(potential);}
    public boolean busy(){return job!=null;}
    public HeartLink link(){return new HeartLink(level.dimension(),worldPosition,identity,lease);}
    public boolean validHolder(ServerPlayer p){return !busy()&&potential>=8192&&p.isAlive()&&!p.isSpectator()&&p.level()==level&&p.getUUID().equals(holder)&&link().matches(p.getMainHandItem());}
    public void takeHeart(ServerPlayer p){
        if(busy()||potential<8192||!p.getMainHandItem().isEmpty()||p.isSpectator())return;
        recall();holder=p.getUUID();p.setItemInHand(InteractionHand.MAIN_HAND,link().stack());sync();
    }
    public boolean placeHeart(ServerPlayer p,BlockPos pos){
        if(!validHolder(p)||!level.setBlock(pos,MyriadCodexContent.HEART.get().defaultBlockState(),3))return false;
        if(!(level.getBlockEntity(pos) instanceof WorldHeartEntity heart)){level.removeBlock(pos,false);return false;}
        heartPos=pos.immutable();holder=null;heart.link=link();heart.diameter=diameter();heart.setChanged();
        p.getMainHandItem().setCount(0);level.sendBlockUpdated(pos,heart.getBlockState(),heart.getBlockState(),2);sync();return true;
    }
    public void recall(){
        if(level==null||level.isClientSide)return;
        var oldLink=link();var oldPos=heartPos;heartPos=null;
        if(holder!=null){var p=level.getServer().getPlayerList().getPlayer(holder);if(p!=null){
            for(int i=0;i<p.getInventory().getContainerSize();i++)if(oldLink.matches(p.getInventory().getItem(i)))p.getInventory().setItem(i,ItemStack.EMPTY);
            if(oldLink.matches(p.containerMenu.getCarried()))p.containerMenu.setCarried(ItemStack.EMPTY);
            for(var slot:p.containerMenu.slots)if(oldLink.matches(slot.getItem()))slot.set(ItemStack.EMPTY);
        }}
        holder=null;fade=0;outsideTicks=0;lease=UUID.randomUUID();
        if(oldPos!=null&&level.hasChunkAt(oldPos)&&level.getBlockEntity(oldPos) instanceof WorldHeartEntity heart&&oldLink.equals(heart.link))level.removeBlock(oldPos,false);
        sync();
    }
    public void tick(){
        var power=PulseNetworks.power(level,worldPosition);
        double mechanical=mechanicalPotential();
        int next=power.connected()?(power.running()?(int)Math.floor(power.allocated()+1e-6):0):(int)Math.floor(mechanical>=8192?mechanical:energyInput.rate()+1e-6);
        boolean electric=!power.connected()&&mechanical<8192&&next>=8192;
        if(next!=potential||electric!=electrical){electrical=electric;potential=next;if(potential<8192)recall();sync();}
        if(busy()){
            CodexExecution.update(this);
            return;
        }
        if(heartPos!=null&&(!level.hasChunkAt(heartPos)||!(level.getBlockEntity(heartPos) instanceof WorldHeartEntity heart)||!link().equals(heart.link))){recall();return;}
        if(holder!=null){var p=level.getServer().getPlayerList().getPlayer(holder);if(p==null||!validHolder(p)){recall();return;}}
        var target=heartPos!=null?net.minecraft.world.phys.Vec3.atCenterOf(heartPos):holder==null?null:level.getServer().getPlayerList().getPlayer(holder).position();
        if(target!=null&&target.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(worldPosition))>32*32){
            outsideTicks++;if(outsideTicks%20==0){fade=outsideTicks/20;sync();}if(outsideTicks>=200)recall();
        }else if(outsideTicks!=0){outsideTicks=0;fade=0;sync();}
    }
    public String start(ServerPlayer player){
        if(busy())return "busy";
        if(potential<8192)return "power";
        if(heartPos==null||!level.hasChunkAt(heartPos)||!(level.getBlockEntity(heartPos) instanceof WorldHeartEntity heart)||!link().equals(heart.link))return "heart";
        if(fade>0||heartPos.distSqr(worldPosition)>32*32)return "range";
        return CodexExecution.start(this,player);
    }
    public void sync(){setChanged();if(host!=null)host.setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override public Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new MyriadCodexMenu(id,inventory,this);}
    @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){
        super.saveAdditional(t,r);t.putUUID("Identity",identity);t.putUUID("Lease",lease);if(holder!=null)t.putUUID("Holder",holder);if(job!=null)t.putUUID("Job",job);if(requester!=null)t.putUUID("Requester",requester);if(heartPos!=null)t.putLong("Heart",heartPos.asLong());t.putInt("Phase",phase);
        t.putInt("Potential",potential);t.putBoolean("Electrical",electrical);t.putInt("Fade",fade);t.putInt("OutsideTicks",outsideTicks);t.putInt("Mode",mode);t.putInt("Shape",shape);t.putInt("Axis",axis);t.putBoolean("Hollow",hollow);t.put("Materials",materials.serializeNBT(r));
    }
    @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){
        energyInput.reset();
        electrical=t.getBoolean("Electrical");
        super.loadAdditional(t,r);if(t.hasUUID("Identity"))identity=t.getUUID("Identity");if(t.hasUUID("Lease"))lease=t.getUUID("Lease");holder=t.hasUUID("Holder")?t.getUUID("Holder"):null;job=t.hasUUID("Job")?t.getUUID("Job"):null;requester=t.hasUUID("Requester")?t.getUUID("Requester"):null;heartPos=t.contains("Heart")?BlockPos.of(t.getLong("Heart")):null;phase=Math.clamp(t.getInt("Phase"),0,4);
        potential=Math.clamp(t.getInt("Potential"),0,16384);fade=Math.clamp(t.getInt("Fade"),0,10);outsideTicks=Math.clamp(t.getInt("OutsideTicks"),0,200);mode=Math.clamp(t.getInt("Mode"),0,2);shape=Math.clamp(t.getInt("Shape"),0,2);axis=Math.clamp(t.getInt("Axis"),0,2);hollow=t.getBoolean("Hollow");materials.deserializeNBT(r,t.getCompound("Materials"));
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var t=new CompoundTag();saveAdditional(t,r);return t;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
