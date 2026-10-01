package com.mcmagic.omnira.mana;

import com.mcmagic.omnira.registry.ModItems;
import net.minecraft.world.item.ItemStack;

public enum Affinity {
    NONE(0xFFFFFF,0,0,0,0), LIGHT(0xFFE5A3,200,.2,0,0),
    DARK(0xB49CDE,0,0,.2,10), ELEMENTAL(0x9FE3D6,100,.1,.1,5), DREAM(0xAC9DEF,200,0,.3,10);

    public final int color;
    public final double capacity, regeneration, power, reduction;
    Affinity(int color,double capacity,double regeneration,double power,double reduction) {
        this.color=color;this.capacity=capacity;this.regeneration=regeneration;this.power=power;this.reduction=reduction;
    }
    public static Affinity byId(int id) {return id>=0 && id<values().length?values()[id]:NONE;}
    public static Affinity offering(ItemStack stack) {
        if(stack.is(ModItems.DREAM_SPELL_CORE.get()))return DREAM;
        if(stack.is(ModItems.LIGHT_MICROCORE.get()))return LIGHT;
        if(stack.is(ModItems.DARK_MICROCORE.get()))return DARK;
        return stack.is(ModItems.ANALYSIS_CRYSTAL.get())?ELEMENTAL:NONE;
    }
    public ItemStack offering() {
        return new ItemStack(switch(this) {
            case LIGHT -> ModItems.LIGHT_MICROCORE.get();
            case DARK -> ModItems.DARK_MICROCORE.get();
            case DREAM -> ModItems.DREAM_SPELL_CORE.get();
            default -> ModItems.ANALYSIS_CRYSTAL.get();
        });
    }
    public int penaltyDuration() {return this==DARK?1200:2400;}
    public String key() {return "mana_affinity.omnira."+(this==NONE?"unattuned":name().toLowerCase(java.util.Locale.ROOT));}
}
