package com.mcmagic.omnira.client;

import com.mcmagic.omnira.network.RemoteContainerSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Detached client copy, exposed only while constructing the native remote screen. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira",value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class RemoteContainerView {
    private static int menuId=-1;
    private static boolean reading;
    private static BlockEntity entity;
    private static BlockState state;
    public static void receive(RemoteContainerSnapshot payload){
        var level=Minecraft.getInstance().level;if(level==null||payload.snapshot()==null)return;
        var tag=payload.snapshot().getCompound("Entity");var pos=new BlockPos(tag.getInt("x"),tag.getInt("y"),tag.getInt("z"));
        var next=NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK),payload.snapshot().getCompound("State"));
        if(menuId!=payload.menuId()||entity==null||!entity.getBlockPos().equals(pos)||state==null||next.getBlock()!=state.getBlock()){
            entity=BlockEntity.loadStatic(pos,next,tag,level.registryAccess());if(entity!=null)entity.setLevel(level);
        }else {entity.setBlockState(next);entity.handleUpdateTag(tag,level.registryAccess());}
        menuId=payload.menuId();state=next;
    }
    public static void begin(int id){reading=menuId==id&&entity!=null;}
    public static void end(){reading=false;}
    public static BlockEntity entity(BlockPos pos){return reading&&entity!=null&&entity.getBlockPos().equals(pos)?entity:null;}
    public static BlockState state(BlockPos pos){return entity(pos)!=null?state:null;}
    private static void clear(){menuId=-1;reading=false;entity=null;state=null;}
    @net.neoforged.bus.api.SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){clear();}
    @net.neoforged.bus.api.SubscribeEvent
    public static void closed(net.neoforged.neoforge.client.event.ScreenEvent.Closing event){
        if(event.getScreen() instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen&&screen.getMenu().containerId==menuId)clear();
    }
    private RemoteContainerView(){}
}
