package com.mcmagic.omnira.item;

import com.mcmagic.omnira.item.staff.*;
import com.mcmagic.omnira.menu.CrystalGridMenu;
import com.mcmagic.omnira.registry.*;
import com.mcmagic.omnira.spell.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** A complete Create implement. Active sessions are server-only and never survive logout. */
@EventBusSubscriber(modid="omnira")
public final class IndustrialWheelItem extends Item {
    public static final StaffAssembly.Stats STATS=new StaffAssembly.Stats(.2,10,1,40,1,1,0,0);
    private static final Map<UUID,Session> ACTIVE=new HashMap<>();
    private record Session(ItemStack staff,ItemStack grid,InteractionHand hand,net.minecraft.resources.ResourceKey<Level> dimension){}
    public IndustrialWheelItem(Properties p){super(p.stacksTo(1).rarity(Rarity.UNCOMMON).attributes(attributes()));}
    private static net.minecraft.world.item.component.ItemAttributeModifiers attributes(){
        var b=net.minecraft.world.item.component.ItemAttributeModifiers.builder();
        b.add(ModAttributes.SPELL_POWER,new net.minecraft.world.entity.ai.attributes.AttributeModifier(CastAttributes.STAFF_POWER,.2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        b.add(ModAttributes.COST_REDUCTION,new net.minecraft.world.entity.ai.attributes.AttributeModifier(CastAttributes.STAFF_DISCOUNT,10,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.MAINHAND);
        for(var attribute:List.of(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_INTERACTION_RANGE,net.minecraft.world.entity.ai.attributes.Attributes.ENTITY_INTERACTION_RANGE))
            b.add(attribute,new net.minecraft.world.entity.ai.attributes.AttributeModifier(net.minecraft.resources.ResourceLocation.parse("omnira:industrial_reach"),2,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE),net.minecraft.world.entity.EquipmentSlotGroup.HAND);
        return b.build();
    }
    public static boolean available(){return net.neoforged.fml.ModList.get().isLoaded("create");}
    public static boolean active(Player p){return ACTIVE.containsKey(p.getUUID());}
    public static double rotation(ItemStack stack,double time){
        double phase=stack.getOrDefault(ModDataComponents.WHEEL_PHASE,0D);
        if(stack.has(ModDataComponents.WHEEL_SPIN_START))phase+=Math.max(0,time-stack.get(ModDataComponents.WHEEL_SPIN_START))*1.5;
        return phase%360;
    }
    public static void stop(Player p){
        var session=ACTIVE.remove(p.getUUID());if(session==null)return;
        session.staff.set(ModDataComponents.WHEEL_PHASE,rotation(session.staff,p.level().getGameTime()));
        session.staff.remove(ModDataComponents.WHEEL_SPIN_START);
        // Inventory replacement may already have copied the transient visual component.
        for(var held:List.of(p.getMainHandItem(),p.getOffhandItem()))if(held.getItem() instanceof IndustrialWheelItem&&held!=session.staff){
            held.set(ModDataComponents.WHEEL_PHASE,rotation(held,p.level().getGameTime()));held.remove(ModDataComponents.WHEEL_SPIN_START);
        }
    }
    public static StaffCastContext context(Player p){return new StaffCastContext(STATS,CastAttributes.power(p,.2),CastAttributes.reduction(p,10),CastAttributes.cooldownReduction(p,0),List.of());}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player p,InteractionHand hand){
        if(p instanceof ServerPlayer server)toggle(server,hand);
        return InteractionResultHolder.success(p.getItemInHand(hand));
    }
    public static void toggle(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);
        if(!available()||!(stack.getItem() instanceof IndustrialWheelItem)||!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu)return;
        // Always allow stopping, even while the last spell is cooling down.
        if(p.isShiftKeyDown()){stop(p);CrystalGridItem.open(p,-2);return;}
        if(active(p)){stop(p);return;}
        if(p.getCooldowns().isOnCooldown(stack.getItem()))return;
        if(cast(p,stack)){
            stack.set(ModDataComponents.WHEEL_SPIN_START,p.level().getGameTime());
            ACTIVE.put(p.getUUID(),new Session(stack,CrystalGridMenu.locate(p,-2),hand,p.level().dimension()));
        }
    }
    private static boolean cast(ServerPlayer p,ItemStack stack){
        int special=StaffItem.nextSpellCooldown(p);
        var context=context(p);
        if(StaffItem.castSequenceContents(p,context,1)==0)return false;
        p.getCooldowns().addCooldown(stack.getItem(),Math.max(special,SpellCooldowns.ticks(40,context.cooldownReduction(),1)));
        int step=(stack.getOrDefault(ModDataComponents.WHEEL_STEP,0)+1)%16;
        stack.set(ModDataComponents.WHEEL_STEP,step);
        stack.set(ModDataComponents.WHEEL_CAST_AT,p.level().getGameTime());
        return true;
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;
        var s=ACTIVE.get(p.getUUID());if(s==null)return;
        if(!p.isAlive()||p.isSpectator()||p.getItemInHand(s.hand)!=s.staff||p.containerMenu!=p.inventoryMenu
                ||CrystalGridMenu.locate(p,-2)!=s.grid||!p.level().dimension().equals(s.dimension)){stop(p);return;}
        if(!p.getCooldowns().isOnCooldown(s.staff.getItem())&&!cast(p,s.staff))stop(p);
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){stop(e.getEntity());}
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){stop(e.getEntity());}
    @SubscribeEvent public static void respawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent e){stop(e.getEntity());}
    @SubscribeEvent public static void shutdown(net.neoforged.neoforge.event.server.ServerStoppedEvent e){ACTIVE.clear();}
    @Override public boolean shouldCauseReequipAnimation(ItemStack oldStack,ItemStack newStack,boolean slotChanged){return slotChanged||!oldStack.is(newStack.getItem());}
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag){
        lines.add(Component.translatable("tooltip.omnira.industrial_wheel.toggle").withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.omnira.industrial_wheel.edit").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}
