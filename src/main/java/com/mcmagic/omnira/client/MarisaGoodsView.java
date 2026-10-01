package com.mcmagic.omnira.client;

import com.mcmagic.omnira.network.MarisaGoodsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import java.util.List;

@EventBusSubscriber(modid="omnira",value=Dist.CLIENT)
public final class MarisaGoodsView {
    private static List<ItemStack> goods=List.of();
    private static Object connection;
    private static long day=Long.MIN_VALUE;
    private MarisaGoodsView(){}
    public static void receive(MarisaGoodsPayload payload){
        connection=Minecraft.getInstance().getConnection();day=payload.day();goods=payload.goods();
    }
    public static List<ItemStack> goods(){
        var mc=Minecraft.getInstance();
        return connection==mc.getConnection()&&mc.level!=null&&Math.floorDiv(mc.level.getGameTime(),24000)==day?goods:List.of();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){goods=List.of();connection=null;day=Long.MIN_VALUE;}
}
