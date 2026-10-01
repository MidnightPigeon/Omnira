package com.mcmagic.omnira.item;

import com.mcmagic.omnira.Omnira;
import com.mcmagic.omnira.registry.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class CrystalArmorEffects {
    private static final ResourceLocation SET=ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"crystal_armor_set");
    private CrystalArmorEffects() {}

    public enum SetBonus {
        NONE(0,0,0), PLATE(.2,.2,0), DRESS(.3,.1,-.3);
        public final double power,reduction,gravity;
        SetBonus(double power,double reduction,double gravity){this.power=power;this.reduction=reduction;this.gravity=gravity;}
    }

    public static SetBonus equipped(Player player){
        boolean plate=true,dress=true;
        for(var entry:ArmorContent.PLATE.entrySet()){
            var stack=player.getItemBySlot(entry.getKey().getSlot());
            plate &= stack.is(entry.getValue().get());
            dress &= stack.is(ArmorContent.CLOTH.get(entry.getKey()).get());
            if(!plate && !dress)return SetBonus.NONE;
        }
        return plate?SetBonus.PLATE:SetBonus.DRESS;
    }

    @SubscribeEvent public static void attributes(ItemAttributeModifierEvent event){
        if(!(event.getItemStack().getItem() instanceof CrystalArmorItem armor))return;
        var slot=armor.getType().getSlot();
        event.addModifier(ModAttributes.MAX_MANA,new AttributeModifier(
                ResourceLocation.fromNamespaceAndPath(Omnira.MOD_ID,"crystal_armor_mana/"+slot.getName()),
                50,AttributeModifier.Operation.ADD_VALUE),EquipmentSlotGroup.bySlot(slot));
    }

    @SubscribeEvent public static void tick(PlayerTickEvent.Post event){
        if(!event.getEntity().level().isClientSide)refresh(event.getEntity());
    }

    public static void refresh(Player player){
        var bonus=equipped(player);
        update(player,ModAttributes.SPELL_POWER,bonus.power,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        update(player,Attributes.GRAVITY,bonus.gravity,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }

    private static void update(Player player,Holder<Attribute> attribute,double amount,AttributeModifier.Operation operation){
        var instance=player.getAttribute(attribute);
        if(instance==null)return;
        var old=instance.getModifier(SET);
        if(old!=null && old.amount()==amount && old.operation()==operation)return;
        if(old!=null)instance.removeModifier(SET);
        if(amount!=0)instance.addTransientModifier(new AttributeModifier(SET,amount,operation));
    }

    @SubscribeEvent public static void melee(LivingIncomingDamageEvent event){
        if(event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getDirectEntity() instanceof Player player
                && !(player.getMainHandItem().getItem() instanceof RitualSwordItem)
                && !(player.getMainHandItem().getItem() instanceof InfusedGrimoireItem))
            event.setAmount(event.getAmount()*(float)(1+meleeBonus(player)));
    }

    public static double meleeBonus(Player player){return (equipped(player)==SetBonus.PLATE?.2:0)
            +(player.getData(ModAttachments.DREAM_AFFINITY)?.3:0)+player.getAttributeValue(ModAttributes.MELEE_DAMAGE);}

    @SubscribeEvent public static void reduce(LivingDamageEvent.Pre event){
        if(event.getEntity() instanceof Player player)
            event.setNewDamage(event.getNewDamage()*(float)(1-equipped(player).reduction));
    }
}
