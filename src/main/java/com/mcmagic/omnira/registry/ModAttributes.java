package com.mcmagic.omnira.registry;

import com.mcmagic.omnira.Omnira;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@EventBusSubscriber(modid=Omnira.MOD_ID)
public final class ModAttributes {
    public static final DeferredRegister<Attribute> TYPES=DeferredRegister.create(Registries.ATTRIBUTE,Omnira.MOD_ID);
    public static final DeferredHolder<Attribute,Attribute> MAX_MANA=attribute("max_mana",100,1,1000000);
    public static final DeferredHolder<Attribute,Attribute> MANA_REGEN=attribute("mana_regeneration",1,0,100);
    public static final DeferredHolder<Attribute,Attribute> COST_REDUCTION=attribute("mana_cost_reduction",0,0,1000000);
    public static final DeferredHolder<Attribute,Attribute> SPELL_POWER=attribute("spell_power",1,.01,100);
    public static final DeferredHolder<Attribute,Attribute> DAMAGE_DEALT=attribute("damage_dealt",1,0,100);
    public static final DeferredHolder<Attribute,Attribute> MELEE_DAMAGE=TYPES.register("melee_damage_bonus",()->new net.neoforged.neoforge.common.PercentageAttribute("attribute.omnira.melee_damage_bonus",0,-1,100).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> LIFESTEAL=TYPES.register("lifesteal",()->new net.neoforged.neoforge.common.PercentageAttribute("attribute.omnira.lifesteal",0,0,100).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> GLIDING_SPEED=attribute("gliding_speed",1,0,100);
    public static final DeferredHolder<Attribute,Attribute> DAMAGE_TAKEN=TYPES.register("damage_taken",()->
            new RangedAttribute("attribute.omnira.damage_taken",1,0,100).setSentiment(Attribute.Sentiment.NEGATIVE).setSyncable(true));
    public static final DeferredHolder<Attribute,Attribute> COOLDOWN_REDUCTION=TYPES.register("spell_cooldown_reduction",()->
            new net.neoforged.neoforge.common.PercentageAttribute("attribute.omnira.spell_cooldown_reduction",0,-10,com.mcmagic.omnira.spell.SpellCooldowns.MAX_REDUCTION).setSyncable(true));
    private static DeferredHolder<Attribute,Attribute> attribute(String name,double base,double min,double max) {
        return TYPES.register(name,()->new RangedAttribute("attribute.omnira."+name,base,min,max).setSyncable(true));
    }
    @SubscribeEvent public static void attributes(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER,MAX_MANA); event.add(EntityType.PLAYER,MANA_REGEN);
        event.add(EntityType.PLAYER,COST_REDUCTION); event.add(EntityType.PLAYER,SPELL_POWER);
        event.add(EntityType.PLAYER,COOLDOWN_REDUCTION);
        event.add(EntityType.PLAYER,DAMAGE_DEALT); event.add(EntityType.PLAYER,DAMAGE_TAKEN);
        event.add(EntityType.PLAYER,GLIDING_SPEED);
        event.add(EntityType.PLAYER,MELEE_DAMAGE); event.add(EntityType.PLAYER,LIFESTEAL);
    }
}
