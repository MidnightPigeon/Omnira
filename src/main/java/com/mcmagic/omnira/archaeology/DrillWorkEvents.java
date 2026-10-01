package com.mcmagic.omnira.archaeology;

@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class DrillWorkEvents {
    static final ThreadLocal<Boolean> ERASING=ThreadLocal.withInitial(()->false);
    @net.neoforged.bus.api.SubscribeEvent public static void drops(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){if(ERASING.get()&&event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity)event.setCanceled(true);}
    private DrillWorkEvents(){}
}
