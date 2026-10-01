package com.mcmagic.omnira.item;

import com.mcmagic.omnira.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/** One exclusive plugin slot, with compatibility for the original boolean component. */
public enum ArquebusPlugin implements StringRepresentable {
    NONE("none",2,4,2,1,.3,40),
    ANCESTOR_LAUNCHER("ancestor_launcher",2,4,2,1,.5,60),
    KINGS_NEW_CLOTHES("kings_new_clothes",1.5,3,1.5,2,.3,40);

    public static final Codec<ArquebusPlugin> CODEC=StringRepresentable.fromEnum(ArquebusPlugin::values);
    public final double costMultiplier,selectionMultiplier,speedMultiplier,powerBonus;
    public final int shots,cooldownTicks;
    private final String id;
    ArquebusPlugin(String id,double cost,double selection,double speed,int shots,double powerBonus,int cooldownTicks) {
        this.id=id;this.costMultiplier=cost;this.selectionMultiplier=selection;this.speedMultiplier=speed;this.shots=shots;
        this.powerBonus=powerBonus;this.cooldownTicks=cooldownTicks;
    }
    @Override public String getSerializedName() {return id;}
    public static ArquebusPlugin of(ItemStack gun) {
        var plugin=gun.get(ModDataComponents.ARQUEBUS_PLUGIN);
        if(plugin!=null && plugin!=NONE) return plugin;
        return gun.getOrDefault(ModDataComponents.ANCESTOR_LAUNCHER,false)?ANCESTOR_LAUNCHER:NONE;
    }
}
