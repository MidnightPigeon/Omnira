package com.mcmagic.omnira.fate;

import com.mcmagic.omnira.item.FateCurioItem;
import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class ManuscriptReward {
    private ManuscriptReward() {}

    public static void finish(ServerPlayer player) {
        if(!player.getMainHandItem().is(ModItems.TRAVELER_MANUSCRIPT.get())
                && !player.getOffhandItem().is(ModItems.TRAVELER_MANUSCRIPT.get()))return;
        var advancement=player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath("omnira","too_late_scholar"));
        if(advancement!=null)player.getAdvancements().award(advancement,"read_final_page");
        if(!grant(player,"talent",new ItemStack(ModItems.PSYKER_TALENT.get())))return;
        var blessings=ModItems.BLESSINGS;
        grant(player,"curse",new ItemStack(blessings.get(player.getRandom().nextInt(blessings.size())).get()));
    }

    public static boolean grant(ServerPlayer player,String slot,ItemStack stack) {
        var inventory=CuriosApi.getCuriosInventory(player).orElse(null);
        if(inventory==null)return false;
        var handler=inventory.getStacksHandler(slot).orElse(null);
        if(handler==null)return false;
        for(int i=0;i<handler.getSlots();i++)if(!handler.getStacks().getStackInSlot(i).isEmpty())return false;
        if(handler.getSlots()==0) {
            inventory.addPermanentSlotModifier(slot,ResourceLocation.fromNamespaceAndPath("omnira","earned_"+slot),
                    1,AttributeModifier.Operation.ADD_VALUE);
            inventory.processSlots();
        }
        if(inventory.getStacksHandler(slot).map(h->h.getSlots()).orElse(0)<1)return false;
        inventory.setEquippedCurio(slot,0,stack);
        return true;
    }

    public static FateCurioItem.Kind equipped(ServerPlayer player,String slot) {
        return CuriosApi.getCuriosInventory(player).flatMap(inv->inv.getStacksHandler(slot))
                .filter(handler->handler.getSlots()>0).map(handler->handler.getStacks().getStackInSlot(0).getItem())
                .filter(FateCurioItem.class::isInstance).map(item->((FateCurioItem)item).kind()).orElse(null);
    }

    public static void cleanse(ServerPlayer player) {
        FateRuntime.clear(player);
        var inventory=CuriosApi.getCuriosInventory(player).orElse(null);
        if(inventory==null)return;
        for(String slot:java.util.List.of("talent","curse")) {
            inventory.getStacksHandler(slot).ifPresent(handler->{
                for(int i=0;i<handler.getSlots();i++) {
                    handler.getStacks().setStackInSlot(i,ItemStack.EMPTY);
                    handler.getCosmeticStacks().setStackInSlot(i,ItemStack.EMPTY);
                }
                // Empty before shrinking: cleansing destroys contents, not ejects them.
                for(var id:java.util.List.copyOf(handler.getModifiers().keySet()))inventory.removeSlotModifier(slot,id);
                handler.update();
                // Curios cache clearing defers resizing for the current tick; shrink first.
                if(!handler.getCachedModifiers().isEmpty())handler.clearCachedModifiers();
            });
        }
        inventory.processSlots();
    }
}
