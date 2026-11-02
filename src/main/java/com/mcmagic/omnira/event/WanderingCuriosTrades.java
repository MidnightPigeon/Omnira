package com.mcmagic.omnira.event;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Independent per-trader chances, rather than a chance diluted by the vanilla offer pool. */
@EventBusSubscriber(modid="omnira")
public final class WanderingCuriosTrades {
    @SubscribeEvent public static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(event.getLevel().isClientSide||!(event.getEntity() instanceof WanderingTrader trader))return;
        var data=trader.getPersistentData();if(data.getBoolean("OmniraCuriosOffers"))return;
        data.putBoolean("OmniraCuriosOffers",true);
        var random=trader.getRandom();
        boolean statue=random.nextInt(100)<5,core=random.nextInt(100)<2;
        if(statue)trader.getOffers().add(offer(ModItems.NIGHT_HERON_STATUE.get(),32));
        if(core)trader.getOffers().add(offer(ModItems.DREAM_SPELL_CORE.get(),64));
    }
    public static MerchantOffer offer(Item item,int emeralds){return new MerchantOffer(new ItemCost(Items.EMERALD,emeralds),new ItemStack(item),1,1,0);}
    private WanderingCuriosTrades(){}
}
