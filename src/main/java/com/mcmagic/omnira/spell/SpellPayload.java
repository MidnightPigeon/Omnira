package com.mcmagic.omnira.spell;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mcmagic.omnira.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;

/** Authored costs are stored before equipment discounts. Old crystals have an empty payload. */
public record SpellPayload(int baseCost,float damage,java.util.List<SpellEffect> effects,java.util.List<String> keywords) {
    public SpellPayload { effects=java.util.List.copyOf(effects); keywords=java.util.List.copyOf(keywords); }
    public SpellPayload(int baseCost,float damage,java.util.List<SpellEffect> effects) {this(baseCost,damage,effects,java.util.List.of());}
    public SpellPayload(int baseCost,float damage) {this(baseCost,damage,java.util.List.of());}
    public boolean hasHarm() {return damage>0 || effects.stream().anyMatch(e->e.utility()?(e.operation().equals("construction")?e.enhancement()>0:e.delay()==0):!e.healing());}
    public boolean hasHealing() {return effects.stream().anyMatch(SpellEffect::healing);}
    public static final SpellPayload EMPTY=new SpellPayload(componentCost(2),0);
    public static final Codec<SpellPayload> CODEC=RecordCodecBuilder.create(i->i.group(
            Codec.intRange(0,Integer.MAX_VALUE).fieldOf("base_cost").forGetter(SpellPayload::baseCost),
            Codec.floatRange(0,10000).optionalFieldOf("damage",0F).forGetter(SpellPayload::damage),
            SpellEffect.CODEC.listOf().optionalFieldOf("effects",java.util.List.of()).forGetter(SpellPayload::effects),
            Codec.STRING.listOf().optionalFieldOf("keywords",java.util.List.of()).forGetter(SpellPayload::keywords)
    ).apply(i,SpellPayload::new));
    public static int componentCost(int components) {
        if(components<0) throw new IllegalArgumentException("Negative component count");
        return Math.multiplyExact(components,10);
    }
    public static SpellPayload of(ItemStack stack) {
        SpellPayload stored=stack.getOrDefault(ModDataComponents.SPELL_PAYLOAD,EMPTY);
        if(!(stack.getItem() instanceof com.mcmagic.omnira.item.LowTierMagicCrystalItem))return stored;
        var uniqueEffects=stored.effects().stream().distinct().toList();
        var seen=new java.util.HashSet<String>();
        var uniqueKeywords=stored.keywords().stream().filter(key->!isElementKeyword(key) || seen.add(key)).toList();
        if(uniqueEffects.size()==stored.effects().size() && uniqueKeywords.size()==stored.keywords().size())return stored;
        return new SpellPayload(stored.baseCost(),stored.damage(),uniqueEffects,uniqueKeywords);
    }
    private static boolean isElementKeyword(String key) {
        return switch(key) {
            case "harm","healing","dark_breath","dissociation","construction" -> true;
            default -> false;
        };
    }
}
