package com.mcmagic.omnira.block.entity;

import com.mcmagic.omnira.block.NightHeronStatueBlock;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class NightHeronStatueBlockEntity extends BlockEntity {
    private int color;
    private long stretchStart=-1000,nextStretch;
    private boolean milkPending;
    private boolean autoMilk=true;
    public boolean autoMilk(){return autoMilk;}
    public void toggleAutoMilk() {
        if(level==null || level.isClientSide)return;
        autoMilk=!autoMilk;sync();
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,net.minecraft.sounds.SoundSource.BLOCKS,.5F,autoMilk?1.1F:.8F);
    }
    public NightHeronStatueBlockEntity(BlockPos p,BlockState s){super(ModBlockEntityTypes.NIGHT_HERON_STATUE.get(),p,s);}
    public int color(){return color;}
    public float stretch(float partialTick) {
        if(level==null)return 0;
        double elapsed=level.getGameTime()+partialTick-stretchStart;
        if(elapsed<0 || elapsed>60)return 0;
        return (float)Math.min(1,Math.min(elapsed/8,(60-elapsed)/14));
    }
    public void cycleColor() {
        color=(color+1)%3;sync();
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.STONE_BUTTON_CLICK_ON,net.minecraft.sounds.SoundSource.BLOCKS,.5F,1.1F);
    }
    public void tick() {
        if(level==null || level.isClientSide)return;
        long now=level.getGameTime();
        if(milkPending && now-stretchStart>=60){milkPending=false;setChanged();if(autoMilk)ejectMilk();}
        if(nextStretch==0){nextStretch=now+200+level.random.nextInt(500);setChanged();}
        if(now>=nextStretch){stretchStart=now;milkPending=true;nextStretch=now+300+level.random.nextInt(500);sync();}
    }
    public void call() {
        if(level!=null && !level.isClientSide)level.playSound(null,worldPosition,ModSounds.NIGHT_HERON_CALL.get(),net.minecraft.sounds.SoundSource.BLOCKS,.7F,1F);
    }
    public void ejectMilk() {
        if(level==null || level.isClientSide)return;
        var right=NightHeronStatueBlock.viewerRight(getBlockState().getValue(NightHeronStatueBlock.FACING));
        var destination=worldPosition.relative(right);
        if(level.hasChunkAt(destination) && level.getBlockEntity(destination) instanceof LiquidCrystalBallBlockEntity ball) {
            var fluid=new net.neoforged.neoforge.fluids.FluidStack(net.neoforged.neoforge.common.NeoForgeMod.MILK.get(),1000);
            if(ball.tank.fill(fluid,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE)==1000) {
                ball.tank.fill(fluid,net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);return;
            }
        }
        ItemStack milk=new ItemStack(ModItems.MILK_SUSPENSION.get());
        if(level.hasChunkAt(destination)) {
            var handler=level.getCapability(Capabilities.ItemHandler.BLOCK,destination,right.getOpposite());
            if(handler!=null)milk=ItemHandlerHelper.insertItemStacked(handler,milk,false);
        }
        if(!milk.isEmpty()) {
            var drop=new ItemEntity(level,worldPosition.getX()+.5+right.getStepX()*.72,worldPosition.getY()+.98,worldPosition.getZ()+.5+right.getStepZ()*.72,milk);
            drop.setDeltaMovement(right.getStepX()*.13,.08,right.getStepZ()*.13);level.addFreshEntity(drop);
        }
    }
    private void sync(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putInt("Color",color);tag.putLong("StretchStart",stretchStart);tag.putLong("NextStretch",nextStretch);tag.putBoolean("MilkPending",milkPending);tag.putBoolean("AutoMilk",autoMilk);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);color=Math.clamp(tag.getInt("Color"),0,2);stretchStart=tag.contains("StretchStart")?tag.getLong("StretchStart"):-1000;nextStretch=tag.getLong("NextStretch");milkPending=tag.getBoolean("MilkPending");autoMilk=!tag.contains("AutoMilk") || tag.getBoolean("AutoMilk");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
