package com.mcmagic.omnira.combat;

import com.mcmagic.omnira.registry.ModAttributes;
import com.mcmagic.omnira.item.InfusedGrimoireItem;
import com.mcmagic.omnira.weaver.FounderApprovalItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Native item attributes with one shared modifier per held benefit, including hand swaps. */
@EventBusSubscriber(modid="omnira")
public final class HeldCombatAttributes {
    public static final ResourceLocation FOUNDER_DAMAGE=ResourceLocation.parse("omnira:founder_damage"),
            FOUNDER_LEECH=ResourceLocation.parse("omnira:founder_leech"),GRIMOIRE_LEECH=ResourceLocation.parse("omnira:grimoire_leech");
    public static ItemAttributeModifiers founder(){return ItemAttributeModifiers.builder()
            .add(ModAttributes.DAMAGE_BONUS,new AttributeModifier(FOUNDER_DAMAGE,2,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.HAND)
            .add(ModAttributes.LIFESTEAL,new AttributeModifier(FOUNDER_LEECH,.25,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.HAND).build();}
    public static boolean corrupted(Player player){return player.getMainHandItem().getItem() instanceof InfusedGrimoireItem book&&book.kind()==InfusedGrimoireItem.Kind.CORRUPTED;}
    public static void refresh(Player player){
        boolean founder=FounderApprovalItem.held(player);
        set(player,ModAttributes.DAMAGE_BONUS,FOUNDER_DAMAGE,founder?2:0);
        set(player,ModAttributes.LIFESTEAL,FOUNDER_LEECH,founder?.25:0);
        set(player,ModAttributes.LIFESTEAL,GRIMOIRE_LEECH,corrupted(player)?.5:0);
    }
    private static void set(Player player,Holder<Attribute> type,ResourceLocation id,double amount){
        var attribute=player.getAttribute(type);if(attribute==null)return;
        var old=attribute.getModifier(id);if(old!=null&&old.amount()==amount&&old.operation()==AttributeModifier.Operation.ADD_VALUE)return;
        if(old!=null)attribute.removeModifier(id);if(amount!=0)attribute.addTransientModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    public static float flatBonus(Entity owner){
        if(owner instanceof Player player)refresh(player);
        var attribute=owner instanceof LivingEntity living?living.getAttribute(ModAttributes.DAMAGE_BONUS):null;
        return attribute==null?0:(float)attribute.getValue();
    }
    public static double lifeSteal(Player player,DamageSource source){
        refresh(player);double rate=player.getAttributeValue(ModAttributes.LIFESTEAL);
        boolean shot=source.getDirectEntity() instanceof com.mcmagic.omnira.spell.entity.SpellEntity spell&&spell.knowledgeLeech();
        // The corrupted grimoire retains its original melee/own-projectile scope and projectile snapshot.
        if(corrupted(player)&&!source.is(DamageTypes.PLAYER_ATTACK)&&!shot)rate-=.5;
        if(shot&&!corrupted(player))rate+=.5;
        return Math.max(0,rate);
    }
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){if(!event.getEntity().level().isClientSide)refresh(event.getEntity());}
    private HeldCombatAttributes(){}
}
