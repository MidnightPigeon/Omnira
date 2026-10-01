package com.mcmagic.omnira.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import com.mcmagic.omnira.registry.ModAttributes;

/** One equipped source per accessory family; inventory grids are only editable. */
@net.neoforged.fml.common.EventBusSubscriber(modid="omnira")
public final class PrimarySpellAccessory {
    private static final java.util.List<String> SLOTS=java.util.List.of("spell_core","crystal_grid");
    private static final ResourceLocation CORE_ID=ResourceLocation.fromNamespaceAndPath("omnira","primary_spell_core");
    private static final ResourceLocation GRID_ID=ResourceLocation.fromNamespaceAndPath("omnira","primary_crystal_grid");
    private PrimarySpellAccessory(){}
    public static int firstIndex(LivingEntity entity,String slot){
        if(entity==null)return -1;
        return CuriosApi.getCuriosInventory(entity).flatMap(inv->inv.getStacksHandler(slot)).map(handler->{
            var stacks=handler.getStacks();
            for(int i=0;i<stacks.getSlots();i++){
                var item=stacks.getStackInSlot(i).getItem();
                if(slot.equals("spell_core")?item instanceof SpellCoreItem:item instanceof CrystalGridItem)return i;
            }
            return -1;
        }).orElse(-1);
    }
    public static ItemStack equipped(LivingEntity entity,String slot){
        int index=firstIndex(entity,slot);
        return index<0?ItemStack.EMPTY:CuriosApi.getCuriosInventory(entity).flatMap(inv->inv.findCurio(slot,index)).map(result->result.stack()).orElse(ItemStack.EMPTY);
    }
    public static boolean primary(SlotContext context){
        if(context.cosmetic())return false;
        int index=firstIndex(context.entity(),context.identifier());
        return context.index()==(index<0?0:index);
    }
    public static ResourceLocation modifierId(String slot){return slot.equals("spell_core")?CORE_ID:GRID_ID;}
    @net.neoforged.bus.api.SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST)
    public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event){
        var player=event.getEntity();if(player.level().isClientSide)return;
        for(String slot:SLOTS){
            var stack=equipped(player,slot);var id=modifierId(slot);
            var wanted=stack.getItem() instanceof ICurioItem item?item.getAttributeModifiers(
                    new SlotContext(slot,player,Math.max(0,firstIndex(player,slot)),false,true),id,stack):com.google.common.collect.ImmutableMultimap.<net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute>,net.minecraft.world.entity.ai.attributes.AttributeModifier>of();
            // Curios only refreshes changed stacks. Reconcile when removal promotes a later slot.
            for(var attribute:java.util.List.of(ModAttributes.MAX_MANA,ModAttributes.MANA_REGEN,ModAttributes.SPELL_POWER,ModAttributes.COST_REDUCTION,ModAttributes.COOLDOWN_REDUCTION)){
                var instance=player.getAttribute(attribute);if(instance==null)continue;
                var desired=wanted.get(attribute).stream().findFirst().orElse(null);
                if(java.util.Objects.equals(instance.getModifier(id),desired))continue;
                instance.removeModifier(id);if(desired!=null)instance.addTransientModifier(desired);
            }
        }
    }
}
