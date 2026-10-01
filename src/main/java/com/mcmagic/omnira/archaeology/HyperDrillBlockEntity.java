package com.mcmagic.omnira.archaeology;

import com.mcmagic.omnira.block.entity.ManaEngineAccess;
import com.mcmagic.omnira.registry.TimeNatureContent;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Idle/depleted machine only. A running rig is an independently saved entity. */
public final class HyperDrillBlockEntity extends BlockEntity {
    public static final int CAPACITY=1200,CRYSTAL=300;
    private int charge=CAPACITY;
    public HyperDrillBlockEntity(BlockPos p,BlockState s){super(ArchaeologyContent.DRILL_ENTITY.get(),p,s);}
    public int charge(){return charge;}
    public ItemStack item(){return HyperDrillItem.withCharge(new ItemStack(ArchaeologyContent.DRILL_ITEM.get()),charge);}
    public static void install(Level level,BlockPos root,int charge){
        boolean before=HyperDrillBlock.MOVING.get();HyperDrillBlock.MOVING.set(true);
        try{
            for(int n=0;n<18;n++)level.setBlock(root.offset(HyperDrillBlock.offset(n)),ArchaeologyContent.DRILL.get().defaultBlockState().setValue(HyperDrillBlock.PART,n).setValue(HyperDrillBlock.CHARGED,charge>=CAPACITY),2);
            if(level.getBlockEntity(root) instanceof HyperDrillBlockEntity drill){drill.charge=Math.clamp(charge,0,CAPACITY);drill.sync();}
        }finally{HyperDrillBlock.MOVING.set(before);}
    }
    private void addCharge(int amount){
        charge=Math.min(CAPACITY,charge+amount);sync();
        if(charge==CAPACITY)for(var p:HyperDrillBlock.parts(level,worldPosition))level.setBlock(p,level.getBlockState(p).setValue(HyperDrillBlock.CHARGED,true),2);
    }
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public void interact(Player player,ItemStack stack){interact(player,stack,false);}
    public void interact(Player player,ItemStack stack,boolean panel){
        if(com.mcmagic.omnira.time.StilledTime.stopped(level,worldPosition)||HyperDrillBlock.parts(level,worldPosition).size()!=18)return;
        if(charge<CAPACITY&&stack.is(TimeNatureContent.SPATIAL_SHARD.get())){
            addCharge(CRYSTAL);if(!player.isCreative())stack.shrink(1);
            player.displayClientMessage(Component.translatable("tooltip.omnira.drill.charge",charge*100/CAPACITY),true);return;
        }
        if(player.getVehicle() instanceof DrillSeat seat&&seat.anchor().equals(worldPosition)){
            if(panel&&charge==CAPACITY)bore(player);return;
        }
        if(player.isPassenger()||!level.getEntitiesOfClass(DrillSeat.class,new net.minecraft.world.phys.AABB(worldPosition).inflate(3),s->s.anchor().equals(worldPosition)&&s.isVehicle()).isEmpty())return;
        var seat=ArchaeologyContent.SEAT.get().create(level);if(seat==null)return;
        seat.place(worldPosition);level.addFreshEntity(seat);player.setYRot(180);player.startRiding(seat);
    }
    public void tick(){
        if(charge>=CAPACITY||level==null||HyperDrillBlock.parts(level,worldPosition).size()!=18)return;
        for(var d:Direction.Plane.HORIZONTAL){
            var p=worldPosition.relative(d,2);
            if(!level.hasChunkAt(p)||com.mcmagic.omnira.time.StilledTime.stopped(level,p))continue;
            var s=level.getBlockState(p);
            if(level.getBlockEntity(p) instanceof ManaEngineAccess engine&&s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    &&s.getValue(BlockStateProperties.HORIZONTAL_FACING)==d.getOpposite()&&engine.engineState().running()){addCharge(1);return;}
        }
    }
    public static int lowerLimit(Level level,int y){
        if(level.dimension().location().toString().equals("omnira:spacetime_corridor")){
            if(y>=com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE)return com.mcmagic.omnira.spacetime.CorridorLayout.TIME_BASE+1;
            if(y<112)return 25;
        }return level.getMinBuildHeight();
    }
    public void bore(Player player){
        if(level.isClientSide||charge<CAPACITY||!player.mayBuild()||!(player.getVehicle() instanceof DrillSeat seat)||!seat.anchor().equals(worldPosition))return;
        var rig=ArchaeologyContent.MOVING_RIG.get().create(level);if(rig==null)return;
        rig.start(worldPosition,player);
        if(!level.addFreshEntity(rig))return;
        HyperDrillBlock.clear(level,worldPosition);player.startRiding(rig,true);seat.discard();
    }
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);n.putInt("Charge",charge);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);charge=Math.clamp(n.getInt("Charge"),0,CAPACITY);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){var n=new CompoundTag();saveAdditional(n,r);return n;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
